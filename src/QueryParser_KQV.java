//Sources:
//https://docs.oracle.com/javase/7/docs/api/java/util/ArrayList.html
//https://docs.oracle.com/javase/7/docs/api/java/lang/String.html#split(java.lang.String)
//https://docs.oracle.com/javase/tutorial/java/javaOO/constructors.html
//https://docs.oracle.com/javase/7/docs/api/java/lang/String.html#startsWith(java.lang.String)
//https://docs.oracle.com/javase/7/docs/api/java/lang/String.html#endsWith(java.lang.String)

//Class includes a call to implementations of the Intersect algorithm from 
//"Introduction to Information Retrieval, Online Edition" by
//Christopher D. Manning, Prabhakar Raghavan, and Hinrich Schutze
//Cambridge University Press, 2009, p. 11, Figure 1.6
//https://nlp.stanford.edu/IR-book/pdf/irbookonlinereading.pdf

import java.io.*;
import java.util.*;

class QueryParser_KQV{
    
    private PositionalInvertedIndex posIndex; //Used in andQuery(), orQuery()
    private List<List<PositionalPosting>> AndCollection; 
    BooleanRetrieval intersector = new BooleanRetrieval();
    
    QueryParser_KQV(){}
    QueryParser_KQV(PositionalInvertedIndex posIndex){
        this.posIndex = posIndex;
    }
      
    private String getPhrase(SimpleTokenStream andReader, String pBegCandidate){
        SimpleTokenStream phraseDetector = andReader;   
        String phraseQuery = "", nextCandidate = "";

        if(pBegCandidate.startsWith("\"")){
            
            phraseQuery = pBegCandidate;
            
            //If phrase if only one token long
            if(phraseQuery.endsWith("\"")){
                return phraseQuery;
            }

            nextCandidate = phraseDetector.nextToken();

            //Add all phrase tokens to final phrase
            while(!nextCandidate.endsWith("\"")){
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
                andQueries.addLiteral(pBegCandidate);   
            }
        }   
        return andQueries;                
    }
     
    //Splits up query into subqueries Q_1...Q_k, placing AND queries into collection.
    public List<Subquery> collectOrQueries(String query){
        
        SimpleTokenStream tReader = new SimpleTokenStream(query);
        List<Subquery> allQueries = new ArrayList<Subquery>();   
        
        //Constructs a string representing a complete AND query Q_i;
        //Stops when first OR token ("+") is encountered
        String qString = "";
        while(tReader.hasNextToken()){
            String plusCandidate = tReader.nextToken();
            if(!plusCandidate.equals("+")){
                qString = qString + " " + plusCandidate;
                
                if(!tReader.hasNextToken()){
                    //Once a complete AND query has been formed, add its 
                    //postings list to the AndCollection of postings lists.
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
        BooleanRetrieval intersector = new BooleanRetrieval();
        QueryProcessor qProcessor = new QueryProcessor();
        SimpleTokenStream preStemmer = new SimpleTokenStream(andQueryLiterals.getLiterals().get(0));
                
        //Stems the first token
        String preMasterList = preStemmer.nextToken();
        masterList = posIndex.getPostingsList(preMasterList);
            
        //Merge all of the postings lists of each. 
        //query literal of a given AND query into one master postings list. 
        for(int i = 1; i < andQueryLiterals.getSize() - 1; i++){
            SimpleTokenStream stemmer = new SimpleTokenStream(andQueryLiterals.getLiterals().get(i));
            
            if(stemmer.hasNextToken()){
                String normalizedLiteral = stemmer.nextToken();
                
                if(normalizedLiteral.contains("\"")){
                    masterList = qProcessor.positionalIntersect(masterList,
                        posIndex.getPostingsList(normalizedLiteral), 0);
                }
                
                masterList = intersector.intersectList(masterList, 
                    posIndex.getPostingsList(normalizedLiteral));
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
        for(int i = 1; i < AndCollection.size(); i++){
            masterList = intersector.orList(masterList, AndCollection.get(i));
        }
        return masterList;
    }
    
    //Takes a string representing a query, returns list of relevant documents
    public List<Integer> getDocumentList(String query){            
    
        List<Integer> documentList = new ArrayList<Integer>();
        
        //Parse query and store in a collection, then
        //performs the query and returns a master postings list.
        List<Subquery> allQueries = collectOrQueries(query);          
        List<PositionalPosting> masterPostings = orQuery(allQueries);       

        //Constuct a list of document IDs from this final postings list.
        for(int i = 0; i < masterPostings.size(); i++){
           int currentDocID = masterPostings.get(i).getDocumentID();           
           documentList.add(currentDocID);
        }   
        return documentList;
    }
}