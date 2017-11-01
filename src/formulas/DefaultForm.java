package formulas;

import indexes.diskPart.DiskInvertedIndex;
import indexes.diskPart.DiskPosting;

/**
 *
 * @author Sandra
 */
public class DefaultForm extends Formular{

    public DefaultForm(DiskInvertedIndex dIndex) {
        super(dIndex);
    }
    
    @Override
    public double calcWQT(DiskPosting[] tDocIDs) {
        return Math.log((((double) dIndex.getCorpusSize()) / ((double) tDocIDs.length)) + 1);
    }

    @Override
    public double calcWDT(DiskPosting dPosting) {
        return (1 + Math.log(dPosting.getTermFrequency()));
    }

    @Override
    public double getL_D(int docID) {
        return dIndex.getDocWeight(docID);
    }
    
}
