
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


/**
 * Class that generates the 1-, 2-, and 3-grams for a type and
 * maps the grams to their types
 */
public class KGramIndex {
    
    private HashMap<String, List<String>> mIndex;
    
    public KGramIndex() {
        mIndex = new HashMap<String, List<String>>();
    }
    
    /**
     * Generate the k-grams of the given vocabulary type and call a
     * helper method insert them to the index
     * @param type 
     */
    public void addType(String type) {
        if (type.length() > 1) {
            String kGramType = "$" + type + "$";
            for (int i = 0; i < type.length(); i++) {
                addType(Character.toString(type.charAt(i)), type); // 1-grams
            }
            for (int i = 0; i + 2 <= kGramType.length(); i++) {
                addType(kGramType.substring(i, i + 2), type); // 2-grams
            }
            for (int i = 0; i + 3 <= kGramType.length(); i++) {
                addType(kGramType.substring(i, i + 3), type); // 3-grams
            }
        } else if (type.length() == 1) {
            addType(type, type);
            addType("$" + type, type);
            addType(type + "$", type);
        }
    }
    
    /**
     * Insert the k-gram and its type to the index
     * @param kgram
     * @param type 
     */
    private void addType(String kgram, String type) {
        if (mIndex.containsKey(kgram)) {
            mIndex.get(kgram).add(type);
        } else {
            List<String> typeList = new ArrayList<String>();
            typeList.add(type);
            mIndex.put(kgram, typeList);
        }
    }
    
    /**
     * Retrieve the list of types that contain a given k-gram
     * @param kgram
     * @return
     */
    public List<String> getTypes(String kgram){
        return mIndex.get(kgram);
    }
    
}
