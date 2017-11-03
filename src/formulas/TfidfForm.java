package formulas;

import indexes.diskPart.DiskInvertedIndex;
import indexes.diskPart.DiskPosting;
import java.util.List;

/**
 *
 * @author Sandra
 */
public class TfidfForm extends Formular{

    public TfidfForm(DiskInvertedIndex dIndex) {
        super(dIndex);
    }   

    @Override
    public double calcWQT(List<DiskPosting> tDocIDs) {
        return Math.log(((double) dIndex.getCorpusSize()) / ((double) tDocIDs.size()));
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
