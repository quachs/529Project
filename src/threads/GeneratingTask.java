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
     * Constructor for making a ranked retrival
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
     * Constructor for making boolean retrival
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
     * Constructor for getting all terms of a corpus and print them.
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
                this.resultsBool = (ArrayList<String>) BooleanRetrieval.booleanQuery(query, searchType, kgIndex, sIndex, dIndex);
                break;
            default:
                rank = new RankedRetrieval(dIndex, form);
                RankedParser parser = new RankedParser(dIndex, kgIndex);
                q = parser.collectAndQueries(query);
                resultsRank = rank.rankedQuery(kgIndex, q, k);
                break;
        }
        System.out.println("Time for Generating process: " + (new Date().getTime() - timer)); // print time that process took
        callback.notifyThreadFinished();
    }

    // Getter
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
