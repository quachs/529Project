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
 * this is kind of a thread to do the indexing in the background
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
    
    long timer;

    // saving the path
    private Path path;

    /**
     * Constructors
     */
    public Indexing() {
        path = Paths.get(".");
    }
// this constructor with the path is important to find the directory the user likes
    public Indexing(Path path) {
        this.path = path;
    }    

    public void setPath(Path p) {
        this.path = p;
    }

    /**
     * Indexing should be a thread working in the background that the
     * progressbar still can work
     *
     * @return null when the process is finished
     * @throws IOException for file problems
     */
    @Override
    public Void doInBackground() throws IOException {
        //
        setProgress(0); 
        timer = new Date().getTime();
        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
            int mDocumentID = 0;

            @Override
            public FileVisitResult preVisitDirectory(Path dir,
                    BasicFileAttributes attrs) {
                /*
                // make sure we only process the current working directory
                if (currentWorkingPath.equals(dir)) {
                    return FileVisitResult.CONTINUE;
                }
                return FileVisitResult.SKIP_SUBTREE;
                 */

                // process the current working directory and subdirectories
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file,
                    BasicFileAttributes attrs) throws FileNotFoundException {
                // only process .json files
                if (file.toString().endsWith(".json")) {
                    //System.out.println("Indexing file " + file.getFileName());
                    fileNames.add(file.getFileName().toString());
                    indexFile(file.toFile(), index, vocabTree, sIndex, mDocumentID);
                    mDocumentID++;
                }
                return FileVisitResult.CONTINUE;
            }

            // don't throw exceptions if files are locked/other errors occur
            @Override
            public FileVisitResult visitFileFailed(Path file,
                    IOException e) {

                return FileVisitResult.CONTINUE;
            }
        });
        return null;
    }

    /**
     * method is called when doInBackgrond is finished -> process is finished
     */
    @Override
    public void done() {
        System.out.println("Time for indexing: "+ (new Date().getTime()-timer));
        // iterate the vocab tree to build the kgramindex
        Iterator<String> iter = vocabTree.iterator();
        while (iter.hasNext()) {
            kgIndex.addType(iter.next());
        }
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
    private static void indexFile(File file, PositionalInvertedIndex index,
            SortedSet vocabTree, SoundexIndex sIndex, int docID) throws FileNotFoundException {

        Gson gson = new Gson();
        JsonDocument doc;
        String docBody, docAuthor;

        JsonReader reader = new JsonReader(new FileReader(file));
        doc = gson.fromJson(reader, JsonDocument.class);
        docBody = doc.getBody();
        docAuthor = doc.getAuthor();

        // process the body field of the document
        SimpleTokenStream s = new SimpleTokenStream(docBody);
        int positionNumber = 0;
        while (s.hasNextToken()) {
            TokenProcessorStream t = new TokenProcessorStream(s.nextToken());
            while (t.hasNextToken()) {
                String proToken = t.nextToken(); // the processed token
                if (proToken != null) {
                    // add the processed and stemmed token to the inverted index
                    index.addTerm(PorterStemmer.getStem(proToken), docID, positionNumber);
                    // add the processed token to the vocab tree
                    vocabTree.add(proToken);
                }

            }
            positionNumber++;
        }

        // process the author field of the document and add to soundex
        if (docAuthor != null) {
            s = new SimpleTokenStream(docAuthor);
            while (s.hasNextToken()) {
                TokenProcessorStream t = new TokenProcessorStream(s.nextToken());
                while (t.hasNextToken()) { // process the author's name
                    String name = t.nextToken();
                    if (name != null) {
                        sIndex.addToSoundex(name, docID);
                    }
                }
            }
        }
    }
    
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
