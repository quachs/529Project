/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Retrivals.booleanRetrival;

import Indexes.KGramIndex;
import Indexes.PositionalInvertedIndex;
import Indexes.SoundexIndex;
import Indexes.diskPart.DiskInvertedIndex;
import Indexes.diskPart.DiskPosting;
import Threads.GeneratingTask;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JLabel;

/**
 *
 * @author Sandra
 */
public class BooleanRetrival {

    public BooleanRetrival() {

    }

    public static List<String> booleanQuery(String query, PositionalInvertedIndex posIndex, boolean searchType, KGramIndex kIndex, SoundexIndex sIndex, ArrayList<String> fileNames) {

        QueryParser parser = new QueryParser(posIndex, kIndex); // create the parser                
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
