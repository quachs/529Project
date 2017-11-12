package indexes;

import threads.Indexing;
import helper.ProgressDialog;
import threads.ThreadFinishedCallBack;
import java.nio.file.Path;
import javax.swing.*;

/**
 * The GUI for the indexing part. It is a frame and implements the
 * threadFinishedCallBack to get notified when the background thread for
 * indexing is done.
 */
public class IndexingGUI extends JFrame implements ThreadFinishedCallBack {

    private Path path; // save the path
    private Indexing indexedCorpus = new Indexing(); // task for indexing
    private Thread thread; // thread for indexing in background

    private ProgressDialog progress; // progress dialog to show the user that program is working
    private ImageIcon img = new ImageIcon(System.getProperty("user.dir") + "/icon.png"); // logo icon

    /**
     * Start indexing with the given path.
     *
     * @param path Path where to index.
     */
    public IndexingGUI(Path path) {
        this.path = path;

        progress = new ProgressDialog("Indexing..."); // create the progress bar
        indexing(); // Start indexing. While indexing, show a progress bar to user
    }

    /**
     * While Indexing processes show a progress bar
     */
    private void indexing() {
        progress.setVisible(true); // show the progressbar   
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
                "Result of stemming", JOptionPane.INFORMATION_MESSAGE, this.img); // show the user that indexing is finished
        System.exit(0); // exit program
    }
}
