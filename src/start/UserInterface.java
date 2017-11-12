package start;

import formulas.FormEnum;
import indexes.IndexingGUI;
import retrievals.RetrievalGUI;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import threads.ThreadFinishedCallBack;
import helper.ProgressDialog;
import java.nio.file.Files;

/**
 * UserInterface is a Frame and implements ThreadFinishedCallBack to get
 * notified, when the UI is created.
 *
 * @author Sandra
 */
public class UserInterface extends JFrame implements ThreadFinishedCallBack {

    private Path path = Paths.get("");
    private ImageIcon img = new ImageIcon(System.getProperty("user.dir") + "/icon.png"); // logo icon
    private ProgressDialog progress;

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
        progress = new ProgressDialog("Creating view..."); // create the progress dialog
        Object[] optionsBeginning = {"Index a corpus",
            "Process Queries"}; // first step user can dicide if he wants to index or process a query
        int resultBeginning = JOptionPane.showOptionDialog(this,
                "What do you want to do?",
                "Search Engine Start",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE,
                img,
                optionsBeginning,
                optionsBeginning[1]);  // save users answer
        if (resultBeginning == 0) { // 0 = indexig
            path = chooseDirectory(); // let the user select where the corpus is he wants to index
            new IndexingGUI(path); // create indexing GUI.
        } else //otherwise the user wants to process a query
        {
            path = chooseDirectory(); // let the user select where the corpus is saved
            Path indexPath = Paths.get(path.toString() + "\\Indexes");
            // If the given path doesn´t have a indexes folder than it is not a corpus that is alread indexed. Let the user try again.
            if (!Files.exists(indexPath)) {
                JOptionPane.showMessageDialog(this, "You entered a corpus that is not indexed yet. Either you index it or you choose another one.",
                        "Not a indexed corpus", JOptionPane.INFORMATION_MESSAGE, this.img);
                new UserInterface();
                return;
            }
            // Now let the user decide which type of retrival he wants to have.
            Object[] optionsRetrival = {"Boolean Retrieval",
                "Ranked Retrieval"};
            int resultRetrival = JOptionPane.showOptionDialog(this,
                    "Please choose retrieval type",
                    "Retrieval Type",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE,
                    img,
                    optionsRetrival,
                    optionsRetrival[1]);
            if (resultRetrival == 0) { // boolean retrival.
                progress.setVisible(true); // show progress bar
                // create the GUI
                new RetrievalGUI('b', null, path.toString(), this);
            } else { // ranked retrival
                // let the user choose between the different types of formulars.
                Object[] optionsForm = {"Default",
                    "Traditional", "Okapi BM25", "Wacky"};
                int resultFormular = JOptionPane.showOptionDialog(this,
                        "Please choose the type of formula",
                        "Formula",
                        JOptionPane.YES_NO_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE,
                        img,
                        optionsForm,
                        optionsForm[1]);
                progress.setVisible(true); // show the progress bar
                // create the GUI
                new RetrievalGUI('r', FormEnum.getFormByID(resultFormular), path.toString(), this);
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

    @Override
    public void notifyThreadFinished() {
        this.progress.setVisible(false); // close the progress bar dialog
    }
}
