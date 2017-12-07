package classification;

import indexes.NaiveInvertedIndex;
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
import java.util.PriorityQueue;
import java.util.SortedSet;
import java.util.TreeSet;

public class BayesTraining {

    private final List<TrainingDocument> trainingDocs;
    private final SortedSet<String> trainingTerms;
    private final List<String> discSet; // discriminating set
    private final List<Double> hamiltonTermProb; 
    private final List<Double> madisonTermProb; 
    private final List<Double> jayTermProb; 
    private int hamiltonCount; 
    private int madisonCount; 
    private int jayCount;

    
    
    public BayesTraining(String path, int k) {
        
        // Build the discriminating set
        trainingDocs = new ArrayList<TrainingDocument>();
        trainingTerms = new TreeSet<String>();
        NaiveInvertedIndex hIndex = new NaiveInvertedIndex(); // Hamilton index
        NaiveInvertedIndex mIndex = new NaiveInvertedIndex(); // Madison index
        NaiveInvertedIndex jIndex = new NaiveInvertedIndex(); // Jay index
        hamiltonCount = 0;
        madisonCount = 0;
        jayCount = 0;
        indexFile(Paths.get(path + "\\HAMILTON"), hIndex);
        indexFile(Paths.get(path + "\\MADISON"), mIndex);
        indexFile(Paths.get(path + "\\JAY"), jIndex);
        discSet = getDiscSet(k);
        
        // Save the term probabilities for each class
        hamiltonTermProb = new ArrayList<Double>(k);
        madisonTermProb = new ArrayList<Double>(k);
        jayTermProb = new ArrayList<Double>(k);
        calcTermProb(discSet, hIndex, hamiltonTermProb);
        calcTermProb(discSet, mIndex, madisonTermProb);
        calcTermProb(discSet, jIndex, jayTermProb);
        
    }

    /**
     * From the discriminating set, save the term probabilities for a class
     * @param discSet the discriminating set
     * @param index of the class
     * @param termProb map of a term to its probability
     */
    private void calcTermProb(List<String> discSet, NaiveInvertedIndex index, List<Double> termProb) {
        
        int totalTf = 0; // total number of ocurrences in the class
        
        // Save (tf + 1) for each term in the class
        for(int i = 0; i < discSet.size(); i++){
            double tf = 0;
            if(index.getPostings(discSet.get(i)) != null){
                tf = index.getPostings(discSet.get(i)).size();
                totalTf += tf;
            }
            termProb.add(tf + 1);
        }

        // Divide to get the probability - update list
        for(int i = 0; i < discSet.size(); i++){
            termProb.set(i, termProb.get(i) / (totalTf + discSet.size()));
        }

    }

    /**
     * Get the discriminating set of terms from the training set
     *
     * @param k size of the discriminating set
     * @return discriminating set of terms
     */
    private List<String> getDiscSet(int k) {

        PriorityQueue<MutualInfo> miQueue = new PriorityQueue<MutualInfo>();

        for (Authors author : Authors.values()) { // iterate the classes
            for (String term : trainingTerms) { // iterate the terms

                int n00 = 0; // doc not in class and does not contain term
                int n01 = 0; // doc not in class and contains term
                int n10 = 0; // doc in class and does not contain term
                int n11 = 0; // doc in class and contains term

                for (TrainingDocument doc : trainingDocs) {
                    if (doc.getAuthor() == author) { // in the class
                        if (doc.getTerms().contains(term)) {
                            n11++;
                        } else {
                            n10++;
                        }

                    } else // not in the class
                    {
                        if (doc.getTerms().contains(term)) {
                            n01++;
                        } else {
                            n00++;
                        }
                    }
                }

                /*
                System.out.println(author + ":" + term);
                System.out.println("n00: " + n00);
                System.out.println("n01: " + n01);
                System.out.println("n10: " + n10);
                System.out.println("n11: " + n11);
                */
                
                double n = n00 + n01 + n10 + n11; // size of training set
                
                // add 1 for Laplace smoothing
                double f00 = (n * n00 + 1) / ((n00 + n01) * (n00 + n10) + 1);
                double f01 = (n * n01 + 1) / ((n00 + n01) * (n01 + n11) + 1);
                double f10 = (n * n10 + 1) / ((n10 + n11) * (n00 + n10) + 1);
                double f11 = (n * n11 + 1) / ((n10 + n11) * (n01 + n11) + 1);
                
                /*
                System.out.println("f00: " + f00);
                System.out.println("f01: " + f01);
                System.out.println("f10: " + f10);
                System.out.println("f11: " + f11);
                */
                
                // use change of base to get log base-2
                double score = (n00 / n) * (Math.log(f00) / Math.log(2))
                        + (n01 / n) * (Math.log(f01) / Math.log(2))
                        + (n10 / n) * (Math.log(f10) / Math.log(2))
                        + (n11 / n) * (Math.log(f11) / Math.log(2));

                /*
                MutualInfo mi = new MutualInfo(term, author, score);
                System.out.println(mi.toString());
                */
                miQueue.add(new MutualInfo(term, author, score));

            } // terms loop
        } // authors loop

        SortedSet<String> termSet = new TreeSet<String>();

        while (termSet.size() < k) {
            String term = miQueue.poll().getTerm();
            if (!termSet.contains(term)) {
                termSet.add(term);
            }
        }

        return new ArrayList<String>(termSet);
    }

