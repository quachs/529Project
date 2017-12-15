package classification;

import token.TokenStream;
import helper.PorterStemmer;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Scanner;

/**
 * Returns tokens with processing: 
 * + removing all non-alphanumeric characters from the beginning and end
 * + removing apostrophes and hyphens
 * + converting to lowercase
 * + stemming
 */
public class ClassTokenStream implements TokenStream {

    private Scanner mReader;
    
    public ClassTokenStream(File fileToOpen) throws FileNotFoundException {
      mReader = new Scanner(new FileReader(fileToOpen));
   }

    @Override
    public boolean hasNextToken() {
        return mReader.hasNext();
    }

    @Override
    public String nextToken() {
        if (!hasNextToken()) {
            return null;
        }

        // remove non-alphanumeric characters from beginning and end
        // remove apostrophes and hyphens, convert to lowercase
        String token = mReader.next();
        token = token.replaceAll("^\\W+|\\W+$", "");
        token = token.replaceAll("['-]", "").toLowerCase();
        token = PorterStemmer.getStem(token);

        return token.length() > 0 ? token
                : hasNextToken() ? nextToken() : null;
    }
}
