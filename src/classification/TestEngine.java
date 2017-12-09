package classification;

import java.io.*;

public class TestEngine {

    public static void main(String[] args) throws IOException {
        
        System.out.println("==============================");
        System.out.println("BAYESIAN CLASSIFICATION");
        System.out.println("==============================\n");
        
        // Classification with a discriminating set size of 50
        System.out.println("***** DISCSIZE: 50 *****\n");
        BayesianClassification c50 = new BayesianClassification("C:\\Users\\t420\\Documents\\federalist-papers", 50);
        c50.runClassifier();
        
        // Classification with a discriminating set size of 100
        System.out.println("***** DISCSIZE: 100 *****\n");
        BayesianClassification c100 = new BayesianClassification("C:\\Users\\t420\\Documents\\federalist-papers", 100);
        c100.runClassifier();
        
        // Classification with a discriminating set size of 200
        System.out.println("***** DISCSIZE: 200 *****\n");
        BayesianClassification c200 = new BayesianClassification("C:\\Users\\t420\\Documents\\federalist-papers", 200);
        c200.runClassifier();
        
        
    }

    

}
