package Retrivals.booleanRetrival;

// See README for references

import Helper.Subquery;
import Helper.PorterStemmer;
import Indexes.PositionalPosting;
import Indexes.PositionalInvertedIndex;
import Indexes.KGramIndex;
import java.util.*;

/**
 * Class to parse user query into query literals and
 * store in a data structure for later evaluation.
 *
 */
public interface QueryParser {

    /**
     * Splits up an individual query Q_i into its query literals.
     *
     * @param queryString All query literals of one AND query
     * @return Subquery data structure that stores all query literals
     */
    public Subquery collectAndQueries(String queryString);

    /**
     * Takes a string representing a query, returns list of relevant documents
     *
     * @param query A string representing a user query.
     * @return A list of Positional Postings representing the results of a user
     * query.
     */
    public List<Integer> getDocumentList(String query);
}
