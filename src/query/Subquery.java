package query;



import helper.*;
import java.util.ArrayList;
import java.util.List;

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
