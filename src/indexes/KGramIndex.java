package Indexes;


import Indexes.Index;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Class for a K-Gram index. The keys are the 1-, 2-, and 3-grams for a
 * vocabulary type and the postings are the corresponding types.
 *
 */
public class KGramIndex extends Index<String> implements Serializable{

    public KGramIndex() {
        super();
    }

    /**
     * Generate the k-grams of the given vocabulary type and call a helper
     * method to insert to the index
     *
     * @param type vocabulary type from the corpus
     */
    public void addType(String type) {
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
    }

    /**
     * Insert the k-gram and its type to the index
     *
     * @param kgram from the vocabulary type
     * @param type vocabulary type from the corpus
     */
    private void addType(String kgram, String type) {
        // The term exists in the index. Add the vocab type to the
        // corresponding k-gram if it doesn't already exist.
        if (mIndex.containsKey(kgram)) {
            if (!mIndex.get(kgram).get(mIndex.get(kgram).size() - 1).equals(type)) {
                mIndex.get(kgram).add(type);
            }

        } else {
            // Add a new posting to the index
            List<String> typeList = new ArrayList<String>();
            typeList.add(type);
            mIndex.put(kgram, typeList);
        }
    }

}
