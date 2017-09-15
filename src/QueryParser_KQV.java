//Sources:
//https://docs.oracle.com/javase/7/docs/api/java/util/ArrayList.html
//https://docs.oracle.com/javase/7/docs/api/java/lang/String.html#split(java.lang.String)
//https://docs.oracle.com/javase/tutorial/java/javaOO/constructors.html
//https://nlp.stanford.edu/IR-book/pdf/irbookonlinereading.pdf

import java.io.*;
import java.util.*;

class QueryParser_KQV{
    
    QueryParser_KQV(){}
    QueryParser_KQV(PositionalInvertedIndex positionalIndex){
        this.posIndex = positionalIndex;
    }

    //Used in andQuery(), orQuery()
    private PositionalInvertedIndex posIndex;
    private List<List<PositionalPosting>> AndCollection; 
   
    //Splits up an individual query Q_i into its query literals, 
    //splitting on whitespace;  This will have to be modified
    //once phrase query functionality is implemented.
    private String[] collectAndQueries(String queryString){
        
        String[] andSplitter = queryString.split(" ");
        return andSplitter;                
    }
     
    //Splits up an OR query (Q_1...Q_k), and places AND queries 
    //into collection.  Unlike the andQueryParser(), this uses 
    //Professor Neal Terrell's SimpleTokenStream to parse query literals
    private List<String[]> collectOrQueries(String query){
        
        SimpleTokenStream tReader = new SimpleTokenStream(query);
        List<String[]> allQueries = new ArrayList<String[]>();
        
        //Constructs a string representing a complete AND query Q_i;
        //Stops when first OR token ("+") is encountered
        String qString = "";
        while(tReader.hasNextToken()){
            if(!tReader.nextToken().equals("+")){
                qString = qString + tReader.nextToken();
            }
            
            //Once a complete AND query has been formed,
            //add its positional postings list to the 
            //AndCollection of positional postings lists.
            allQueries.add(collectAndQueries(qString));
        }     
        
        return allQueries;
    }
   
    //Implementation of the Intersect algorithm from 
    //"Introduction to Information Retreival, Online Edition" by
    //Christopher D. Manning, Prabhakar Raghavan, and Hinrich Schutze
    //Cambridge University Press, 2009, p. 11, Figure 1.6
    //https://nlp.stanford.edu/IR-book/pdf/irbookonlinereading.pdf
    //
    //[Note: I don't see why we couldn't use this as a source since this 
    //algorithm was the one presented to us during lecture, but it won't
    //hurt to make sure it's okay to use before we actually implement it.
    //For now, we can just keep this and orIntersect() blank.]
    //
    private List<PositionalPosting> andIntersect(List<PositionalPosting> list1, 
        List<PositionalPosting> list2){
       
        //To be implemented
        return null;
    }
   
    //Modification of andIntersect() using OR logic
    private List<PositionalPosting> orIntersect(List<PositionalPosting> list1, 
        List<PositionalPosting> list2){
       
        //To be implemented
        return null;
    }
    
    //Add the positional postings list of an AND query to the 
    //collection of AND query positional postings lists
    private void addAndQuery(String[] andQueryLiterals){
        
        List<PositionalPosting> masterList = new ArrayList<PositionalPosting>();
        masterList = posIndex.getPostingsList(andQueryLiterals[0]);

        //Merge all of the positional postings lists of each
        //query literal of a given AND query into one list.  
        //This final list represents the positional postings list
        //for an entire AND query.
        for(int i = 1; i < andQueryLiterals.length - 1; i++){
            masterList = andIntersect(masterList, 
                posIndex.getPostingsList(andQueryLiterals[i]));
        }

        //Add this AND positional postings list to the 
        //collection of AND positional postings lists
        AndCollection.add(masterList);       
    }
    
    //Run all AND Queries Q_i, store them in a collection of positional 
    //postings lists, then run an OR query that merges all postings lists
    //into one master positional postings list representing the entire query.
    //
    //The argument here is the entire user query split into a collection.
    //See getDocumentList() below for more information.
    private List<PositionalPosting> query(List<String[]> allQueries){
       
        //Add all Q_i positional postings lists to AndCollection
        for(int i = 0; i < allQueries.size(); i++){
            addAndQuery(allQueries.get(i));
        }
        
        //Merge all Q_i positional postings lists
        //in a Master List using OR intersection
        List<PositionalPosting> masterList = AndCollection.get(0);
        for(int i = 1; i < AndCollection.size(); i++){
            masterList = orIntersect(masterList, AndCollection.get(i));
        }

        return masterList;
    }
    
    private List<PositionalPosting> wildcardQuery(String query){
        return null;}
   
    //Takes a string representing a query, returns list of relevant documents
    public List<Integer> getDocumentList(String query){            
    
        if(query.contains("*")){wildcardQuery(query);}
        
        List<Integer> documentList = new ArrayList<Integer>();
        
        //Parse query and store in a collection.
        //
        //An element of the list represents an entire AND query;  
        //together, the entire list constitutes an OR query
        //
        //An element of a String[] array represents 
        //all of the query literals of an AND query 
        List<String[]> allQueries = collectOrQueries(query);          
        
        //Performs the query and returns a positional 
        //postings list that represents its results.
        List<PositionalPosting> masterPostings = query(allQueries);       

        //Constuct a list of document IDs from this final postings list.
        for(int i = 0; i < masterPostings.size(); i++){
           int currentDocID = masterPostings.get(i).getDocumentID();           
           documentList.add(currentDocID);
        }
        
        return documentList;
    }
}