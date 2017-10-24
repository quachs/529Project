
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

public class SpellingCorrection {

    /**
     * Generate a spelling correction for the given query type
     *
     * @param query mispelled query
     * @param kIndex k-gram index
     * @param index positional inverted index
     * @return spelling correction of the given mispelled query
     */
    public String getCorrection(String query, KGramIndex kIndex, PositionalInvertedIndex index) {

        // Get the k-grams for the query type
        List<String> qKGrams = getKGrams(query);

        // Get the vocabulary types that have k-grams in common with the query
        List<String> candidates = new ArrayList<String>();
        for (int i = 0; i < qKGrams.size(); i++) {
            if (kIndex.getPostingsList(qKGrams.get(i)) != null) {
                candidates = QueryProcessor.unionList(candidates, kIndex.getPostingsList(qKGrams.get(i)));
            }
        }

        // Assume the first letter is correct
        String first2Gram = "$" + Character.toString(query.charAt(0));
        if (kIndex.getPostingsList(first2Gram) != null) {
            candidates = QueryProcessor.intersectList(candidates, kIndex.getPostingsList(first2Gram));
        }

        // Calculate the Jaccard coefficient for the candidates
        // Perform edit distance on those that exceed the threshold
        List<String> editDistanceCandidates = new ArrayList<String>();
        for (String candidate : candidates) {
            // Get the k-grams for the candidate
            List<String> cKGrams = getKGrams(candidate);

            // Calculate the Jaccard coefficient
            List<String> intersection = QueryProcessor.intersectList(qKGrams, cKGrams);
            int unionSize = qKGrams.size() + cKGrams.size() - intersection.size();
            double jCoefficient = (double) intersection.size() / unionSize;

            // Keep the candidates that exceed the threshold
            if (jCoefficient > 0.75) {
                editDistanceCandidates.add(candidate);
            }
        }

        // Select the candidate(s) with the lowest edit distance
        List<String> finalCandidates = new ArrayList<String>();
        finalCandidates.add(editDistanceCandidates.get(0)); // set the first candidate as the min
        int minDistance = editDistance(query, editDistanceCandidates.get(0));
        for (int i = 1; i < editDistanceCandidates.size(); i++) {
            int distance = editDistance(query, editDistanceCandidates.get(i));
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
            int maxDocFrequency = index.getPostingsList(term).size();
            for (int i = 1; i < finalCandidates.size(); i++) {
                term = PorterStemmer.getStem(finalCandidates.get(i));
                int docFrequency = index.getPostingsList(term).size();
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
     * @param string1
     * @param string2
     * @return edit distance between two strings
     */
    private int editDistance(String string1, String string2) {
        return editDistance(string1, string2, string1.length(), string2.length());
    }

    /**
     * Helper method for edit distance algorithm
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
        return min(1 + editDistance(string1, string2, i - 1, j),
                1 + editDistance(string1, string2, i, j - 1),
                1 + editDistance(string1, string2, i - 1, j - 1));
    }
    
    /**
     * Get the minimum of three integers
     * @param a
     * @param b
     * @param c
     * @return minimum integer
     */
    private int min(int a, int b, int c) {
        return Math.min(Math.min(a, b), c);
    }

}
