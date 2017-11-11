package formulas;

import indexes.diskPart.DiskInvertedIndex;
import indexes.diskPart.DiskPosting;
import java.util.List;
/**
 *
 * @author Sandra
 */
public class DefaultForm extends Formula{

    public DefaultForm(DiskInvertedIndex dIndex) {
        super(dIndex);
    }
    
    @Override
    public double calcWQT(List<DiskPosting> tDocIDs) {
        return Math.log((((double) dIndex.getCorpusSize()) / ((double) tDocIDs.size())) + 1);
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
