package formulas;

import indexes.diskPart.DiskInvertedIndex;
import indexes.diskPart.DiskPosting;
import java.util.List;

/**
 * Formular for calclatiing the traditional algorithm as discribed in the paper.
 */
public class TfidfForm extends Formula {

    /**
     * Call parent constructor to save the disk inverted index.
     *
     * @param dIndex The disk inverted index is needed for all calculations in
     * every formular.
     */
    public TfidfForm(DiskInvertedIndex dIndex) {
        super(dIndex);
    }

    /**
     * Calculate the weight for the query and the given postingslist.
     *
     * @param tDocIDs List of postings
     * @return The weight as a double
     */
    @Override
    public double calcWQT(List<DiskPosting> tDocIDs) {
        return Math.log(((double) dIndex.getCorpusSize()) / ((double) tDocIDs.size()));
    }

    /**
     * Calculate the weight for term per document.
     *
     * @param dPosting The Posting that saves the term frequency in a document.
     * @return The weight as a double
     */
    @Override
    public double calcWDT(DiskPosting dPosting) {
        return dPosting.getTermFrequency();
    }

    /**
     * Calculate the lengh of the given document.
     *
     * @param docID ID of the document.
     * @return The lengh of the document
     */
    @Override
    public double getL_D(int docID) {
        return dIndex.getDocWeight(docID);
    }

}
