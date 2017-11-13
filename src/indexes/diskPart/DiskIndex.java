package indexes.diskPart;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

/**
 * Abstract super class for a disk index
 */
public abstract class DiskIndex {
    
    protected RandomAccessFile mVocabList;
    protected RandomAccessFile mPostings;
    protected long[] mVocabTable;
    protected List<String> mFileNames;
    protected List<String> terms = null;
    protected String[] dictionary;
    
    /**
     * Locates the byte position of the postings for the given term.
     * @param term
     * @return byte position of the posting
     */
    protected long binarySearchVocabulary(String term) {
        // do a binary search over the vocabulary, using the vocabTable and the file vocabList.
        int i = 0, j = mVocabTable.length / 2 - 1;
        while (i <= j) {
            try {
                int m = (i + j) / 2;
                long vListPosition = mVocabTable[m * 2];
                int termLength;
                if (m == mVocabTable.length / 2 - 1) {
                    termLength = (int) (mVocabList.length() - mVocabTable[m * 2]);
                } else {
                    termLength = (int) (mVocabTable[(m + 1) * 2] - vListPosition);
                }

                mVocabList.seek(vListPosition);

                byte[] buffer = new byte[termLength];
                mVocabList.read(buffer, 0, termLength);
                String fileTerm = new String(buffer, "ASCII");

                int compareValue = term.compareTo(fileTerm);
                if (compareValue == 0) {
                    // found it!
                    return mVocabTable[m * 2 + 1];
                } else if (compareValue < 0) {
                    j = m - 1;
                } else {
                    i = m + 1;
                }
            } catch (IOException ex) {
                System.out.println(ex.toString());
            }
        }
        return -1;
    }
    
     /**
     * Walk the file tree to get the names of the files
     * @param path directory path
     * @return array of file names
     */
    protected final List<String> readFileNames(String path) {
        List<String> fileNames = new ArrayList<String>();
        try {

            Files.walkFileTree(Paths.get(path), new SimpleFileVisitor<Path>() {

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    // process the current working directory and subdirectories
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file,
                        BasicFileAttributes attrs) throws FileNotFoundException {
                    // only process .json files
                    if (file.toString().endsWith(".json")) {
                        fileNames.add(file.toFile().getName()); // add to list
                    }
                    return FileVisitResult.CONTINUE;
                }

                // don't throw exceptions if files are locked/other errors occur
                @Override
                public FileVisitResult visitFileFailed(Path file, IOException e) {
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException ex) {
            System.out.println(ex.toString());
        }
        return fileNames;
    }
    
    /**
     * Get the terms of the index
     * @return array of index terms
     */
    public String[] getDictionary() {
        if (dictionary == null) {
            List<String> vocabList = new ArrayList<String>();
            int i = 0, j = mVocabTable.length / 2 - 1;
            while (i <= j) {
                try {
                    int termLength;
                    if (i == j) {
                        termLength = (int) (mVocabList.length() - mVocabTable[i * 2]);
                    } else {
                        termLength = (int) (mVocabTable[(i + 1) * 2] - mVocabTable[i * 2]);
                    }

                    byte[] buffer = new byte[termLength];
                    mVocabList.read(buffer, 0, termLength);
                    String term = new String(buffer, "ASCII");
                    vocabList.add(term);
                } catch (IOException ex) {
                    System.out.println(ex.toString());
                }
                i++;
            }
            dictionary = vocabList.toArray(new String[0]);
        }
        return dictionary;

    }
    
    public int getTermCount() {
        return mVocabTable.length / 2;
    }
    
    public List<String> getFileNames() {
        return mFileNames;
    }
    
}

