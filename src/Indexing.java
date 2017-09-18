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
import java.util.List;
import javax.swing.SwingWorker;

/**
 * this is kind of a thread that works in the background
 *
 * @author Sandra
 */
class Indexing extends SwingWorker<Void, Void> {

    // initialize the positional index
    private PositionalIndex index = new PositionalIndex();

    // saving the path
    private Path path;
    // the list of file names that were processed
    final List<String> fileNames = new ArrayList<String>();

    public Indexing() {
        path = Paths.get(".");
    }

    public PositionalIndex getIndex() {
        return index;
    }
    // this constructor with the path is important to find the directory the user likes
    public Indexing(Path path) {
        this.path = path;
    }
    
    public void setPath(Path p){
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
        // the standard "walk through all .txt files" code.
        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
            int mDocumentID = 0;

            @Override
            public FileVisitResult preVisitDirectory(Path dir,
                    BasicFileAttributes attrs) {
                // make sure we only process the current working directory
                if (path.equals(dir)) {
                    return FileVisitResult.CONTINUE;
                }
                return FileVisitResult.SKIP_SUBTREE;
            }

            @Override
            public FileVisitResult visitFile(Path file,
                    BasicFileAttributes attrs) throws FileNotFoundException {
                // only process .json files -> .json because of the given corpus
                if (file.toString().endsWith(".json")) {
                    // we have found a .json file; add its name to the fileName list,
                    // then index the file and increase the document ID counter.
                    //System.out.println("Indexing file " + file.getFileName());

                    fileNames.add(file.getFileName().toString());
                    indexFile(file.toFile(), index, mDocumentID);
                    setProgress(mDocumentID);
                    mDocumentID++;
                }
                return FileVisitResult.CONTINUE;
            }

            // don't throw exceptions if files are locked/other errors occur
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
    }

    /**
     * Indexing the file
     * @param file: file that should get indexed
     * @param index: the index itself
     * @param docID: the docID of the file
     * @throws FileNotFoundException 
     */
    private static void indexFile(File file, PositionalIndex index,
            int docID) throws FileNotFoundException {
        // indexing a particular file.
        // Construct a SimpleTokenStream for the given File.
        // Read each token from the stream and add it to the index.
        // TO-DO: stemming and so on
        SimpleTokenStream s = new SimpleTokenStream(file);
        while (s.hasNextToken()) {
            index.addTerm(s.nextToken(), docID);
        }
    }
}
