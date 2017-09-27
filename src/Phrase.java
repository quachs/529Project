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
                phraseList = ListMerge.positionalIntersect(posIndex.getPostingsList(splitPhrase[j]),
                posIndex.getPostingsList(splitPhrase[j + 1]), 1);
            }       
        return phraseList;
    }
}
