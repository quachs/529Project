package threads;

import formulas.FormEnum;
import query.Subquery;
import indexes.KGramIndex;
import indexes.PositionalInvertedIndex;
import indexes.SoundexIndex;
import indexes.diskPart.DiskInvertedIndex;
import retrivals.booleanRetrival.BooleanRetrival;
import retrivals.rankedRetrival.RankedItem;
import retrivals.rankedRetrival.RankedRetrieval;
import java.util.ArrayList;
import java.util.Date;
import javax.swing.*;

/**
 * Task to process in backround the walking trough all vocabulary of the corpus.
 * There are three ways that generating task is needed: 1. Boolean retrival
 * results 2. Print all vocabulary 3. Ranked retrival results
 *
 * @author Sandra
 */
public class GeneratingTask implements Runnable {

    private long timer; // time for printing how long task took
    private GeneratingOpportunities opportunities;
    private ArrayList<JLabel> labels; // arraylist of labels
    private String allTerms; // result string for all voc

    private String[] dic;

    private boolean searchType;
    private KGramIndex kgIndex;
    private SoundexIndex sIndex;
    private String query;
    private DiskInvertedIndex dIndex;
    private ArrayList<String> resultsBool;
    private Subquery q;
    private int k;
    private ThreadFinishedCallBack callback;
    private RankedItem[] resultsRank;
    private FormEnum form;

    /**
     * Constructor for making a ranked retrival
     *
     * @param dIndex
     * @param kgIndex
     * @param query
     * @param k
     */
    public GeneratingTask(DiskInvertedIndex dIndex, KGramIndex kgIndex, Subquery query, int k, ThreadFinishedCallBack finish, FormEnum form) {
        opportunities = GeneratingOpportunities.RANKED;
        this.dIndex = dIndex;
        this.kgIndex = kgIndex;
        this.q = query;
        this.k = k;
        this.callback = finish;
        this.form = form;
    }

    /**
     * Constructor for making boolean retrival
     */
    public GeneratingTask(String query, DiskInvertedIndex dIndex, boolean searchType, KGramIndex kIndex, SoundexIndex sIndex, ThreadFinishedCallBack finish) {
        opportunities = GeneratingOpportunities.BOOLEAN;
        this.query = query;
        this.kgIndex = kIndex;
        this.dIndex = dIndex;
        this.sIndex = sIndex;
        this.searchType = searchType;
        this.callback = finish;
    }

    /**
     * Constructor for getting all terms of a corpus and print them.
     *
     * @param elements
     */
    public GeneratingTask(String[] dictionary, ThreadFinishedCallBack finish) {
        opportunities = GeneratingOpportunities.ALL;
        this.dic = dictionary;
        this.callback = finish;
    }

    /*
    /**
     * With this parameter we know that we don´t have to create labels.
     * A string for representing it in an text area is enough.
     * This saves time!
     * @param docNames Array of vocabulary
     *
    public GeneratingTask(String[] docArray) {
        this.docArray = docArray;
        this.docIds = new ArrayList<Integer>();
        this.labels = new ArrayList<JLabel>();
    }

    /**
     * With this parameter we need to create labels to later add mouse click listener
     * Documents can be open
     * @param docIds List of document IDs
     * @param docNames List of all names of all documents
     *
    public GeneratingTask(ArrayList<Integer> docIds, ArrayList<String> docNames) {
        this.docIds = docIds;
        this.fileNames = docNames;
        this.docArray = new String[0];
        this.labels = new ArrayList<JLabel>();
    }
     */
    // Getter
    public ArrayList<JLabel> getArray() {
        return labels;
    }

    public String getAllTerms() {
        return allTerms;
    }

    @Override
    public void run() {
        timer = new Date().getTime(); // start timer for printing out how long process took
        switch (opportunities) {
            case ALL:
                String res = "";
                for (String s : this.dic) {
                    res = res + s + "\n"; // add every single voc with a linebreak to the result string
                }
                this.allTerms = res;
                break;
            case BOOLEAN:
                //DiskPosting[] array = dIndex.getPostingsWithPositions(query);
                PositionalInvertedIndex p = new PositionalInvertedIndex();
                /*for(DiskPosting posting: array){
                    for(int position: posting.getPositions()){
                        p.addTerm(query, posting.getDocumentID(), position);
                    }
                }*/
                this.resultsBool = (ArrayList<String>) BooleanRetrival.booleanQuery(query, p, searchType, kgIndex, sIndex, (ArrayList<String>) dIndex.getFileNames());
                break;
            default:
                RankedRetrieval rank = new RankedRetrieval(dIndex, form);
                resultsRank = rank.rankedQuery(kgIndex, q, k);
                break;
        }
        System.out.println("Time for Generating process: " + (new Date().getTime() - timer)); // print time that process took
        callback.notifyThreadFinished();
    }

    public GeneratingOpportunities getOpportunities() {
        return opportunities;
    }

    public RankedItem[] getResultsRank() {
        return resultsRank;
    }

    public ArrayList<String> getResultsBool() {
        return resultsBool;
    }

}
