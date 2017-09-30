
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/*

Class to process a query.

 */
public class QueryProcessor {

    private static List<List<PositionalPosting>> AndCollection = new ArrayList<List<PositionalPosting>>();

    //Add the positional postings list of an AND query to the 
    //collection of AND query positional postings lists
    private static void addAndQuery(Subquery andQueryLiterals, PositionalInvertedIndex posIndex,
            KGramIndex kgIndex) {

        List<PositionalPosting> masterList = new ArrayList<PositionalPosting>();
        String preLiteral = andQueryLiterals.getLiterals().get(0);
        List<PositionalPosting> intermediateList = new ArrayList<PositionalPosting>();

        if (preLiteral.contains("\"")) {
            masterList = Phrase.phraseQuery(preLiteral, posIndex);
        } else if (preLiteral.contains("*")) {
            masterList = QueryProcessor.wildcardQuery(preLiteral, posIndex, kgIndex);
        } else if (preLiteral.contains("near")) {
            masterList = Phrase.nearQuery(preLiteral, posIndex);
        } 
        else {
            masterList = posIndex.getPostingsList(preLiteral);
        }

        //Merge all of the postings lists of each. 
        //query literal of a given AND query into one master postings list. 
        if (andQueryLiterals.getSize() > 1) {

            for (int i = 1; i < andQueryLiterals.getSize(); i++) {
                String currentLiteral = andQueryLiterals.getLiterals().get(i);

                if (currentLiteral.contains("\"")) {
                    intermediateList = Phrase.phraseQuery(currentLiteral, posIndex);
                    if (masterList != null && intermediateList != null) {
                        masterList = ListMerge.intersectList(masterList, intermediateList);
                    }
                } else if (currentLiteral.contains("*")) {
                    intermediateList = QueryProcessor.wildcardQuery(currentLiteral, posIndex, kgIndex);
                    if (masterList != null && intermediateList != null) {
                        masterList = ListMerge.intersectList(masterList, intermediateList);
                    }
                } else if (currentLiteral.contains("near")) {
                    intermediateList = Phrase.nearQuery(currentLiteral, posIndex);
                    if (masterList != null && intermediateList != null) {
                        masterList = ListMerge.intersectList(masterList, intermediateList);
                    }
                } else {
                    if (masterList != null && posIndex.getPostingsList(currentLiteral) != null) {
                        masterList = ListMerge.intersectList(masterList, posIndex.getPostingsList(currentLiteral));
                    }
                }
            }
        }
        //Add this AND postings list to the collection of AND postings lists
        AndCollection.add(masterList);
    }

    //Run all AND Queries Q_i, store results in collection of positional 
    //postings lists, then run an OR query that merges all postings lists
    //into one master positional postings list representing the entire query.
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
                if (masterList != null && AndCollection.get(i) != null){
                    masterList = ListMerge.orList(masterList, AndCollection.get(i));
                }
            }
        }
        return masterList;
    }

    /**
     * Retrieve a list of positional postings that match the wildcard query
     *
     * @param wcQuery the wildcard query from user input
     * @param index positional inverted index of the corpus
     * @param kGramIndex K-Gram index of the vocabulary type
     * @return list of resulting positional postings
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
}
