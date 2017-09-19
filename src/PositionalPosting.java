import java.util.ArrayList;
import java.util.List;


/**
 * Posting for a positional inverted index
 * 
 */
public class PositionalPosting implements Comparable<PositionalPosting>{
    private int documentID;
    private List<Integer> termPositions;
    
    public PositionalPosting(int documentID, int position){
        this.documentID = documentID;
        this.termPositions = new ArrayList<Integer>();
        this.termPositions.add(position);
    }

    /**
     * @return the docID
     */
    public int getDocumentID() {
        return documentID;
    }

    /**
     * @return the position
     */
    public List<Integer> getTermPositions() {
        return termPositions;
    }
    
    /**
     * Add a position to this posting
     * @param position 
     */
    public void addPosition(int position){
        termPositions.add(position);
    }
    
    @Override
    public int compareTo(PositionalPosting p){
        if(documentID == p.getDocumentID()){
            return 0;
        }
        if(documentID > p.getDocumentID()){
            return 1;
        }
        return -1;
    }
}