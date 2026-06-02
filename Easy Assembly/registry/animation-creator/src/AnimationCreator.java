import java.io.FileWriter;
import java.io.IOException;

public class AnimationCreator {
    public static void main(String[] args) throws IOException {
        StringBuilder sb = new StringBuilder();
        // Simple 3-frame animation using DISPLAY lines
        sb.append("DISPLAY \"Frame 1: (\"\n");
        sb.append("DISPLAY \"  *      \"\n");
        sb.append("DISPLAY \"       )\"\n");
        sb.append("DISPLAY \"---FRAME---\"\n");
        sb.append("DISPLAY \"Frame 2: (\"\n");
        sb.append("DISPLAY \"   *     \"\n");
        sb.append("DISPLAY \"       )\"\n");
        sb.append("DISPLAY \"---FRAME---\"\n");
        sb.append("DISPLAY \"Frame 3: (\"\n");
        sb.append("DISPLAY \"    *    \"\n");
        sb.append("DISPLAY \"       )\"\n");
        sb.append("HALT\n");

        try (FileWriter fw = new FileWriter("animation.ea")) {
            fw.write(sb.toString());
        }
        System.out.println("Wrote animation.ea");
    }
}
