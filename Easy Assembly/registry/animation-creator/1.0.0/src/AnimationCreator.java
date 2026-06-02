import java.io.FileWriter;
import java.io.IOException;

public class AnimationCreator {
    public static void main(String[] args) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("DISPLAY \"Frame 1: *        \"\n");
        sb.append("DISPLAY \"Frame 2:  *       \"\n");
        sb.append("DISPLAY \"Frame 3:   *      \"\n");
        sb.append("DISPLAY \"Animation v1.0.0\"\n");
        sb.append("HALT\n");

        try (FileWriter fw = new FileWriter("animation.ea")) {
            fw.write(sb.toString());
        }
        System.out.println("Wrote animation.ea (v1.0.0)");
    }
}
