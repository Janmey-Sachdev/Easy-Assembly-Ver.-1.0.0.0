#!/usr/bin/env node
const fs = require('fs');
const path = require('path');
const os = require('os');
const { execSync } = require('child_process');

const root = process.cwd();
const registryDir = path.join(root, 'registry');
const installDir = path.join(root, 'esa_modules');

function ensureDir(dir) {
  if (!fs.existsSync(dir)) fs.mkdirSync(dir, { recursive: true });
}

function readPackageMetadata(pkgDir) {
  const pkgPath = path.join(pkgDir, 'package.json');
  if (!fs.existsSync(pkgPath)) return null;
  try {
    return JSON.parse(fs.readFileSync(pkgPath, 'utf8'));
  } catch {
    return null;
  }
}

function semverCompare(a, b) {
  const pa = a.split('.').map(Number);
  const pb = b.split('.').map(Number);
  for (let i = 0; i < Math.max(pa.length, pb.length); i++) {
    const na = pa[i] || 0;
    const nb = pb[i] || 0;
    if (na !== nb) return na - nb;
  }
  return 0;
}

function getPackageVersions(name) {
  const dir = path.join(registryDir, name);
  if (!fs.existsSync(dir) || !fs.statSync(dir).isDirectory()) return [];
  return fs.readdirSync(dir)
    .filter(v => !v.startsWith('.') && /^\d+\.\d+\.\d+$/.test(v) && fs.statSync(path.join(dir, v)).isDirectory())
    .sort((a, b) => semverCompare(a, b));
}

function listRegistry() {
  if (!fs.existsSync(registryDir)) { console.log('No registry found.'); return; }
  const items = fs.readdirSync(registryDir).filter(i => !i.startsWith('.'));
  if (items.length === 0) { console.log('Registry is empty.'); return; }
  console.log('Available packages:');
  items.forEach((item) => {
    const versions = getPackageVersions(item);
    if (versions.length > 0) {
      console.log('  ' + item + ': ' + versions.join(', '));
    } else {
      const meta = readPackageMetadata(path.join(registryDir, item));
      const label = meta && meta.version ? `${item}@${meta.version}` : item;
      console.log('  ' + label);
    }
  });
}

function listInstalled() {
  ensureDir(installDir);
  const items = fs.readdirSync(installDir).filter(i => !i.startsWith('.'));
  if (items.length === 0) { console.log('No packages installed.'); return; }
  console.log('Installed packages:');
  items.forEach((item) => {
    const meta = readPackageMetadata(path.join(installDir, item));
    const label = meta && meta.version ? `${item}@${meta.version}` : item;
    console.log('  ' + label);
  });
}

function copyRecursive(src, dest) {
  const stat = fs.statSync(src);
  if (stat.isDirectory()) {
    ensureDir(dest);
    for (const name of fs.readdirSync(src)) {
      copyRecursive(path.join(src, name), path.join(dest, name));
    }
  } else {
    ensureDir(path.dirname(dest));
    fs.copyFileSync(src, dest);
  }
}

function isGitSource(spec) {
  return /^(?:git\+ssh|git\+https|git|https?|ssh):\/\//i.test(spec)
    || /^(?:git@|[^@\s]+:[^\s]+\.git)$/i.test(spec)
    || spec.endsWith('.git');
}

function parseInstallSpec(spec) {
  if (isGitSource(spec)) {
    const [url, ref] = spec.split('#');
    return { type: 'git', url, ref };
  }
  if (spec.startsWith('http://') || spec.startsWith('https://')) {
    return { type: 'remote', url: spec };
  }
  const atIndex = spec.lastIndexOf('@');
  if (atIndex > 0) {
    const name = spec.slice(0, atIndex);
    const version = spec.slice(atIndex + 1);
    return { type: 'registry', name, version };
  }
  return { type: 'registry', name: spec };
}

