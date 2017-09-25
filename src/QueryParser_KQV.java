//Class includes a call to implementations of the Intersect algorithm from 
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
    private BooleanRetrieval intersector = new BooleanRetrieval();
    private KGramIndex kgIndex = new KGramIndex();
    
    QueryParser_KQV(){}
    QueryParser_KQV(PositionalInvertedIndex posIndex, KGramIndex kgIndex){
        this.posIndex = posIndex;
        this.kgIndex = kgIndex;
    }
      
    private String getPhrase(SimpleTokenStream andReader, String pBegCandidate){
        SimpleTokenStream phraseDetector = andReader;   
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
      
    //Splits up an individual query Q_i into its query literals.
    //Uses professor Neal Terrell's SimpleTokenStream to parse query literals
    public Subquery collectAndQueries(String queryString){
             
        Subquery andQueries = new Subquery();
        SimpleTokenStream andReader = new SimpleTokenStream(queryString);
                
        while(andReader.hasNextToken()){
            String pBegCandidate = andReader.nextToken();
            
            if (pBegCandidate.startsWith("\"")){ //If starts with dbl quotes
                String phrase = getPhrase(andReader, pBegCandidate);
                andQueries.addLiteral(phrase);        
            }   
            else{
                if (andReader.hasNextToken()){
                    String nearCandidate = andReader.nextToken();
                    
                    //https://docs.oracle.com/javase/tutorial/java/data/converting.html
                    if(nearCandidate.contains("near")){
                        int k = Integer.valueOf(nearCandidate.substring(4));
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
        
        SimpleTokenStream tReader = new SimpleTokenStream(query);
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
     
    //Add the positional postings list of an AND query to the 
    //collection of AND query positional postings lists
    private void addAndQuery(Subquery andQueryLiterals){
        
        List<PositionalPosting> masterList = new ArrayList<PositionalPosting>();
        String preLiteral = andQueryLiterals.getLiterals().get(0);
        List<PositionalPosting> intermediateList = new ArrayList<PositionalPosting>();
        
        if(preLiteral.contains("\"")){       
            preLiteral = preLiteral.replaceAll("\"", "");         
            String[] splitPhrase = preLiteral.split(" ");
            
            for(int j = 0; j < splitPhrase.length - 1; j++){
                masterList = QueryProcessor.positionalIntersect(posIndex.getPostingsList(splitPhrase[j]),
                posIndex.getPostingsList(splitPhrase[j + 1]), 1);
            }
        }
        else if (preLiteral.contains("*")){
            masterList = QueryProcessor.wildcardQuery(preLiteral, posIndex, kgIndex);  
        }
        else{
            masterList = posIndex.getPostingsList(preLiteral);
        }
                        
        //Merge all of the postings lists of each. 
        //query literal of a given AND query into one master postings list. 
        if (andQueryLiterals.getSize() > 1){
            
            for(int i = 1; i < andQueryLiterals.getSize(); i++){
                String currentLiteral = andQueryLiterals.getLiterals().get(i);

                if(currentLiteral.contains("\"")){        
                    currentLiteral = currentLiteral.replaceAll("\"", "");                   
                    String[] splitPhrase = currentLiteral.split(" ");
                                   
                    for(int j = 0; j < splitPhrase.length - 1; j++){
                        intermediateList = QueryProcessor.positionalIntersect(posIndex.getPostingsList(splitPhrase[j]),
                        posIndex.getPostingsList(splitPhrase[j + 1]), 1);
                    }       
                    masterList = BooleanRetrieval.intersectList(masterList, intermediateList);
                }
                else if (currentLiteral.contains("*")){
                    intermediateList = QueryProcessor.wildcardQuery(currentLiteral, posIndex, kgIndex);
                    masterList = BooleanRetrieval.intersectList(masterList, intermediateList);
                }
                else{
                    masterList = BooleanRetrieval.intersectList(masterList, posIndex.getPostingsList(currentLiteral));
                }
            }
        }
        //Add this AND postings list to the collection of AND postings lists
        AndCollection.add(masterList);       
    }
    
    //Run all AND Queries Q_i, store results in collection of positional 
    //postings lists, then run an OR query that merges all postings lists
    //into one master positional postings list representing the entire query.
    private List<PositionalPosting> orQuery(List<Subquery> allQueries){
       
        //Add all Q_i positional postings lists to AndCollection
        for(int i = 0; i < allQueries.size(); i++){
            addAndQuery(allQueries.get(i));
        }
        
        //Merge all Q_i postings list into Master List using OR intersection
        List<PositionalPosting> masterList = AndCollection.get(0);
        
        if(AndCollection.size() > 1){
            for(int i = 1; i < AndCollection.size(); i++){
                masterList = BooleanRetrieval.orList(masterList, AndCollection.get(i));
            }
        }
        return masterList;
    }
    
    //Takes a string representing a query, returns list of relevant documents
    public List<Integer> getDocumentList(String query){            
    
        //Parse query, store in a collection, perform the query, return a final postings list.
        List<Subquery> allQueries = collectOrQueries(query);      
        List<PositionalPosting> masterPostings = orQuery(allQueries);       
        List<Integer> documentList = new ArrayList<Integer>();
        
        //Constuct a list of document IDs from this final postings list.
        for(int i = 0; i < masterPostings.size(); i++){
           int currentDocID = masterPostings.get(i).getDocumentID();    
           documentList.add(currentDocID);
        }   
        return documentList;
    }
}