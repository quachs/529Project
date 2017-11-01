package retrivals.rankedRetrival;

import formulas.DefaultForm;
import formulas.FormEnum;
import formulas.Formular;
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
public class RankedRetrieval {

    private DiskInvertedIndex dIndex;
    private FormEnum formEnum;
    private Formular form;

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

    private double calcWQT(DiskPosting[] tDocIDs) {
        return form.calcWQT(tDocIDs);
    }

    //Adapted from Sylvia's IndexWriter.buildWeightFile;
    private double calcWDT(DiskPosting dPosting) {
        return form.calcWDT(dPosting);
    }

    private double getL_D(int docID) {
        return form.getL_D(docID);
    }

    private static void accumulate(HashMap<Integer, Double> acc, int docID, double A_d) {

        if (acc.containsKey(docID)) {
            double newA_d = acc.get(docID) + A_d;
            acc.put(docID, newA_d);
        } else {
            acc.put(docID, A_d);
        }
    }

    public RankedItem[] rankedQuery(KGramIndex kIndex, Subquery query, int k) {
        DiskPosting[] dPostings = null;
        HashMap<Integer, Double> acc = new HashMap<Integer, Double>();
        PriorityQueue<RankedItem> A_dQueue = new PriorityQueue<RankedItem>();
        List<RankedItem> returnedRIs = new ArrayList<RankedItem>();

        for (String queryLit : query.getLiterals()) {

            //Collect A_d values for each document, add to priority queue
            if (queryLit.contains("*")) {
                List<DiskPosting> wcResults = DiskQueryProcessor.wildcardQuery(queryLit, dIndex, kIndex);
                dPostings = new DiskPosting[wcResults.size()];
                wcResults.toArray(dPostings);
            } else {
                dPostings = dIndex.getPostings(queryLit);
            }

            if (dPostings != null) {

                double WQT = calcWQT(dPostings);
                double A_d = 0.0;

                for (DiskPosting dPosting : dPostings) {
                    double newAccumulator = calcWDT(dPosting) * WQT;
                    accumulate(acc, dPosting.getDocumentID(), newAccumulator);
                }
            }
        }

        Integer[] relevantDocuments = new Integer[acc.size()];
        acc.keySet().toArray(relevantDocuments);

        for (int relevantDocument : relevantDocuments) {

            double accumulator = acc.get(relevantDocument);
            if (accumulator > 0.0) {
                if (form instanceof OkapiForm) {
                    for (DiskPosting dPosting : dPostings) {
                        if (acc.containsKey(dPosting.getDocumentID())) {
                            ((OkapiForm) form).setdPosting(dPosting);
                        }
                    }
                }
                double docWeight = getL_D(relevantDocument);
                double rank = accumulator / docWeight;
                A_dQueue.add(new RankedItem(rank, relevantDocument));
            }
        }

        if(A_dQueue.isEmpty()){
            return null;
        }
        if (acc.size() < k) {
            k = acc.size();
        }
        for (int i = 0; i < k; i++) {
            RankedItem ri = A_dQueue.poll();
            returnedRIs.add(ri);
        }

        RankedItem[] results = new RankedItem[returnedRIs.size()];
        returnedRIs.toArray(results);
        return results;
    }
}
