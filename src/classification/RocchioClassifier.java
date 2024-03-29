package classification;

import retrievals.rankedRetrieval.RankedRetrieval;
import java.util.HashMap;
import java.io.File;
import indexes.diskPart.*;
import retrievals.rankedRetrieval.RankedDocument;
import java.util.PriorityQueue;
import java.util.List;
import java.util.ArrayList;
import java.util.Scanner;
import java.io.FileNotFoundException;

// Number of Known Hamilton: 51
// Number of Known Jay: 5
// Number of Known Madison: 15
// Number of Disputed: 11

/**
 * Implementation of Rocchio Classifier as detailed in
 * C.D. Manning et al., Introduction to Information Retrieval, Online Edition
 * Cambridge University Press, 2009, p. 292;
 * https://nlp.stanford.edu/IR-book/pdf/irbookonlinereading.pdf
 * 
 */
public class RocchioClassifier {

    private int hamSize; // 51
    private int jaySize; // 5
    private int madSize; // 15
    private HashMap<authors, Centroid> centroids = new HashMap<authors, Centroid>();
    private HashMap<Integer, Double> disputedVectors = new HashMap<Integer, Double>();
    
    public enum authors {
        HAMILTON,
        JAY,
        MADISON
    }
    
    /**
     * Use default idf formula to calculate WDT value
     * 
     * @param dPosting
     * @return WDT value for the corresponding document
     */
    private double calcWDT(DiskPosting dPosting) {
        return (1 + Math.log(dPosting.getTermFrequency()));
    }
    
    /**
     * Used to calculate the WDT value for a term in a 
     * single document vector
     * 
     * @param sfx Single File Index
     * @param term
     * @return 
     */
    private double calcWDT(SingleFileIndex sfx, String term) {    
        if (sfx.getPostings(term) == null){
            return 0.0;
        }
        return 1 + Math.log(sfx.getPostings(term).size());
    }
    
    /**
     * Returns the weight of a term component found in a document vector
     * 
     * @param termMap Can either be the normalized term weights for an 
     * individual document, or the normalized, averaged term weights for a centroid
     * @param term
     * @return 
     */
    private double getTermComponentWeight(HashMap<String, Double> termMap, String term){
        if (termMap.get(term) == null){
            return 0.0;
        }
        return termMap.get(term);
    }
    
    /**
     * Finds the Euclidean Distance between a centroid vector and a
     * document vector.
     * 
     * @param masterDictionary
     * @param centroid
     * @param docTerms
     * @return 
     */
    private double getEuclideanDistance(String[] masterDictionary, Centroid centroid, HashMap<String, Double> docTerms){
    
        //Implementation of euclidean distance formula as presented in:
        // C.D. Manning et al., Introduction to Information Retrieval, Online Edition
        //Cambridge University Press, 2009, p. 131;
        //https://nlp.stanford.edu/IR-book/pdf/irbookonlinereading.pdf
        double euclideanDistance = 0.0;
        
        for (String masterTerm : masterDictionary){
            euclideanDistance += Math.pow(getTermComponentWeight(centroid.getCentroid(), masterTerm) - 
                    getTermComponentWeight(docTerms, masterTerm), 2);
        }
        euclideanDistance = Math.sqrt(euclideanDistance);
        
        return euclideanDistance;
    }
    
    /**
     * Uses the number of relevant documents associated with a class
     * to find an averaged score of all document vector components 
     * 
     * @param fedIndex
     * @return 
     */
    public Centroid calculateCentroid(DiskInvertedIndex fedIndex){
   
        String[] dictionary = fedIndex.getDictionary();        
        HashMap<String, Double> fedMap = new HashMap<String, Double>();
        Centroid fedCentroid = new Centroid();

        int numberOfRelevantDocs = fedIndex.getCorpusSize(); 
        
        for(String term : dictionary){
            
            fedMap.put(term, 0.0);
            
            List<DiskPosting> dPostings = fedIndex.getPostings(term);           
            
            for(DiskPosting dPosting : dPostings){
                
                double docWeight = fedIndex.getDocWeight(dPosting.getDocumentID());
                double termVector = calcWDT(dPosting) / docWeight;  
                fedMap.put(term, termVector + fedMap.get(term));
            }   
            
            fedMap.put(term, fedMap.get(term) / (double) numberOfRelevantDocs);
            
        }                
        
        fedCentroid.setCentroid(fedMap);
        return fedCentroid;
    }
    
    /**
     * Associate a class centroid with an author
     * 
     * @param author 
     * @param centroid 
     */
    public void addCentroid(authors author, Centroid centroid){
        centroids.put(author, centroid);
    }
       
