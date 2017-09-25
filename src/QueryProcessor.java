
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/*

ISSUES:
+ Positional merge assumes the terms exist in the index. Check before using!!!

UPDATE:
+ Wildcard and PositionalMerge returns an empty postings list if no matches.

 */
public class QueryProcessor {

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
        Set<String> wcKGrams = new TreeSet<String>();
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
        if (kGramIndex.getTypes(kgrams[0]) != null) {
            candidates = kGramIndex.getTypes(kgrams[0]);
            for (int i = 1; i < wcKGrams.size(); i++) {
                if (kGramIndex.getTypes(kgrams[i]) != null) {
                    candidates = BooleanRetrieval.intersectList(candidates, kGramIndex.getTypes(kgrams[i]));
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
                String proToken = PorterStemmer.getStem(t.nextToken());
                if (index.getPostingsList(proToken) != null) {
                    results = BooleanRetrieval.orList(results, index.getPostingsList(proToken));
                }

            }
        }

        return results;

    }

    /**
     * Positional intersection of two terms where the second term appears within
     * k positions after the first. Source: Introduction to Information
     * Retrieval (Figure 2.12)
     *
     * @param term1 positional postings list of the first term
     * @param term2 positional postings list of the second term
     * @param k max positions that term2 appears after term1
     * @return positional postings list from the intersection; the position
     * corresponds to term2
     */
    public static List<PositionalPosting> positionalIntersect(List<PositionalPosting> term1, List<PositionalPosting> term2, int k) {

        List<PositionalPosting> result = new ArrayList<PositionalPosting>();
        List<Integer> docs1 = new ArrayList<Integer>(); // term1 documents
        List<Integer> docs2 = new ArrayList<Integer>(); //term2 documents
        int i = 0; // term1 document index
        int j = 0; // term2 document index

        // load the docIDs to list
        for (PositionalPosting p : term1) {
            docs1.add(p.getDocumentID());
        }
        for (PositionalPosting p : term2) {
            docs2.add(p.getDocumentID());
        }

        // intersect the docs
        while (i < docs1.size() && j < docs2.size()) {
            // both terms appear in the doc
            if ((int) docs1.get(i) == docs2.get(j)) {
                List<Integer> candidate = new ArrayList<Integer>();
                List<Integer> pp1 = term1.get(i).getTermPositions(); // term1 positions
                List<Integer> pp2 = term2.get(j).getTermPositions(); // term2 positions
                int ii = 0; // term1 position index
                int jj = 0; // term2 position index

                // check if term2 appears within k positions after term1
                while (ii < pp1.size()) {
                    while (jj < pp2.size()) {
                        int relativePos = pp2.get(jj) - pp1.get(ii);
                        if (relativePos > 0 && relativePos <= k) {
                            // add term2 position to candidates
                            candidate.add(pp2.get(jj));
                        } else if (pp2.get(jj) > pp1.get(ii)) {
                            break;
                        }
                        jj++;
                    }
                    while (!candidate.isEmpty() && Math.abs(candidate.get(0) - pp1.get(ii)) > k) {
                        candidate.remove(0);
                    }
                    for (Integer pos : candidate) {
                        int currentIndex = result.size() - 1;
                        if (!result.isEmpty() && result.get(currentIndex).getDocumentID() == docs1.get(i)) {
                            // the query appears more than once in the doc
                            // add the position to existing posting
                            result.get(currentIndex).addPosition(pos);
                        } else { // add a new posting to the answer  
                            result.add(new PositionalPosting(docs1.get(i), pos));
                        }
                    }
                    ii++;
                }
                i++;
                j++;
            } else if (docs1.get(i) < docs2.get(j)) {
                i++;
            } else {
                j++;
            }
        }

        return result;
    }
}
