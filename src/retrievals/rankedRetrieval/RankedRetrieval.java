package retrievals.rankedRetrieval;

import formulas.DefaultForm;
import formulas.FormEnum;
import formulas.Formula;
import formulas.OkapiForm;
import formulas.TfidfForm;
import formulas.WackyForm;
import query.Subquery;
import indexes.KGramIndex;
import indexes.diskPart.DiskInvertedIndex;
import indexes.diskPart.DiskPosting;
import query.processor.DiskQueryProcessor;
import java.util.*;
import java.lang.*;

//https://docs.oracle.com/javase/7/docs/api/java/lang/Double.html
//https://docs.oracle.com/javase/8/docs/api/java/util/Scanner.html

/**
 * Class used to process ranked queries;
 * follows term-at-a-time document scoring
 * as presented during CECS 429/529 lecture.
 * 
 */
public class RankedRetrieval {

    private DiskInvertedIndex dIndex;
    private FormEnum formEnum;
    private Formula form;
    private int sizeOfFoundDocs = 0;

    public RankedRetrieval(DiskInvertedIndex dIndex, FormEnum formEnum) {
        this.dIndex = dIndex;
        this.formEnum = formEnum;
        switch (formEnum) {
            case OKAPI:
                form = new OkapiForm(dIndex);
                break;
            case TFIDF:
                form = new TfidfForm(dIndex);
                break;
            case WACKY:
                form = new WackyForm(dIndex);
                break;
            default:
                form = new DefaultForm(dIndex);
                break;
        }
    }

    private double calcWQT(List<DiskPosting> tDocIDs) {
        System.out.println("FormEnum: " + formEnum);
        double t = form.calcWQT(tDocIDs);
        System.out.println("RR: WQT: " + t);
        return t;
    }

    /**
     * Use selected idf formula to calculate WDT value
     * 
     * @param dPosting
     * @return WDT value for the corresponding document
     */
    private double calcWDT(DiskPosting dPosting) {
        if (dIndex.getFileNames().get(dPosting.getDocumentID()).equals("769.json")) {
            System.out.println("RR: WDT for " + dPosting.getDocumentID() + ": " + form.calcWDT(dPosting));
        }
        return form.calcWDT(dPosting);
    }

    /**
     * Retrieve the normalized length value for the selected document
     * 
     * @param docID
     * @return L_D, which represents the normalized document length
     */ 
    private double getL_D(int docID) {
        if (dIndex.getFileNames().get(docID).equals("769.json")) {
            System.out.println("RR: L_D for " + docID + ": " + form.getL_D(docID));
        }
        return form.getL_D(docID);
    }

    /**
     * Term-at-a-time scoring algorithm
     * as presented during lecture
     * 
     * @param accumulators Hashmap associating a document with its accumulated score
     * @param docID A document which may or may not have a non-zero accumulated score
     * @param accDocScore Accumulated document score associated with docID
     */
    private static void accumulate(HashMap<DiskPosting, Double> accumulators, DiskPosting docID, double accDocScore) {

        if (accumulators.containsKey(docID)) {
            double updatedAccumulation = accumulators.get(docID) + accDocScore;
            accumulators.put(docID, updatedAccumulation);
        } else {
            accumulators.put(docID, accDocScore);
        }
    }

    /**
     * Term-at-a-time document scoring method.
     * 
     * @param kIndex KGramIndex used to process a wildcard query
     * @param query User input stored in Subquery structure
     * @param k Number of relevant documents to return in the result set
     * @return Array of top k relevant documents and their scores,
     * sorted by most relevant to least relevant
     */
    public RankedDocument[] rankedQuery(KGramIndex kIndex, Subquery query, int k) {
        List<DiskPosting> dPostings = null;
        HashMap<DiskPosting, Double> accumulators = new HashMap<DiskPosting, Double>();
        PriorityQueue<RankedDocument> rankedQueue = new PriorityQueue<RankedDocument>();
        List<RankedDocument> returnedRIs = new ArrayList<RankedDocument>();

        for (String queryLit : query.getLiterals()) {
            System.out.println("Literal: " + queryLit);
            
            // Collect accDocScore values for each document, add to priority queue
            if (queryLit.contains("*")) {
                List<DiskPosting> wcResults = DiskQueryProcessor.wildcardQuery(queryLit, dIndex, kIndex);
                dPostings = new ArrayList<DiskPosting>(wcResults.size());
                dPostings = wcResults;
            } else {
                dPostings = dIndex.getPostings(queryLit);
            }

            if (dPostings != null) {

                double WQT = calcWQT(dPostings);
                double accDocScore = 0.0;

                for (DiskPosting dPosting : dPostings) {
                    double newAccumulator = calcWDT(dPosting) * WQT;
                    accumulate(accumulators, dPosting, newAccumulator);
                    
                    if (dIndex.getFileNames().get(dPosting.getDocumentID()).equals("769.json")) {
                        System.out.println("RR: Accumulator for " + dPosting.getDocumentID() + ": " + accumulators.get(dPosting));
                    }
                }
            }
        }
        
        this.sizeOfFoundDocs = accumulators.size();
        
        
        // Create an array of all relevant documents.
        // Relevancy is determine by its membership
        // in the hashmap of accumulators
        DiskPosting[] relevantDocuments = new DiskPosting[accumulators.size()];
        accumulators.keySet().toArray(relevantDocuments);

        // Add all normalized, non-zero document scores to priority queue
        for (DiskPosting relevantDocument : relevantDocuments) {

            double accumulator = accumulators.get(relevantDocument);
            
            if (accumulator > 0.0) {
                
                if (form instanceof OkapiForm) {
                    ((OkapiForm) form).setdPosting(relevantDocument);
                }
                
                double docWeight = getL_D(relevantDocument.getDocumentID());
                double rank = accumulator / docWeight;
                rankedQueue.add(new RankedDocument(rank, relevantDocument.getDocumentID()));
            }
        }

        if (rankedQueue.isEmpty()) {
            return null;
        }
        
        // Adjust size of result set if there are
        // fewer results than the k value.
        if (accumulators.size() < k) {
            k = accumulators.size();
        }
        
        // Add the top k documents to the final result set.
        for (int i = 0; i < k; i++) {
            RankedDocument ri = rankedQueue.poll();
            returnedRIs.add(ri);
        }

        // Convert result set to array
        RankedDocument[] results = new RankedDocument[returnedRIs.size()];
        returnedRIs.toArray(results);
        return results;
    }

    public int getSizeOfFoundDocs() {
        return sizeOfFoundDocs;
    }

    public void testDiskIndex() {
        System.out.println("DiskIndex 744: " + dIndex.getFileNames().get(744));
        System.out.println("DiskIndex 999: " + dIndex.getFileNames().get(999));
    }
}