function cloneGitPackage(url, ref) {
  const tmpDir = path.join(os.tmpdir(), `ea-install-${Date.now()}`);
  fs.mkdirSync(tmpDir, { recursive: true });
  try {
    execSync(`git clone ${url} .`, { cwd: tmpDir, stdio: 'ignore' });
    if (ref) {
      execSync(`git checkout ${ref}`, { cwd: tmpDir, stdio: 'ignore' });
    }
  } catch (error) {
    console.error('Git install failed:', error.message);
    return null;
  }
  const gitDir = path.join(tmpDir, '.git');
  if (fs.existsSync(gitDir)) {
    fs.rmSync(gitDir, { recursive: true, force: true });
  }
  return tmpDir;
}

function resolveRegistryPackage(name, version) {
  const packageRoot = path.join(registryDir, name);
  if (fs.existsSync(packageRoot) && fs.statSync(packageRoot).isDirectory()) {
    const versions = getPackageVersions(name);
    if (versions.length > 0) {
      if (!version || version === 'latest') {
        return path.join(packageRoot, versions[versions.length - 1]);
      }
      if (versions.includes(version)) return path.join(packageRoot, version);
      return null;
    }
    const meta = readPackageMetadata(packageRoot);
    if (!version || version === 'latest' || (meta && meta.version === version)) return packageRoot;
  }
  if (!fs.existsSync(registryDir)) return null;
  const candidates = fs.readdirSync(registryDir).filter(i => !i.startsWith('.'));
  for (const candidate of candidates) {
    const candidateDir = path.join(registryDir, candidate);
    const meta = readPackageMetadata(candidateDir);
    if (candidate === name || (meta && meta.name === name)) {
      if (!version || version === 'latest' || (meta && meta.version === version)) {
        return candidateDir;
      }
    }
  }
  return null;
}

function installFromRegistry(name, version) {
  ensureDir(installDir);
  const src = resolveRegistryPackage(name, version);
  if (!src) {
    console.error('Package not found in registry:', version ? `${name}@${version}` : name);
    process.exitCode = 1;
    return;
  }
  const meta = readPackageMetadata(src) || { name };
  const destName = meta.name || name;
  const dest = path.join(installDir, destName);
  if (fs.existsSync(dest)) { console.log('Package already installed:', destName); return; }
  copyRecursive(src, dest);
  console.log('Installed', destName);
}

