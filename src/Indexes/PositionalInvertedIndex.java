package Indexes;


import Indexes.Index;
import java.util.*;

/**
 * Class for a positional inverted index
 *
 */
public class PositionalInvertedIndex extends Index<PositionalPosting> {

    public PositionalInvertedIndex() {
        super();
    }

    public void addTerm(String term, int docID, int position) {
        if (mIndex.containsKey(term)) { // the index contains the term
            List<PositionalPosting> postingsList = mIndex.get(term);
            if (docID == postingsList.get(postingsList.size() - 1).getDocumentID()) {
                // add position to the existing posting
                postingsList.get(postingsList.size() - 1).addPosition(position);
            } else { // add a new posting
                postingsList.add(new PositionalPosting(docID, position));
            }
        } else { // add the term and new posting to the index
            mIndex.put(term, new ArrayList<PositionalPosting>());
            mIndex.get(term).add(new PositionalPosting(docID, position));
        }
    }

    /**
     * Get a list of docIDs for the given term
     *
     * @param term
     * @return list of docIDs
     */
    public List<Integer> getDocumentPostingsList(String term) {
        List<Integer> docList = new ArrayList<Integer>();
        for (PositionalPosting p : mIndex.get(term)) {
            docList.add(p.getDocumentID());
        }
        return docList;
    }

    /**
     * Get a list of term position for the given term and docID
     *
     * @param term
     * @param docID
     * @return list of term positions
     */
    public List<Integer> getDocumentTermPositions(String term, int docID) {
        int docIndex = Collections.binarySearch(getDocumentPostingsList(term), docID);
        if (docIndex >= 0) {
            return mIndex.get(term).get(docIndex).getTermPositions();
        }
        return null;
    }
    

}
