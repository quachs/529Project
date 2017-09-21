//Source:
//Implementation of the soundex algorithm as described in 
//"Introduction to Information Retrieval, Online Edition" by
//Christopher D. Manning, Prabhakar Raghavan, and Hinrich Schutze
//Cambridge University Press, 2009, p. 64
//https://nlp.stanford.edu/IR-book/pdf/irbookonlinereading.pdf

//https://docs.oracle.com/javase/7/docs/api/java/lang/String.html#replaceAll(java.lang.String,%20java.lang.String)

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

class Soundex_KQV{

    private String term;
    private HashMap<String, List<String>> soundex 
            = new HashMap<String, List<String>>();
    
    Soundex_KQV(){}
    Soundex_KQV(String term){this.term = term;}
    
    private String reduceToSoundex(String term){
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
    
    public void addToSoundex(String soundexTerm){
        List<String> updatedSoundexValues = soundex.get(soundexTerm);
        updatedSoundexValues.add(reduceToSoundex(soundexTerm));
        soundex.put(reduceToSoundex(soundexTerm), updatedSoundexValues);
    }
}