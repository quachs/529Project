package Indexes.diskPart;

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
import Helper.JsonDocument;
import Helper.PorterStemmer;
import Helper.SimpleTokenStream;
import Helper.TokenProcessorStream;
import Indexes.KGramIndex;
import Indexes.PositionalInvertedIndex;
import Indexes.PositionalPosting;
import Indexes.SoundexIndex;

/**
 * Writes an inverted indexing of a directory to disk.
 */
public class IndexWriter {

    private String mFolderPath;
    // the indices
    final static PositionalInvertedIndex index = new PositionalInvertedIndex();
    final static KGramIndex kgIndex = new KGramIndex();
    final static SoundexIndex sIndex = new SoundexIndex();
    // the set of vocabulary types in the corpus
    final static SortedSet<String> vocabTree = new TreeSet<String>();

    private static List<Map<String, Integer>> docTermFrequency; // term frequencies for a document
    private static List<Integer> docLength; // the number of tokens in each document
    private static List<Double> docByteSize; // byte size of each document

    /**
     * Constructs an IndexWriter object which is prepared to index the given
     * folder.
     */
    public IndexWriter(String folderPath) {
        mFolderPath = folderPath;
        File dir = new File(folderPath + "\\Indexes");
        // if the directory does not exist, create it
        if (!dir.exists()) {
            try {
                dir.mkdir();
            } catch (SecurityException se) {
                //handle it
            }
        }
        docTermFrequency = new ArrayList<Map<String, Integer>>();
        docLength = new ArrayList<Integer>();
        docByteSize = new ArrayList<Double>();
    }

    /**
     * Builds and writes an inverted index to disk. Creates three files:
     * vocab.bin, containing the vocabulary of the corpus; postings.bin,
     * containing the postings list of document IDs; vocabTable.bin, containing
     * a table that maps vocab terms to postings locations
     */
    public void buildIndex() {
        indexFile(Paths.get(mFolderPath), index);
        buildKGram(vocabTree, kgIndex);
        buildIndexForDirectory(index, mFolderPath);
        buildWeightFile(mFolderPath);
    }

    private static void buildKGram(SortedSet<String> vocabTree, KGramIndex kgIndex) {
        // iterate the vocab tree to build the kgramindex
        Iterator<String> iter = vocabTree.iterator();
        while (iter.hasNext()) {
            kgIndex.addType(iter.next());
        }
    }

    /**
     * Walk through all .json files in a directory and subdirectory and call a
     * helper method to do the indexing.
     *
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
                        // get the number of bytes in the file and add to list
                        double size = file.toFile().length();
                        docByteSize.add(size);
                        // do the indexing
                        indexFile(file.toFile(), index, vocabTree, sIndex, mDocumentID);
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
     *
     * @param file the file to be indexed
     * @param index the positional inverted index
     * @param docID document ID
     */
    private static void indexFile(File file, PositionalInvertedIndex index,
            SortedSet vocabTree, SoundexIndex sIndex, int docID) throws FileNotFoundException {
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
                        String term = PorterStemmer.getStem(proToken);
                        // add the processed and stemmed token to the inverted index
                        index.addTerm(term, docID, positionNumber);
                        // add the processed token to the vocab tree
                        vocabTree.add(proToken);
                        // get the frequency of the term and increment it
                        int termFrequency = docTermFrequency.get(docID).containsKey(term) ? docTermFrequency.get(docID).get(term) : 0;
                        docTermFrequency.get(docID).put(term, termFrequency + 1);
                    }

                }
                positionNumber++;
            }

            // process the author field of the document and add to soundex
            if (docAuthor != null) {
                s = new SimpleTokenStream(docAuthor);
                while (s.hasNextToken()) {
                    TokenProcessorStream t = new TokenProcessorStream(s.nextToken());
                    while (t.hasNextToken()) { // process the author's name
                        String name = t.nextToken();
                        if (name != null) {
                            sIndex.addToSoundex(name, docID);
                        }
                    }
                }
            }// add the number of tokens in the document to list
            docLength.add(positionNumber);

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
        folder = folder + "\\Indexes";
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
                System.out.println("Term: " + s);
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
                    System.out.println("Found Doc ID: " + lastDocId);
                    // write the term frequency
                    int termFrequency = p.getTermPositions().size();
                    byte[] termFreqBytes = ByteBuffer.allocate(4).putInt(termFrequency).array();
                    postingsFile.write(termFreqBytes, 0, termFreqBytes.length);
                    System.out.println("Found Term Frequency: " + termFrequency);
                    // write the positions (encode a gap, not a position)
                    int lastPos = 0;
                    for (Integer position : p.getTermPositions()) {
                        byte[] positionBytes = ByteBuffer.allocate(4).putInt(position - lastPos).array();
                        postingsFile.write(positionBytes, 0, positionBytes.length);
                        lastPos = position;
                        System.out.println("Found Position: " + lastPos);
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

    private void buildWeightFile(String folder) {
        FileOutputStream weightsFile = null;
        try {

            weightsFile = new FileOutputStream(new File(folder, "docWeights.bin"));
            for(int docId = 0; docId < docTermFrequency.size(); docId++) {

                double docWeight = 0; // L_d
                double aveTermFrequency = 0; // the average tf for the doc
                
                for (Integer termFrequency : docTermFrequency.get(docId).values()) {
                    double termWeight = 1 + (Math.log(termFrequency)); // w_d,t
                    docWeight += Math.pow(termWeight, 2); // increment by the term weight squared
                    aveTermFrequency += termFrequency;
                }
                
                // write the doc weight to file
                docWeight = Math.sqrt(docWeight);
                byte[] docWeightByte = ByteBuffer.allocate(8).putDouble(docWeight).array();
                weightsFile.write(docWeightByte, 0, docWeightByte.length);
                
                // **************************************************************
                // NEED FOR VARIANT FORMULAS
                // not written to file yet
                int length = docLength.get(docId); // number of tokens in the doc
                double byteSize = docByteSize.get(docId); // number of bytes in the doc
                aveTermFrequency /= length; // the average tf for the doc
                // **************************************************************
                
                /*
                byte[] docLengthByte = ByteBuffer.allocate(8).putDouble(docLength).array();
                weightsFile.write(docLengthByte, 0, docLengthByte.length);
                byte[] docSizeByte = ByteBuffer.allocate(8).putDouble(byteSize).array();
                weightsFile.write(docSizeByte, 0, docSizeByte.length);
                byte[] aveTfByte = ByteBuffer.allocate(8).putDouble(aveTermFrequency).array();
                weightsFile.write(aveTfByte, 0, aveTfByte.length);
                */
            }
            
            weightsFile.close();
        } catch (FileNotFoundException ex) {
        } catch (UnsupportedEncodingException ex) {
            System.out.println(ex.toString());
        } catch (IOException ex) {
        } finally {
            try {
                weightsFile.close();
            } catch (IOException ex) {
            }
        }

    }
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private static void buildSoundexFile() {

    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private static void buildKgramFile() {

    }
}