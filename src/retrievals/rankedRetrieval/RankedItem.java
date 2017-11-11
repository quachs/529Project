package retrievals.rankedRetrieval;

import indexes.diskPart.DiskPosting;
import java.lang.*;

/**
 * 
 * A class that maintains an accumulated document score for 
 * every relevant document found through a ranked query.
 * 
 * @author Sean
 */
//https://docs.oracle.com/javase/7/docs/api/java/lang/Comparable.html
public class RankedItem implements Comparable<RankedItem>{
    
    private double A_d;
    private DiskPosting dPosting;
    private int docID;
    
    public RankedItem(double A_d, int docID){
        this.A_d = A_d;
        this.docID = docID;
    }
    
    public void setA_d(double A_d){
        this.A_d = A_d;
    }
    
    public double getA_d(){
        return A_d;
    } 
    
    public void setDPosting(DiskPosting dPosting){
        this.dPosting = dPosting;
    }
    
    public DiskPosting getPosting(){
        return dPosting;
    }
    
    public int getDocID(){
        return docID;
    }
    
    @Override
    public int compareTo(RankedItem ri){
        if (ri.A_d < this.A_d){
            return -1;
        }
        if (this.A_d == ri.A_d) {
            return 0;
        }
        else {
            return 1;
        }
                
    }
}