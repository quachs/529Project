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

//https://docs.oracle.com/javase/7/docs/api/java/lang/Double.html
//https://docs.oracle.com/javase/8/docs/api/java/util/Scanner.html
public class RankedRetrieval {

    private DiskInvertedIndex dIndex;
    private Formula form;
    private int sizeOfFoundDocs = 0;

    /**
     * Depending on the given form create a new Object for its instance.
     * @param dIndex
     * @param formEnum 
     */
    public RankedRetrieval(DiskInvertedIndex dIndex, FormEnum formEnum) {
        this.dIndex = dIndex;
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
        return form.calcWQT(tDocIDs);
    }

    // Adapted from Sylvia's IndexWriter.buildWeightFile;
    private double calcWDT(DiskPosting dPosting) {
        return form.calcWDT(dPosting);
    }

    private double getL_D(int docID) {
        return form.getL_D(docID);
    }

    /**
     * Term-at-a-time scoring algorithm
     * as presented during lecture
     * 
     * @param accumulators
     * @param docID
     * @param accDocScore 
     */
    private static void accumulate(HashMap<DiskPosting, Double> accumulators, DiskPosting docID, double accDocScore) {

        if (accumulators.containsKey(docID)) {
            double updatedAccumulation = accumulators.get(docID) + accDocScore;
            accumulators.put(docID, updatedAccumulation);
        } else {
            accumulators.put(docID, accDocScore);
        }
    }

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

                for (DiskPosting dPosting : dPostings) {
                    double newAccumulator = calcWDT(dPosting) * WQT;
                    accumulate(accumulators, dPosting, newAccumulator);
                }
            }
        }
        this.sizeOfFoundDocs = accumulators.size();
        DiskPosting[] relevantDocuments = new DiskPosting[accumulators.size()];
        accumulators.keySet().toArray(relevantDocuments);

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
        
        for (int i = 0; i < k; i++) {
            RankedDocument ri = rankedQueue.poll();
            returnedRIs.add(ri);
        }

        RankedDocument[] results = new RankedDocument[returnedRIs.size()];
        returnedRIs.toArray(results);
        return results;
    }

    public int getSizeOfFoundDocs() {
        return sizeOfFoundDocs;
    }
}
