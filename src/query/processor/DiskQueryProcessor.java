package query.processor;

import helper.PorterStemmer;
import indexes.KGramIndex;
import indexes.PositionalPosting;
import indexes.SoundexIndex;
import indexes.diskPart.DiskInvertedIndex;
import indexes.diskPart.DiskPosting;
import query.processor.QueryProcessor;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Scanner;

/**
 * Class to process various types of queries
 *
 */
public class DiskQueryProcessor {

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
    
    /**
     * Uses positional intersection algorithm to merge all phrase terms into one
     * final list representing the results for the entire phrase.
     *
     * @param phraseLiteral A sequential set of terms enclosed in double quotes
     * @param posIndex Positional inverted index of selected corpus
     * @return A PositionalPosting list of the results of the phrase query.
     */
    public static DiskPosting[] phraseQuery(String phraseLiteral, DiskInvertedIndex dIndex) {
        phraseLiteral = phraseLiteral.replaceAll("\"", "");
        String[] spPhrase = phraseLiteral.split(" ");
        DiskPosting[] phraseList = new DiskPosting[0]; //= new ArrayList<PositionalPosting>();

        for (int i = 0; i < spPhrase.length; i++) {
            spPhrase[i] = PorterStemmer.getStem(spPhrase[i]);
        }

        if (dIndex.getPostings(spPhrase[0]) != null) {
            phraseList = dIndex.getPostings(spPhrase[0]);
            for (int j = 1; j < spPhrase.length; j++) {
                if (dIndex.getPostings(spPhrase[j]) != null) {
                    phraseList = positionalIntersect(phraseList,
                            dIndex.getPostings(spPhrase[j]), 1);
                } else { // return empty list
                    //phraseList.clear();
                    return phraseList;

                }
            }
        } else { // return empty list
            phraseList = new DiskPosting[0];
            return phraseList;
        }
        return phraseList;
    }
    
