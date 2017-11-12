package formulas;

import indexes.diskPart.DiskInvertedIndex;
import indexes.diskPart.DiskPosting;
import java.util.List;

/**
 * Abstract class for all formulars. They have to provide the three given
 * methods.
 */
public abstract class Formula {

    DiskInvertedIndex dIndex;

    /**
     * Save the Diskinverted Index for caluclating wqt, wtd and L_d
     *
     * @param dIndex
     */
    public Formula(DiskInvertedIndex dIndex) {
        this.dIndex = dIndex;
    }

    /**
     * Calculate the weight for the query and the given postingslist.
     *
     * @param tDocIDs List of postings
     * @return The weight as a double
     */
    public abstract double calcWQT(List<DiskPosting> tDocIDs);

    /**
     * Calculate the weight for term per document.
     *
     * @param dPosting The Posting that saves the term frequency in a document.
     * @return The weight as a double
     */
    public abstract double calcWDT(DiskPosting dPosting);

    /**
     * Calculate the lengh of the given document.
     *
     * @param docID ID of the document.
     * @return The lengh of the document
     */
    public abstract double getL_D(int docID);
}
