
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

        // the inverted index
        final PositionalInvertedIndex index = new PositionalInvertedIndex();

        // the list of file names that were processed
        final List<String> fileNames = new ArrayList<String>();

        // This is our standard "walk through all .txt files" code.
        Files.walkFileTree(currentWorkingPath, new SimpleFileVisitor<Path>() {
            int mDocumentID = 0;

            public FileVisitResult preVisitDirectory(Path dir,
                    BasicFileAttributes attrs) {
                // make sure we only process the current working directory
                if (currentWorkingPath.equals(dir)) {
                    return FileVisitResult.CONTINUE;
                }
                return FileVisitResult.SKIP_SUBTREE;
            }

            public FileVisitResult visitFile(Path file,
                    BasicFileAttributes attrs) throws FileNotFoundException {
                // only process .txt files
                if (file.toString().endsWith(".txt")) {
                    // we have found a .txt file; add its name to the fileName list,
                    // then index the file and increase the document ID counter.
                    System.out.println("Indexing file " + file.getFileName());

                    fileNames.add(file.getFileName().toString());
                    indexFile(file.toFile(), index, mDocumentID);
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
        
        System.out.println(index.getTermCount());
        printResults(index, fileNames);
            
        /*
        // Implement the same program as in Homework 1: ask the user for a term,
        // retrieve the postings list for that term, and print the names of the 
        // documents which contain the term.
        
        Scanner input = new Scanner(System.in);
        String term;
        
        while(true){
            
            System.out.print("Enter a term to search for: ");
            term = input.next();
            
            if(term.equals("quit")){
                break;
            }
            
            // check if the term exists in the index
            if(Arrays.binarySearch(index.getDictionary(), term) >= 0){
                // iterate postings list and print file name
                System.out.println("These documents contain that term: ");
                for(Integer posting : index.getPostings(term)){ 
                    System.out.printf("%s ",fileNames.get(posting));
                }
                System.out.println();
            }
            else{
                System.out.println("The term does not exist!");
            }
        }
        input.close();
        System.out.println("Bye!");
        */
        
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
            int docID) throws FileNotFoundException {
        // TO-DO: finish this method for indexing a particular file.
        // Construct a SimpleTokenStream for the given File.
        // Read each token from the stream and add it to the index.
        SimpleTokenStream s = new SimpleTokenStream(file);
        int positionNumber = 0;
        while(s.hasNextToken()){
            index.addTerm(s.nextToken(), docID, positionNumber);
            positionNumber++;
        }

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
            for(Integer docID : index.getDocumentPostingsList(term)){
                System.out.printf("< %s ",fileNames.get(docID));
                System.out.print("[");
                for(Integer pos : index.getDocumentTermPositions(term, docID)){
                    System.out.printf(" %d ",pos);
                }
                System.out.print("]");
                System.out.print(" > ");
                
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
