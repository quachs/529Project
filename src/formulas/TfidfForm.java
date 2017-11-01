package formulas;

import indexes.diskPart.DiskInvertedIndex;
import indexes.diskPart.DiskPosting;

/**
 *
 * @author Sandra
 */
public class TfidfForm extends Formular{

    public TfidfForm(DiskInvertedIndex dIndex) {
        super(dIndex);
    }   

    @Override
    public double calcWQT(DiskPosting[] tDocIDs) {
        return Math.log((double) dIndex.getCorpusSize()) / ((double) tDocIDs.length);
    }

    @Override
    public double calcWDT(DiskPosting dPosting) {
        return dPosting.getTermFrequency();
    }

    @Override
    public double getL_D(int docID) {
        return dIndex.getDocWeight(docID);
    }
    
}
