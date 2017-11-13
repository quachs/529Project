package indexes.diskPart;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.*;

/**
 * Positional inverted index on disk
 */
public class DiskInvertedIndex extends DiskIndex {
    
    private RandomAccessFile mWeightList;
    private int mCorpusSize;

    /**
     * Opens a disk inverted index that was constructed in the given path.
     * @param path 
     */
    public DiskInvertedIndex(String path) {
        try {
            String pathIndexes = path + "//Indexes";
            mVocabList = new RandomAccessFile(new File(pathIndexes, "vocab.bin"), "r");
            mPostings = new RandomAccessFile(new File(pathIndexes, "postings.bin"), "r");
            mWeightList = new RandomAccessFile(new File(pathIndexes, "docWeights.bin"), "r");
            mVocabTable = readVocabTable(pathIndexes);
            mFileNames = readFileNames(path);
            mCorpusSize = readCorpusSize(pathIndexes);
        } catch (FileNotFoundException ex) {
            System.out.println(ex.toString());
        }
    }

    /**
     * Read the postings from file
     * @param postings
     * @param postingsPosition
     * @param withPositions
     * @return list of disk postings
     */
    private static List<DiskPosting> readPostingsFromFile(RandomAccessFile postings,
            long postingsPosition, boolean withPositions) {
        try {
            // seek to the position in the file where the postings start.
            postings.seek(postingsPosition);

            // read the 4 bytes for the document frequency
            byte[] buffer = new byte[4];
            postings.read(buffer, 0, buffer.length);

            // use ByteBuffer to convert the 4 bytes into an int.
            int documentFrequency = ByteBuffer.wrap(buffer).getInt();

            // initialize the array that will hold the postings.
            List<DiskPosting> diskPostings = new ArrayList<DiskPosting>(documentFrequency);

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

                if (withPositions) {
                    // the positions for the document
                    List<Integer> positions = new ArrayList<Integer>(termFrequency);
                    int lastPosition = 0;
                    for (int positionIndex = 0; positionIndex < termFrequency; positionIndex++) {

                        // read the 4 bytes for the positions; add lastPosition to decode gap
                        postings.read(buffer, 0, buffer.length);
                        int position = ByteBuffer.wrap(buffer).getInt() + lastPosition;
                        positions.add(position);
                        lastPosition = position;
                    }
                    diskPostings.add(new DiskPosting(docId, termFrequency, positions));
                } else {
                    postings.skipBytes(4 * termFrequency); // skip over the positions
                    diskPostings.add(new DiskPosting(docId, termFrequency));
                }
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
    public List<DiskPosting> getPostings(String term) {
        long postingsPosition = binarySearchVocabulary(term);
        if (postingsPosition >= 0) {
            return readPostingsFromFile(mPostings, postingsPosition, false);
        }
        return null;
    }

    /**
     * Reads and returns a list of document IDs, term frequencies, 
     * and positions that contain the given term. For use with phrase queries.
     * @param term
     * @return list of disk postings, null if the term is not found
     */
    public List<DiskPosting> getPostingsWithPositions(String term) {
        long postingsPosition = binarySearchVocabulary(term);
        if (postingsPosition >= 0) {
            return readPostingsFromFile(mPostings, postingsPosition, true);
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

            RandomAccessFile tableFile = new RandomAccessFile(new File(indexName, "vocabTable.bin"), "r");

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

    /**
     * Reads the file corpusSize.bin into memory.
     * @param indexName
     * @return corpus size
     */
    private int readCorpusSize(String indexName) {

        int corpusSize = 0;

        try {
            RandomAccessFile corpusFile = new RandomAccessFile(new File(indexName, "corpusSize.bin"), "r");
            byte[] byteBuffer = new byte[4];

            corpusFile.read(byteBuffer);
            corpusSize = ByteBuffer.wrap(byteBuffer).getInt();
            corpusFile.close();
        } catch (FileNotFoundException ex) {
            System.out.println(ex.toString());
        } catch (IOException ex) {
            System.out.println(ex.toString());
        } finally {
            return corpusSize;
        }
    }

    /**
     * Read the document weight from the weight file
     * @param docId
     * @return document weight
     */
    public Double getDocWeight(int docId) {
        try {
            mWeightList.seek(docId * 32);
            byte[] buffer = new byte[8];
            mWeightList.read(buffer, 0, buffer.length);
            return ByteBuffer.wrap(buffer).getDouble();
        } catch (IOException ex) {
            System.out.println(ex.toString());
        }
        return null;
    }

    /**
     * Read the document length from the weight file
     * @param docId
     * @return document length
     */
    public Double getDocLength(int docId) {
        try {
            mWeightList.seek((docId * 32) + 8);
            byte[] buffer = new byte[8];
            mWeightList.read(buffer, 0, buffer.length);
            return ByteBuffer.wrap(buffer).getDouble();
        } catch (IOException ex) {
            System.out.println(ex.toString());
        }
        return null;
    }

    /**
     * Read the document byte size from the weight file
     * @param docId
     * @return document byte size
     */
    public Double getDocSize(int docId) {
        try {
            mWeightList.seek((docId * 32) + 16);
            byte[] buffer = new byte[8];
            mWeightList.read(buffer, 0, buffer.length);
            return ByteBuffer.wrap(buffer).getDouble();
        } catch (IOException ex) {
            System.out.println(ex.toString());
        }
        return null;
    }

    /**
     * Read the average term frequency from the weight file
     * @param docId
     * @return document term frequency
     */
    public Double getAvgTermFrequency(int docId) {
        try {
            mWeightList.seek((docId * 32) + 24);
            byte[] buffer = new byte[8];
            mWeightList.read(buffer, 0, buffer.length);
            return ByteBuffer.wrap(buffer).getDouble();
        } catch (IOException ex) {
            System.out.println(ex.toString());
        }
        return null;
    }

    /**
     * Read the average document length of the corpus from the weight file
     * @return average document length of the corpus
     */
    public Double getAvgDocLength() {
        try {
            mWeightList.seek(mCorpusSize * 32);
            byte[] buffer = new byte[8];
            mWeightList.read(buffer, 0, buffer.length);
            return ByteBuffer.wrap(buffer).getDouble();
        } catch (IOException ex) {
            System.out.println(ex.toString());
        }
        return null;
    }

    public int getCorpusSize() {
        return mCorpusSize;
    }

}
