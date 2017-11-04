package threads;

import helper.JsonDocument;
import helper.PorterStemmer;
import query.SimpleTokenStream;
import query.processor.TokenProcessorStream;
import indexes.diskPart.IndexWriter;
import indexes.KGramIndex;
import indexes.PositionalInvertedIndex;
import indexes.PositionalPosting;
import indexes.SoundexIndex;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

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
