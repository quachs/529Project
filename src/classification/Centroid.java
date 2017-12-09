/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package classification;

import java.util.*;

/**
 *
 * @author Sean
 */
public class Centroid {
    
    private HashMap<String, Double> centroid = new HashMap<String, Double>();
    
    public Centroid(){}
    
    public Centroid(HashMap<String, Double> centroid){
        this.centroid = centroid;
    }
    
    public void setCentroid(HashMap<String, Double> centroid){
        this.centroid = centroid;
    }
    
    
    public HashMap<String, Double> getCentroid(){
        return centroid;
    }
    
}
