import java.io.*;

public class HtmlCssStyler {
    public static void main(String[] args) throws IOException {
        if (args.length == 0) { System.err.println("Usage: HtmlCssStyler <file.html>"); return; }
        File in = new File(args[0]);
        File out = new File("styled_output.ea");
        try (BufferedReader br = new BufferedReader(new FileReader(in)); FileWriter fw = new FileWriter(out)) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("<h1>") && line.endsWith("</h1>")) {
                    String inner = line.substring(4, line.length()-5);
                    fw.write("DISPLAY \"" + inner.toUpperCase() + "\"\n");
                } else if (line.startsWith("<p>") && line.endsWith("</p>")) {
                    String inner = line.substring(3, line.length()-4);
                    fw.write("DISPLAY \"" + inner + "\"\n");
                }
            }
            fw.write("HALT\n");
        }
        System.out.println("Wrote styled_output.ea (v1.0.0)");
    }
}
