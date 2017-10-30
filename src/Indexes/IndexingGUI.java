package Indexes;

import Threads.Indexing;
import Helper.ProgressDialog;
import Threads.ThreadFinishedCallBack;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.swing.*;

public class IndexingGUI extends JFrame implements ThreadFinishedCallBack{
private Path path; // for saving the path// background tasks
    private Indexing indexedCorpus = new Indexing(); // task for indexing
    
    private ProgressDialog progress;
    private ImageIcon img = new ImageIcon(System.getProperty("user.dir") + "/icon.png"); // logo icon
    private Thread thread;
    
    public IndexingGUI(Path path) {
        this.path = path;
        
            progress = new ProgressDialog("Indexing..."); // create the progress bar
            indexing(); // Start indexing. While indexing, show a progress bar to user
    }
    

    /**
     * While Indexing processes show a progress bar
     */
    private void indexing() {
        progress.setVisible(true); // show the dialog   
        indexedCorpus = new Indexing(path, this); // start the task idexing  
        thread = new Thread(indexedCorpus);  
        thread.start(); // start the task  
    }

    /**
     * This is called when the task is finished.
     */
    @Override
    public void notifyThreadFinished() {
        progress.setVisible(false); // close the dialog
        JOptionPane.showMessageDialog(this, "Indexing is finished.",
                            "Result of stemming", JOptionPane.INFORMATION_MESSAGE, this.img);
        System.exit(0);
    }
}
