
import java.io.*;
import java.util.*;

/**
 * Reads tokens one at a time from an input stream for query processing. 
 * Returns tokens with processing: removing all non-alphanumeric characters
 * (except for double quotes) from beginning and end, removing apostrophe 
 * and hyphens, and converting to lowercase. Also use Porter stemmer on
 * non-wildcards.
 */
public class QueryTokenStream implements TokenStream {

    private Scanner mReader;

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
        //Modified 9/17 to ignore double quotes
        next = next.replaceAll("[^[\\W&&[^\"\\*]]+|[\\W&&[^\"\\*]]+$]+", "");
        next = next.replaceAll("'", "").toLowerCase();
        next = next.replaceAll("-", ""); // modified token
        if (!next.contains("*")){ // do not stem wildcard 
            next = PorterStemmer.getStem(next);
        }
        return next.length() > 0 ? next
                : hasNextToken() ? nextToken() : null;

    }

}
