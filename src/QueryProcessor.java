
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Class to process various types of queries
 * 
 */
public class QueryProcessor {

    private static List<List<PositionalPosting>> AndCollection = new ArrayList<List<PositionalPosting>>();

    /**
     * Add the positional postings list of an AND query to the 
     * collection of AND query positional postings lists.
     * 
     * @param andQueryLiterals
     * @param posIndex Positional inverted index of selected corpus
     * @param kgIndex KGram index of all types in corpus
     */
    private static void addAndQuery(Subquery andQueryLiterals, PositionalInvertedIndex posIndex,
            KGramIndex kgIndex) {

        List<PositionalPosting> masterList = new ArrayList<PositionalPosting>();
        String preLiteral = andQueryLiterals.getLiterals().get(0);
        List<PositionalPosting> intermediateList = new ArrayList<PositionalPosting>();

        if (preLiteral.contains("\"")) {
            masterList = phraseQuery(preLiteral, posIndex);
        } else if (preLiteral.contains("*")) {
            masterList = QueryProcessor.wildcardQuery(preLiteral, posIndex, kgIndex);
        } else if (preLiteral.contains("near")) {
            masterList = nearQuery(preLiteral, posIndex);
        } else {
            if (posIndex.getPostingsList(preLiteral) != null){
                masterList = posIndex.getPostingsList(preLiteral);
            }
        }

        /* Merge all of the postings lists of each query literal
        of a given AND query into one master postings list. */
        if (andQueryLiterals.getSize() > 1) {

            for (int i = 1; i < andQueryLiterals.getSize(); i++) {
                String currentLiteral = andQueryLiterals.getLiterals().get(i);

                if (currentLiteral.contains("\"")) {
                    intermediateList = phraseQuery(currentLiteral, posIndex);
                    if (masterList != null && intermediateList != null) {
                        masterList = ListMerge.intersectList(masterList, intermediateList);
                    }
                } else if (currentLiteral.contains("*")) {
                    intermediateList = QueryProcessor.wildcardQuery(currentLiteral, posIndex, kgIndex);
                    masterList = ListMerge.intersectList(masterList, intermediateList);
                } else if (currentLiteral.contains("near")) {
                    intermediateList = nearQuery(currentLiteral, posIndex); 
                    masterList = ListMerge.intersectList(masterList, intermediateList);
                } else {
                    if (posIndex.getPostingsList(currentLiteral) != null) {
                        masterList = ListMerge.intersectList(masterList, posIndex.getPostingsList(currentLiteral));
                    }
                    else {
                        masterList.clear();
                    }
                }
            }
        }
        //Add this AND postings list to the collection of AND postings lists
        if (masterList != null){
            AndCollection.add(masterList);
        }
        else {
            AndCollection.add(masterList);
            masterList.clear();
        } 
    }

    /**
     * Run all AND Queries Q_i, store results in collection of positional 
     * postings lists, then run an OR query that merges all postings lists
     * into one master positional postings list representing the entire query.
    
     * @param allQueries List of subqueries that represents user query
     * @param posIndex Positional inverted index of selected corpus
     * @param kgIndex KGram index of all types in corpus
     * @return Positional Posting list representing all AND queries
     * merged together using OR logic.
     */
    public static List<PositionalPosting> orQuery
        (List<Subquery> allQueries, PositionalInvertedIndex posIndex, KGramIndex kgIndex) {

        //Add all Q_i positional postings lists to AndCollection
        for (int i = 0; i < allQueries.size(); i++) {
            addAndQuery(allQueries.get(i), posIndex, kgIndex);
        }

        //Merge all Q_i postings list into Master List using OR intersection
        List<PositionalPosting> masterList = AndCollection.get(0);

        if (AndCollection.size() > 1) {
            for (int i = 1; i < AndCollection.size(); i++) {
                masterList = ListMerge.orList(masterList, AndCollection.get(i));
            }
        }
        AndCollection.clear();
        return masterList;
    }

