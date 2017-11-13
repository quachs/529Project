package threads;

import indexes.diskPart.IndexWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;

/**
 * Thread to do perform indexing as a background process.
 *
 * @author Sandra
 */
public class Indexing implements Runnable {
    long timer; // Timer to print how long the task took

    // Save the path of the corpus's directory
    private Path path;

    private ThreadFinishedCallBack callback;

    /**
     * Constructors
     */
    public Indexing() {
        path = Paths.get(".");
    }

    // Constructor for a user-specified corpus
    public Indexing(Path path, ThreadFinishedCallBack finish) {
        this.path = path;
        this.callback = finish;        
    }

    /**
     * Create Positional Inverted Index on disk.
     */
    @Override
    public void run() {
        timer = new Date().getTime(); // Start the timer    
        IndexWriter writer = new IndexWriter(path.toString());  
        writer.buildIndex();
        System.out.println("Time for indexing: " + (new Date().getTime() - timer)); // Print the length of time the process took   
        callback.notifyThreadFinished();
    }
}
