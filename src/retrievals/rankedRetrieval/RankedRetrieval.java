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

    //Adapted from Sylvia's IndexWriter.buildWeightFile;
    private double calcWDT(DiskPosting dPosting) {
        if (dIndex.getFileNames().get(dPosting.getDocumentID()).equals("769.json")) {
            System.out.println("RR: WDT for " + dPosting.getDocumentID() + ": " + form.calcWDT(dPosting));
        }
        return form.calcWDT(dPosting);
    }

    private double getL_D(int docID) {
        if (dIndex.getFileNames().get(docID).equals("769.json")) {
            System.out.println("RR: L_D for " + docID + ": " + form.getL_D(docID));
        }
        return form.getL_D(docID);
    }

    private static void accumulate(HashMap<DiskPosting, Double> acc, DiskPosting docID, double A_d) {

        if (acc.containsKey(docID)) {
            double newA_d = acc.get(docID) + A_d;
            acc.put(docID, newA_d);
        } else {
            acc.put(docID, A_d);
        }
    }

    public RankedItem[] rankedQuery(KGramIndex kIndex, Subquery query, int k) {
        List<DiskPosting> dPostings = null;
        HashMap<DiskPosting, Double> acc = new HashMap<DiskPosting, Double>();
        PriorityQueue<RankedItem> A_dQueue = new PriorityQueue<RankedItem>();
        List<RankedItem> returnedRIs = new ArrayList<RankedItem>();

        for (String queryLit : query.getLiterals()) {
            System.out.println("Literal: " + queryLit);
            //Collect A_d values for each document, add to priority queue
            if (queryLit.contains("*")) {
                List<DiskPosting> wcResults = DiskQueryProcessor.wildcardQuery(queryLit, dIndex, kIndex);
                dPostings = new ArrayList<DiskPosting>(wcResults.size());
                dPostings = wcResults;
            } else {
                dPostings = dIndex.getPostings(queryLit);
            }

            if (dPostings != null) {

                double WQT = calcWQT(dPostings);
                double A_d = 0.0;

                for (DiskPosting dPosting : dPostings) {
                    double newAccumulator = calcWDT(dPosting) * WQT;
                    accumulate(acc, dPosting, newAccumulator);
                    if (dIndex.getFileNames().get(dPosting.getDocumentID()).equals("769.json")) {
                        System.out.println("RR: Accumulator for " + dPosting.getDocumentID() + ": " + acc.get(dPosting));
                    }
                }
            }
        }
        this.sizeOfFoundDocs = acc.size();
        DiskPosting[] relevantDocuments = new DiskPosting[acc.size()];
        acc.keySet().toArray(relevantDocuments);

        for (DiskPosting relevantDocument : relevantDocuments) {

            double accumulator = acc.get(relevantDocument);
            if (accumulator > 0.0) {
                if (form instanceof OkapiForm) {
                    ((OkapiForm) form).setdPosting(relevantDocument);
                }
                double docWeight = getL_D(relevantDocument.getDocumentID());
                double rank = accumulator / docWeight;
                A_dQueue.add(new RankedItem(rank, relevantDocument.getDocumentID()));
            }
        }

        if (A_dQueue.isEmpty()) {
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

    public int getSizeOfFoundDocs() {
        return sizeOfFoundDocs;
    }

    public void testDiskIndex() {
        System.out.println("DiskIndex 744: " + dIndex.getFileNames().get(744));
        System.out.println("DiskIndex 999: " + dIndex.getFileNames().get(999));
    }
}
