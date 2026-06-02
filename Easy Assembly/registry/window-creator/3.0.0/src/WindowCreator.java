import java.io.FileWriter;
import java.io.IOException;

public class WindowCreator {
    public static void main(String[] args) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("DISPLAY \"+------------------------------------+\"\n");
        sb.append("DISPLAY \"| Easy Assembly Window Creator v3   |\"\n");
        sb.append("DISPLAY \"+------------------------------------+\"\n");
        sb.append("DISPLAY \"|  [1] Open  [2] Save  [3] Exit    |\"\n");
        sb.append("DISPLAY \"|  Use commands to build a window.  |\"\n");
        sb.append("DISPLAY \"+------------------------------------+\"\n");
        sb.append("DISPLAY \"Footer: version 3 features\"\n");
        sb.append("HALT\n");

        try (FileWriter fw = new FileWriter("window.ea")) {
            fw.write(sb.toString());
        }
        System.out.println("Wrote window.ea (v3.0.0)");
    }
}
