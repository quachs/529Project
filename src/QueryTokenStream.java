
import java.io.*;
import java.util.*;

/**
 * Reads tokens one at a time from an input stream. Returns tokens with minimal
 * processing: removing all non-alphanumeric characters, and converting to
 * lowercase.
 */
public class QueryTokenStream implements TokenStream {

    private Scanner mReader;
    private Queue<String> tokenQueue;

    /**
     * Constructs a SimpleTokenStream to read from the specified file.
     */
    public QueryTokenStream(File fileToOpen) throws FileNotFoundException {
        mReader = new Scanner(new FileReader(fileToOpen));
    }

    /**
     * Constructs a SimpleTokenStream to read from a String of text.
     */
    public QueryTokenStream(String text) {
        mReader = new Scanner(text);
    }

    /**
     * Returns true if the stream has tokens remaining.
     */
    @Override
    public boolean hasNextToken() {
        return mReader.hasNext();
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

        String next = mReader.next();

        // remove non-alphanumeric characters from beginning and end
        // https://stackoverflow.com/questions/24967089/java-remove-all-non-alphanumeric-character-from-beginning-and-end-of-string
        
        //Modified 9/17 to ignore double quotes
        next = next.replaceAll("[^[\\W&&[^\"\\*]]+|[\\W&&[^\"\\*]]+$]+", "");
        next = next.replaceAll("'", "");
        if (next.contains("-")) { // split hyphenated token
            String[] tokens = next.split("-");
            for (String t : tokens) {
                t = t.toLowerCase();
                if (!t.contains("*")){
                    t = PorterStemmer.getStem(t);
                }
            }
            next = next.replaceAll("-", ""); // modified token
        }
        next = next.toLowerCase();
        if (!next.contains("*")){
            next = PorterStemmer.getStem(next);
        }
        return next.length() > 0 ? next
                : hasNextToken() ? nextToken() : null;

    }

}