    /**
     * File walk to to begin indexing the files in a folder/class
     *
     * @param path
     * @param index
     */
    private void indexFile(Path path, NaiveInvertedIndex index) {
        try {
            // This is our standard "walk through all .txt files" code.
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                int mDocumentID = 0;

                public FileVisitResult preVisitDirectory(Path dir,
                        BasicFileAttributes attrs) {
                    // make sure we only process the current working directory
                    if (path.equals(dir)) {
                        return FileVisitResult.CONTINUE;
                    }
                    return FileVisitResult.SKIP_SUBTREE;
                }

                public FileVisitResult visitFile(Path file,
                        BasicFileAttributes attrs) throws FileNotFoundException {
                    // only process .txt files
                    if (file.toString().endsWith(".txt")) {
                        indexFile(file.toFile(), mDocumentID, index);
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
        } catch (IOException ex) {
            System.out.println(ex.toString());
        }

    }

    /**
     * Indexing, instantiate a TrainingDocument object the document, and
     * collect the terms for the training set.
     *
     * @param file
     * @param docID
     * @param index
     * @throws FileNotFoundException
     */
    private void indexFile(File file, int docID, NaiveInvertedIndex index) throws FileNotFoundException {

        // Store the document terms for TrainingDocument object
        SortedSet<String> docTerms = new TreeSet<String>();

        ClassTokenStream s = new ClassTokenStream(file);
        while (s.hasNextToken()) {
            String term = s.nextToken();
            trainingTerms.add(term);
            index.addTerm(term, docID);
            docTerms.add(term);
        }
        
        // Increment the document count for the class
        switch (Authors.valueOf(file.getParentFile().getName())) {
            case HAMILTON:
                hamiltonCount++;
                break;
            case JAY:
                jayCount++;
                break;
            case MADISON:
                madisonCount++;
                break;
        }

        trainingDocs.add(new TrainingDocument(Authors.valueOf(file.getParentFile().getName()), docTerms));

    }

    /**
     * @return the hTermProb
     */
    public List<Double> getHamiltonProb() {
        return hamiltonTermProb;
    }

    /**
     * @return the mTermProb
     */
    public List<Double> getMadisonProb() {
        return madisonTermProb;
    }

    /**
     * @return the jTermProb
     */
    public List<Double> getJayProb() {
        return jayTermProb;
    }

    /**
     * @return the hDocCount
     */
    public int getHamiltonCount() {
        return hamiltonCount;
    }

    /**
     * @return the mDocCount
     */
    public int getMadisonCount() {
        return madisonCount;
    }

    /**
     * @return the jDocCount
     */
    public int getJayCount() {
        return jayCount;
    }

    /**
     * @return the discSet
     */
    public List<String> getDiscSet() {
        return discSet;
    }

}
