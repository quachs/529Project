package classification;

import java.io.*;

public class ClassifierEngine {
    
    
    public static void main(String[] args) throws IOException {
        
        /*
    
        Hamilton: 51
        Jay: 5
        Madison: 15

        As k increases, results lean towards Hamilton due to the higher volume 
        of Hamilton docs in the training set -> higher probability

        */
        
        System.out.println("==============================");
        System.out.println("BAYESIAN CLASSIFICATION");
        System.out.println("==============================\n");
        
        // Classification with a discriminating set size of 50
        System.out.println("***** DISCSIZE: 50 *****\n");
        BayesianClassifier c50 = new BayesianClassifier("C:\\Users\\t420\\Documents\\federalist-papers", 50);
        c50.runClassifier();
        
        // Classification with a discriminating set size of 100
        System.out.println("***** DISCSIZE: 100 *****\n");
        BayesianClassifier c100 = new BayesianClassifier("C:\\Users\\t420\\Documents\\federalist-papers", 100);
        c100.runClassifier();
        
        // Classification with a discriminating set size of 3935
        System.out.println("***** DISCSIZE: 3935 *****\n");
        BayesianClassifier c3935 = new BayesianClassifier("C:\\Users\\t420\\Documents\\federalist-papers", 3935);
        c3935.runClassifier();
        
        
    }

    

}
