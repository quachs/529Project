package indexes.diskPart;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.*;
import indexes.SoundexIndex;

/**
 * Adaptation of DiskInvertedIndex to work for soundex indexes
 */
public class DiskSoundexIndex extends DiskIndex {

    /**
     * Opens a disk soundex index that was constructed in the given path.
     * @param path 
     */
    public DiskSoundexIndex(String path) {
        try {
            String pathIndexes = path + "//Indexes";
            mVocabList = new RandomAccessFile(new File(pathIndexes, "sVocab.bin"), "r");
            mPostings = new RandomAccessFile(new File(pathIndexes, "soundex.bin"), "r");
            mVocabTable = readVocabTable(pathIndexes);
            mFileNames = readFileNames(path);
        } catch (FileNotFoundException ex) {
            System.out.println(ex.toString());
        }
    }

    /**
     * Read the postings from file
     * @param postings
     * @param postingsPosition
     * @return list of disk postings
     */
    private static List<Integer> readPostingsFromFile(RandomAccessFile postings,      
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

                diskPostings.add(docId);
            }

            return diskPostings;
        } catch (IOException ex) {
            System.out.println(ex.toString());
        }
        return null;
    }

    /**
     * Reads and returns a list of document IDs that contain the given term.
     * @param term
     * @return list of disk postings, null if the term is not found
     */
    public List<Integer> getPostings(String term) {
        SoundexIndex reducer = new SoundexIndex();
        String sTerm = reducer.reduceToSoundex(term);
        long postingsPosition = binarySearchVocabulary(sTerm);
        if (postingsPosition >= 0) {
            return readPostingsFromFile(mPostings, postingsPosition);
        }
        return null;
    }

    /**
     * Reads the file vocabTable.bin into memory.
     * @param indexName
     * @return byte position of vocabulary
     */
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
    
}