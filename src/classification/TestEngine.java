package classification;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.util.*;

public class TestEngine {

    public static void main(String[] args) throws IOException {
        
        //final Path path = Paths.get("C:\\Users\\t420\\Documents\\federalist-papers");
        BayesTraining b = new BayesTraining("C:\\Users\\t420\\Documents\\federalist-papers", 30);
        
        System.out.println("H count: "+b.getHamiltonCount());
        System.out.println("M count: "+b.getMadisonCount());
        System.out.println("J count: "+b.getJayCount());
        
    }

    

}
