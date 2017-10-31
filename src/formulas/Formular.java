package formulas;

import indexes.diskPart.DiskInvertedIndex;
import indexes.diskPart.DiskPosting;

public abstract class Formular {
    public abstract double calcWQT(DiskPosting[] tDocIDs);
    public abstract double calcWDT(DiskPosting dPosting);
    public abstract double getL_D(DiskInvertedIndex dIndex, int docID);
}