function installFromGit(url, ref) {
  ensureDir(installDir);
  const tmp = cloneGitPackage(url, ref);
  if (!tmp) { process.exitCode = 1; return; }
  const meta = readPackageMetadata(tmp); 
  const destName = meta?.name || path.basename(url).replace(/\.git$/, '').replace(/.*\//, '');
  const dest = path.join(installDir, destName);
  if (fs.existsSync(dest)) { console.log('Package already installed:', destName); return; }
  copyRecursive(tmp, dest);
  console.log(`Installed ${destName} from git ${url}${ref ? '@' + ref : ''}`);
}

function installPackage(pkg) {
  const spec = parseInstallSpec(pkg);
  if (spec.type === 'git') {
    installFromGit(spec.url, spec.ref);
  } else {
    installFromRegistry(spec.name, spec.version);
  }
}

function uninstallPackage(pkg) {
  const dest = path.join(installDir, pkg);
  if (!fs.existsSync(dest)) { console.error('Package not installed:', pkg); process.exitCode = 1; return; }
  fs.rmSync(dest, { recursive: true, force: true });
  console.log('Uninstalled', pkg);
}

function updatePackage(pkg) {
  const dest = path.join(installDir, pkg);
  if (!fs.existsSync(dest)) { console.error('Package not installed:', pkg); process.exitCode = 1; return; }
  const meta = readPackageMetadata(dest) || { name: pkg };
  const pkgName = meta.name || pkg;
  const versions = getPackageVersions(pkgName);
  if (versions.length === 0) { console.log('No versioned updates available for', pkg); return; }
  const currentMeta = readPackageMetadata(dest);
  const currentVersion = currentMeta?.version || '0.0.0';
  let targetVersion = null;
  for (const v of versions) {
    if (semverCompare(v, currentVersion) > 0) targetVersion = v;
  }
  if (!targetVersion) { console.log('Already on latest version:', currentVersion); return; }
  const src = path.join(registryDir, pkgName, targetVersion);
  fs.rmSync(dest, { recursive: true, force: true });
  copyRecursive(src, dest);
  console.log(`Updated ${pkg} from ${currentVersion} to ${targetVersion}`);
}

function downgradePackage(pkg, toVersion) {
  const dest = path.join(installDir, pkg);
  if (!fs.existsSync(dest)) { console.error('Package not installed:', pkg); process.exitCode = 1; return; }
  const meta = readPackageMetadata(dest) || { name: pkg };
  const pkgName = meta.name || pkg;
  const versions = getPackageVersions(pkgName);
  if (!toVersion) {
    if (versions.length < 2) { console.log('No earlier versions available'); return; }
    toVersion = versions[versions.length - 2];
  } else if (!versions.includes(toVersion)) {
    console.error('Version not found:', toVersion);
    process.exitCode = 1;
    return;
  }
  const src = path.join(registryDir, pkgName, toVersion);
  fs.rmSync(dest, { recursive: true, force: true });
  copyRecursive(src, dest);
  console.log(`Downgraded ${pkg} to ${toVersion}`);
}

function getDefaultPackageEntry(pkgDir) {
  const meta = readPackageMetadata(pkgDir);
  if (meta && meta.main) {
    const mainPath = path.join(pkgDir, meta.main);
    if (fs.existsSync(mainPath)) return mainPath;
  }
  const candidates = ['main.ea', 'index.ea', 'package.ea', 'hello.ea'];
  for (const name of candidates) {
    const full = path.join(pkgDir, name);
    if (fs.existsSync(full)) return full;
  }
  const allEa = fs.readdirSync(pkgDir).find(x => x.endsWith('.ea'));
  return allEa ? path.join(pkgDir, allEa) : null;
}

function evalExpression(expr, vars) {
  let replaced = expr;
  const funcNames = ['ABS', 'SQRT', 'MIN', 'MAX', 'ROUND', 'FLOOR', 'CEIL', 'POW', 'LEN', 'SUBSTR', 'UPPER', 'LOWER', 'TRIM', 'RANDOM'];
  for (const name of funcNames) {
    replaced = replaced.replace(new RegExp(name + '\\(', 'g'), `__${name}(`);
  }
  
  // Create variable declarations for arrays and mark them
  const arrayNames = new Set(Object.keys(vars).filter(k => Array.isArray(vars[k])));
  const varDecls = Array.from(arrayNames)
    .map(name => `const ${name} = ${JSON.stringify(vars[name])};`)
    .join('\n');
  
  // Replace variables, but keep array names as references
  replaced = replaced.replace(/([a-zA-Z_][a-zA-Z0-9_]*)/g, (m) => {
    if (m.startsWith('__') || arrayNames.has(m)) return m; // Keep as-is
    return JSON.stringify(vars[m] ?? 0); // Quote other variables
  });
  
  try {
    const code = `
      const __ABS = Math.abs;
      const __SQRT = Math.sqrt;
      const __MIN = Math.min;
      const __MAX = Math.max;
      const __ROUND = Math.round;
      const __FLOOR = Math.floor;
      const __CEIL = Math.ceil;
      const __POW = Math.pow;
      const __LEN = (s) => String(s).length;
      const __SUBSTR = (s, start, len) => String(s).substring(start, start + len);
      const __UPPER = (s) => String(s).toUpperCase();
      const __LOWER = (s) => String(s).toLowerCase();
      const __TRIM = (s) => String(s).trim();
      const __RANDOM = (min, max) => {
        const lo = Number(min) || 0;
        const hi = Number(max) || 0;
        return Math.floor(Math.random() * (hi - lo + 1)) + lo;
      };
      ${varDecls}
      return (${replaced});
    `;
    return Function(code)();
  } catch (e) {
    throw new Error('Expression error: ' + expr + ' -> ' + e.message);
  }
}

function evalCondition(condition, vars) {
  try {
    return Boolean(evalExpression(condition, vars));
  } catch (e) {
    console.error('CHECK error:', e.message);
    return false;
  }
}

function findMatchingEnd(lines, startIndex, startToken, endToken) {
  let depth = 1;
  for (let i = startIndex; i < lines.length; i++) {
    const trimmed = lines[i].trim();
    if (trimmed.startsWith('#') || trimmed.startsWith('//')) continue;
    const instr = trimmed.split(/\s+/)[0].toUpperCase();
    if (instr === startToken) depth++;
    if (instr === endToken) {
      depth--;
      if (depth === 0) return i;
    }
  }
  return -1;
}

function runLines(lines, vars) {
  let i = 0;
  let breakFlag = false, continueFlag = false;
  while (i < lines.length) {
    if (breakFlag || continueFlag) break;
    const raw = lines[i];
    let line = raw.trim();
    if (!line || line.startsWith('#') || line.startsWith('//')) { i++; continue; }
    if (line.includes('#')) line = line.substring(0, line.indexOf('#')).trim();
    if (line.includes('//')) line = line.substring(0, line.indexOf('//')).trim();
    if (!line) { i++; continue; }
    const parts = line.split(/\s+/);
    const instr = parts[0].toUpperCase();
    switch (instr) {
      case 'DECLARE':
        vars[parts[2] || parts[1]] = 0;
        i++;
        break;
      case 'ASSIGN':
        if (parts[1].includes('[')) {
          const match = parts[1].match(/^([a-zA-Z_][a-zA-Z0-9_]*)\[(.+)\]$/);
          if (match) {
            const arrName = match[1];
            const indexExpr = match[2];
            const value = isNaN(parts[2]) ? parts.slice(2).join(' ') : Number(parts[2]);
            if (Array.isArray(vars[arrName])) {
              try {
                const index = evalExpression(indexExpr, vars);
                vars[arrName][index] = value;
              } catch {
                // Index evaluation failed, skip
              }
            }
          }
        } else {
          vars[parts[1]] = isNaN(parts[2]) ? parts.slice(2).join(' ') : Number(parts[2]);
        }
        i++;
        break;
      case 'DISPLAY': {
        const rest = line.slice(7).trim();
        if (rest.startsWith('"') || rest.startsWith("'")) {
          console.log(rest.replace(/^['\"]|['\"]$/g, ''));
        } else if (rest.includes('[')) {
          const match = rest.match(/^([a-zA-Z_][a-zA-Z0-9_]*)\[(.+)\]$/);
          if (match) {
            const arrName = match[1];
            const indexExpr = match[2];
            if (Array.isArray(vars[arrName])) {
              try {
                const index = evalExpression(indexExpr, vars);
                console.log(vars[arrName][index]);
              } catch {
                console.log(vars[rest] ?? rest);
              }
            } else {
              console.log(vars[rest] ?? rest);
            }
          } else {
            console.log(vars[rest] ?? rest);
          }
        } else {
          console.log(vars[rest] ?? rest);
        }
        i++;
        break;
      }
      case 'CALC': {
        const target = parts[1];
        const expr = parts.slice(2).join(' ');
        try {
          const result = evalExpression(expr, vars);
          if (target.includes('[')) {
            const match = target.match(/^([a-zA-Z_][a-zA-Z0-9_]*)\[(.+)\]$/);
            if (match) {
              const arrName = match[1];
              const indexExpr = match[2];
              if (Array.isArray(vars[arrName])) {
                try {
                  const index = evalExpression(indexExpr, vars);
                  vars[arrName][index] = result;
                } catch {
                  vars[target] = result;
                }
              }
            }
          } else {
            vars[target] = result;
          }
        } catch (e) {
          console.error(e.message);
        }
        i++;
        break;
      }
      case 'READ':
        vars[parts[1]] = 0;
        i++;
        break;
      case 'CHECK': {
        const condition = parts.slice(1).join(' ');
        const endIndex = findMatchingEnd(lines, i + 1, 'CHECK', 'ENDCHECK');
        if (endIndex < 0) { console.error('Missing ENDCHECK'); return; }
        
        let elseIndex = -1;
        for (let j = i + 1; j < endIndex; j++) {
          const trimmed = lines[j].trim();
          if (trimmed.startsWith('#') || trimmed.startsWith('//')) continue;
          const instr = trimmed.split(/\s+/)[0].toUpperCase();
          if (instr === 'ELSE' || instr === 'ELSEIF') {
            elseIndex = j;
            break;
          }
        }
        
        if (evalCondition(condition, vars)) {
          runLines(lines.slice(i + 1, elseIndex >= 0 ? elseIndex : endIndex), vars);
        } else if (elseIndex >= 0) {
          const elseInstr = lines[elseIndex].trim().split(/\s+/)[0].toUpperCase();
          if (elseInstr === 'ELSE') {
            runLines(lines.slice(elseIndex + 1, endIndex), vars);
          } else if (elseInstr === 'ELSEIF') {
            const elseIfCondition = lines[elseIndex].trim().slice(6).trim();
            if (evalCondition(elseIfCondition, vars)) {
              runLines(lines.slice(elseIndex + 1, endIndex), vars);
            }
          }
        }
        i = endIndex + 1;
        break;
      }
      case 'REPEAT': {
        let count = Number(parts[1]);
        if (Number.isNaN(count)) count = Number(vars[parts[1]] ?? 0);
        const endIndex = findMatchingEnd(lines, i + 1, 'REPEAT', 'ENDREPEAT');
        if (endIndex < 0) { console.error('Missing ENDREPEAT'); return; }
        const block = lines.slice(i + 1, endIndex);
        for (let n = 0; n < count; n++) {
          runLines(block, vars);
        }
        i = endIndex + 1;
        break;
      }
      case 'MEMORY':
        i++;
        break;
      case 'ARRAY': {
        const arrName = parts[1];
        const size = Number(parts[2]) || 10;
        vars[arrName] = new Array(size).fill(0);
        i++;
        break;
      }
      case 'PUSH': {
        const arrName = parts[1];
        const valueExpr = parts.slice(2).join(' ');
        if (!Array.isArray(vars[arrName])) vars[arrName] = [];
        try {
          const value = evalExpression(valueExpr, vars);
          vars[arrName].push(value);
        } catch (e) {
          console.error('PUSH error:', e.message);
        }
        i++;
        break;
      }
      case 'POP': {
        const arrName = parts[1];
        const target = parts[2];
        if (!Array.isArray(vars[arrName])) { i++; break; }
        const value = vars[arrName].pop();
        if (!target) { i++; break; }
        if (target.includes('[')) {
          const match = target.match(/^([a-zA-Z_][a-zA-Z0-9_]*)\[(.+)\]$/);
          if (match && Array.isArray(vars[match[1]])) {
            try {
              const index = evalExpression(match[2], vars);
              vars[match[1]][index] = value;
            } catch {
              // ignore invalid index
            }
          }
        } else {
          vars[target] = value;
        }
        i++;
        break;
      }
      case 'LOOP': {
        const condition = parts.slice(1).join(' ');
        const endIndex = findMatchingEnd(lines, i + 1, 'LOOP', 'ENDLOOP');
        if (endIndex < 0) { console.error('Missing ENDLOOP'); return; }
        const block = lines.slice(i + 1, endIndex);
        while (evalCondition(condition, vars)) {
          runLines(block, vars);
          if (breakFlag) { breakFlag = false; break; }
          if (continueFlag) { continueFlag = false; continue; }
        }
        i = endIndex + 1;
        break;
      }
      case 'FOR': {
        const varName = parts[1];
        const start = Number(parts[2]);
        const end = Number(parts[3]);
        const step = Number(parts[4]) || 1;
        const endIndex = findMatchingEnd(lines, i + 1, 'FOR', 'ENDFOR');
        if (endIndex < 0) { console.error('Missing ENDFOR'); return; }
        const block = lines.slice(i + 1, endIndex);
        for (let f = start; f <= end; f += step) {
          vars[varName] = f;
          runLines(block, vars);
          if (breakFlag) { breakFlag = false; break; }
          if (continueFlag) { continueFlag = false; continue; }
        }
        i = endIndex + 1;
        break;
      }
      case 'BREAK':
        breakFlag = true;
        i++;
        break;
      case 'CONTINUE':
        continueFlag = true;
        i++;
        break;
      case 'INPUT': {
        const varName = parts[1];
        vars[varName] = '';
        i++;
        break;
      }
      case 'HALT':
        return;
      default:
        i++;
        break;
    }
  }
}

function runEaFile(filePath) {
  if (!fs.existsSync(filePath)) { console.error('File not found:', filePath); process.exitCode = 1; return; }
  const lines = fs.readFileSync(filePath, 'utf8').split(/\r?\n/);
  const vars = {};
  runLines(lines, vars);
}

function runTarget(target) {
  const pathTarget = path.resolve(target);
  if (fs.existsSync(pathTarget) && fs.statSync(pathTarget).isFile()) {
    return runEaFile(pathTarget);
  }

  const installedPkg = path.join(installDir, target);
  if (fs.existsSync(installedPkg) && fs.statSync(installedPkg).isDirectory()) {
    const entry = getDefaultPackageEntry(installedPkg);
    if (!entry) { console.error('No .ea entry found in package:', target); process.exitCode = 1; return; }
    return runEaFile(entry);
  }

  console.error('Cannot find file or installed package:', target);
  process.exitCode = 1;
}

function showHelp() {
  console.log('ea - Easy Assembly installer, manager, and runner');
  console.log('Usage:');
  console.log('  ea list                        # list registry packages');
  console.log('  ea installed                   # list installed packages');
  console.log('  ea install <pkg>               # install latest from registry');
  console.log('  ea install <pkg>@<version>     # install specific version');
  console.log('  ea install <pkg>@latest        # explicitly install latest');
  console.log('  ea install <git-url>           # install package from git');
  console.log('  ea uninstall <pkg>             # remove installed package');
  console.log('  ea update <pkg>                # upgrade to newer version');
  console.log('  ea downgrade <pkg> [<version>] # downgrade package');
  console.log('  ea run <file.ea|pkg>           # run an EA file or installed package');
  console.log('  ea <file.ea>                   # run an EA file directly');
}

const argv = process.argv.slice(2);
const cmd = argv[0];
const arg = argv[1];
const arg2 = argv[2];

if (!cmd) {
  showHelp();
  process.exit(0);
}

switch (cmd) {
  case 'list':
    listRegistry();
    break;
  case 'installed':
    listInstalled();
    break;
  case 'install':
    if (!arg) { console.error('Usage: ea install <package>'); process.exitCode = 1; break; }
    installPackage(arg);
    break;
  case 'uninstall':
    if (!arg) { console.error('Usage: ea uninstall <package>'); process.exitCode = 1; break; }
    uninstallPackage(arg);
    break;
  case 'update':
    if (!arg) { console.error('Usage: ea update <package>'); process.exitCode = 1; break; }
    updatePackage(arg);
    break;
  case 'downgrade':
    if (!arg) { console.error('Usage: ea downgrade <package> [<version>]'); process.exitCode = 1; break; }
    downgradePackage(arg, arg2);
    break;
  case 'run':
    if (!arg) { console.error('Usage: ea run <file.ea|pkg>'); process.exitCode = 1; break; }
    runTarget(arg);
    break;
  default:
    runTarget(cmd);
}
