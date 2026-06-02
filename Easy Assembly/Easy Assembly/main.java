import java.util.*;
import java.io.*;
import java.nio.file.*;

public class main {
    public static void main(String[] args) {
        EAInterpreter interpreter = new EAInterpreter();
        if (args.length > 0) {
            interpreter.executeFile(args[0]);
        } else {
            interpreter.executeFile("program.ea");
        }
    }
}

class EAInterpreter {
    private Map<String, Object> variables = new HashMap<>();
    private String[] allLines;
    private int instructionPointer = 0;
    private Map<String,Integer> functions = new HashMap<>();

    public void executeFile(String filename) {
        try {
            String content = new String(Files.readAllBytes(Paths.get(filename)));
            execute(content);
        } catch (IOException e) {
            System.err.println("Error: File not found - " + filename);
        }
    }

    public void execute(String program) {
        allLines = program.split("\\r?\\n");
        instructionPointer = 0;
        // first pass: index function definitions (header ends with ':', body terminated by a single ';' line)
        indexFunctions();

        while (instructionPointer < allLines.length) {
            String original = allLines[instructionPointer];
            String line = original.trim();
            int lineNo = instructionPointer + 1;
            instructionPointer++;

            if (line.isEmpty() || line.startsWith("//")) continue;

            // Blocks and special tokens that do not require trailing ';'
            String firstToken = tokenAt(line, 0).toUpperCase();
            boolean isBlockCommand = firstToken.equals("REPEAT") || firstToken.equals("ENDREPEAT")
                    || firstToken.equals("CHECK") || firstToken.equals("ENDCHECK")
                    || line.endsWith(":") || firstToken.equals("HALT") || firstToken.equals("MEMORY")
                    || functions.containsKey(firstToken.toLowerCase()) || firstToken.equals("CALL");

            boolean expectsTerminator = firstToken.equals("DECLARE") || firstToken.equals("ASSIGN")
                    || firstToken.equals("CALC") || firstToken.equals("DISPLAY")
                    || firstToken.equals("READ") || firstToken.equals("MEMORY");

            boolean hasTerminator = line.endsWith(";");
            if (expectsTerminator && !hasTerminator) {
                System.err.println("Error (line " + lineNo + "): missing ';' terminator");
                // continue processing but not removing anything
            }
            // if ends with ';' remove it for processing
            if (hasTerminator) line = line.substring(0, line.lastIndexOf(';')).trim();

            executeLine(line, lineNo);
        }
    }

    private void indexFunctions() {
        for (int i = 0; i < allLines.length; i++) {
            String ln = allLines[i].trim();
            if (ln.endsWith(":")) {
                String name = ln.substring(0, ln.length()-1).trim().toLowerCase();
                functions.put(name, i+1); // function body starts next line
            }
        }
    }

    private void executeLine(String line, int lineNo) {
        String[] parts = line.split("\\s+", 2);
        String command = parts[0];
        String args = parts.length > 1 ? parts[1] : "";

        switch (command.toUpperCase()) {
            case "DECLARE" -> declareVariable(args, lineNo);
            case "ASSIGN" -> assignVariable(args, lineNo);
            case "CALC" -> calculate(args, lineNo);
            case "DISPLAY" -> display(args, lineNo);
            case "READ" -> readInput(args, lineNo);
            case "REPEAT" -> repeat(args, lineNo);
            case "ENDREPEAT" -> { /* handled by repeat */ }
            case "CHECK" -> check(args, lineNo);
            case "ENDCHECK" -> { /* handled by check */ }
            case "JUMP" -> jump(args, lineNo);
            case "HALT" -> { System.out.println("[HALT] Program Terminated"); System.exit(0); }
            case "MEMORY" -> showMemory();
            default -> {
                // function header or call
                if (command.endsWith(":")) {
                    // function header - skip body (already indexed)
                    skipFunctionBody(lineNo);
                } else if (command.equalsIgnoreCase("CALL")) {
                    callFunction(args, lineNo);
                } else {
                    System.err.println("Error (line " + lineNo + "): Unknown command '" + command + "'");
                }
            }
        }
    }

    private void declareVariable(String args, int lineNo) {
        String[] parts = args.split("\\s+");
        if (parts.length >= 2) {
            String type = parts[0].toUpperCase();
            String varName = parts[1];
            Object defaultValue;
            switch (type) {
                case "INT" -> defaultValue = 0;
                case "STR" -> defaultValue = "";
                case "BOOL" -> defaultValue = false;
                case "DEC" -> defaultValue = 0.0;
                default -> {
                    System.err.println("Error (line " + lineNo + "): Unknown type '" + type + "'");
                    return;
                }
            }
            variables.put(varName, defaultValue);
        } else {
            System.err.println("Error (line " + lineNo + "): DECLARE expects 'TYPE name'");
        }
    }

