import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.util.*;
import javax.swing.JFileChooser;

/**
 * A very simple search engine. Uses an inverted index over a folder of TXT
 * files.
 */
public class SimpleEngine {

    public static void main(String[] args) throws IOException {
                
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select Folder");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.showSaveDialog(chooser);
        
        final Path currentWorkingPath = chooser.getSelectedFile().toPath();

        // the indices
        final PositionalInvertedIndex index = new PositionalInvertedIndex();
        final KGramIndex kgIndex = new KGramIndex(); // add to sandra branch
        final SoundexIndex sIndex = new SoundexIndex(); // add to sandra branch

        // the list of file names that were processed
        final List<String> fileNames = new ArrayList<String>();      

        // the set of vocabulary types in the corpus
        final SortedSet<String> vocabTree = new TreeSet<String>(); // add to sandra branch
        
        // This is our standard "walk through all .txt files" code.
        Files.walkFileTree(currentWorkingPath, new SimpleFileVisitor<Path>() {
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
                // only process .txt files
                /*
                if (file.toString().endsWith(".txt")) { 
                    // we have found a .txt file; add its name to the fileName list,
                    // then index the file and increase the document ID counter.
                    System.out.println("Indexing file " + file.getFileName());

                    fileNames.add(file.getFileName().toString());
                    indexFile(file.toFile(), index, vocabTree, sIndex, mDocumentID); // add to sandra branch
                    mDocumentID++;
                } */
                // only process .json files
                if (file.toString().endsWith(".json")){
                    System.out.println("Indexing file " + file.getFileName());
                    fileNames.add(file.getFileName().toString());
                    indexFile(file.toFile(), index, vocabTree, sIndex, mDocumentID); // add to sandra branch
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
        
        // iterate the vocab tree to build the kgramindex
        Iterator<String> iter = vocabTree.iterator();
        while(iter.hasNext()){
            kgIndex.addType(iter.next());
        }
        
        // ===================================================================
        
        System.out.println(index.getTermCount());
        //printResults(index, fileNames);
        
        /*
        //https://docs.oracle.com/javase/8/docs/api/java/util/Scanner.html
        System.out.println("Enter a query: "); //don't need
        Scanner uScanner = new Scanner(System.in); //don't need
        String uInput = uScanner.nextLine(); //don't need
        QueryParser_KQV qParser = new QueryParser_KQV(index, kgIndex);
        List<Integer> docList = qParser.getDocumentList(uInput); //don't need
        for(int i = 0; i < docList.size(); i++){ //don't need
            System.out.println("document" + docList.get(i)); 
        }
        */
        
        // TESTING SOUNDEX ===================================================
        
        // *** make reducedToSoundex public for testing purposes ***
        
        String hash = sIndex.reduceToSoundex("michael");
        List<Integer> docList = sIndex.getPostingsList(hash);
        for(int i = 0; i < docList.size(); i++){ 
            // docID corresponds to fileNames index
            System.out.println(fileNames.get(docList.get(i))); 
        }
        
    }        
    
    /**
     * Indexes a file by reading a series of tokens from the file, treating each
     * token as a term, and then adding the given document's ID to the inverted
     * index for the term.
     *
     * @param file a File object for the document to index.
     * @param index the current state of the index for the files that have
     * already been processed.
     * @param docID the integer ID of the current document, needed when indexing
     * each term from the document.
     */
    private static void indexFile(File file, PositionalInvertedIndex index,
            SortedSet vocabTree, SoundexIndex sIndex, int docID) throws FileNotFoundException { //// add to sandra branch
        
        Gson gson = new Gson();
        JsonDocument doc;
        String docBody, docAuthor;
        
        JsonReader reader = new JsonReader(new FileReader(file));
        doc = gson.fromJson(reader,JsonDocument.class);
        docBody = doc.getBody();
        docAuthor = doc.getAuthor();
        
        // process the body field of the document
        SimpleTokenStream s = new SimpleTokenStream(docBody);       
        int positionNumber = 0;
        while(s.hasNextToken()){
            TokenProcessorStream t = new TokenProcessorStream(s.nextToken());
            while(t.hasNextToken()){
                String proToken = t.nextToken(); // the processed token
                if(proToken != null){
                    // add the processed and stemmed token to the inverted index
                    index.addTerm(PorterStemmer.getStem(proToken), docID, positionNumber);
                    // add the processed token to the vocab tree
                    vocabTree.add(proToken);
                }
                
            }
            positionNumber++;
        }
        
        // process the author field of the document and add to soundex
        if(docAuthor != null){
            s = new SimpleTokenStream(docAuthor);
            while(s.hasNextToken()){
                TokenProcessorStream t = new TokenProcessorStream(s.nextToken());
                while(t.hasNextToken()){ // process the author's name
                    String name = t.nextToken();
                    if(name != null){
                        sIndex.addToSoundex(name, docID);
                    }
                }
            }
        }
        
        // build kgram index when vocab tree is complete (after walkFileTree)
    }
    
    private static void printResults(PositionalInvertedIndex index,
            List<String> fileNames) {
        
        int longestTerm = 0;
        for(String term : index.getDictionary()){
            longestTerm = Math.max(longestTerm, term.length());
        }

        for(String term : index.getDictionary()){
            System.out.printf("%s:", term);
            printSpaces(longestTerm - term.length() + 1);
            for(PositionalPosting p : index.getPostingsList(term)){
                System.out.print(p.toString());      
            }
            System.out.println();            
        }
        
    }
    
    private static void printSpaces(int spaces) {
        for (int i = 0; i < spaces; i++) {
            System.out.print(" ");
        }
    }
}
