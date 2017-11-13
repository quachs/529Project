package helper;

import indexes.KGramIndex;
import indexes.diskPart.DiskInvertedIndex;
import query.QueryTokenStream;
import query.processor.DiskQueryProcessor;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Spelling correction module
 *
 */
public class SpellingCorrection {

    private static final double JACCARD_THRESHOLD = 0.35;
    private static final int DF_THRESHOLD = 3;
    private static final String OP_REGEX = "^[near/\\d+]+$"; // match boolean operators

    private final DiskInvertedIndex dIndex;
    private final KGramIndex kIndex;
    private String[] queryTokens; // store the unprocessed tokens from the query
    private List<Integer> correctionIndex; // index of where to do the correction
    private Boolean isPhrase;

    public SpellingCorrection(String query, DiskInvertedIndex dIndex, KGramIndex kIndex) {
        this.dIndex = dIndex;
        this.kIndex = kIndex;

        // Trim the quotations if phrase
        if (query.contains("\"")) {
            query = query.substring(1, query.length() - 1);
            isPhrase = true;
        } else {
            isPhrase = false;
        }

        queryTokens = query.split(" ");
        correctionIndex = getCorrectionIndexList(queryTokens);

    }

    /**
     * Get a list of the indices of the query where spelling correction is
     * needed
     *
     * @param query
     * @return indices from the query
     */
    private List<Integer> getCorrectionIndexList(String[] queryTokens) {

        int queryIndex = 0;
        List<Integer> ciList = new ArrayList<Integer>();

        for (String token : queryTokens) {
            if (!token.matches(OP_REGEX)) { // ignore boolean operators
                QueryTokenStream t = new QueryTokenStream(token);
                String term = t.nextToken();
                if (term != null && !term.contains("*")) { // ignore wildcards
                    if (dIndex.getPostings(term) == null || dIndex.getPostings(term).size() < DF_THRESHOLD) {
                        ciList.add(queryIndex);
                    }
                }
            }
            queryIndex++;

        }
        return ciList;
    }

    /**
     * Return true if the query contains a term that does not exist in the index
     * or if the document frequency is below the threshold. False otherwise.
     *
     * @return true if need to call spelling correction
     */
    public Boolean needCorrection() {

        // Workaround for the df threshold in a small corpus
        int postingSize = 0;
        for (String token : this.queryTokens) {
            if (!token.matches(OP_REGEX) && !token.contains("*")) {
                try {
                    postingSize += this.dIndex.getPostings(token).size();
                } catch (NullPointerException ex) {
                }
            }
        }

        if (postingSize <= DF_THRESHOLD) {
            for (String token : this.queryTokens) {
                QueryTokenStream t = new QueryTokenStream(token);
                String term = t.nextToken();
                if (!token.matches(OP_REGEX) && !token.contains("*") && !term.equals(getCorrection(token))) {
                    return true;
                }
            }
            return false;
        }
        return !correctionIndex.isEmpty();
    }

    /**
     * Get the modified query with spelling correction
     *
     * @return modified query
     */
    public String getModifiedQuery() {
        String modified = "";
        // Correct the misspelled token in the query
        for (int i = 0; i < queryTokens.length; i++) {
            if (correctionIndex.contains(i)) {
                String correction = getCorrection(queryTokens[i]);
                if (correction == null) {
                    modified += queryTokens[i] + " ";
                } else {
                    modified += correction + " ";
                }
            }else{
                modified += queryTokens[i]+" ";
            }
        }
        return modified;
    }

