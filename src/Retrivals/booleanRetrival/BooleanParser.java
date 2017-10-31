package Retrivals.booleanRetrival;

// See README for references

//import .*;
import java.util.*;
import Indexes.*;
import Helper.*;

/**
 * Class to parse user query into query literals and
 * store in a data structure for later evaluation.
 *
 */
public class BooleanParser implements QueryParser{

    private PositionalInvertedIndex posIndex;
    private KGramIndex kgIndex;

    public BooleanParser(PositionalInvertedIndex posIndex, KGramIndex kgIndex) {
        this.posIndex = posIndex;
        this.kgIndex = kgIndex;
    }

    /**
     * Splits up an individual query Q_i into its query literals.
     *
     * @param queryString All query literals of one AND query
     * @return Subquery data structure that stores all query literals
     */
    @Override
    public Subquery collectAndQueries(String queryString) {

        Subquery andQueries = new Subquery();
        QueryTokenStream andReader = new QueryTokenStream(queryString);

        while (andReader.hasNextToken()) {
            String pBegCandidate = andReader.nextToken();

            // Phrase candidate must start with a left double quote
            System.out.println("phrase candidate: " + pBegCandidate);
            if (pBegCandidate.startsWith("\"")) {
                String phrase = getPhrase(andReader, pBegCandidate);
                System.out.println("returned phrase: " + phrase);
                 
                 if (andReader.hasNextToken()) {
                    String nearCandidate = andReader.nextToken();
                    
                    /* Search for "near" keyword.  If found,
                        rebuild near literal in the form [term1] near[k] [term2] */
                    if (nearCandidate.contains("near")) {
                        String lNearOp = phrase;
                        String rNearOp = andReader.nextToken();

                        if (rNearOp.startsWith("\"")){
                            rNearOp = getPhrase(andReader, rNearOp);
                        }

                        String nearLiteral = lNearOp + " " + nearCandidate + " " + rNearOp;     
                        andQueries.addLiteral(nearLiteral);
                    } else {
                        /* Add original token, pBegCandidate
                        Since reader was advanced past original token in order to
                        look for "near," nearCandidate token must be added here */
                        andQueries.addLiteral(phrase);
                        andQueries.addLiteral(nearCandidate);
                    }
                 } else {
                     andQueries.addLiteral(phrase);
                 }
            } else if (andReader.hasNextToken()) {
                String nearCandidate = andReader.nextToken();

                /* Search for "near" keyword.  If found,
                    rebuild near literal in the form [term1] near[k] [term2] */
                if (nearCandidate.contains("near")) {
                    String lNearOp = pBegCandidate;
                    String rNearOp = andReader.nextToken();
                    
                    if (rNearOp.startsWith("\"")){
                        rNearOp = getPhrase(andReader, rNearOp);
                    }
                    
                    String nearLiteral = lNearOp + " " + nearCandidate + " " + rNearOp;
                    andQueries.addLiteral(nearLiteral);
                } else {
                    /* Add original token, pBegCandidate
                        Since reader was advanced past original token in order to
                        look for "near," nearCandidate token must be added here */
                    andQueries.addLiteral(pBegCandidate);
                    
                    if (nearCandidate.startsWith("\"")){
                        nearCandidate = getPhrase(andReader, nearCandidate);
                    }
                    
                    andQueries.addLiteral(nearCandidate);
                }
            } else {
                andQueries.addLiteral(pBegCandidate);
            }
        }
        return andQueries;
    }

    /**
     * Splits up query into subqueries Q_1...Q_k, placing AND queries into
     * collection.
     *
     * @param query All query literals of a user query.
     * @return A list of all Subquery structures; essentially, a list of
     * Positional Posting lists
     */
    public List<Subquery> collectOrQueries(String query) {

        Scanner tReader = new Scanner(query);
        List<Subquery> allQueries = new ArrayList<Subquery>();
        String qString = "";

        // Constructs a complete AND query Q_i (stops at OR token ("+"));
        while (tReader.hasNext()) {
            String plusCandidate = tReader.next();

            if (!plusCandidate.equals("+")) {
                qString = qString + " " + plusCandidate;

                if (!tReader.hasNext()) {
                    allQueries.add(collectAndQueries(qString));
                }
            } else {
                allQueries.add(collectAndQueries(qString));
                qString = ""; // clears string for later use          
            }
        }
        return allQueries;
    }

    /**
     * Constructs a string representing a phrase.
     *
     * @param tokenReader
     * @param pBegCandidate Beginning of a phrase, identified by a left double
     * quote
     * @return Phrase, enclosed in double quotes. If phrase is only one term
     * long, a single term *without* double quotes is returned.
     */
    public String getPhrase(QueryTokenStream tokenReader, String pBegCandidate) {
        String phraseQuery = "", nextCandidate = "";

        if (pBegCandidate.startsWith("\"")) {
            phraseQuery = pBegCandidate;

            if (phraseQuery.endsWith("\"")) { // If phrase if only one token long
                System.out.println("phrase is only one query long");
                PorterStemmer onePhraseStemmer = new PorterStemmer();
                phraseQuery = phraseQuery.replaceAll("\"", "");
                phraseQuery = onePhraseStemmer.getStem(phraseQuery);
                return phraseQuery;
            }

            nextCandidate = tokenReader.nextToken();
            System.out.println("next candidate: " + nextCandidate);
            while (!nextCandidate.endsWith("\"")) { // build phrase
                phraseQuery = phraseQuery + " " + nextCandidate;

                if (tokenReader.hasNextToken()) {
                    nextCandidate = tokenReader.nextToken();
                }
            }
            phraseQuery = phraseQuery + " " + nextCandidate;
            return phraseQuery;
        } else {
            pBegCandidate = tokenReader.nextToken();
        }
        return phraseQuery;
    }

    /**
     * Takes a string representing a query, returns list of relevant documents
     *
     * @param query A string representing a user query.
     * @return A list of Positional Postings representing the results of a user
     * query.
     */
    @Override
    public List<Integer> getDocumentList(String query) {

        // Parse query, store in a collection, perform the query, return a final postings list.
        List<Subquery> allQueries = collectOrQueries(query);
        List<PositionalPosting> masterPostings = QueryProcessor.orQuery(allQueries, posIndex, kgIndex);
        List<Integer> documentList = new ArrayList<Integer>();

        // Constuct a list of document IDs from this final postings list.
        if (masterPostings.size() > 0) {
            for (int i = 0; i < masterPostings.size(); i++) {
                int currentDocID = masterPostings.get(i).getDocumentID();
                documentList.add(currentDocID);
            }
        }
        return documentList;
    }
}
