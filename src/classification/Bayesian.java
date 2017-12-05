package classification;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.SortedSet;
import java.util.TreeSet;

public class Bayesian {
    
    /**
     * Get the discriminating set of terms from the training set
     * @param trainingDocs list of training documents
     * @param trainingTerms terms in the training set
     * @param k size of the discriminating set
     * @return discriminating set of terms
     */
    public static List<String> getDiscSet(List<TrainingDocument> trainingDocs,
            SortedSet<String> trainingTerms, int k) {

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
    
}
