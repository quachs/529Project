
import java.util.LinkedList;
import java.util.Queue;

/**
 * Returns tokens with processing: removing all non-alphanumeric characters from
 * the beginning and end, removing apostrophes, and converting to lowercase.
 * Also processes hyphenated tokens.
 *
 */
public class TokenProcessorStream implements TokenStream {

    private String token;
    private Queue<String> tokenQueue; // store result of hyphenated tokens
    private Boolean firstPass; // flag for if processing the first token

    public TokenProcessorStream(String text) {
        token = text;
        tokenQueue = new LinkedList<String>();
        firstPass = true;
    }

    @Override
    public boolean hasNextToken() {
        return firstPass == true || !tokenQueue.isEmpty();
    }

    @Override
    public String nextToken() {
        if (!tokenQueue.isEmpty()) {
            return tokenQueue.poll();
        }

        // remove non-alphanumeric characters from beginning and end
        // remove apostrophes and convert to lowercase
        token = token.replaceAll("^\\W+|\\W+$", "");
        token = token.replaceAll("'", "").toLowerCase();
        if (token.contains("-")) {
            // split hyphenated token and add to queue
            String[] tokens = token.split("-");
            for (String t : tokens) {
                if (t.length() > 0) {
                    tokenQueue.add(t);
                }
            }
            // remove hyphens from the original token
            token = token.replaceAll("-", "");
        }

        firstPass = false; // done with the first token

        // return the first token
        // subsequent calls to nextToken() will poll from the queue
        return token.length() > 0 ? token
                : hasNextToken() ? nextToken() : null;
    }
}
