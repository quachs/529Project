package Indexes;

import Indexes.diskPart.IndexWriter;
import Helper.SimpleTokenStream;
import Helper.TokenProcessorStream;
import Helper.PorterStemmer;
import Helper.JsonDocument;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.swing.SwingWorker;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import java.io.FileReader;
import java.util.Date;

/**
 * Thread to do the indexing in the background
 *
 * @author Sandra
 */
class Indexing extends SwingWorker<Void, Void> {
    // the indices
    final PositionalInvertedIndex index = new PositionalInvertedIndex();
    final KGramIndex kgIndex = new KGramIndex(); 
    final SoundexIndex sIndex = new SoundexIndex();

    // the list of file names that were processed
    final List<String> fileNames = new ArrayList<String>();

    // the set of vocabulary types in the corpus
    final SortedSet<String> vocabTree = new TreeSet<String>();
    
    long timer; // timer to print how long task took

    // saving the path of the directory of the corpus
    private Path path;

    /**
     * Constructors
     */
    public Indexing() {
        path = Paths.get(".");
    }
    // this constructor for the directory the user chooses
    public Indexing(Path path) {
        this.path = path;
    }       

    /**
     * Create Positional Inverted Index on disk
     *
     * @return null when the process is finished
     * @throws IOException for file problems
     */
    @Override
    public Void doInBackground() throws IOException {
        timer = new Date().getTime(); // start the timer        
        IndexWriter writer = new IndexWriter(path.toString());
        writer.buildIndex();
        return null;
    }

    /**
     * Method is called when doInBackgrond is finished -> process is finished
     * Build the k-gram index when all the vocabulary types are collected.
     */
    @Override
    public void done() {
        System.out.println("Time for indexing: "+ (new Date().getTime()-timer)); // print out time that process took        
    }

    /**
     * Index a file by reading a series of tokens from the body of the file. 
     * Each token is processed and added to a tree of vocabulary types. The
     * same token is stemmed and added to the positional inverted index with
     * the given document ID. If the json file contains an author field, 
     * add the document to the soundex index.
     * @param file a json file object for the document to index.
     * @param index the current state of the positional inverted index for the 
     * files that have been already processed
     * @param vocabTree the current state of the vocabulary type tree
     * @param sIndex the current state of the soundex index
     * @param docID the integer ID of the current document, needed when indexing
     * each term from the document.
     * @throws FileNotFoundException 
     */
    
    
    // Getter
    public KGramIndex getKgIndex() {
        return kgIndex;
    }
    public SoundexIndex getsIndex() {
        return sIndex;
    }
    public List<String> getFileNames() {
        return fileNames;
    }
    public PositionalInvertedIndex getIndex() {
        return index;
    }
}
