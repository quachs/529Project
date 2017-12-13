package indexes;

import java.util.ArrayList;
import java.util.List;

/**
 * Posting for a positional inverted index
 *
 */
public class PositionalPosting implements Comparable<PositionalPosting> {

    private int documentID;
    private List<Integer> termPositions;

    public PositionalPosting(int documentID, int position) {
        this.documentID = documentID;
        termPositions = new ArrayList<Integer>();
        termPositions.add(position);
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
     *
     * @param position
     */
    public void addPosition(int position) {
        termPositions.add(position);
    }

    @Override
    public int compareTo(PositionalPosting p) {
        return documentID - p.getDocumentID();
    }

    @Override
    public String toString() {
        return "<" + documentID + ":" + termPositions.toString() + ">";
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof PositionalPosting) {
            if (documentID == ((PositionalPosting) o).documentID) {
                return true;
            }
        }
        return false;
    }
}
