package threads;

import indexes.diskPart.IndexWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;

/**
 * Thread to do the indexing in the background
 *
 * @author Sandra
 */
public class Indexing implements Runnable {
    long timer; // timer to print how long task took

    // saving the path of the directory of the corpus
    private Path path;

    private ThreadFinishedCallBack callback;

    /**
     * Constructors
     */
    public Indexing() {
        path = Paths.get(".");
    }

    // this constructor for the directory the user chooses
    public Indexing(Path path, ThreadFinishedCallBack finish) {
        this.path = path;
        this.callback = finish;        
    }

    /**
     * Create Positional Inverted Index on disk.
     */
    @Override
    public void run() {
        timer = new Date().getTime(); // start the timer    
        IndexWriter writer = new IndexWriter(path.toString());  
        writer.buildIndex();
        System.out.println("Time for indexing: " + (new Date().getTime() - timer)); // print out time that process took   
        callback.notifyThreadFinished();
    }
}
