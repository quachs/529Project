
import java.io.*;
import java.util.*;
import org.tartarus.snowball.*; // http://snowball.tartarus.org/download.html

/**
 * Reads tokens one at a time from an input stream. Returns tokens with minimal
 * processing: removing all non-alphanumeric characters, and converting to
 * lowercase.
 */
public class SimpleTokenStream implements TokenStream {
    private Scanner mReader;
    private Queue<String> tokenQueue;

    /**
     * Constructs a SimpleTokenStream to read from the specified file.
     */
    public SimpleTokenStream(File fileToOpen) throws FileNotFoundException {
        mReader = new Scanner(new FileReader(fileToOpen));
        tokenQueue = new LinkedList<String>();
    }

    /**
     * Constructs a SimpleTokenStream to read from a String of text.
     */
    public SimpleTokenStream(String text) {
        mReader = new Scanner(text);
    }

    /**
     * Returns true if the stream has tokens remaining.
     */
    @Override
    public boolean hasNextToken() {
        return mReader.hasNext() || !tokenQueue.isEmpty();
    }

    /**
     * Returns the next token from the stream, or null if there is no token
     * available.
     */
    @Override
    public String nextToken() {
        if (!hasNextToken()) {
            return null;
        }
        
        if(!tokenQueue.isEmpty()){
            return tokenQueue.poll();
        }
        
        String next = mReader.next();
        
        // remove non-alphanumeric characters from beginning and end
        // https://stackoverflow.com/questions/24967089/java-remove-all-non-alphanumeric-character-from-beginning-and-end-of-string
        next = next.replaceAll("^\\W+|\\W+$", ""); 
        next = next.replaceAll("'", "");
        if(next.contains("-")){ // split hyphenated token
            String[] tokens = next.split("-");
            for(String t : tokens){
                t = t.toLowerCase();
                t = getStem(t);
                if(t.length() > 0){
                    tokenQueue.add(t);
                }    
            }
            next = next.replaceAll("-",""); // modified token
        }
        next = next.toLowerCase();
        next = getStem(next);
        return next.length() > 0 ? next
                : hasNextToken() ? nextToken() : null;
        
        
        
        /*
        String next = mReader.next().replaceAll("\\W", "").toLowerCase();
        return next.length() > 0 ? next
                : hasNextToken() ? nextToken() : null; */

    }
    
    private String getStem(String token){
        try {            
            Class stemClass = Class.forName("org.tartarus.snowball.ext.englishStemmer");
            SnowballStemmer stemmer = (SnowballStemmer) stemClass.newInstance();
            stemmer.setCurrent(token);
            stemmer.stem();
            token = stemmer.getCurrent();            
        } catch (ClassNotFoundException | 
                InstantiationException | 
                IllegalAccessException ex) {}
        return token;       
    }
}