    private void assignVariable(String args, int lineNo) {
        String[] parts = args.split("\\s+", 2);
        if (parts.length >= 2) {
            String varName = parts[0];
            String valueToken = parts[1].trim();
            Object val = parseValue(valueToken, lineNo);
            variables.put(varName, val);
        } else {
            System.err.println("Error (line " + lineNo + "): ASSIGN expects 'name value'");
        }
    }

    private void calculate(String args, int lineNo) {
        String[] parts = args.split("\\s+");
        if (parts.length >= 4) {
            String dest = parts[0];
            String operand1 = parts[1];
            String operator = parts[2];
            String operand2 = parts[3];

            double val1 = getNumericDoubleValue(operand1, lineNo);
            double val2 = getNumericDoubleValue(operand2, lineNo);
            double result = 0;

            switch (operator) {
                case "+" -> result = val1 + val2;
                case "-" -> result = val1 - val2;
                case "*" -> result = val1 * val2;
                case "/" -> result = (val2 != 0 ? val1 / val2 : 0);
                case "%" -> result = (val2 != 0 ? val1 % val2 : 0);
                default -> {
                    System.err.println("Error (line " + lineNo + "): Unknown operator '" + operator + "'");
                    return;
                }
            }
            // store as integer if both operands were integers and result is whole
            if (isWhole(result)) variables.put(dest, (int)result);
            else variables.put(dest, result);
        } else {
            System.err.println("Error (line " + lineNo + "): CALC expects 'dest op1 operator op2'");
        }
    }

    private void display(String args, int lineNo) {
        String token = args.trim();
        // support new print marker: no."text"  or legacy: "text"
        if (token.startsWith("no.")) {
            String after = token.substring(3).trim();
            if (after.startsWith("\"") && after.endsWith("\"") && after.length() >= 2) {
                System.out.println(after.substring(1, after.length() - 1));
            } else {
                System.err.println("Error (line " + lineNo + "): print marker 'no.' must be followed by quoted text");
            }
            return;
        }
        if (token.startsWith("\"") && token.endsWith("\"") && token.length() >= 2) {
            System.out.println(token.substring(1, token.length() - 1));
            return;
        }
        // variable or bracketed literals
        Object value = parseValue(token, lineNo);
        if (value == null) System.out.println("UNDEFINED");
        else System.out.println(value);
    }

    private void readInput(String args, int lineNo) {
        String varName = args.trim();
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter value for " + varName + ": ");
        String input = scanner.nextLine();
        Object val = parseValue(input, lineNo);
        variables.put(varName, val);
    }

    private void repeat(String args, int lineNo) {
        String[] parts = args.split("\\s+");
        if (parts.length >= 1) {
            int times = (int)getNumericDoubleValue(parts[0], lineNo);
            int loopStart = instructionPointer;
            int loopEnd = findEndRepeat(loopStart);
            for (int i = 0; i < times; i++) {
                int saved = instructionPointer;
                instructionPointer = loopStart;
                while (instructionPointer < loopEnd) {
                    String line = allLines[instructionPointer].trim();
                    instructionPointer++;
                    if (line.isEmpty() || line.startsWith("//")) continue;
                    // remove trailing ';' if present
                    if (line.endsWith(";")) line = line.substring(0, line.lastIndexOf(';')).trim();
                    executeLine(line, instructionPointer);
                }
                instructionPointer = saved;
            }
            instructionPointer = loopEnd + 1;
        } else {
            System.err.println("Error (line " + lineNo + "): REPEAT expects a count");
        }
    }

    private void check(String args, int lineNo) {
        String[] parts = args.split("\\s+");
        if (parts.length >= 3) {
            double val1 = getNumericDoubleValue(parts[0], lineNo);
            String operator = parts[1];
            double val2 = getNumericDoubleValue(parts[2], lineNo);

            boolean condition = switch (operator) {
                case "==" -> val1 == val2;
                case "!=" -> val1 != val2;
                case ">" -> val1 > val2;
                case "<" -> val1 < val2;
                case ">=" -> val1 >= val2;
                case "<=" -> val1 <= val2;
                default -> {
                    System.err.println("Error (line " + lineNo + "): Unknown operator '" + operator + "'");
                    yield false;
                }
            };

            int checkStart = instructionPointer;
            int checkEnd = findEndCheck(checkStart);
            if (condition) {
                int saved = instructionPointer;
                instructionPointer = checkStart;
                while (instructionPointer < checkEnd) {
                    String line = allLines[instructionPointer].trim();
                    instructionPointer++;
                    if (line.isEmpty() || line.startsWith("//")) continue;
                    if (line.endsWith(";")) line = line.substring(0, line.lastIndexOf(';')).trim();
                    executeLine(line, instructionPointer);
                }
                instructionPointer = checkEnd + 1;
            } else {
                instructionPointer = checkEnd + 1;
            }
        } else {
            System.err.println("Error (line " + lineNo + "): CHECK expects 'val1 op val2'");
        }
    }

