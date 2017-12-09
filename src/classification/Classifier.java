/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package classification;

/**
 *
 * @author Sean
 */
public class Classifier {
    
    public static void main(String[] args){
        RocchioClassifier rc = new RocchioClassifier();
        
        rc.classify();
        
    }
    
}
