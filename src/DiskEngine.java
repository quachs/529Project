
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Scanner;

public class DiskEngine {

    public static void main(String[] args) throws IOException {
        Scanner scan = new Scanner(System.in);

        System.out.println("Menu:");
        System.out.println("1) Build index");
        System.out.println("2) Read and query index");
        System.out.println("Choose a selection:");
        int menuChoice = scan.nextInt();
        scan.nextLine();

        switch (menuChoice) {
            case 1:
                System.out.println("Enter the name of a directory to index: ");
                String folder = scan.nextLine();

                IndexWriter writer = new IndexWriter(folder);
                // You must hook up your code to create an inverted index object,
                // then pass it to the WriteIndex method of the writer.
                writer.buildIndex();

                break;

            case 2:
                System.out.println("Enter the name of an index to read:");
                String indexName = scan.nextLine();

                DiskInvertedIndex index = new DiskInvertedIndex(indexName);
                KGramIndex kgIndex = new KGramIndex();

                // Read KGramIndex from file
                KGramIndex kIndex = null;
                try {
                    FileInputStream fileIn = new FileInputStream(indexName + "\\kGramIndex.bin");
                    ObjectInputStream objectIn = new ObjectInputStream(fileIn);
                    kIndex = (KGramIndex) objectIn.readObject();
                    objectIn.close();
                    fileIn.close();
                } catch (IOException | ClassNotFoundException ex) {
                    System.out.println(ex.toString());
                }

                // Read KGramIndex from file
                KGramIndex kIndex = null;
                try {
                    FileInputStream fileIn = new FileInputStream(indexName + "\\kGramIndex.bin");
                    ObjectInputStream objectIn = new ObjectInputStream(fileIn);
                    kIndex = (KGramIndex) objectIn.readObject();
                    objectIn.close();
                    fileIn.close();
                } catch (IOException | ClassNotFoundException ex) {
                    System.out.println(ex.toString());
                }

                while (true) {
                    System.out.println("Enter one or more search terms, separated by spaces:");
                    String input = scan.nextLine();

                    if (input.equals("EXIT")) {
                        break;
                    }

                    RankedParser rParser = new RankedParser(index);
                    Subquery query = rParser.collectAndQueries(input);
                    System.out.println(query.getLiterals());
                    
                    //DiskPosting[] postingsList = index.getPostings(input.toLowerCase());
                    RankedItem[] postingsList = RankedRetrieval.rankedQuery(index, kgIndex, query, 10);
                    
                    if (postingsList == null) {
                        System.out.println("Term not found");
                    } else {
                        System.out.println("Docs: ");
                        for (RankedItem ri : postingsList) {
                            System.out.println("Doc# " + index.getFileNames().get(ri.getPosting().getDocumentID()));
                            System.out.println("A_d score: " + ri.getA_d());
                        }
                        System.out.println();
                        System.out.println();
                    }

                    if (postingsList == null || postingsList.length < 5) {
                        SpellingCorrection spellCorrect = new SpellingCorrection();
                        String correction = spellCorrect.getCorrection(input, index, kIndex);
                        System.out.println("Did you mean " + correction.toUpperCase() + "?");
                        
                        // TODO : ASK IF USER WANTS TO RERUN QUERY WITH CORRECTION
                    }

                    //break;
                }
        }
    }
}
