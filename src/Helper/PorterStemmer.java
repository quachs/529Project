// See README for references
import org.tartarus.snowball.ext.englishStemmer;


/**
 * Class for a Porter Stemmer in English
 * 
 */
public final class PorterStemmer {
    private final static englishStemmer STEMMER = new englishStemmer();
    
    /**
     * Stem the given token string
     * @param token
     * @return stem of the token
     */
    public static String getStem(String token){
        STEMMER.setCurrent(token);
        if(STEMMER.stem()){
            return STEMMER.getCurrent();
        }
        return null;
    }
}
