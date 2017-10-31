package retrivals.booleanRetrival;

import query.processor.QueryProcessor;
import query.parser.BooleanParser;
import query.parser.QueryParser;
import indexes.KGramIndex;
import indexes.PositionalInvertedIndex;
import indexes.SoundexIndex;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Sandra
 */
public class BooleanRetrival {

    public BooleanRetrival() {

    }

    public static List<String> booleanQuery(String query, PositionalInvertedIndex posIndex, boolean searchType, KGramIndex kIndex, SoundexIndex sIndex, ArrayList<String> fileNames) {

        BooleanParser parser = new BooleanParser(posIndex, kIndex); // create the parser
        List<Integer> foundDocs; // create a list to save found documents  
        if (searchType) {
            foundDocs = QueryProcessor.authorQuery(query, sIndex); // save DocIds for author query
        } else {
            foundDocs = parser.getDocumentList(query);// if yes, parse query, save docID results 
        }
        List<String> res = new ArrayList<String>();
        if (foundDocs != null && foundDocs.size() > 0) {
            for (int doc : foundDocs) {
                res.add(posIndex.getDictionary()[doc]);
            }
        } else {
            res.add("No document found."); // if no, print that there are no documents
        }
        return res;
    }
}
