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
                if (line.startsWith("<h1") && line.contains("</h1>")) {
                    String inner = line.substring(line.indexOf('>') + 1, line.indexOf("</h1>"));
                    fw.write("DISPLAY \"" + inner.toUpperCase() + "\"\n");
                } else if (line.startsWith("<p") && line.contains("</p>")) {
                    String inner = line.substring(line.indexOf('>') + 1, line.indexOf("</p>"));
                    fw.write("DISPLAY \"" + inner + "\"\n");
                } else if (line.startsWith("<div") && line.contains("</div>")) {
                    String content = line.substring(line.indexOf('>') + 1, line.indexOf("</div>"));
                    if (line.contains("color: red")) {
                        fw.write("DISPLAY \"RED DIV: " + content + "\"\n");
                    } else {
                        fw.write("DISPLAY \"DIV: " + content + "\"\n");
                    }
                }
            }
            fw.write("HALT\n");
        }
        System.out.println("Wrote styled_output.ea (v3.0.0)");
    }
}
