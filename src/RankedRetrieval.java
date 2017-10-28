import java.util.*;
import java.lang.*;

//https://docs.oracle.com/javase/7/docs/api/java/lang/Double.html
//https://docs.oracle.com/javase/8/docs/api/java/util/Scanner.html

class RankedRetrieval{
    
    private String mFolderPath;
    private static int mCorpusSize;
    
    public RankedRetrieval(){}
    
    public RankedRetrieval(String folderPath){
        this.mFolderPath = folderPath;
    }
    
    // this is ok
    private static float calcWqt(DiskPosting[] tDocIDs){
        System.out.println("wqt: "+(float)(Math.log(1 + ((float)mCorpusSize / tDocIDs.length))));
        return (float)(Math.log(1 + ((float)mCorpusSize / tDocIDs.length)));
    }
    
    //Adapted from Sylvia's IndexWriter.buildWeightFile;
    private static float calcWdt(DiskPosting dPosting){
        return (1 + (float)Math.log(dPosting.getTermFrequency()));
    }
    
    private static double calcLd(DiskInvertedIndex dIndex, DiskPosting dPosting){
        return dIndex.getDocWeight(dPosting.getDocumentID());
    }
       
    public static RankedItem[] rankedQuery(DiskInvertedIndex dIndex, KGramIndex kgIndex, Subquery query, int k){
        
        mCorpusSize = dIndex.getCorpusSize();
        
        //DiskPosting[] dPostings;
        PriorityQueue<RankedItem> riQueue = new PriorityQueue<RankedItem>();
        //List<Integer> returnedDocs = new ArrayList<Integer>();
        //List<RankedItem> returnedRIs = new ArrayList<RankedItem>();
        
        // Map of documentID to its score
        Map<Integer, Float> docScores = new HashMap<Integer, Float>();
        
        
        for (String queryLit : query.getLiterals()){

            /*
            if (queryLit.contains("*")){
                List<DiskPosting> wcResults = wildcardQuery(queryLit, dIndex, kgIndex);
                dPostings = new DiskPosting[wcResults.size()];
                wcResults.toArray(dPostings);
            }
            */
            
            DiskPosting[] dPostings = dIndex.getPostings(queryLit);
            float wqt = calcWqt(dPostings);
            
            //float A_d = 0;
            for (DiskPosting posting : dPostings){
                
                // TODO: check for null
                
                if(docScores.containsKey(posting.getDocumentID())){
                    // Increment the accumulator score for the document
                    float accumulator = docScores.get(posting.getDocumentID());
                    accumulator += (calcWdt(posting) * wqt);
                    docScores.put(posting.getDocumentID(), accumulator);
                }
                else {
                    // Add a new document/score pair to the map
                    docScores.put(posting.getDocumentID(), calcWdt(posting) * wqt);
                }
                //float newA_d = calcWDT(dPostings[i]) * WQT;
                //A_d = A_d + newA_d;

                /*
                if (A_d > 0.0) {
                    
                       // Obtain docWeight L_d, divide A_d by L_d, then
                       // place docID and doc weight in PQ, which is 
                       // ordered by doc weight.
                       float docWeight = calcL_D(dIndex, dPostings[i]);
                       float rank = A_d / docWeight;
                       RankedItem newRI = new RankedItem(rank, dPostings[i]);
                       A_dQueue.add(newRI);
                }
                */
            }
        }
        
        // Divide each A_d by L_d for any non-zero A_d
        for (Integer docId : docScores.keySet()) {
            if(docScores.get(docId) > 0){
                double a = (double)docScores.get(docId) / dIndex.getDocWeight(docId);
                RankedItem ri = new RankedItem(a, docId);
                riQueue.add(ri);
            }
            
        }
        
        RankedItem[] results = new RankedItem[k];        
        for (int i = 0; i < k; i++){
            results[i] = riQueue.poll();
        }
        
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
                    //results = QueryProcessor.unionList(results, index.getPostings(term));
                }

            }
        }
        return results;
    }   
}
