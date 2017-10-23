package Indexes;

import Helper.ProgressDialog;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.swing.*;

public class IndexingGUI extends JFrame{
private Path path; // for saving the path// background tasks
    private Indexing indexedCorpus = new Indexing(); // task for indexing
    
    private ProgressDialog progress;
    private ImageIcon img = new ImageIcon(System.getProperty("user.dir") + "/icon.png"); // logo icon
    
    public IndexingGUI() {
        path = Paths.get("");
        chooseDirectory();
    }
    /**
     * Open a file chooser dialog for choosing the directory the user wants to
     * index
     */
    private void chooseDirectory() {
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
            path = Paths.get(chooser.getSelectedFile().getPath()); // save the returned path,
            progress = new ProgressDialog("Indexing..."); // create the progress bar
            indexing(); // Start indexing. While indexing, show a progress bar to user
        } else {
            System.exit(0); // Close the system if user presses x or cancel
        }
    }    

    /**
     * While Indexing processes show a progress bar
     */
    private void indexing() {
        progress.setVisible(true); // show the dialog        
        indexedCorpus = new Indexing(path); // start the task idexing
        indexedCorpus.execute(); // start the task  
        // wait till the task is finished -> set true when done() is finished
        while (!indexedCorpus.isDone()) {
        }        
        progress.setVisible(false); // close the dialog
        JOptionPane.showMessageDialog(this, "Indexing is finished.",
                            "Result of stemming", JOptionPane.INFORMATION_MESSAGE, this.img);
        System.exit(0);
    }
}
