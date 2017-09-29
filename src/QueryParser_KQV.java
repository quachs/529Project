//Class includes a call to implementations of the ListMerge algorithm from 
//"Introduction to Information Retrieval, Online Edition" by
//Christopher D. Manning, Prabhakar Raghavan, and Hinrich Schutze
//Cambridge University Press, 2009, p. 11, Figure 1.6
//https://nlp.stanford.edu/IR-book/pdf/irbookonlinereading.pdf

//Sources:
//https://docs.oracle.com/javase/7/docs/api/java/util/ArrayList.html
//https://docs.oracle.com/javase/7/docs/api/java/lang/String.html#split(java.lang.String)
//https://docs.oracle.com/javase/tutorial/java/javaOO/constructors.html
//https://docs.oracle.com/javase/7/docs/api/java/lang/String.html#startsWith(java.lang.String)
//https://docs.oracle.com/javase/7/docs/api/java/lang/String.html#endsWith(java.lang.String)

import java.io.*;
import java.util.*;

class QueryParser_KQV{
    
    private PositionalInvertedIndex posIndex; //Used in andQuery(), orQuery()
    private List<List<PositionalPosting>> AndCollection = new ArrayList<List<PositionalPosting>>(); 
    private ListMerge intersector = new ListMerge();
    private KGramIndex kgIndex = new KGramIndex();
    
    QueryParser_KQV(){}
    QueryParser_KQV(PositionalInvertedIndex posIndex, KGramIndex kgIndex){
        this.posIndex = posIndex;
        this.kgIndex = kgIndex;
    }
            
    //Splits up an individual query Q_i into its query literals.
    //Uses professor Neal Terrell's SimpleTokenStream to parse query literals
    public Subquery collectAndQueries(String queryString){
             
        Subquery andQueries = new Subquery();
        QueryTokenStream andReader = new QueryTokenStream(queryString);
                
        while(andReader.hasNextToken()){
            String pBegCandidate = andReader.nextToken();
            
            if (pBegCandidate.startsWith("\"")){ //If starts with dbl quotes
                String phrase = Phrase.getPhrase(andReader, pBegCandidate);
                andQueries.addLiteral(phrase);        
            }   
            else{
                if (andReader.hasNextToken()){
                    String nearCandidate = andReader.nextToken();
                    
                    //https://docs.oracle.com/javase/tutorial/java/data/converting.html
                    if(nearCandidate.contains("near")){
                        String lNearOp = pBegCandidate;
                        String rNearOp = andReader.nextToken();
                        String nearLiteral = lNearOp + " " + nearCandidate + " " + rNearOp;
                        andQueries.addLiteral(nearLiteral);
                    }
                    else{
                        andQueries.addLiteral(pBegCandidate);
                        andQueries.addLiteral(nearCandidate);
                    }
                }
                else{
                    andQueries.addLiteral(pBegCandidate);
                }
            }
        }
        return andQueries;                
    }
     
    //Splits up query into subqueries Q_1...Q_k, placing AND queries into collection.
    public List<Subquery> collectOrQueries(String query){
        
        QueryTokenStream tReader = new QueryTokenStream(query);
        List<Subquery> allQueries = new ArrayList<Subquery>();   
        
        //Constructs a complete AND query Q_i (stops at OR token ("+"));
        String qString = "";
        while(tReader.hasNextToken()){
            String plusCandidate = tReader.nextToken();
            
            if(!plusCandidate.equals("+")){
                qString = qString + " " + plusCandidate;
                
                if(!tReader.hasNextToken()){
                    allQueries.add(collectAndQueries(qString));    
                }
            }
            else{
                 allQueries.add(collectAndQueries(qString)); 
                 qString = ""; //clears string for later use          
            }
        }     
        return allQueries;
    }
      
    //Takes a string representing a query, returns list of relevant documents
    public List<Integer> getDocumentList(String query){            
    
        //Parse query, store in a collection, perform the query, return a final postings list.
        List<Subquery> allQueries = collectOrQueries(query);      
        List<PositionalPosting> masterPostings = QueryProcessor.orQuery(allQueries, posIndex, kgIndex);       
        List<Integer> documentList = new ArrayList<Integer>();
        
        //Constuct a list of document IDs from this final postings list.
        for(int i = 0; i < masterPostings.size(); i++){
           int currentDocID = masterPostings.get(i).getDocumentID();    
           documentList.add(currentDocID);
        }   
        return documentList;
    }
}