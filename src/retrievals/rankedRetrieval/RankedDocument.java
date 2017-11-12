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
public class RankedDocument implements Comparable<RankedDocument>{
    
    private double accumulatedScore;
    private DiskPosting dPosting;
    private int docID;
    
    public RankedDocument(double accumulatedScore, int docID){
        this.accumulatedScore = accumulatedScore;
        this.docID = docID;
    }
    
    public void setAccumulatedScore(double accumulatedScore){
        this.accumulatedScore = accumulatedScore;
    }
    
    public double getAccumulatedScore(){
        return accumulatedScore;
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
    
    /**
     * Method of comparison that allows documents
     * to be ranked by document score and presented
     * to a user in ranked order.
     * 
     * @param ri
     * @return 
     */
    @Override
    public int compareTo(RankedDocument ri){
        if (ri.accumulatedScore < this.accumulatedScore){
            return -1;
        }
        if (this.accumulatedScore == ri.accumulatedScore) {
            return 0;
        }
        else {
            return 1;
        }
                
    }
}