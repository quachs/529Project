
import java.util.ArrayList;
import java.util.List;

/*

Class to process a phrase.

 */
public class Phrase {

    /**
     * Constructs a string representing a phrase.
     *
     * @param tokenReader
     * @param pBegCandidate Beginning of a phrase, identified by a left double
     * quote
     * @return Phrase, enclosed in double quotes. If phrase is only one term
     * long, a single term *without* double quotes is returned.
     */
    public static String getPhrase(QueryTokenStream tokenReader, String pBegCandidate) {
        QueryTokenStream phraseDetector = tokenReader;
        String phraseQuery = "", nextCandidate = "";

        if (pBegCandidate.startsWith("\"")) {
            phraseQuery = pBegCandidate;

            if (phraseQuery.endsWith("\"")) { //If phrase if only one token long
                PorterStemmer onePhraseStemmer = new PorterStemmer();
                phraseQuery = phraseQuery.replaceAll("\"", "");
                phraseQuery = onePhraseStemmer.getStem(phraseQuery);
                return phraseQuery;
            }

            nextCandidate = phraseDetector.nextToken();
            while (!nextCandidate.endsWith("\"")) { //build phrase
                phraseQuery = phraseQuery + " " + nextCandidate;

                if (phraseDetector.hasNextToken()) {
                    nextCandidate = phraseDetector.nextToken();
                }
            }
            phraseQuery = phraseQuery + " " + nextCandidate;
            return phraseQuery;
        } else {
            pBegCandidate = phraseDetector.nextToken();
        }
        return phraseQuery;
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
     *
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
