import java.util.*;
import java.lang.*;

//https://docs.oracle.com/javase/7/docs/api/java/lang/Double.html
//https://docs.oracle.com/javase/8/docs/api/java/util/Scanner.html

class RankedRetrieval{
    
    private String mFolderPath;
    private static int mCorpusSize;
    
    public RankedRetrieval(String folderPath){
        this.mFolderPath = folderPath;
    }
    
    private static double calcWQT(DiskPosting[] tDocIDs){
        return (Math.log(1 + (mCorpusSize / tDocIDs.length)));
    }
    
    //Adapted from Sylvia's IndexWriter.buildWeightFile;
    private static double calcWDT(DiskPosting dPosting){
        return (1 + (Math.log(dPosting.getTermFrequency())));
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
        DiskPosting[] dPostings;
        HashMap<Integer, Double> acc = new HashMap<Integer, Double>();
        PriorityQueue<RankedItem> A_dQueue = new PriorityQueue<RankedItem>();
        List<RankedItem> returnedRIs = new ArrayList<RankedItem>();
                
        for (String queryLit : query.getLiterals()){

            //Collect A_d values for each document, add to priority queue
            if (queryLit.contains("*")){
                List<DiskPosting> wcResults = wildcardQuery(queryLit, dIndex, kIndex);
                dPostings = new DiskPosting[wcResults.size()];
                wcResults.toArray(dPostings);
            }
            else{
                dPostings = dIndex.getPostings(queryLit);
            }
            
            if (dPostings != null){
                
                double WQT = calcWQT(dPostings);
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
        return results;
    }
    
    /**
     * Retrieve a list of positional postings that match the wildcard query
     *
     * @param wcQuery the wildcard query from user input
     * @param index positional inverted index of the corpus
     * @param kGramIndex K-Gram index of the vocabulary type
     * @return list of resulting Positional Postings
     */
    public static List<DiskPosting> wildcardQuery(String wcQuery, DiskInvertedIndex index, KGramIndex kGramIndex) {

        // Generate all the k-grams for the wildcard
        SortedSet<String> wcKGrams = new TreeSet<String>();
        List<DiskPosting> results = new ArrayList<DiskPosting>();

        // Append '$' if the beginning or end of the wildcard
        String modifiedQuery = wcQuery;
        if (modifiedQuery.charAt(0) != '*') {
            modifiedQuery = "$" + modifiedQuery;
        }
        if (modifiedQuery.charAt(modifiedQuery.length() - 1) != '*') {
            modifiedQuery = modifiedQuery + "$";
        }

        // Split the query at the '*' and generate the largest k-grams
        String[] fragments = modifiedQuery.split("\\*");
        for (String fragment : fragments) {
            if (fragment.length() > 3) {
                // Iterate the length of the fragment to generate 3-grams
                for (int i = 0; i + 3 <= fragment.length(); i++) {
                    wcKGrams.add(fragment.substring(i, i + 3));
                }
            } else if (fragment.length() > 0) {
                wcKGrams.add(fragment); // 1-,2-, or 3-grams
            }
        }

        // Convert treeset to array to make iterating easier
        String[] kgrams = new String[wcKGrams.size()];
        kgrams = wcKGrams.toArray(kgrams);

        // Merge the postings list of each k-gram
        List<String> candidates = new ArrayList<String>();
        if (kGramIndex.getPostingsList(kgrams[0]) != null) {
            candidates = kGramIndex.getPostingsList(kgrams[0]);
            for (int i = 1; i < kgrams.length; i++) {
                if (kGramIndex.getPostingsList(kgrams[i]) != null) {
                    candidates = QueryProcessor.intersectList(candidates, kGramIndex.getPostingsList(kgrams[i]));
                } else { // return if no matches
                    return results;
                }
            }
        } else { // return if no matches
            return results;
        }

        // Remove candidates that do not match the original query
        Iterator<String> iter = candidates.iterator();
        while (iter.hasNext()) {
            if (!iter.next().matches(wcQuery.replace("*", ".*"))) {
                iter.remove();
            }
        }

        // OR together the postings for the processed/stemmed term from each candidate    
        for (String candidate : candidates) { // will skip if candidates is empty
            // process and stem the token
            TokenProcessorStream t = new TokenProcessorStream(candidate);
            while (t.hasNextToken()) {
                String term = PorterStemmer.getStem(t.nextToken());
                if (index.getPostings(term) != null) {
                    DiskPosting[] tempArray = index.getPostings(term);
                    List<DiskPosting> tempList = new ArrayList<DiskPosting>();
                    
                    for (DiskPosting dPosting : tempArray){
                        tempList.add(dPosting);
                    }
                    
                    results = QueryProcessor.unionList(results, tempList);
                }

            }
        }
        return results;
    }   
}