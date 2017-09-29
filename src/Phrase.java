import java.util.ArrayList;
import java.util.List;

/*

Class to process a phrase.

 */
public class Phrase {

    public static String getPhrase(QueryTokenStream andReader, String pBegCandidate){
        QueryTokenStream phraseDetector = andReader;   
        String phraseQuery = "", nextCandidate = "";

        if(pBegCandidate.startsWith("\"")){            
            phraseQuery = pBegCandidate;
            
            if(phraseQuery.endsWith("\"")){ //If phrase if only one token long
                return phraseQuery;  
            }

            nextCandidate = phraseDetector.nextToken();
            while(!nextCandidate.endsWith("\"")){ //build phrase
                phraseQuery = phraseQuery + " " + nextCandidate;
                
                if(phraseDetector.hasNextToken()){
                    nextCandidate = phraseDetector.nextToken();
                }
            }     
            phraseQuery = phraseQuery + " " + nextCandidate;
            return phraseQuery;
        }
        else{
            pBegCandidate = phraseDetector.nextToken();
        }
        return phraseQuery;      
    }
    
    public static List<PositionalPosting> phraseQuery(String phraseLiteral, PositionalInvertedIndex posIndex){
        phraseLiteral = phraseLiteral.replaceAll("\"", "");                   
        String[] splitPhrase = phraseLiteral.split(" ");
        List<PositionalPosting> phraseList = new ArrayList<PositionalPosting>();

        for(int j = 0; j < splitPhrase.length - 1; j++){
            if(posIndex.getPostingsList(splitPhrase[j]) != null && 
            posIndex.getPostingsList(splitPhrase[j+1]) != null){
                phraseList = ListMerge.positionalIntersect(posIndex.getPostingsList(splitPhrase[j]),
                posIndex.getPostingsList(splitPhrase[j + 1]), 1);
            }
        }       
        return phraseList;
    }
    
    public static List<PositionalPosting> nearQuery(String nearLiteral, PositionalInvertedIndex posIndex){                 
        String[] splitNear = nearLiteral.split(" ");
        List<PositionalPosting> nearList = new ArrayList<PositionalPosting>();
        int k = Integer.valueOf(splitNear[1].substring(4));
        
        if(posIndex.getPostingsList(splitNear[0]) != null && 
        posIndex.getPostingsList(splitNear[2]) != null){
            nearList = ListMerge.positionalIntersect(posIndex.getPostingsList(splitNear[0]),
            posIndex.getPostingsList(splitNear[2]), k);    
        }
        return nearList;
    }
}
