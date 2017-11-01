package formulas;

import indexes.diskPart.DiskInvertedIndex;
import indexes.diskPart.DiskPosting;

/**
 *
 * @author Sandra
 */
public class WackyForm extends Formular{

    public WackyForm(DiskInvertedIndex dIndex) {
        super(dIndex);
    }
    
    @Override
    public double calcWQT(DiskPosting[] tDocIDs) {
        int dft = tDocIDs.length;
        double res = Math.log(((double) (dIndex.getCorpusSize() - dft)) / (double) dft);
        return Math.max(0, res);
    }

    @Override
    public double calcWDT(DiskPosting dPosting) {
        int tftd = dPosting.getTermFrequency();
        double ave = dIndex.getAvgTermFrequency(dPosting.getDocumentID());
        return (1 + Math.log(tftd)) / (1 + Math.log(ave));
    }

    @Override
    public double getL_D(int docID) {
        return Math.sqrt(dIndex.getDocSize(docID));
    }
}
