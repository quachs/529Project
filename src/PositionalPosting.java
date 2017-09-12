import java.util.ArrayList;
import java.util.List;



public class PositionalPosting {
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
}
