package query.parser;

// See README for references

import query.Subquery;
import indexes.KGramIndex;
import indexes.diskPart.DiskInvertedIndex;
import query.parser.QueryParser;
import query.QueryTokenStream;
import retrivals.rankedRetrival.RankedItem;
import retrivals.rankedRetrival.RankedRetrieval;
import java.util.*;

/**
 * Class to parse user query into query literals and
 * store in a data structure for later evaluation.
 *
 */
class RankedParser implements QueryParser{

    private DiskInvertedIndex dIndex;
    private KGramIndex kgIndex;

    RankedParser(DiskInvertedIndex dIndex) {
        this.dIndex = dIndex;
    }
    
    RankedParser(DiskInvertedIndex dIndex, KGramIndex kgIndex) {
        this.dIndex = dIndex;
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
            andQueries.addLiteral(pBegCandidate);
        }
        return andQueries;
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
        Subquery allQueryLiterals = collectAndQueries(query);
        RankedItem[] masterPostings = RankedRetrieval.rankedQuery(dIndex, kgIndex, allQueryLiterals, 10);
        List<Integer> documentList = new ArrayList<Integer>();

        // Constuct a list of document IDs from this final postings list.
        if (masterPostings.length > 0) {
            for (int i = 0; i < masterPostings.length; i++) {
                int currentDocID = masterPostings[i].getDocID();
                documentList.add(currentDocID);
            }
        }
        return documentList;
    } 
}