    /**
     * Uses disk intersection algorithm to merge two terms in a string
     * literal that contains the NEAR operator.
     *
     * @param nearLiteral Near literal in the form [term1] near[k] [term2]
     * @param dIndex Positional inverted index of selected corpus
     * @return List resulting from the disk intersect of term1 and term2
     */
    public static DiskPosting[] nearQuery(String nearLiteral, DiskInvertedIndex dIndex) {
        
        Scanner nearSearcher = new Scanner(nearLiteral);
        DiskPosting[] nearList = new DiskPosting[0]; // = new ArrayList<DiskPosting>();
        DiskPosting[] leftList = new DiskPosting[0]; // = new ArrayList<DiskPosting>();
        DiskPosting[] rightList = new DiskPosting[0]; // = new ArrayList<DiskPosting>();
        
        // https://docs.oracle.com/javase/tutorial/java/data/converting.html                    
        int k = 0;
        
        while(nearSearcher.hasNext()){
            String nearCandidate = nearSearcher.next();
            if (nearCandidate.startsWith("near")){
                k = Integer.valueOf(nearCandidate.substring(5));
                break;
            }
        }
        
        String[] spNear = nearLiteral.split(" near/[\\d+] ");
        
        if (spNear[0].contains("\"")){
            leftList = phraseQuery(spNear[0], dIndex);
        } else {
            if(dIndex.getPostings(spNear[0]) != null){
                leftList = dIndex.getPostings(spNear[0]);
            }
        }
        if (spNear[1].contains("\"")){
            rightList = phraseQuery(spNear[1], dIndex);
        } else {
            if(dIndex.getPostings(spNear[1]) != null){
                rightList = dIndex.getPostings(spNear[1]);
            }   
        }
        
        if (leftList.length > 0 && rightList.length > 0) {
            nearList = positionalIntersect(leftList, rightList, k);
        }
        return nearList;
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
                result = unionList(result, sIndex.getPostingsList(name));
            }
        }
        return result;
    }

    /**
     * Get the intersection of two ordered lists
     *
     * @param <T>
     * @param list1 list of comparable elements
     * @param list2 list of comparable elements
     * @return resulting list of intersecting list1 and list2
     */
    public static <T extends Comparable> List<T> intersectList(List<T> list1, List<T> list2) {
        List<T> result = new ArrayList<T>();
        int i = 0;
        int j = 0;

        while (i < list1.size() && j < list2.size()) {
            if (list1.get(i).compareTo(list2.get(j)) == 0) {
                result.add(list1.get(i));
                i++;
                j++;
            } else if (list1.get(i).compareTo(list2.get(j)) < 0) { // list1 before list2
                i++;
            } else { // list2 before list1
                j++;
            }
        }

        return result;
    }

    /**
     * Get the union of two ordered lists
     *
     * @param <T>
     * @param list1 list of comparable elements
     * @param list2 list of comparable elements
     * @return resulting list of the union of list1 and list2
     */
    public static <T extends Comparable> List<T> unionList(List<T> list1, List<T> list2) {
        List<T> result = new ArrayList<T>();
        int i = 0;
        int j = 0;

        while (i < list1.size() && j < list2.size()) {
            if (list1.get(i).compareTo(list2.get(j)) == 0) {
                result.add(list1.get(i));
                i++;
                j++;
            } else if (list1.get(i).compareTo(list2.get(j)) < 0) { // list1 before list2 
                result.add(list1.get(i));
                i++;
            } else { // list2 before list1
                result.add(list2.get(j));
                j++;
            }
        }

        // append the longer list to the results
        if (i < list1.size()) {
            while (i < list1.size()) {
                result.add(list1.get(i));
                i++;
            }
        } else if (j < list2.size()) {
            while (j < list2.size()) {
                result.add(list2.get(j));
                j++;
            }
        }

        return result;
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
    public static DiskPosting[] positionalIntersect(DiskPosting[] term1, DiskPosting[] term2, int k) {

        List<PositionalPosting> result = new ArrayList<PositionalPosting>();
        int[] docs1 = new int[term1.length]; // term1 documents
        int[] docs2 = new int[term2.length]; // term2 documents
        int i = 0; // term1 document index
        int j = 0; // term2 document index

        // load the docIDs to array
        for (int x = 0; x < term1.length; x++) {
            docs1[x] = term1[x].getDocumentID();
        }
        for (int x = 0; x < term2.length; x++) {
            docs2[x] = term2[x].getDocumentID();
        }

        // intersect the docs
        while (i < docs1.length && j < docs2.length) {
            // both terms appear in the doc
            if ((int) docs1[i] == docs2[j]) {
                List<Integer> candidate = new ArrayList<Integer>();
                int[] pp1 = term1[i].getPositions(); // term1 positions
                int[] pp2 = term2[j].getPositions(); // term2 positions
                int ii = 0; // term1 position index
                int jj = 0; // term2 position index

                // check if term2 appears within k positions after term1
                while (ii < pp1.length) {
                    while (jj < pp2.length) {
                        int relativePos = pp2[jj] - pp1[ii];
                        if (relativePos > 0 && relativePos <= k) {
                            // add term2 position to candidates
                            candidate.add(pp2[jj]);
                        } else if (pp2[jj] > pp1[ii]) {
                            break;
                        }
                        jj++;
                    }
                    // remove duplicate matches
                    while (!candidate.isEmpty() && Math.abs(candidate.get(0) - pp1[ii]) > k) {
                        candidate.remove(0);
                    }
                    // add candidates to the result 
                    for (Integer pos : candidate) {
                        int currentIndex = result.size() - 1;
                        if (!result.isEmpty() && result.get(currentIndex).getDocumentID() == docs1[i]) {
                            // the query appears more than once in the doc
                            // add the position to existing posting
                            result.get(currentIndex).addPosition(pos);
                        } else { // add a new posting to the result 
                            result.add(new PositionalPosting(docs1[i], pos));
                        }
                    }
                    ii++;
                }
                i++;
                j++;
            } else if (docs1[i] < docs2[j]) {
                i++;
            } else {
                j++;
            }
        }

        // Convert list of positional posting to array of disk posting
        DiskPosting[] diskResult = new DiskPosting[result.size()];
        for(int x = 0; x < result.size(); x++) {
            int docID = result.get(x).getDocumentID();
            int termFrequency = result.get(x).getTermPositions().size();
            int[] positions = new int[termFrequency];
            for(int y = 0; y < termFrequency; y++) {
                positions[y] = result.get(x).getTermPositions().get(y);
            }
            diskResult[x] = new DiskPosting(docID, termFrequency, positions);
        }
        return diskResult;
    }
}