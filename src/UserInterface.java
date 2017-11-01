
import formulas.FormEnum;
import indexes.IndexingGUI;
import retrivals.RetrievalGUI;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

/**
 * UserInterface Implements MouseListener for user input handling
 *
 * @author Sandra
 */
public class UserInterface extends JFrame {

    private Path path = Paths.get("");
    private ImageIcon img = new ImageIcon(System.getProperty("user.dir") + "/icon.png"); // logo icon

    public UserInterface() {
        // Change look and feel of swing components
        // resource: http://docs.oracle.com/javase/tutorial/uiswing/lookandfeel/nimbus.html
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException | IllegalAccessException
                | InstantiationException | UnsupportedLookAndFeelException e) {
            System.err.println(e.toString());
        }
        Object[] optionsBeginning = {"Indexing a corpus",
            "Process Queries"};
        int resultBeginning = JOptionPane.showOptionDialog(this,
                "What do you want to do?",
                "Search Engine Start",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE,
                img,
                optionsBeginning,
                optionsBeginning[1]);
        if (resultBeginning == 0) {
            path = chooseDirectory();
            new IndexingGUI(path);
        } else //Custom button text
        {
            path = chooseDirectory();            
            Object[] optionsRetrival = {"Boolean Retrival",
                "Ranked Retrieval"};
            int resultRetrival = JOptionPane.showOptionDialog(this,
                    "Please choose retrival type",
                    "Retrival Type",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE,
                    img,
                    optionsRetrival,
                    optionsRetrival[1]);
            if (resultRetrival == 0) {
                new RetrievalGUI('b', null, path.toString());
            } else {
                Object[] optionsForm = {"Default",
                    "tf-idf", "Okapi BM25", "Wacky"};
                int resultFormular = JOptionPane.showOptionDialog(this,
                        "Please choose the type of formular",
                        "Formular",
                        JOptionPane.YES_NO_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE,
                        img,
                        optionsForm,
                        optionsForm[1]);
                new RetrievalGUI('r', FormEnum.getFormByID(resultFormular), path.toString());
            }
        }
    }

    /**
     * Open a file chooser dialog for choosing the directory the user wants to
     * index
     */
    private Path chooseDirectory() {
        JFileChooser chooser = new JFileChooser(); // initialize file chooser        
        chooser.setCurrentDirectory(new java.io.File(path.toString())); // start with the saved path        
        chooser.setDialogTitle("Choose Directory for corpus"); // set the title of the dialog

        // tell the file chooser that it only shows the directory that the user is 
        // only able to choose a directory and not a file
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        // resChooser saves result of dialog: int for JFileChooser options (APPROVE_OPTION, ABORT,...)
        // chooser.showOpenDialog(null) opens the dialog in the 
        // middle of the screen, it is not depended by any frame or component
        int resChooser = chooser.showOpenDialog(null);

        // if user approves -> clicks "Open" ...
        if (JFileChooser.APPROVE_OPTION == resChooser) {
            return Paths.get(chooser.getSelectedFile().getPath()); // save the returned path,
        } else {
            System.exit(0); // Close the system if user presses x or cancel
            return null;
        }
    }
}