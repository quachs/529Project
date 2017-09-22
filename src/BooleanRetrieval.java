
import java.util.*;


/**
 * Class of generic methods to handle boolean retrieval operations
 *
 */
public final class BooleanRetrieval {
    
    /**
     * Merge the two given lists
     * @param list1
     * @param list2
     * @return 
     */
    public static List<PositionalPosting> intersectList(List<PositionalPosting> list1, 
            List<PositionalPosting> list2){
        List<PositionalPosting> result = new ArrayList<PositionalPosting>();
        int i = 0;
        int j = 0;
        
        while( i < list1.size() && j < list2.size()){
            if(list1.get(i).getDocumentID() == list2.get(j).getDocumentID()){
                result.add(list1.get(i));
                i++;
                j++;
            }
            else if(list1.get(i).getDocumentID() < list2.get(j).getDocumentID()){ // list1 before list2
                i++; 
            }
            else{ // list2 before list1
                j++;
            }
        }
        
        return result;
    }
    
     /**
     * Merge the two given lists
     * @param list1
     * @param list2
     * @return 
     */
    public static List<String> intersectList2(List<String> list1, List<String> list2){
        List<String> result = new ArrayList<String>();
        int i = 0;
        int j = 0;
        
        while( i < list1.size() && j < list2.size()){
            if(list1.get(i).equals(list2.get(j))){
                result.add(list1.get(i));
                i++;
                j++;
            }
            else if(list1.get(i).compareTo(list2.get(j)) < 0){ // list1 before list2
                i++; 
            }
            else{ // list2 before list1
                j++;
            }
        }
        
        return result;
    }
    
     /**
     * OR the two given lists
     * @param list1
     * @param list2
     * @return 
     */
    public static List<PositionalPosting> orList(List<PositionalPosting> list1, List<PositionalPosting> list2){
        List<PositionalPosting> result = new ArrayList<PositionalPosting>();
        int i = 0;
        int j = 0;
        
        while( i < list1.size() && j < list2.size()){
            if(list1.get(i).getDocumentID() == list2.get(j).getDocumentID()){
                result.add(list1.get(i));
                i++;
                j++;
            }
            else if(list1.get(i).getDocumentID() < list2.get(j).getDocumentID()){ // list1 before list2 
                result.add(list1.get(i));
                i++;
            }
            else{ // list2 before list1
                result.add(list2.get(j));
                j++;
            }
        }
        
        // append the longer list to the results
        if( i < list1.size()){
            while( i < list1.size()){
                result.add(list1.get(i));
                i++;
            }  
        }
        else if( j < list2.size()){
            while( j < list2.size()){
                result.add(list2.get(j));
                j++;
            }
        }
        
        return result;
    }
    
    /**
     * OR the two given lists
     * @param <T>
     * @param list1
     * @param list2
     * @return 
     */
    public static List<String> orList2(List<String> list1, List<String> list2){
        List<String> result = new ArrayList<String>();
        int i = 0;
        int j = 0;
        
        while( i < list1.size() && j < list2.size()){
            if(list1.get(i).equals(list2.get(j))){
                result.add(list1.get(i));
                i++;
                j++;
            }
            else if(list1.get(i).compareTo(list2.get(j)) < 0){ // list1 before list2 
                result.add(list1.get(i));
                i++;
            }
            else{ // list2 before list1
                result.add(list2.get(j));
                j++;
            }
        }
        
        // append the longer list to the results
        if( i < list1.size()){
            while( i < list1.size()){
                result.add(list1.get(i));
                i++;
            }  
        }
        else if( j < list2.size()){
            while( j < list2.size()){
                result.add(list2.get(j));
                j++;
            }
        }
        
        return result;
    }
    
}
