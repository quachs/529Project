package classification;

import java.util.SortedSet;

/**
 * Class to represent a document from the training set.
 * Store the class, id, and set of terms.
 */
public class TrainingDocument {

    private Authors author;
    private int documentID;
    private SortedSet terms;

    public TrainingDocument(Authors author, int documentID, SortedSet terms) {
        this.author = author;
        this.documentID = documentID;
        this.terms = terms;
    }

    /**
     * @return the documentID
     */
    public int getDocumentID() {
        return documentID;
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
        return author + ":" + documentID + " -> " + terms;
    }

}

