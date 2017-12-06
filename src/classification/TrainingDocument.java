package classification;

import java.util.SortedSet;

/**
 * Class to represent a document from the training set.
 * Store the class and set of terms.
 */
public class TrainingDocument {

    private Authors author;
    private SortedSet terms;

    public TrainingDocument(Authors author, SortedSet terms) {
        this.author = author;
        this.terms = terms;
    }

    /**
     * @return the terms
     */
    public SortedSet getTerms() {
        return terms;
    }

    /**
     * @return the author
     */
    public Authors getAuthor() {
        return author;
    }

    public String toString() {
        return author + " -> " + terms;
    }

}