    private void jump(String args, int lineNo) {
        try {
            int lineNum = Integer.parseInt(args.trim());
            if (lineNum >= 0 && lineNum < allLines.length) instructionPointer = lineNum;
            else System.err.println("Error (line " + lineNo + "): JUMP out of range: " + lineNum);
        } catch (NumberFormatException e) {
            System.err.println("Error (line " + lineNo + "): Invalid JUMP target: " + args);
        }
    }

    private void showMemory() {
        System.out.println("[MEMORY] Variables:");
        for (Map.Entry<String, Object> e : variables.entrySet()) {
            System.out.println("  " + e.getKey() + " = " + e.getValue());
        }
    }

    private int findEndRepeat(int start) {
        for (int i = start; i < allLines.length; i++) {
            if (allLines[i].trim().equalsIgnoreCase("ENDREPEAT")) return i;
        }
        return allLines.length - 1;
    }

    private int findEndCheck(int start) {
        for (int i = start; i < allLines.length; i++) {
            if (allLines[i].trim().equalsIgnoreCase("ENDCHECK")) return i;
        }
        return allLines.length - 1;
    }

    private void skipFunctionBody(int lineNo) {
        // when encountering a function header we skip until a single-line ";" end marker
        for (int i = instructionPointer; i < allLines.length; i++) {
            if (allLines[i].trim().equals(";")) {
                instructionPointer = i + 1;
                return;
            }
        }
    }

    private void callFunction(String args, int lineNo) {
        String fname = args.trim().toLowerCase();
        if (!functions.containsKey(fname)) {
            System.err.println("Error (line " + lineNo + "): function '" + fname + "' not defined");
            return;
        }
        int start = functions.get(fname);
        int saved = instructionPointer;
        instructionPointer = start;
        // run until single-line ';'
        while (instructionPointer < allLines.length) {
            String line = allLines[instructionPointer].trim();
            instructionPointer++;
            if (line.equals(";")) break;
            if (line.isEmpty() || line.startsWith("//")) continue;
            if (line.endsWith(";")) line = line.substring(0, line.lastIndexOf(';')).trim();
            executeLine(line, instructionPointer);
        }
        instructionPointer = saved;
    }

    private Object parseValue(String raw, int lineNo) {
        String value = raw.trim();
        // integer literal {123}
        if (value.startsWith("{")) {
            if (!value.endsWith("}")) {
                System.err.println("Error (line " + lineNo + "): missing '}' for integer literal");
                value = value.substring(1); // try to continue
            } else {
                String inside = value.substring(1, value.length()-1).trim();
                if (isIntegerString(inside)) return Integer.parseInt(inside);
                System.err.println("Error (line " + lineNo + "): invalid integer literal {" + inside + "}");
                return 0;
            }
        }
        // decimal literal (12.34)
        if (value.startsWith("(")) {
            if (!value.endsWith(")")) {
                System.err.println("Error (line " + lineNo + "): missing ')' for decimal literal");
                value = value.substring(1);
            } else {
                String inside = value.substring(1, value.length()-1).trim();
                try { return Double.parseDouble(inside); }
                catch (NumberFormatException e) {
                    System.err.println("Error (line " + lineNo + "): invalid decimal literal (" + inside + ")");
                    return 0.0;
                }
            }
        }
        // char literal [c]
        if (value.startsWith("[")) {
            if (!value.endsWith("]")) {
                System.err.println("Error (line " + lineNo + "): missing ']' for char literal");
                value = value.substring(1);
            } else {
                String inside = value.substring(1, value.length()-1);
                if (inside.length() == 1) return inside.charAt(0);
                System.err.println("Error (line " + lineNo + "): char literal must be single character");
                return '\0';
            }
        }
        // quoted string
        if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) {
            return value.substring(1, value.length()-1);
        }
        // variable reference
        if (variables.containsKey(value)) return variables.get(value);
        // fallback numeric
        if (isIntegerString(value)) return Integer.parseInt(value);
        try {
            double d = Double.parseDouble(value);
            return d;
        } catch (Exception ignored) {}
        // unknown token -> return as raw string
        return value;
    }

    private double getNumericDoubleValue(String token, int lineNo) {
        Object v = parseValue(token, lineNo);
        if (v instanceof Integer) return ((Integer)v).doubleValue();
        if (v instanceof Double) return (Double)v;
        if (v instanceof String) {
            String s = (String)v;
            if (isIntegerString(s)) return Double.parseDouble(s);
            try { return Double.parseDouble(s); } catch (NumberFormatException ignored) {}
        }
        return 0;
    }

    private boolean isIntegerString(String s) {
        if (s == null || s.isEmpty()) return false;
        try { Integer.parseInt(s); return true; } catch (NumberFormatException e) { return false; }
    }

    private boolean isWhole(double d) {
        return Math.abs(d - Math.round(d)) < 1e-9;
    }

    private String tokenAt(String line, int index) {
        String[] t = line.split("\\s+");
        if (index < t.length) return t[index];
        return "";
    }
}