    public void classify(String fedPath){
                
        String hamPath = fedPath + "\\HAMILTON\\";
        String jayPath = fedPath + "\\JAY\\";
        String madPath = fedPath + "\\MADISON\\";
        String disputedPath = fedPath + "\\DISPUTED\\";
               
        // Index all folders
        IndexWriter masterWriter = new IndexWriter(fedPath);
        IndexWriter hamWriter = new IndexWriter(hamPath);
        IndexWriter jayWriter = new IndexWriter(jayPath);
        IndexWriter madWriter = new IndexWriter(madPath);
        
        masterWriter.buildIndex();
        hamWriter.buildIndex();
        jayWriter.buildIndex();
        madWriter.buildIndex();
        
        DiskInvertedIndex masterIndex = new DiskInvertedIndex(fedPath);
        DiskInvertedIndex hamIndex = new DiskInvertedIndex(hamPath);
        DiskInvertedIndex jayIndex = new DiskInvertedIndex(jayPath);
        DiskInvertedIndex madIndex = new DiskInvertedIndex(madPath);
        
        // Store dictionary of masterIndex to use for calculating Euclidian distance
        String[] masterDictionary = masterIndex.getDictionary();
                      
        // Generate centroids for all known classes
        Centroid hamCentroid = calculateCentroid(hamIndex);
        Centroid jayCentroid = calculateCentroid(jayIndex);
        Centroid madCentroid = calculateCentroid(madIndex);
        
        addCentroid(authors.HAMILTON, hamCentroid);
        addCentroid(authors.JAY, jayCentroid);
        addCentroid(authors.MADISON, madCentroid);
        
        for (int i = 0; i < 30; i++){
            System.out.println("Master Vocabulary Term " + i + " - " + masterDictionary[i]);
        }
        
        System.out.println("");
         
        String[] hamComponents = new String[hamCentroid.getCentroid().keySet().size()];
        hamCentroid.getCentroid().keySet().toArray(hamComponents);

        for (int i = 0; i < 30; i++){
            System.out.println("Hamilton Component " + i + " - " + 
                    hamComponents[i] + ": " + hamCentroid.getCentroid().get(hamComponents[i]));
        }
        
        System.out.println("");
        
        String[] madComponents = new String[madCentroid.getCentroid().keySet().size()];
        madCentroid.getCentroid().keySet().toArray(madComponents);

        for (int i = 0; i < 30; i++){
            System.out.println("Madison Component " + i + " - " + 
                    madComponents[i] + ": " + madCentroid.getCentroid().get(madComponents[i]));
        }
        
        System.out.println("");
               
        String[] jayComponents = new String[jayCentroid.getCentroid().keySet().size()];
        jayCentroid.getCentroid().keySet().toArray(jayComponents);

        for (int i = 0; i < 30; i++){
            System.out.println("Jay Component " + i + " - " + 
                    jayComponents[i] + ": " + jayCentroid.getCentroid().get(jayComponents[i]));
        }
        
        System.out.println("");
        
        
        // C.D. Manning et al., Introduction to Information Retrieval, Online Edition
        //Cambridge University Press, 2009, p. 295;
        //https://nlp.stanford.edu/IR-book/pdf/irbookonlinereading.pdf
        
        //https://docs.oracle.com/javase/7/docs/api/java/io/File.html#listFiles()
        File folder = new File(disputedPath);
        File[] files = folder.listFiles();
               
        for (File file : files){
            
            PriorityQueue<RankedClass> pq = new PriorityQueue<RankedClass>();
            
            if (!file.getName().equals("Indexes")){

                try{
                    SingleFileIndex sfx = new SingleFileIndex(file);
                    String[] fileDictionary = sfx.getDictionary();
                    HashMap<String, Double> docTerms = new HashMap<String, Double>();
                    
                    for (String term : fileDictionary){
                        double docWeight = sfx.getDocWeight();
                        double termVector = calcWDT(sfx, term) / docWeight;               
                        docTerms.put(term, termVector);
                    }
                                        
                    System.out.println("");

                    double hamEuclideanDistance = getEuclideanDistance(masterDictionary, hamCentroid, docTerms);
                    pq.add(new RankedClass(hamEuclideanDistance, authors.HAMILTON));

                    double jayEuclideanDistance = getEuclideanDistance(masterDictionary, jayCentroid, docTerms);
                    pq.add(new RankedClass(jayEuclideanDistance, authors.JAY));

                    double madEuclideanDistance = getEuclideanDistance(masterDictionary, madCentroid, docTerms);
                    pq.add(new RankedClass(madEuclideanDistance, authors.MADISON));    

                    RankedClass bestClass = pq.poll();
                    RankedClass secondBest = pq.poll();
                    RankedClass thirdBest = pq.poll();
                    
                    System.out.println("**** File: " + file.getName() + " ****");
                    
                    System.out.println("Class designation: " + bestClass.getAuthor());
                    System.out.println("Euclidean distance: " + bestClass.getCentroid());
                    System.out.println(" ");
                    
                    System.out.println("Second Place class designation: " + secondBest.getAuthor());
                    System.out.println("Euclidean distance: " + secondBest.getCentroid());
                    System.out.println(" ");
                    
                    System.out.println("Third Place designation : " + thirdBest.getAuthor());
                    System.out.println("Euclidean distance: " + thirdBest.getCentroid());
                    System.out.println(" ");
                    
                    pq.clear();
                    
                    if (file.getName().equals("paper_52.txt")){
                        String[] paper52Components = new String[fileDictionary.length];
                        docTerms.keySet().toArray(paper52Components);
                    
                        for (int i = 0; i < 30; i++){
                            System.out.println("paper_52 component " + i + " - " + 
                                    paper52Components[i] + ": " + docTerms.get(paper52Components[i]));
                        }
                    }
                }
                catch(FileNotFoundException e){
                    
                }
            }
        }
    }
    
    
}
