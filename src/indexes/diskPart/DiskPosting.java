package indexes.diskPart;

/**
 * Class to represent a positional posting on disk.
 *
 */
public class DiskPosting implements Comparable<DiskPosting> {

    private int documentID;
    private int termFrequency;
    private int[] positions;

    public DiskPosting(int documentID, int termFrequency, int[] positions) {
        this.documentID = documentID;
        this.termFrequency = termFrequency;
        this.positions = positions;
    }

    public DiskPosting(int documentID, int termFrequency) {
        this.documentID = documentID;
        this.termFrequency = termFrequency;
        positions = null;
    }

    /**
     * @return the documentID
     */
    public int getDocumentID() {
        return documentID;
    }

    /**
     * @param documentID the documentID to set
     */
    public void setDocumentID(int documentID) {
        this.documentID = documentID;
    }

    /**
     * @return the termFrequency
     */
    public int getTermFrequency() {
        return termFrequency;
    }

    /**
     * @param termFrequency the termFrequency to set
     */
    public void setTermFrequency(int termFrequency) {
        this.termFrequency = termFrequency;
    }

    /**
     * @return the positions
     */
    public int[] getPositions() {
        return positions;
    }

    /**
     * @param positions the positions to set
     */
    public void setPositions(int[] positions) {
        this.positions = positions;
    }
    
    @Override
    public int compareTo(DiskPosting d) {
        return documentID - d.getDocumentID();
    }

}