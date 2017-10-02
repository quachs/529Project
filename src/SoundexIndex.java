//Source:
//Implementation of the soundex algorithm as described in 
//"Introduction to Information Retrieval, Online Edition" by
//Christopher D. Manning, Prabhakar Raghavan, and Hinrich Schutze
//Cambridge University Press, 2009, p. 64
//https://nlp.stanford.edu/IR-book/pdf/irbookonlinereading.pdf
//https://docs.oracle.com/javase/7/docs/api/java/lang/String.html#replaceAll(java.lang.String,%20java.lang.String)
import java.util.ArrayList;
import java.util.List;

/**
 * Class for a soundex index. The keys are a phonetic hashing of a term and
 * the postings are the docIDs that contain the term.
 * 
 */
public class SoundexIndex extends Index<Integer> {

    public SoundexIndex() {
        super();
    }
    
    public void addToSoundex(String term, int docID){
        String soundexHash = reduceToSoundex(term);
        if(mIndex.containsKey(soundexHash)){
            // add docID to existing soundex hash key
            mIndex.get(soundexHash).add(docID);
        }
        else{
            // add the soundex hash and docID to the index
            mIndex.put(soundexHash, new ArrayList<Integer>());
            mIndex.get(soundexHash).add(docID);
        }
    }
    
    /**
     * Soundex algorithm to generate a phonetic hashing from the given term
     * @param term
     * @return soundex hash
     */
    public String reduceToSoundex(String term){
        String soundexForm = term;
        String firstLetter = soundexForm.substring(0,1); //left alone
        String remainingString = soundexForm.substring(1); //to be processed
        
        //Number replacement step
        //https://docs.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html#sum
        remainingString = remainingString.replaceAll("[AaEeIiOoUuHhWwYy]", "0");
        remainingString = remainingString.replaceAll("[BbFfPpVv]", "1");
        remainingString = remainingString.replaceAll("[CcGgJjKkQqSsXxZz]", "2");
        remainingString = remainingString.replaceAll("[DdTt]", "3");
        remainingString = remainingString.replaceAll("[Ll]", "4");
        remainingString = remainingString.replaceAll("[MmNn]", "5");
        remainingString = remainingString.replaceAll("[Rr]", "6");
                
        //Consecutive 1-6 Removal
        remainingString = remainingString.replaceAll("1{2}", "1");
        remainingString = remainingString.replaceAll("2{2}", "2");
        remainingString = remainingString.replaceAll("3{2}", "3");
        remainingString = remainingString.replaceAll("4{2}", "4");
        remainingString = remainingString.replaceAll("5{2}", "5");
        remainingString = remainingString.replaceAll("6{2}", "6");
                
        //Remove all Zeroes
        remainingString = remainingString.replaceAll("0", "");
        
        //Concatenation Step
        soundexForm = firstLetter + remainingString;
        
        //Insert trailing zeroes (Manning p. 64)
        if (soundexForm.length() < 4){
            int padAmount = 4 - soundexForm.length();           
            switch (padAmount){
                case 1: 
                        soundexForm = soundexForm + "0"; 
                        break;
                case 2: 
                        soundexForm = soundexForm + "00"; 
                        break;
                case 3: 
                        soundexForm = soundexForm + "000";
            }
        }
        return soundexForm.substring(0, 4);
    }
    
    @Override
    public List<Integer> getPostingsList(String term){
        return mIndex.get(reduceToSoundex(term));
    }
    
    
}
