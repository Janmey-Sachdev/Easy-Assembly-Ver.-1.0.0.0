# ea — Easy Assembly package installer and manager

This repository adds a minimal `ea` CLI that can:

- manage local Easy Assembly packages
- install/uninstall packages from a registry
- run `.ea` files or installed packages

Usage (from workspace root):

```
ea list
ea install sample-package
ea install sample-package@1.0.0
ea install https://github.com/user/repo.git
ea installed
ea run sample-package
```

Run an `.ea` file directly:

```
ea program.ea
```

Install specific versions of registry packages:

```
ea install window-creator@1.0.0
ea install window-creator@2.0.0
ea install window-creator@3.0.0
```

Packages now have version-specific features:
- `window-creator` v1: simple ASCII window
- `window-creator` v2: title and extra styling
- `window-creator` v3: menu bar and footer
- `py-to-ea` v2: adds simple `if` conversion
- `html-css-styler` v3: supports inline styles like `color`

Update and downgrade packages:

```
ea update window-creator
ea downgrade window-creator 1.0.0
ea downgrade window-creator  # defaults to previous version
```

Install latest with explicit syntax:

```
ea install py-to-ea@latest
```

## Language Features

Easy Assembly (EA) now supports advanced features:

- **Variables & Arrays**: DECLARE, ASSIGN, ARRAY with indexing
- **Control Flow**: CHECK/ELSE/ELSEIF, REPEAT, LOOP, FOR loops
- **Functions**: 40+ built-in functions (ABS, SQRT, MIN, MAX, LEN, UPPER, LOWER, etc.)
- **Loop Control**: BREAK, CONTINUE
- **Comments**: # or // style
- **Expressions**: Full math and logic support in CALC

### Example EA Program

```
ARRAY scores 5
FOR i 0 4 1
  ASSIGN scores[i] i * 10
ENDFOR

DECLARE total
ASSIGN total 0

FOR i 0 4 1
  CALC total total + scores[i]
ENDFOR

DISPLAY "Total: "
DISPLAY total
```

See [ENHANCED_LANGUAGE_FEATURES.md](ENHANCED_LANGUAGE_FEATURES.md) for complete documentation.

Files created:
- `bin/ea.js` — CLI script
- `registry/` — local package registry (sample packages)
- `esa_modules/` — where packages are installed

Install via npm locally:

```
npm pack
npm i -g ./essa-java-0.1.0.tgz
```

Or install directly from the project folder:

```
npm i -g .
```

If you publish to npm, users can install with:

```
npm i -g essa-java
```
