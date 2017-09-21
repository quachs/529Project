
import java.util.*;


/**
 * Class of generic methods to handle boolean retrieval operations
 *
 */
public final class BooleanRetrieval {
    
    /**
     * Get the intersection of two ordered lists
     * @param <T>
     * @param list1 list of comparable elements
     * @param list2 list of comparable elements
     * @return resulting list of intersecting list1 and list2
     */
    public static <T extends Comparable> List<T> intersectList(List<T> list1, List<T> list2){
        List<T> result = new ArrayList<T>();
        int i = 0;
        int j = 0;
        
        while( i < list1.size() && j < list2.size()){
            if(list1.get(i).compareTo(list2.get(j)) == 0){
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
     * Get the union of two ordered lists
     * @param <T>
     * @param list1 list of comparable elements
     * @param list2 list of comparable elements
     * @return resulting list of ORing list1 and list2 
     */
    public static <T extends Comparable> List<T> orList(List<T> list1, List<T> list2){
        List<T> result = new ArrayList<T>();
        int i = 0;
        int j = 0;
        
        while( i < list1.size() && j < list2.size()){
            if(list1.get(i).compareTo(list2.get(j)) == 0){
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
