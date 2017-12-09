/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package classification;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import helper.JsonDocument;
import helper.PorterStemmer;
import indexes.Index;
import indexes.KGramIndex;
import indexes.PositionalInvertedIndex;
import indexes.PositionalPosting;
import indexes.SoundexIndex;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import token.SimpleTokenStream;
import token.TokenProcessorStream;

/**
 *
 * @author Sean
 */
public class SingleFileIndex extends Index<Integer>{
    
    private File mFile;
    private List<Map<String, Integer>> docTermFrequency; // term frequencies for a document
    private int docLength; // the number of tokens in each document
    private List<Double> docByteSize; // byte size of each document
    private int corpusSize;

    /**
     * Constructs an IndexWriter object which is prepared to index the given
     * folder.
     *
     * @param folderPath Folder where to write the index
     */
    public SingleFileIndex(File file) throws FileNotFoundException {
        
        mIndex = new HashMap<String, List<Integer>>();
        SortedSet<String> vocabTree = new TreeSet<String>();
        mFile = file; // save the path
        docTermFrequency = new ArrayList<Map<String, Integer>>();
        docByteSize = new ArrayList<Double>();
        
        indexFile(mFile, vocabTree);
    }
    
    public void addTerm(String term, int position) {
        if (mIndex.containsKey(term)) { // the index contains the term
            List<Integer> postingsList = mIndex.get(term);
            postingsList.add(position);
        } else { // add the term and new posting to the index
            mIndex.put(term, new ArrayList<Integer>());
            mIndex.get(term).add(position);
        }
    }
    
    /**
     * Get a list of docIDs for the given term
     *
     * @param term
     * @return list of docIDs
     */
    public List<Integer> getPostings(String term) {
        List<Integer> docList = new ArrayList<Integer>();
        if (mIndex.get(term) != null){
            docList = mIndex.get(term);
        }
        return docList;
    }
    
    public double getDocumentFrequency(String term){
        List<Integer> docList = getPostings(term);
        if (docList != null){
            return (double) docList.size();
        }
        else {
            return 0.0;
        }
    }
    
    public double getDocLength(){
        return (double) docLength;
    }
    
    public double getDocWeight(){
        
        double docWeight = 0; // L_d
        
        for (String term : getDictionary()) {
            double termWeight = 1 + (Math.log(getDocumentFrequency(term))); // w_d,t
            docWeight += Math.pow(termWeight, 2); // increment by the term weight squared
        }
        docWeight = Math.sqrt(docWeight);
        return docWeight;
    }

    private int indexFile(File file, SortedSet vocabTree) throws
            FileNotFoundException {
                
        // add the document to the list for tracking terms
            docTermFrequency.add(new HashMap<String, Integer>());

            // process the body field of the document
            SimpleTokenStream s = new SimpleTokenStream(file);
            int positionNumber = 0;
            while (s.hasNextToken()) {
                TokenProcessorStream t = new TokenProcessorStream(s.nextToken());
                while (t.hasNextToken()) {
                    String proToken = t.nextToken(); // the processed token
                    if (proToken != null) {
                        String term = PorterStemmer.getStem(proToken);
                        // add the term to the inverted index
                        addTerm(term, positionNumber);
                        // add the processed token to the vocab tree
                        vocabTree.add(proToken);

                    }
                }
            positionNumber++;
            }
            
            // increment the corpus size
            corpusSize++;

            // add the number of tokens in the document to list
            docLength++;

        return corpusSize;
    }    
}
