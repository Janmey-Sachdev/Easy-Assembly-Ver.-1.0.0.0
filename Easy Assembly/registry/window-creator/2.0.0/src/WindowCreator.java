import java.io.FileWriter;
import java.io.IOException;

public class WindowCreator {
    public static void main(String[] args) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("DISPLAY \"+-----------------------------+\"\n");
        sb.append("DISPLAY \"|   Easy Assembly Window v2   |\"\n");
        sb.append("DISPLAY \"+-----------------------------+\"\n");
        sb.append("DISPLAY \"|  [1] Start   [2] Settings  |\"\n");
        sb.append("DISPLAY \"+-----------------------------+\"\n");
        sb.append("HALT\n");

        try (FileWriter fw = new FileWriter("window.ea")) {
            fw.write(sb.toString());
        }
        System.out.println("Wrote window.ea (v2.0.0)");
    }
}
