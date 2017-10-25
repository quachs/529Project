
import java.lang.*;

/**
 *
 * @author Sean
 */
//https://docs.oracle.com/javase/7/docs/api/java/lang/Comparable.html
public class RankedItem implements Comparable<RankedItem>{
    
    private double A_d;
    private DiskPosting dPosting;
    
    public RankedItem(double A_d, DiskPosting dPosting){
        this.A_d = A_d;
        this.dPosting = dPosting;
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
        
        //Sylvia's suggestion; there may be rounding errors,
        //however, so she advises that we don't use this, after all.
        //return (int)(this.A_d - ri.A_d);
        
    }
}
