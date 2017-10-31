package indexes;


import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 *
 * Abstract superclass for an inverted index
 * @param <T>
 */
public abstract class Index<T> implements java.io.Serializable {

    protected HashMap<String, List<T>> mIndex;

    public Index() {
        mIndex = new HashMap<String, List<T>>();
    }

    public int getTermCount() {
        return mIndex.size();
    }

    public List<T> getPostingsList(String key) {
        return mIndex.get(key);
    }
    
    public String[] getDictionary() {
        String[] keys = new String[this.getTermCount()];
        keys = mIndex.keySet().toArray(keys);
        Arrays.sort(keys);
        return keys;
    }

}
