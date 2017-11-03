package formulas;

import indexes.diskPart.DiskInvertedIndex;
import indexes.diskPart.DiskPosting;
import java.util.List;

public abstract class Formular {

    DiskInvertedIndex dIndex;

    public Formular(DiskInvertedIndex dIndex) {
        this.dIndex = dIndex;
    }

    public abstract double calcWQT(List<DiskPosting> tDocIDs);

    public abstract double calcWDT(DiskPosting dPosting);

    public abstract double getL_D(int docID);
}
