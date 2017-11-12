package formulas;

import indexes.diskPart.DiskInvertedIndex;
import indexes.diskPart.DiskPosting;
import java.util.List;

/**
 * Formular for calclatiing the okapi algorithm as discribed in the paper.
 */
public class OkapiForm extends Formula {

    private DiskPosting dPosting;

    /**
     * Call parent constructor to save the disk inverted index.
     *
     * @param dIndex The disk inverted index is needed for all calculations in
     * every formular.
     */
    public OkapiForm(DiskInvertedIndex dIndex) {
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
        double res = Math.log((((double) (dIndex.getCorpusSize() - tDocIDs.size())) + 0.5) / (((double) tDocIDs.size()) + 0.5));
        return Math.max(0.1, res);
    }

    /**
     * Calculate the weight for term per document.
     *
     * @param dPosting The Posting that saves the term frequency in a document.
     * @return The weight as a double
     */
    @Override
    public double calcWDT(DiskPosting dPosting) {
        return (2.2 * ((double) dPosting.getTermFrequency()));
    }

    /**
     * Calculate the lengh of the given document.
     *
     * @param docID ID of the document.
     * @return The lengh of the document
     */
    @Override
    public double getL_D(int docID) {
        return (1.2 * (0.25 + 0.75 * (((double) dIndex.getDocLength(docID)) / ((double) dIndex.getAvgDocLength())))) + ((double) dPosting.getTermFrequency());
    }

    /**
     * This algorithm is the only one who needs the term frequency for a
     * document to calculate the lengh of the document. It is set during the
     * runtime when it is needed.
     *
     * @param dPosting Posting that saves the term frequency.
     */
    public void setdPosting(DiskPosting dPosting) {
        this.dPosting = dPosting;
    }
}
