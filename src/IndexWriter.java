
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

/**
 * Writes an inverted indexing of a directory to disk.
 */
public class IndexWriter {

    private String mFolderPath;

    /**
     * Constructs an IndexWriter object which is prepared to index the given
     * folder.
     */
    public IndexWriter(String folderPath) {
        mFolderPath = folderPath;
    }

    /**
     * Builds and writes an inverted index to disk. Creates three files:
     * vocab.bin, containing the vocabulary of the corpus; postings.bin,
     * containing the postings list of document IDs; vocabTable.bin, containing
     * a table that maps vocab terms to postings locations
     */
    public void buildIndex() {     
        PositionalInvertedIndex index = new PositionalInvertedIndex();
        indexFile(Paths.get(mFolderPath), index);
        buildIndexForDirectory(index, mFolderPath);
    }
    
    /**
     * Walk through all .json files in a directory and subdirectory and
     * call a helper method to do the indexing.
     * @param path path of the directory
     * @param index the positional inverted index
     */
    private static void indexFile(Path path, PositionalInvertedIndex index) {
        try {
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                int mDocumentID = 0;
                
                @Override
                public FileVisitResult preVisitDirectory(Path dir,
                        BasicFileAttributes attrs) {
                    // process the current working directory and subdirectories
                    return FileVisitResult.CONTINUE;
                }
                
                @Override
                public FileVisitResult visitFile(Path file,
                        BasicFileAttributes attrs) throws IOException {
                    // only process .json files
                    if (file.toString().endsWith(".json")) {
                        //System.out.println("Indexing file " + file.getFileName());
                        //fileNames.add(file.getFileName().toString());
                        indexFile(file.toFile(), index, mDocumentID);
                        mDocumentID++;
                    }
                    return FileVisitResult.CONTINUE;
                }
                
                // don't throw exceptions if files are locked/other errors occur
                @Override
                public FileVisitResult visitFileFailed(Path file,
                        IOException e) {
                    
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException ex) {
            System.out.println(ex.toString());
        }
    }
    
    /**
     * Index a file by reading a series of tokens from the body of the file.
     * @param file the file to be indexed
     * @param index the positional inverted index
     * @param docID document ID 
     */
    private static void indexFile(File file, PositionalInvertedIndex index, int docID) {  
        try {
            // Gson object to read json file
            Gson gson = new Gson();
            JsonDocument doc;
            String docBody, docAuthor;
            
            // reader to parse the relevent parts of the document
            JsonReader reader = new JsonReader(new FileReader(file));
            doc = gson.fromJson(reader, JsonDocument.class);
            docBody = doc.getBody();
            docAuthor = doc.getAuthor();
            
            // process the body field of the document
            SimpleTokenStream s = new SimpleTokenStream(docBody);
            int positionNumber = 0;
            while (s.hasNextToken()) {
                TokenProcessorStream t = new TokenProcessorStream(s.nextToken());
                while (t.hasNextToken()) {
                    String proToken = t.nextToken(); // the processed token
                    if (proToken != null) {
                        // add the processed and stemmed token to the inverted index
                        index.addTerm(PorterStemmer.getStem(proToken), docID, positionNumber);
                        // add the processed token to the vocab tree
                        //vocabTree.add(proToken);
                    }                
                }
                positionNumber++;
            }
        } catch (FileNotFoundException ex) {
            System.out.println(ex.toString());
        }
        
    }

    /**
     * Builds the normal PositionalInvertedIndex for the folder.
     */
    private static void buildIndexForDirectory(PositionalInvertedIndex index, String folder) {
        // at this point, "index" contains the in-memory inverted index
        // now we save the index to disk, building three files: the postings index,
        // the vocabulary list, and the vocabulary table.

        // the array of terms
        String[] dictionary = index.getDictionary();
        // an array of positions in the vocabulary file
        long[] vocabPositions = new long[dictionary.length];

        buildVocabFile(folder, dictionary, vocabPositions);
        buildPostingsFile(folder, index, dictionary, vocabPositions);
    }

    /**
     * Builds the postings.bin file for the indexed directory, using the given
     * PositionalInvertedIndex of that directory.
     */
    private static void buildPostingsFile(String folder, PositionalInvertedIndex index,
            String[] dictionary, long[] vocabPositions) {

        FileOutputStream postingsFile = null;
        try {

            postingsFile = new FileOutputStream(new File(folder, "postings.bin"));

            // simultaneously build the vocabulary table on disk, mapping a term index to a
            // file location in the postings file.
            FileOutputStream vocabTable = new FileOutputStream(new File(folder, "vocabTable.bin"));

            // the first thing we must write to the vocabTable file is the number of vocab terms.
            byte[] tSize = ByteBuffer.allocate(4).putInt(dictionary.length).array();
            vocabTable.write(tSize, 0, tSize.length);

            int vocabI = 0;
            for (String s : dictionary) {
                // for each String in dictionary, retrieve its postings.
                List<PositionalPosting> postings = index.getPostingsList(s);

                // write the vocab table entry for this term: the byte location of the term in the vocab list file,
                // and the byte location of the postings for the term in the postings file.
                byte[] vPositionBytes = ByteBuffer.allocate(8).putLong(vocabPositions[vocabI]).array();
                vocabTable.write(vPositionBytes, 0, vPositionBytes.length);

                byte[] pPositionBytes = ByteBuffer.allocate(8).putLong(postingsFile.getChannel().position()).array();
                vocabTable.write(pPositionBytes, 0, pPositionBytes.length);

                // write the postings file for this term:
                // 1. document frequency for the term, df_t
                // 2. document id, encoded as gaps, id
                // 3. term frequency, tf_t,d
                // 4. positions, encoded as gaps, p
                byte[] docFreqBytes = ByteBuffer.allocate(4).putInt(postings.size()).array();
                postingsFile.write(docFreqBytes, 0, docFreqBytes.length);

                int lastDocId = 0;
                for (PositionalPosting p : postings) {

                    // write the document ID (encode a gap, not a doc ID)
                    byte[] docIdBytes = ByteBuffer.allocate(4).putInt(p.getDocumentID() - lastDocId).array();
                    postingsFile.write(docIdBytes, 0, docIdBytes.length);
                    lastDocId = p.getDocumentID();
                  
                    // write the term frequency
                    int termFrequency = p.getTermPositions().size();
                    byte[] termFreqBytes = ByteBuffer.allocate(4).putInt(termFrequency).array();
                    postingsFile.write(termFreqBytes, 0, termFreqBytes.length);

                    // write the positions (encode a gap, not a position)
                    int lastPos = 0;  
                    for (Integer position : p.getTermPositions()) {
                        byte[] positionBytes = ByteBuffer.allocate(4).putInt(position - lastPos).array();
                        postingsFile.write(positionBytes, 0, positionBytes.length);
                        lastPos = position;
                    }
                    
                }
                vocabI++;
            }
            vocabTable.close();
            postingsFile.close();
        } catch (FileNotFoundException ex) {
        } catch (IOException ex) {
        } finally {
            try {
                postingsFile.close();
            } catch (IOException ex) {
            }
        }
    }

    private static void buildVocabFile(String folder, String[] dictionary,
            long[] vocabPositions) {
        OutputStreamWriter vocabList = null;
        try {
            // first build the vocabulary list: a file of each vocab word concatenated together.
            // also build an array associating each term with its byte location in this file.
            int vocabI = 0;
            vocabList = new OutputStreamWriter(new FileOutputStream(new File(folder, "vocab.bin")), "ASCII");

            int vocabPos = 0;
            for (String vocabWord : dictionary) {
                // for each String in dictionary, save the byte position where that term will start in the vocab file.
                vocabPositions[vocabI] = vocabPos;
                vocabList.write(vocabWord); // then write the String
                vocabI++;
                vocabPos += vocabWord.length();
            }
        } catch (FileNotFoundException ex) {
            System.out.println(ex.toString());
        } catch (UnsupportedEncodingException ex) {
            System.out.println(ex.toString());
        } catch (IOException ex) {
            System.out.println(ex.toString());
        } finally {
            try {
                vocabList.close();
            } catch (IOException ex) {
                System.out.println(ex.toString());
            }
        }
    }
}
