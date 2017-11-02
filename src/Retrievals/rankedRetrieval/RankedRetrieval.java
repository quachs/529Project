package Retrievals.rankedRetrieval;

import java.util.*;
import java.lang.*;
import Helper.*;
import Indexes.*;
import Indexes.diskPart.*;

//https://docs.oracle.com/javase/7/docs/api/java/lang/Double.html
//https://docs.oracle.com/javase/8/docs/api/java/util/Scanner.html

public class RankedRetrieval{
    
    private String mFolderPath;
    private static int mCorpusSize;
    
    public RankedRetrieval(String folderPath){
        this.mFolderPath = folderPath;
    }
    
    private static double calcWQT(List<DiskPosting> tDocIDs){
        return (Math.log(1.0 + ((double)mCorpusSize / tDocIDs.size())));
    }
    
    //Adapted from Sylvia's IndexWriter.buildWeightFile;
    private static double calcWDT(DiskPosting dPosting){
        return (1.0 + (Math.log(dPosting.getTermFrequency())));
    }
    
    private static double getL_D(DiskInvertedIndex dIndex, int docID){
        return dIndex.getDocWeight(docID);
    }
    
    private static void accumulate(HashMap<Integer, Double> acc, int docID, double A_d){
 
        if(acc.containsKey(docID)){
            double newA_d = acc.get(docID) + A_d;
            acc.put(docID, newA_d);
        }
        else{
            acc.put(docID, A_d);
        }   
    }
       
    public static RankedItem[] rankedQuery(DiskInvertedIndex dIndex, KGramIndex kIndex, Subquery query, int k){
                
        mCorpusSize = dIndex.getCorpusSize();      
        List<DiskPosting> dPostings;
        HashMap<Integer, Double> acc = new HashMap<Integer, Double>();
        PriorityQueue<RankedItem> A_dQueue = new PriorityQueue<RankedItem>();
        List<RankedItem> returnedRIs = new ArrayList<RankedItem>();
                
        for (String queryLit : query.getLiterals()){

            //Collect A_d values for each document, add to priority queue
            if (queryLit.contains("*")){
                List<DiskPosting> wcResults = DiskQueryProcessor.wildcardQuery(queryLit, dIndex, kIndex);
                dPostings = new ArrayList<DiskPosting>(wcResults.size());
                //wcResults.toArray(dPostings);
                dPostings = wcResults;
            }
            else{
                dPostings = dIndex.getPostings(queryLit);
            }
            
            if (dPostings != null){
                
                double WQT = calcWQT(dPostings);
                System.out.println("WQT: " + WQT);
                double A_d = 0.0;
             
                for (DiskPosting dPosting : dPostings){
                    double newAccumulator = calcWDT(dPosting) * WQT;
                    accumulate(acc, dPosting.getDocumentID(), newAccumulator);
                }
            }           
        }
        
        Integer[] relevantDocuments = new Integer[acc.size()];
        acc.keySet().toArray(relevantDocuments);

        for (int relevantDocument : relevantDocuments){
            
                double accumulator = acc.get(relevantDocument);
                if (accumulator > 0.0){
                    double docWeight = getL_D(dIndex, relevantDocument);
                    double rank = accumulator / docWeight;
                    A_dQueue.add(new RankedItem(rank, relevantDocument));
                }
        }
                       
        for (int i = 0; i < k; i++){
            RankedItem ri = A_dQueue.poll();
            returnedRIs.add(ri);
        }
        
        RankedItem[] results = new RankedItem[returnedRIs.size()];
        returnedRIs.toArray(results);  
        System.out.println("queue size: " + results.length);
        return results;
    }
}