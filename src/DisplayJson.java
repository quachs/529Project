import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.text.Document;
import javax.swing.text.html.HTMLEditorKit;


public class DisplayJson {
    
    public static void main(String[] args) throws IOException{
        new DisplayJson();
    }
    
    public DisplayJson()throws IOException{
        SwingUtilities.invokeLater(new Runnable(){
            public void run(){
                JEditorPane jEditorPane = new JEditorPane();
                jEditorPane.setEditable(false); // read-only
                JScrollPane scrollPane = new JScrollPane(jEditorPane);
                
                HTMLEditorKit kit = new HTMLEditorKit();
                jEditorPane.setEditorKit(kit);
                
                // create some simple html as a string
                String htmlString;
                try {
                    
                    htmlString = getHTMLString();
                    
                    // create a document, set it on the jeditorpane, then add the html
                    Document doc = kit.createDefaultDocument();
                    jEditorPane.setDocument(doc);
                    jEditorPane.setText(htmlString);
                    
                    // start scrollbar at top
                    jEditorPane.setSelectionStart(0);
                    jEditorPane.setSelectionEnd(0);
                    
                } catch (IOException ex) {
                    Logger.getLogger(DisplayJson.class.getName()).log(Level.SEVERE, null, ex);
                }
                
                
                
                // now add it all to a frame
                JFrame j = new JFrame("Document Viewer");
                j.getContentPane().add(scrollPane, BorderLayout.CENTER);
                j.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                j.setSize(new Dimension(400,600)); // size of frame
        
                // pack it, if you prefer
                //j.pack();
        
                // center the jframe, then make it visible
                j.setLocationRelativeTo(null);
                j.setVisible(true);
            }
        });
    }
    
    /**
     * 
     * TODO: should pass in a json file
     * 
     * Parse a Json file to get HTML string
     * @return html stringb
     * @throws IOException 
     */
    private String getHTMLString() throws IOException{
        JsonDocument doc;
        String htmlString, jTitle, jBody, jUrl, jAuthor;
        Gson gson = new Gson();
        
        try (JsonReader reader = new JsonReader
            (new FileReader("C:\\Users\\t420\\Documents\\NPS Files\\article2."))) {
            doc = gson.fromJson(reader,JsonDocument.class);
            jTitle = doc.getTitle();
            jBody = doc.getBody();
            jUrl = doc.getUrl();
            jAuthor = doc.getAuthor();
            htmlString = "<html>\n"
                        + "<body>\n"
                        + "<h1>"+jTitle+"</h1>\n";
            if(jAuthor != null){ // add author if available
                htmlString = htmlString + "<h2>"+jAuthor+"</h2>\n";       
            }
            htmlString = htmlString
                    + "<p>"+jBody+"</p>\n"
                    + "<p><a href="+jUrl+">"+jUrl+"</a></p>\n"
                    + "</body>\n";
        }
        return htmlString;
        
    }
}
