package indexes;

import java.util.*;

/*
* Resuse from Homework3
*/
public class NaiveInvertedIndex {

    private HashMap<String, List<Integer>> mIndex;

    public NaiveInvertedIndex() {
        mIndex = new HashMap<String, List<Integer>>();
    }

    public void addTerm(String term, int documentID) {
        // TO-DO: add the term to the index hashtable. If the table does not have
        // an entry for the term, initialize a new ArrayList<Integer>, add the 
        // docID to the list, and put it into the map. Otherwise add the docID
        // to the list that already exists in the map, but ONLY IF the list does
        // not already contain the docID.
        
        if(mIndex.containsKey(term)){ // the index contains the term
            if(!mIndex.get(term).contains(documentID)){ // check postings list
                mIndex.get(term).add(documentID);
            }
        }
        else{ // add the term to the index hashtable and the docID to the list
            mIndex.put(term, new ArrayList<Integer>());
            mIndex.get(term).add(documentID);
        }

    }

    public List<Integer> getPostings(String term) {
        // TO-DO: return the postings list for the given term from the index map.
        return mIndex.get(term);
    }

    public int getTermCount() {
        // TO-DO: return the number of terms in the index.
        return mIndex.size();
    }

    public String[] getDictionary() {
        // TO-DO: fill an array of Strings with all the keys from the hashtable.
        // Sort the array and return it.
        String[] keys = new String[this.getTermCount()];
        keys = mIndex.keySet().toArray(keys); 
        Arrays.sort(keys);
        return keys;
    }
}
