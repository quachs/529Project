package retrievals.booleanRetrieval;

import query.processor.DiskQueryProcessor;
import query.parser.BooleanParser;
import indexes.KGramIndex;
import indexes.diskPart.DiskSoundexIndex;
import indexes.diskPart.DiskInvertedIndex;
import java.util.ArrayList;
import java.util.List;

/**
 * Class for process a boolean retrival.
 */
public class BooleanRetrival {

    public static List<String> booleanQuery(String query, boolean searchType, KGramIndex kIndex, DiskSoundexIndex sIndex, DiskInvertedIndex dIndex) {

        BooleanParser parser = new BooleanParser(dIndex, kIndex); // create the parser
        List<Integer> foundDocs = new ArrayList<Integer>(); // create a list to save found documents  
        if (searchType) {
            foundDocs = DiskQueryProcessor.authorQuery(query, sIndex); // save DocIds for author query
        } else {
            foundDocs = parser.getDocumentList(query);// if yes, parse query, save docID results 
        }
        List<String> res = new ArrayList<String>();
        if (foundDocs != null && foundDocs.size() > 0) {       
            for (int doc : foundDocs) {
                res.add(dIndex.getFileNames().get(doc));
            }
        } else {
            res.add("No document found."); // if no, print that there are no documents
        }
        return res;
    }
}
