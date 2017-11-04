package query.processor;


import retrivals.booleanRetrival.*;
import query.Subquery;
import query.processor.TokenProcessorStream;
import helper.PorterStemmer;
import indexes.PositionalPosting;
import indexes.PositionalInvertedIndex;
import indexes.SoundexIndex;
import indexes.KGramIndex;
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
public class QueryProcessor {

    private static List<List<PositionalPosting>> andCollection = new ArrayList<List<PositionalPosting>>();

    /**
     * Add the positional postings list of an AND query to the collection of AND
     * query positional postings lists.
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

        if (preLiteral.contains("\"") && !preLiteral.contains("near")) {
            masterList = phraseQuery(preLiteral, posIndex);
        } else if (preLiteral.contains("*")) {
            System.out.println("preliteral: " + preLiteral);
            masterList = QueryProcessor.wildcardQuery(preLiteral, posIndex, kgIndex);
        } else if (preLiteral.contains("near")) {
            masterList = nearQuery(preLiteral, posIndex);
        } else if (posIndex.getPostingsList(preLiteral) != null) {
            masterList = posIndex.getPostingsList(preLiteral);
        }

        /* Merge all of the postings lists of each query literal
        of a given AND query into one master postings list. */
        if (andQueryLiterals.getSize() > 1) {

            for (int i = 1; i < andQueryLiterals.getSize(); i++) {
                String currentLiteral = andQueryLiterals.getLiterals().get(i);

                if (currentLiteral.contains("\"")) {
                    System.out.println("current literal: " + currentLiteral);
                    intermediateList = phraseQuery(currentLiteral, posIndex);
                    if (masterList != null && intermediateList != null) {
                        masterList = intersectList(masterList, intermediateList);
                    }
                } else if (currentLiteral.contains("*")) {
                    intermediateList = QueryProcessor.wildcardQuery(currentLiteral, posIndex, kgIndex);
                    masterList = intersectList(masterList, intermediateList);
                } else if (currentLiteral.contains("near")) {
                    intermediateList = nearQuery(currentLiteral, posIndex);
                    masterList = intersectList(masterList, intermediateList);
                } else if (posIndex.getPostingsList(currentLiteral) != null) {
                    masterList = intersectList(masterList, posIndex.getPostingsList(currentLiteral));
                } else {
                    masterList.clear();
                }
            }
        }
        // Add this AND postings list to the collection of AND postings lists
        if (masterList != null) {
            andCollection.add(masterList);
        } else {
            andCollection.add(masterList);
            masterList.clear();
        }
    }

    /**
     * Run all AND Queries Q_i, store results in collection of positional
     * postings lists, then run an OR query that merges all postings lists into
     * one master positional postings list representing the entire query.
     *
     * @param allQueries List of subqueries that represents user query
     * @param posIndex Positional inverted index of selected corpus
     * @param kgIndex KGram index of all types in corpus
     * @return Positional Posting list representing all AND queries merged
     * together using OR logic.
     */
    public static List<PositionalPosting> orQuery(List<Subquery> allQueries, PositionalInvertedIndex posIndex, KGramIndex kgIndex) {

        // Add all Q_i positional postings lists to AndCollection
        for (int i = 0; i < allQueries.size(); i++) {
            addAndQuery(allQueries.get(i), posIndex, kgIndex);
        }

        // Merge all Q_i postings list into Master List using OR intersection
        List<PositionalPosting> masterList = andCollection.get(0);

        if (andCollection.size() > 1) {
            for (int i = 1; i < andCollection.size(); i++) {
                masterList = unionList(masterList, andCollection.get(i));
            }
        }
        andCollection.clear();
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
    public static List<PositionalPosting> wildcardQuery(String wcQuery, PositionalInvertedIndex index, KGramIndex kGramIndex) {

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
                    candidates = intersectList(candidates, kGramIndex.getPostingsList(kgrams[i]));
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
                    results = unionList(results, index.getPostingsList(term));
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
    public static List<PositionalPosting> phraseQuery(String phraseLiteral, PositionalInvertedIndex posIndex) {
        phraseLiteral = phraseLiteral.replaceAll("\"", "");
        String[] spPhrase = phraseLiteral.split(" ");
        List<PositionalPosting> phraseList = new ArrayList<PositionalPosting>();

        for (int i = 0; i < spPhrase.length; i++) {
            spPhrase[i] = PorterStemmer.getStem(spPhrase[i]);
        }

        if (posIndex.getPostingsList(spPhrase[0]) != null) {
            phraseList = posIndex.getPostingsList(spPhrase[0]);
            for (int j = 1; j < spPhrase.length; j++) {
                if (posIndex.getPostingsList(spPhrase[j]) != null) {
                    phraseList = positionalIntersect(phraseList,
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
        
        Scanner nearSearcher = new Scanner(nearLiteral);
        List<PositionalPosting> nearList = new ArrayList<PositionalPosting>();
        List<PositionalPosting> leftList = new ArrayList<PositionalPosting>();
        List<PositionalPosting> rightList = new ArrayList<PositionalPosting>();
        
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
            leftList = phraseQuery(spNear[0], posIndex);
        } else {
            if(posIndex.getPostingsList(spNear[0]) != null){
                leftList = posIndex.getPostingsList(spNear[0]);
            }
        }
        if (spNear[1].contains("\"")){
            rightList = phraseQuery(spNear[1], posIndex);
        } else {
            if(posIndex.getPostingsList(spNear[1]) != null){
                rightList = posIndex.getPostingsList(spNear[1]);
            }   
        }
        
        if (leftList.size() > 0 && rightList.size() > 0) {
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
    public static List<PositionalPosting> positionalIntersect(List<PositionalPosting> term1, List<PositionalPosting> term2, int k) {

        List<PositionalPosting> result = new ArrayList<PositionalPosting>();
        List<Integer> docs1 = new ArrayList<Integer>(); // term1 documents
        List<Integer> docs2 = new ArrayList<Integer>(); // term2 documents
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
                    // remove duplicate matches
                    while (!candidate.isEmpty() && Math.abs(candidate.get(0) - pp1.get(ii)) > k) {
                        candidate.remove(0);
                    }
                    // add candidates to the result 
                    for (Integer pos : candidate) {
                        int currentIndex = result.size() - 1;
                        if (!result.isEmpty() && result.get(currentIndex).getDocumentID() == docs1.get(i)) {
                            // the query appears more than once in the doc
                            // add the position to existing posting
                            result.get(currentIndex).addPosition(pos);
                        } else { // add a new posting to the result 
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
