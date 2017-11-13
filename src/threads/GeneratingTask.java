package threads;

import formulas.FormEnum;
import query.Subquery;
import indexes.KGramIndex;
import indexes.diskPart.DiskSoundexIndex;
import indexes.diskPart.DiskInvertedIndex;
import retrievals.booleanRetrieval.BooleanRetrieval;
import retrievals.rankedRetrieval.RankedDocument;
import retrievals.rankedRetrieval.RankedRetrieval;
import query.parser.RankedParser;
import helper.SpellingCorrection;
import java.util.ArrayList;
import java.util.Date;
import javax.swing.*;

/**
 * Task to process in background the walking trough the corpus vocabulary.
 * There are three reasons why generating tasks in this fashion is necessary: 
 * 1. Boolean retrieval results, 2. to display all vocabulary 3. Ranked retrieval results
 *
 * @author Sandra
 */
public class GeneratingTask implements Runnable {

    private long timer; // Time for printing how long the task took
    private GeneratingOpportunities opportunities;
    private ArrayList<JLabel> labels; // ArrayList of labels
    private String allTerms; // Result string for all vocabulary
    private RankedRetrieval rank;

    private String[] dic;

    private boolean searchType;
    private KGramIndex kgIndex;
    private DiskSoundexIndex sIndex;
    private String query;
    private DiskInvertedIndex dIndex;
    private ArrayList<String> resultsBool;
    private Subquery q;
    private int k;
    private ThreadFinishedCallBack callback;
    private RankedDocument[] resultsRank;
    private FormEnum form;
    private SpellingCorrection spellCorrect;

    /**
     * Constructor for making a ranked retrieval.
     *
     * @param dIndex
     * @param kgIndex
     * @param query
     * @param k
     */
    public GeneratingTask(DiskInvertedIndex dIndex, KGramIndex kgIndex, String query, int k, ThreadFinishedCallBack finish, FormEnum form) {
        opportunities = GeneratingOpportunities.RANKED;
        this.dIndex = dIndex;
        this.kgIndex = kgIndex;
        this.query = query;
        this.k = k;
        this.callback = finish;
        this.form = form;
    }

    /**
     * Constructor for making boolean retrieval.
     */
    public GeneratingTask(String query, DiskInvertedIndex dIndex, boolean searchType, KGramIndex kIndex, DiskSoundexIndex sIndex, ThreadFinishedCallBack finish) {
        opportunities = GeneratingOpportunities.BOOLEAN;
        this.query = query;
        this.kgIndex = kIndex;
        this.dIndex = dIndex;
        this.sIndex = sIndex;
        this.searchType = searchType;
        this.callback = finish;
    }

    /**
     * Constructor for getting all terms of a corpus and printing them.
     *
     * @param elements
     */
    public GeneratingTask(String[] dictionary, ThreadFinishedCallBack finish) {
        opportunities = GeneratingOpportunities.ALL;
        this.dic = dictionary;
        this.callback = finish;
    }

    @Override
    public void run() {
        this.resultsBool = new ArrayList<String>();
        timer = new Date().getTime(); // Start timer for printing out how long the process took
        switch (opportunities) {
            case ALL:
                String res = "";
                for (String s : this.dic) {
                    res = res + s + "\n"; // Add every single voc with a linebreak to the result string
                }
                this.allTerms = res;
                break;
            case BOOLEAN:
                this.resultsBool = (ArrayList<String>) BooleanRetrieval.booleanQuery(query, searchType, kgIndex, sIndex, dIndex);
                break;
            default:
                rank = new RankedRetrieval(dIndex, form);
                RankedParser parser = new RankedParser(dIndex, kgIndex);
                q = parser.collectAndQueries(query);
                resultsRank = rank.rankedQuery(kgIndex, q, k);
                break;
        }
        System.out.println("Time for Generating process: " + (new Date().getTime() - timer)); // Print the length of time the process took
        callback.notifyThreadFinished();
    }

    // Getters
    public ArrayList<JLabel> getArray() {
        return labels;
    }

    public String getAllTerms() {
        return allTerms;
    }

    public GeneratingOpportunities getOpportunities() {
        return opportunities;
    }

    public RankedDocument[] getResultsRank() {
        return resultsRank;
    }

    public ArrayList<String> getResultsBool() {
        return resultsBool;
    }

    public RankedRetrieval getRank() {
        return rank;
    }

    public SpellingCorrection getSpellCorrect() {
        return spellCorrect;
    }

}
