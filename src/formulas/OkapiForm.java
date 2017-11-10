package formulas;

import indexes.diskPart.DiskInvertedIndex;
import indexes.diskPart.DiskPosting;
import java.util.List;

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
    public double calcWQT(List<DiskPosting> tDocIDs) {
        double res = Math.log((((double) (dIndex.getCorpusSize() - tDocIDs.size())) + 0.5) / (((double) tDocIDs.size()) + 0.5));
        return Math.max(0.1, res);
    }

    @Override
    public double calcWDT(DiskPosting dPosting) {
        if(dIndex.getFileNames().get(dPosting.getDocumentID()).equals("769.json")){
            System.out.println("OK: json TermFrequency-WDT for "+dPosting.getDocumentID()+": "+dPosting.getTermFrequency());
        }
        if(dPosting.getDocumentID() == 744){
            System.out.println("OK: 744 TermFrequency-WDT for "+dPosting.getDocumentID()+": "+dPosting.getTermFrequency());
        }
        return (2.2 * dPosting.getTermFrequency());
    }

    @Override
    public double getL_D(int docID) {
        if(dIndex.getFileNames().get(docID).equals("769.json")){
            System.out.println("OK: json DocLengh for "+dPosting.getDocumentID()+": "+dIndex.getDocLength(docID));
            System.out.println("OK: json AverageDoc for "+dPosting.getDocumentID()+": "+dIndex.getAvgDocLength());
            System.out.println("OK: json TermFrequency-LD for "+dPosting.getDocumentID()+": "+dPosting.getTermFrequency());
        }
        if(docID==744){
            System.out.println("OK: 744 DocLengh for "+dPosting.getDocumentID()+": "+dIndex.getDocLength(docID));
            System.out.println("OK: 744 AverageDoc for "+dPosting.getDocumentID()+": "+dIndex.getAvgDocLength());
            System.out.println("OK: 744 TermFrequency-LD for "+dPosting.getDocumentID()+": "+dPosting.getTermFrequency());
        }
        return (1.2 * (0.25 + 0.75 * (((double)dIndex.getDocLength(docID)) / ((double)dIndex.getAvgDocLength())))) + dPosting.getTermFrequency();
    }

    public void setdPosting(DiskPosting dPosting) {
        this.dPosting = dPosting;
    }

    public void testDiskIndex(){
        System.out.println("DiskIndex 744: "+dIndex.getFileNames().get(744));
        System.out.println("DiskIndex 999: "+dIndex.getFileNames().get(999));
    }
}