    /**
     * Retrieve a list of positional postings that match the wildcard query
     *
     * @param wcQuery the wildcard query from user input
     * @param index positional inverted index of the corpus
     * @param kGramIndex K-Gram index of the vocabulary type
     * @return list of resulting Positional Postings
     */
    public static List<PositionalPosting> wildcardQuery
        (String wcQuery, PositionalInvertedIndex index, KGramIndex kGramIndex) {

        // Generate all the k-grams for the wildcard
        SortedSet<String> wcKGrams = new TreeSet<String>();
        List<PositionalPosting> results = new ArrayList<PositionalPosting>();

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
                    candidates = ListMerge.intersectList(candidates, kGramIndex.getPostingsList(kgrams[i]));
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
                if (index.getPostingsList(term) != null) {
                    results = ListMerge.orList(results, index.getPostingsList(term));
                }

            }
        }
        return results;
    }

    /**
     * Retrieve a list of document IDs that match the author query
     *
     * @param aQuery query for the author's name
     * @param sIndex soundex index
     * @return list of document IDs that match the author's name
     */
    public static List<Integer> authorQuery(String aQuery, SoundexIndex sIndex) {
        List<Integer> result = new ArrayList<Integer>();
        TokenProcessorStream t = new TokenProcessorStream(aQuery);
        while (t.hasNextToken()) {
            String name = t.nextToken();
            if (sIndex.getPostingsList(name) != null) {
                result = ListMerge.orList(result, sIndex.getPostingsList(name));
            }
        }
        return result;
    }
    
     /**
     * Uses positional intersection algorithm to merge all phrase terms into one
     * final list representing the results for the entire phrase.
     *
     * @param phraseLiteral A sequential set of terms enclosed in double quotes
     * @param posIndex Positional inverted index of selected corpus
     * @return A PositionalPosting list of the results of the phrase query.
     */
    public static List<PositionalPosting> phraseQuery(String phraseLiteral, PositionalInvertedIndex posIndex) {
        phraseLiteral = phraseLiteral.replaceAll("\"", "");
        String[] spPhrase = phraseLiteral.split(" ");
        PorterStemmer phraseStemmer = new PorterStemmer();
        List<PositionalPosting> phraseList = new ArrayList<PositionalPosting>();

        for (int i = 0; i < spPhrase.length; i++) {
            spPhrase[i] = phraseStemmer.getStem(spPhrase[i]);
        }
  
        if (posIndex.getPostingsList(spPhrase[0]) != null){
            phraseList = posIndex.getPostingsList(spPhrase[0]);
            for(int j = 1; j < spPhrase.length; j++){
                if(posIndex.getPostingsList(spPhrase[j]) != null) {
                    phraseList = ListMerge.positionalIntersect(phraseList,
                    posIndex.getPostingsList(spPhrase[j]), 1);
                } else { // return empty list
                    phraseList.clear();
                    return phraseList;
                    
                    
                }
            }
        } else { // return empty list 
            return phraseList;
        }
        return phraseList;
    }

    /**
     * Uses positional intersection algorithm to merge two terms in a string
     * literal that contains the NEAR operator.
     *
     * @param nearLiteral Near literal in the form [term1] near[k] [term2]
     * @param posIndex Positional inverted index of selected corpus
     * @return List resulting from the positional intersect of term1 and term2
     */
    public static List<PositionalPosting> nearQuery(String nearLiteral, PositionalInvertedIndex posIndex) {
        String[] spNear = nearLiteral.split(" ");
        List<PositionalPosting> nearList = new ArrayList<PositionalPosting>();

        //https://docs.oracle.com/javase/tutorial/java/data/converting.html                    
        int k = Integer.valueOf(spNear[1].substring(4));

        if (posIndex.getPostingsList(spNear[0]) != null && posIndex.getPostingsList(spNear[2]) != null) {
            nearList = ListMerge.positionalIntersect(posIndex.getPostingsList(spNear[0]),
                    posIndex.getPostingsList(spNear[2]), k);
        }
        return nearList;
    }
    
}
