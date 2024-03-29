package query;

import java.util.ArrayList;
import java.util.List;

/**
 * A class that maintains all of an AND query literals
 * in a list.  Additionally, this class provides methods
 * to add literals to the list and retrieve all literals.
 * 
 */
public class Subquery {
    private List<String> literals;
    
    public Subquery(){
        literals = new ArrayList<String>();
    }
    
    public List<String> getLiterals(){
        return literals;
    }
    
    public void addLiteral(String posLiteral){
        literals.add(posLiteral);
    }
    
    public int getSize(){
        return literals.size();
    }
    
}