    /**
     * Generate a spelling correction for the given token
     *
     * @param token mispelled query token
     * @return spelling correction of the given mispelled query
     */
    private String getCorrection(String token) {

        // Get the k-grams for the token
        List<String> qKGrams = getKGrams(token);

        // Get the vocabulary types that have k-grams in common with the query
        List<String> candidates = new ArrayList<String>();
        for (int i = 0; i < qKGrams.size(); i++) {
            if (kIndex.getPostingsList(qKGrams.get(i)) != null) {
                candidates = DiskQueryProcessor.unionList(candidates, kIndex.getPostingsList(qKGrams.get(i)));
            }
        }

        // Assume the first letter is correct
        String first2Gram = "$" + Character.toString(token.charAt(0));
        if (kIndex.getPostingsList(first2Gram) != null) {
            candidates = DiskQueryProcessor.intersectList(candidates, kIndex.getPostingsList(first2Gram));
        }

        // Calculate the Jaccard coefficient for the candidates
        // Perform edit distance on those that exceed the threshold
        List<String> editDistanceCandidates = new ArrayList<String>();
        for (String candidate : candidates) {
            // Get the k-grams for the candidate
            List<String> cKGrams = getKGrams(candidate);

            // Calculate the Jaccard coefficient
            List<String> intersection = DiskQueryProcessor.intersectList(qKGrams, cKGrams);
            int unionSize = qKGrams.size() + cKGrams.size() - intersection.size();
            double jCoefficient = (double) intersection.size() / unionSize;

            // Keep the candidates that exceed the threshold
            if (jCoefficient > JACCARD_THRESHOLD) {
                editDistanceCandidates.add(candidate);
            }
        }

        // Return if no further filtering is necessary
        if (editDistanceCandidates.isEmpty()) {
            return null;
        } else if (editDistanceCandidates.size() == 1) {
            return editDistanceCandidates.get(0);
        }

        // Select the candidate(s) with the lowest edit distance
        List<String> finalCandidates = new ArrayList<String>();
        finalCandidates.add(editDistanceCandidates.get(0)); // set the first candidate as the min
        int minDistance = editDistance(token, editDistanceCandidates.get(0));

        // Compare the edit distance with the remaining candidates
        for (int i = 1; i < editDistanceCandidates.size(); i++) {
            int distance = editDistance(token, editDistanceCandidates.get(i));
            if (distance < minDistance) {
                minDistance = distance; // update the lowest distance and candidate
                finalCandidates.clear(); // update the candidates list
                finalCandidates.add(editDistanceCandidates.get(i));
            } else if (distance == minDistance) { // tie
                finalCandidates.add(editDistanceCandidates.get(i));
            }
        }

        // If tie for lowest edit distance, select the type with the highest df
        if (finalCandidates.size() > 1) {
            String type = finalCandidates.get(0); // set the first candidate as the max
            String term = PorterStemmer.getStem(type);
            int maxDocFrequency = dIndex.getPostings(term).size();

            // Compare the df with the remaining candidates
            for (int i = 1; i < finalCandidates.size(); i++) {
                term = PorterStemmer.getStem(finalCandidates.get(i));
                int docFrequency = dIndex.getPostings(term).size();
                if (docFrequency > maxDocFrequency) {
                    maxDocFrequency = docFrequency; // update the highest df
                    type = finalCandidates.get(i); // update the type to be returned
                }
            }
            return type;
        }

        return finalCandidates.get(0);
    }

    /**
     * Generate the 1-, 2-, and 3-grams for the given string
     *
     * @param type
     * @return list of k-grams
     */
    private List<String> getKGrams(String type) {
        // Generate all the k-grams for the query
        SortedSet<String> kgrams = new TreeSet<String>(); // ordered set
        String modifiedType = "$" + type + "$";
        for (int i = 0; i < type.length(); i++) {
            kgrams.add(Character.toString(type.charAt(i))); // 1-grams
        }
        for (int i = 0; i + 2 <= modifiedType.length(); i++) {
            kgrams.add(modifiedType.substring(i, i + 2)); // 2-grams
        }
        for (int i = 0; i + 3 <= modifiedType.length(); i++) {
            kgrams.add(modifiedType.substring(i, i + 3)); // 3-grams
        }
        return new ArrayList<String>(kgrams); // return a list
    }

    /**
     * Start the edit distance algorithm for 2 given strings
     *
     * @param string1
     * @param string2
     * @return edit distance between two strings
     */
    private int editDistance(String string1, String string2) {
        return editDistance(string1, string2, string1.length(), string2.length());
    }

    /**
     * Helper method for edit distance algorithm
     *
     * @param string1
     * @param string2
     * @param i length of string1
     * @param j length of string2
     * @return edit distance between two strings
     */
    private int editDistance(String string1, String string2, int i, int j) {
        if (i == 0) {
            return j;
        }
        if (j == 0) {
            return i;
        }
        if (string1.charAt(i - 1) == string2.charAt(j - 1)) {
            return editDistance(string1, string2, i - 1, j - 1);
        }
        return 1 + min(editDistance(string1, string2, i - 1, j),
                editDistance(string1, string2, i, j - 1),
                editDistance(string1, string2, i - 1, j - 1));
    }

    /**
     * Get the minimum of three integers
     *
     * @param a
     * @param b
     * @param c
     * @return minimum integer
     */
    private int min(int a, int b, int c) {
        return Math.min(Math.min(a, b), c);
    }

}
