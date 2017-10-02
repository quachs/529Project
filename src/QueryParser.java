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

class QueryParser{
    
    private PositionalInvertedIndex posIndex;
    private List<List<PositionalPosting>> AndCollection = new ArrayList<List<PositionalPosting>>(); 
    private ListMerge intersector = new ListMerge();
    private KGramIndex kgIndex = new KGramIndex();
    
    QueryParser(){}
    QueryParser(PositionalInvertedIndex posIndex, KGramIndex kgIndex){
        this.posIndex = posIndex;
        this.kgIndex = kgIndex;
    }
    
    /**
     * Splits up an individual query Q_i into its query literals.
     * 
     * @param queryString All query literals of one AND query
     * @return Subquery data structure that stores all query literals
     */
    public Subquery collectAndQueries(String queryString){
             
        Subquery andQueries = new Subquery();
        QueryTokenStream andReader = new QueryTokenStream(queryString);
                
        while(andReader.hasNextToken()){
            String pBegCandidate = andReader.nextToken();
            System.out.println("candidate: " + pBegCandidate);
            
            //Phrase candidate must start with a left double quote
            if (pBegCandidate.startsWith("\"")){
                String phrase = Phrase.getPhrase(andReader, pBegCandidate);
                andQueries.addLiteral(phrase);        
            } else{
                if (andReader.hasNextToken()){
                    String nearCandidate = andReader.nextToken();
                    
                    /* Search for "near" keyword.  If found,
                    rebuild near literal in the form [term1] near[k] [term2] */
                    if(nearCandidate.contains("near")){
                        String lNearOp = pBegCandidate;
                        String rNearOp = andReader.nextToken();
                        String nearLiteral = lNearOp + " " + nearCandidate + " " + rNearOp;
                        andQueries.addLiteral(nearLiteral);
                    } else{
                        /* Add original token, pBegCandidate
                        //Since reader was advanced past original token in order to
                        //look for "near," nearCandidate token must be added here */
                        andQueries.addLiteral(pBegCandidate);
                        andQueries.addLiteral(nearCandidate); 
                    }
                } else{
                    System.out.println("Adding literal: " + pBegCandidate);
                    andQueries.addLiteral(pBegCandidate);
                }
            }
        }
        return andQueries;                
    }
     
    /**
     * Splits up query into subqueries Q_1...Q_k, placing AND queries into collection.
     * 
     * @param query All query literals of a user query.
     * @return A list of all Subquery structures; essentially,
     * a list of Positional Posting lists
     */
    public List<Subquery> collectOrQueries(String query){
        
        Scanner tReader = new Scanner(query);
        List<Subquery> allQueries = new ArrayList<Subquery>();   
        String qString = "";
        
        //Constructs a complete AND query Q_i (stops at OR token ("+"));
        while(tReader.hasNext()){
            String plusCandidate = tReader.next();
            
            if(!plusCandidate.equals("+")){
                qString = qString + " " + plusCandidate;
                
                if(!tReader.hasNext()){
                    allQueries.add(collectAndQueries(qString));    
                }
            } else{
                 allQueries.add(collectAndQueries(qString)); 
                 qString = ""; //clears string for later use          
            }
        }     
        return allQueries;
    }
    
    /**
     * Takes a string representing a query, returns list of relevant documents
     * 
     * @param query A string representing a user query.
     * @return A list of Positional Postings representing the
     * results of a user query.
     */
    public List<Integer> getDocumentList(String query){            
    
        //Parse query, store in a collection, perform the query, return a final postings list.
        List<Subquery> allQueries = collectOrQueries(query);      
        List<PositionalPosting> masterPostings = QueryProcessor.orQuery(allQueries, posIndex, kgIndex);       
        List<Integer> documentList = new ArrayList<Integer>();
        
        //Constuct a list of document IDs from this final postings list.
        if (masterPostings.size() > 0){
            for(int i = 0; i < masterPostings.size(); i++){
                int currentDocID = masterPostings.get(i).getDocumentID();    
                documentList.add(currentDocID);
            }
        }
        return documentList;
    }
}
