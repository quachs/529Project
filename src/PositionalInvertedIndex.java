
import java.util.*;

public class PositionalInvertedIndex {

    private HashMap<String, List<PositionalPosting>> mIndex;

    public PositionalInvertedIndex() {
        mIndex = new HashMap<String, List<PositionalPosting>>();
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

    public int getTermCount() {
        return mIndex.size();
    }

    public String[] getDictionary() {
        String[] terms = new String[this.getTermCount()];
        terms = mIndex.keySet().toArray(terms);
        Arrays.sort(terms);
        return terms;
    }

    public List<Integer> getDocumentPostingsList(String term) {
        List<Integer> docList = new ArrayList<Integer>();
        for (PositionalPosting p : mIndex.get(term)) {
            docList.add(p.getDocumentID());
        }
        return docList;
    }

    public List<Integer> getDocumentTermPositions(String term, int docID) {
        int docIndex = Collections.binarySearch(getDocumentPostingsList(term), docID);
        if (docIndex >= 0) {
            return mIndex.get(term).get(docIndex).getTermPositions();
        }
        return null;
    }
    
    public List<PositionalPosting> getPostingsList(String term){
        return mIndex.get(term);
    }

}
