import java.io.*;

public class JavaToEaConverter {
    public static void main(String[] args) throws IOException {
        if (args.length == 0) { System.err.println("Usage: JavaToEaConverter <file.java>"); return; }
        File in = new File(args[0]);
        File out = new File("converted_from_java.ea");
        try (BufferedReader br = new BufferedReader(new FileReader(in)); FileWriter fw = new FileWriter(out)) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("if (") && line.endsWith(") {")) {
                    String condition = line.substring(4, line.length() - 3).trim();
                    fw.write("CHECK " + condition + "\n");
                } else if (line.startsWith("}") || line.startsWith("else")) {
                    fw.write("ENDCHECK\n");
                } else if (line.contains("System.out.println(")) {
                    int s = line.indexOf('(');
                    int e = line.lastIndexOf(')');
                    String inner = line.substring(s+1, e).trim();
                    if ((inner.startsWith("\"") && inner.endsWith("\"")) || (inner.startsWith("'") && inner.endsWith("'"))) {
                        inner = inner.substring(1, inner.length()-1);
                    }
                    fw.write("DISPLAY \"" + inner + "\"\n");
                } else if (line.contains("=") && !line.startsWith("//")) {
                    String[] parts = line.split("=", 2);
                    String var = parts[0].trim();
                    String val = parts[1].trim();
                    if (val.endsWith(";")) val = val.substring(0, val.length()-1);
                    fw.write("ASSIGN " + var + " " + val + "\n");
                }
            }
            fw.write("HALT\n");
        }
        System.out.println("Wrote converted_from_java.ea (v2.0.0)");
    }
}
