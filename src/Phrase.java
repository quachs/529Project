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
                PorterStemmer onePhraseStemmer = new PorterStemmer();
                phraseQuery = phraseQuery.replaceAll("\"", "");          
                phraseQuery = onePhraseStemmer.getStem(phraseQuery);
                System.out.println(phraseQuery);
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
        System.out.println(phraseQuery);
        return phraseQuery;      
    }
    
    public static List<PositionalPosting> phraseQuery(String phraseLiteral, PositionalInvertedIndex posIndex){
        phraseLiteral = phraseLiteral.replaceAll("\"", "");                   
        String[] splitPhrase = phraseLiteral.split(" ");
        PorterStemmer phraseStemmer = new PorterStemmer();
        List<PositionalPosting> phraseList = new ArrayList<PositionalPosting>();
        
        for (int i = 0; i < splitPhrase.length; i++){
            splitPhrase[i] = phraseStemmer.getStem(splitPhrase[i]);
        }

        phraseList = posIndex.getPostingsList(splitPhrase[0]);
        
        for(int j = 1; j < splitPhrase.length; j++){
            if(posIndex.getPostingsList(splitPhrase[j]) != null) {
                phraseList = ListMerge.positionalIntersect(phraseList,
                posIndex.getPostingsList(splitPhrase[j]), 1);
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
