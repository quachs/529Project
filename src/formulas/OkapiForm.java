package formulas;

import indexes.diskPart.DiskInvertedIndex;
import indexes.diskPart.DiskPosting;

/**
 *
 * @author Sandra
 */
public class OkapiForm extends Formular {

    private DiskPosting dPosting;

    public OkapiForm(DiskInvertedIndex dIndex) {
        super(dIndex);
    }

    @Override
    public double calcWQT(DiskPosting[] tDocIDs) {
        double res = Math.log((((double) (dIndex.getCorpusSize() - tDocIDs.length)) + 0.5) / ((double) tDocIDs.length) + 0.5);
        return Math.max(0.1, res);
    }

    @Override
    public double calcWDT(DiskPosting dPosting) {
        return 2.2 * dPosting.getTermFrequency();
    }

    @Override
    public double getL_D(int docID) {
        return (1.2 * (0.25 + 0.75 * (dIndex.getDocLength(docID) / dIndex.getAvgDocLength()))) + dPosting.getTermFrequency();
    }

    public void setdPosting(DiskPosting dPosting) {
        this.dPosting = dPosting;
    }

    
}
