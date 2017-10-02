
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.text.Document;
import javax.swing.text.html.HTMLEditorKit;

/**
 * Class to display a json file in html format
 *
 */
public class DisplayJson {

    /**
     * Display formatted json file in a pop up dialog 
     *
     * @param file json file to display
     */
    public DisplayJson(File file) throws IOException {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                JEditorPane jEditorPane = new JEditorPane();
                jEditorPane.setEditable(false); // read-only
                JScrollPane scrollPane = new JScrollPane(jEditorPane);

                HTMLEditorKit kit = new HTMLEditorKit();
                jEditorPane.setEditorKit(kit);

                // create html as a string
                String htmlString;
                try {

                    htmlString = getHTMLString(file);

                    // set document to pane
                    Document doc = kit.createDefaultDocument();
                    jEditorPane.setDocument(doc);
                    jEditorPane.setText(htmlString);

                    // start scrollbar at top
                    jEditorPane.setSelectionStart(0);
                    jEditorPane.setSelectionEnd(0);

                } catch (IOException ex) {
                    System.out.println("File not found!");
                }

                String[] name = file.getName().split("\\.");
                JFrame j = new JFrame(name[0]);
                j.getContentPane().add(scrollPane, BorderLayout.CENTER);
                //j.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                j.setSize(new Dimension(400, 600)); // size of frame

                // center the frame and show
                j.setLocationRelativeTo(null);
                j.setVisible(true);
            }
        });
    }

    /**
     * Parse a Json file to get HTML string
     *
     * @return html string
     * @throws IOException
     */
    private String getHTMLString(File file) throws IOException {
        JsonDocument doc;
        String htmlString;
        Gson gson = new Gson();

        try (JsonReader reader = new JsonReader(new FileReader(file.getAbsolutePath()))) {
            doc = gson.fromJson(reader, JsonDocument.class);
            htmlString = "<html>\n"
                    + "<body>\n"
                    + "<h1>" + doc.getTitle() + "</h1>\n";
            if (doc.getAuthor() != null) { // add author if available
                htmlString = htmlString + "<h2>" + doc.getAuthor() + "</h2>\n";
            }
            htmlString = htmlString
                    + "<p>" + doc.getBody() + "</p>\n"
                    + "<p><a href=" + doc.getUrl() + ">" + doc.getUrl() + "</a></p>\n"
                    + "</body>\n";
        }
        return htmlString;

    }
}
