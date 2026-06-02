import java.io.FileWriter;
import java.io.IOException;

public class AnimationCreator {
    public static void main(String[] args) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (int frame = 1; frame <= 5; frame++) {
            sb.append("DISPLAY \"Frame " + frame + ": ");
            for (int i = 0; i < frame; i++) sb.append(' ');
            sb.append("*\"\n");
        }
        sb.append("DISPLAY \"Animation v3.0.0: smooth repeat\"\n");
        sb.append("HALT\n");

        try (FileWriter fw = new FileWriter("animation.ea")) {
            fw.write(sb.toString());
        }
        System.out.println("Wrote animation.ea (v3.0.0)");
    }
}
