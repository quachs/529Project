package classification;

import java.io.*;

public class ClassifierEngine {
    
    
    public static void main(String[] args) throws IOException {
        
        String path = "C:\\Users\\t420\\Documents\\federalist-papers";
        
        System.out.println("==============================");
        System.out.println("BAYESIAN CLASSIFICATION");
        System.out.println("==============================\n");
        
        System.out.println("\n===== DISCSIZE: 10 =====\n");
        BayesianClassifier c10 = new BayesianClassifier(path , 10);
        c10.runClassifier();

        System.out.println("\n===== DISCSIZE: 50 =====\n");
        BayesianClassifier c50 = new BayesianClassifier(path, 50);
        c50.runClassifier();
        /*
        System.out.println("\n===== DISCSIZE: 100 =====\n");
        BayesianClassifier c100 = new BayesianClassifier(path, 100);
        c100.runClassifier();
        
        System.out.println("\n===== DISCSIZE: 3935 =====\n");
        BayesianClassifier c3935 = new BayesianClassifier(path, 3935);
        c3935.runClassifier();
        */
        
    }

    

}
