package indexes.diskPart;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

public class DiskSoundexIndex {

    private RandomAccessFile mVocabList;
    private RandomAccessFile mPostings;
    private RandomAccessFile mWeightList;
    private long[] mVocabTable;
    private List<String> mFileNames;
    private int mCorpusSize;
    private List<String> terms = null;
    private String[] dic;

    // Opens a disk inverted index that was constructed in the given path.
    public DiskSoundexIndex(String path) {
        try {
            String pathIndexes = path + "//Indexes";
            
            mVocabList = new RandomAccessFile(new File(pathIndexes, "sVocab.bin"), "r");
            mPostings = new RandomAccessFile(new File(pathIndexes, "soundex.bin"), "r");
            mVocabTable = readVocabTable(pathIndexes);
            mFileNames = readFileNames(path);
            mCorpusSize = readCorpusSize(pathIndexes);
        } catch (FileNotFoundException ex) {
            System.out.println(ex.toString());
        }
    }

    private static List<Integer> readPostingsFromFile(RandomAccessFile postings,      
    //private static DiskPosting[] readPostingsFromFile(RandomAccessFile postings,
            long postingsPosition) {
        try {
            // seek to the position in the file where the postings start.
            postings.seek(postingsPosition);

            // read the 4 bytes for the document frequency
            byte[] buffer = new byte[4];
            postings.read(buffer, 0, buffer.length);

            // use ByteBuffer to convert the 4 bytes into an int.
            int documentFrequency = ByteBuffer.wrap(buffer).getInt();
            
            // initialize the array that will hold the postings.
            List<Integer> diskPostings = new ArrayList<Integer>(documentFrequency);
           
            // write the following code:
            //
            // read 4 bytes at a time from the file, until you have read as many
            // postings as the document frequency promised.
            //    
            // after each read, convert the bytes to an int posting. this value
            // is the GAP since the last posting. decode the document ID from
            // the gap and put it in the array.
            //
            // repeat until all postings are read.
            int lastDocId = 0;
            for (int postingIndex = 0; postingIndex < documentFrequency; postingIndex++) {

                // read the 4 bytes for the docId; add lastDocId to decode gap
                postings.read(buffer, 0, buffer.length);
                int docId = ByteBuffer.wrap(buffer).getInt() + lastDocId;
                lastDocId = docId;

                // read the 4 bytes for the term frequency
                postings.read(buffer, 0, buffer.length);
                int termFrequency = ByteBuffer.wrap(buffer).getInt();

                diskPostings.add(docId);
            }

            return diskPostings;
        } catch (IOException ex) {
            System.out.println(ex.toString());
        }
        return null;
    }

    // Reads and returns a list of document IDs that contain the given term.
    public List<Integer> getPostings(String term) {
        long postingsPosition = binarySearchVocabulary(term);
        if (postingsPosition >= 0) {
            return readPostingsFromFile(mPostings, postingsPosition);
        }
        return null;
    }

    // Locates the byte position of the postings for the given term.
    private long binarySearchVocabulary(String term) {
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

    // Reads the file vocabTable.bin into memory.
    private static long[] readVocabTable(String indexName) {
        try {
            long[] vocabTable;

            RandomAccessFile tableFile = new RandomAccessFile(new File(indexName, "sVocabTable.bin"), "r");

            byte[] byteBuffer = new byte[4];
            tableFile.read(byteBuffer, 0, byteBuffer.length);

            int tableIndex = 0;
            vocabTable = new long[ByteBuffer.wrap(byteBuffer).getInt() * 2];
            byteBuffer = new byte[8];

            while (tableFile.read(byteBuffer, 0, byteBuffer.length) > 0) { // while we keep reading 4 bytes
                vocabTable[tableIndex] = ByteBuffer.wrap(byteBuffer).getLong();
                tableIndex++;
            }
            tableFile.close();
            return vocabTable;
        } catch (FileNotFoundException ex) {
            System.out.println(ex.toString());
        } catch (IOException ex) {
            System.out.println(ex.toString());
        }
        return null;
    }

     // Reads the file corpusSize.bin into memory.
    private static int readCorpusSize(String indexName) {
        
        int corpusSize = 0;
        
        try {
            RandomAccessFile corpusFile = new RandomAccessFile(new File(indexName, "corpusSize.bin"), "r");
            byte[] byteBuffer = new byte[4];
            
            corpusFile.read(byteBuffer);
            corpusSize = ByteBuffer.wrap(byteBuffer).getInt();
            corpusFile.close();
        } 
        catch (FileNotFoundException ex) {
            System.out.println(ex.toString());
        } 
        catch (IOException ex) {
            System.out.println(ex.toString());
        }
        finally{
            return corpusSize;
        }
    }
    
    public int getTermCount() {
        return mVocabTable.length / 2;
    }

    /**
     * Walk the file tree to get the names of the files
     *
     * @param path directory path
     * @return array of file names
     */
    private static List<String> readFileNames(String path) {
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

    public List<String> getFileNames() {
        return mFileNames;
    }   
    
    public int getCorpusSize(){
        return mCorpusSize;
    }
    
    public String[] getDictionary() {
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
        return vocabList.toArray(new String[0]);
    }
}