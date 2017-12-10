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

public class BayesianClassifier {

    private final List<String> discSet; // discriminating set
    private final List<TrainingDocument> trainingDocs;
    private final SortedSet<String> trainingTerms;
    
    // the index of the list corresponds to terms in the discriminating set
    private final List<Double> hamiltonTermProb;
    private final List<Double> jayTermProb;
    private final List<Double> madisonTermProb;
    private int hamiltonCount;
    private int jayCount;
    private int madisonCount;
    
    private final NaiveInvertedIndex disputedIndex;
    private final List<String> disputedFileNames;

    public BayesianClassifier(String path, int k) {

        // Build the discriminating set
        trainingDocs = new ArrayList<TrainingDocument>();
        trainingTerms = new TreeSet<String>();
        NaiveInvertedIndex hamiltonIndex = new NaiveInvertedIndex();
        NaiveInvertedIndex jayIndex = new NaiveInvertedIndex();
        NaiveInvertedIndex madisonIndex = new NaiveInvertedIndex();
        hamiltonCount = 0;
        jayCount = 0;
        madisonCount = 0;
        indexFile(Paths.get(path + "\\HAMILTON"), hamiltonIndex);
        indexFile(Paths.get(path + "\\JAY"), jayIndex);
        indexFile(Paths.get(path + "\\MADISON"), madisonIndex);
        discSet = getDiscSet(k);

        // Save the term probabilities for each class
        hamiltonTermProb = new ArrayList<Double>(k);
        madisonTermProb = new ArrayList<Double>(k);
        jayTermProb = new ArrayList<Double>(k);
        calcTermProb(hamiltonIndex, hamiltonTermProb);
        calcTermProb(madisonIndex, madisonTermProb);
        calcTermProb(jayIndex, jayTermProb);

        // Index the disputed documents
        disputedFileNames = new ArrayList<String>();
        disputedIndex = new NaiveInvertedIndex();
        indexFile(Paths.get(path + "\\DISPUTED"), disputedIndex);
        
    }

    public void runClassifier() {

        double n = trainingDocs.size();

        for (int docId = 0; docId < disputedFileNames.size(); docId++) {

            // Probability of the doc being in the class
            double hamiltonProb = Math.log(hamiltonCount / n);
            double jayProb = Math.log(jayCount / n);
            double madisonProb = Math.log(madisonCount / n);

            for (int termIndex = 0; termIndex < discSet.size(); termIndex++) {
                
                // Add the log of term probability to the corresponding class
                if (disputedIndex.getPostings(discSet.get(termIndex)) != null) {
                    if (disputedIndex.getPostings(discSet.get(termIndex)).contains(docId)) {
                        hamiltonProb += Math.log(hamiltonTermProb.get(termIndex));
                        jayProb += Math.log(jayTermProb.get(termIndex));
                        madisonProb += Math.log(madisonTermProb.get(termIndex));
                    }
                }
            }

            // Print the results
            System.out.print(disputedFileNames.get(docId)+ " -> ");
            System.out.println(maxProb(new ClassProb(Authors.HAMILTON, hamiltonProb),
                                    new ClassProb(Authors.JAY, jayProb), 
                                    new ClassProb(Authors.MADISON, madisonProb)));
            
            System.out.println("Hamilton: " + hamiltonProb);
            System.out.println("Jay: " + jayProb);
            System.out.println("Madison: " + madisonProb);
            System.out.println();
            
        }
    }

    /**
     * Return the class object with the higher probability
     * @param a
     * @param b
     * @return 
     */
    private ClassProb maxProb(ClassProb a, ClassProb b) {
        if(a.getProb() > b.getProb()) {
            return a;
        }
        return b;
    }
    
    /**
     * Return the class author with the highest probability
     * @param a
     * @param b
     * @param c
     * @return 
     */
    private Authors maxProb(ClassProb a, ClassProb b, ClassProb c) {
        return maxProb(maxProb(a,b), c).getAuthor();
    }

    /**
     * From the discriminating set, save the term probabilities for a class
     *
     * @param discSet the discriminating set
     * @param index of the class
     * @param termProb map of a term to its probability
     */
    private void calcTermProb(NaiveInvertedIndex index, List<Double> termProb) {

        int totalTf = 0; // total number of ocurrences in the class

        // Save tf for each term in the class
        for (int i = 0; i < discSet.size(); i++) {
            double tf = 0;
            if (index.getPostings(discSet.get(i)) != null) {
                tf = index.getPostings(discSet.get(i)).size();
                totalTf += tf;
            }
            termProb.add(tf + 1); // add 1 for smoothing
        }

        // Divide to get the probability - update list
        for (int i = 0; i < discSet.size(); i++) {
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
        double n = trainingDocs.size();

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

                    } else { // not in the class
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

                    // index the disputed documents
                    if (file.getParent().endsWith("DISPUTED")) {
                        if (file.toString().endsWith(".txt")) {
                            disputedFileNames.add(file.getFileName().toString());
                            indexDisputedFile(file.toFile(), mDocumentID, index);
                            mDocumentID++;
                        }
                    } else if (file.toString().endsWith(".txt")) {
                        // process the training documents
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
     * Index, instantiate a TrainingDocument object for the file, and collect
     * the terms for the training set.
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
     * Index the disputed file
     *
     * @param file
     * @param docID
     * @param index
     * @throws FileNotFoundException
     */
    private void indexDisputedFile(File file, int docID, NaiveInvertedIndex index) throws FileNotFoundException {
        ClassTokenStream s = new ClassTokenStream(file);
        while (s.hasNextToken()) {
            index.addTerm(s.nextToken(), docID);
        }

    }

}
