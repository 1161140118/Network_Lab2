import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * 
 */

/**
 * @author chen
 *
 */
public class ModifyTestFile {

    /**
     * @param args
     * @throws URISyntaxException 
     * @throws IOException 
     */
    public static void main(String[] args) throws IOException, URISyntaxException {
        List<String> list = Files.readAllLines(Paths.get("senderfile.txt"));
        PrintWriter writer = new PrintWriter(new FileOutputStream(new File("senderfile.txt"),true));
        
        int i= 0;
        for (String string : list) {
            System.out.println(string);
            writer.write(i+" : " +string+"\n");
            i++;
        }
        writer.close();
               
    }

}
