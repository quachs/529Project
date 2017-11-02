package Helper;


import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import Indexes.*;
import Indexes.diskPart.*;
import Retrievals.rankedRetrieval.*;
import Retrivals.booleanRetrival.*;
import java.util.*;

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
                
                System.out.println("Choose: \n1) Boolean \n2) Ranked Retrieval");
                int choice = scan.nextInt();
                scan.nextLine();
                
                if (choice == 1){
                     while (true) {
                        System.out.println("Enter Boolean Query:");
                        String input = scan.nextLine();

                        if (input.equals("EXIT")) {
                            break;
                        }
                      
                        BooleanParser bParser = new BooleanParser(index, kIndex);
                        List<Subquery> allQueries = bParser.collectOrQueries(input);                        
                        List<DiskPosting> postingsList = DiskQueryProcessor.orQuery(allQueries, index, kIndex);     
                        
                        if (postingsList == null || postingsList.size() == 0) {
                            System.out.println("Term not found");
                        } else {
                            System.out.println("Docs: ");
                            for (DiskPosting posting : postingsList) {
                                System.out.println("Doc# " + index.getFileNames().get(posting.getDocumentID()));
                            }
                            System.out.println();
                            System.out.println();
                        }
                    }
                }
                else{
                    while (true) {
                        System.out.println("Enter one or more search terms, separated by spaces:");
                        String input = scan.nextLine();

                        if (input.equals("EXIT")) {
                            break;
                        }

                        RankedParser rParser = new RankedParser(index, kIndex);
                        Subquery query = rParser.collectAndQueries(input);

                        //DiskPosting[] postingsList = index.getPostings(input.toLowerCase());
                        RankedItem[] postingsList = RankedRetrieval.rankedQuery(index, kIndex, query, 10);

                        if (postingsList == null || postingsList.length == 0) {
                            System.out.println("Term not found");
                        } else {
                            System.out.println("Docs: ");
                            for (RankedItem ri : postingsList) {
                                System.out.println("Doc# " + index.getFileNames().get(ri.getDocID()));
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
}
