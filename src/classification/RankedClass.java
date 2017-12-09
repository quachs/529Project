package classification;

// See README for references

import retrievals.rankedRetrieval.*;
import indexes.diskPart.DiskPosting;

/**
 * 
 * A class that maintains an accumulated document score for 
 * every relevant document found through a ranked query.
 * 
 */
public class RankedClass implements Comparable<RankedClass>{
    
    private double centroid;
    private RocchioClassifier.authors author;
    
    public RankedClass(double centroid, RocchioClassifier.authors author){
        this.centroid = centroid;
        this.author = author;
    }
    
    public void setCentroid(double centroid){
        this.centroid = centroid;
    }
    
    public double getCentroid(){
        return centroid;
    } 
    
    public RocchioClassifier.authors getAuthor(){
        return author;
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
    public int compareTo(RankedClass ri){
        if (ri.centroid > this.centroid){
            return -1;
        }
        if (this.centroid == ri.centroid) {
            return 0;
        }
        else {
            return 1;
        }
                
    }
}