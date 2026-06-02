import java.io.FileWriter;
import java.io.IOException;

public class WindowCreator {
    public static void main(String[] args) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("DISPLAY \"+----------------------+\"\n");
        sb.append("DISPLAY \"|  Simple Window     |\"\n");
        sb.append("DISPLAY \"+----------------------+\"\n");
        sb.append("DISPLAY \"(Use ESA to install and run)\"\n");
        sb.append("HALT\n");

        try (FileWriter fw = new FileWriter("window.ea")) {
            fw.write(sb.toString());
        }
        System.out.println("Wrote window.ea");
    }
}
