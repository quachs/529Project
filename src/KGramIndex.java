
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;


/**
 * Class that generates the 1-, 2-, and 3-grams for a type and
 * inserts them into an inverted index structure
 * 
 */
public class KGramIndex {
    
    private HashMap<String, List<String>> mIndex;
    
    public KGramIndex() {
        mIndex = new HashMap<String, List<String>>();
    }
    
    /**
     * Generate the k-grams of the given vocabulary type and call a
     * helper method to insert to the index
     * @param type vocabulary type from the corpus
     */
    public void addType(String type) {
        if (type.length() > 1) {
            String modifiedType = "$" + type + "$";
            for (int i = 0; i < type.length(); i++) {
                addType(Character.toString(type.charAt(i)), type); // 1-grams
            }
            for (int i = 0; i + 2 <= modifiedType.length(); i++) {
                addType(modifiedType.substring(i, i + 2), type); // 2-grams
            }
            for (int i = 0; i + 3 <= modifiedType.length(); i++) {
                addType(modifiedType.substring(i, i + 3), type); // 3-grams
            }
        } else if (type.length() == 1) {
            addType(type, type);
            addType("$" + type, type);
            addType(type + "$", type);
        }
    }
    
    /**
     * Insert the k-gram and its type to the index
     * @param kgram from the vocabulary type
     * @param type vocabulary type from the corpus
     */
    private void addType(String kgram, String type) {
        // The term exists in the index. Add the vocab type to the
        // corresponding kgram (the types are unique and inserted in order)
        if (mIndex.containsKey(kgram)) {
            mIndex.get(kgram).add(type);
        } else {
            // Add a new posting to the index
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
    
    /**
     * Get an array of the k-grams
     * @return 
     */
    public String[] getDictionary(){
        String[] kgrams = new String[mIndex.size()];
        kgrams = mIndex.keySet().toArray(kgrams);
        Arrays.sort(kgrams);
        return kgrams;
    }
}
