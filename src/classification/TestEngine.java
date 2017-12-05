package classification;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.util.*;

public class TestEngine {

    public static void main(String[] args) throws IOException {

        final Path path = Paths.get("C:\\Users\\t420\\Documents\\federalist-papers");

        List<String> fileNames = new ArrayList<String>();
        List<TrainingDocument> trainingDocs = new ArrayList<TrainingDocument>();
        SortedSet<String> trainingTerms = new TreeSet<String>();

        indexFile(path, fileNames, trainingDocs, trainingTerms);

        
        List<String> discSet = Bayesian.getDiscSet(trainingDocs, trainingTerms, 10);
        System.out.println("Disc Set: "+discSet);

    }

    /**
     * Instantiate a TrainingDocument object for each document and collect
     * the terms from the training set.
     * @param path
     * @param fileNames
     * @param trainingDocs
     * @param trainingTerms 
     */
    private static void indexFile(Path path, List<String> fileNames,
            List<TrainingDocument> trainingDocs, SortedSet<String> trainingTerms) {
        try {
            // This is our standard "walk through all .txt files" code.
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                int mDocumentID = 0;

                public FileVisitResult preVisitDirectory(Path dir,
                        BasicFileAttributes attrs) {
                    return FileVisitResult.CONTINUE; // continues to subfolder
                }

                public FileVisitResult visitFile(Path file,
                        BasicFileAttributes attrs) throws FileNotFoundException {
                    // only process .txt files
                    // skip the disputed folder
                    if (!file.getParent().endsWith(Paths.get("C:\\Users\\t420\\Documents\\federalist-papers\\DISPUTED"))
                            && file.toString().endsWith(".txt")) {

                        fileNames.add(file.getFileName().toString());
                        indexFile(file.toFile(), mDocumentID, trainingDocs, trainingTerms);
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

    private static void indexFile(File file, int docID, List<TrainingDocument> trainingDocs, 
            SortedSet<String> trainingTerms) throws FileNotFoundException {

        SortedSet<String> terms = new TreeSet<String>();
        ClassTokenStream s = new ClassTokenStream(file);
        while (s.hasNextToken()) {
            String term = s.nextToken();
            trainingTerms.add(term);
            terms.add(term);
        }

        trainingDocs.add(new TrainingDocument(Authors.valueOf(file.getParentFile().getName()), docID, terms));

    }

}
