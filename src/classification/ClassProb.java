package classification;

/**
 * Class to associate author classification with a probability score
*/
public class ClassProb {

    private Authors author;
    private double prob;

    public ClassProb(Authors author, double prob) {
        this.author = author;
        this.prob = prob;
    }

    /**
     * @return the author
     */
    public Authors getAuthor() {
        return author;
    }

    /**
     * @return the probability
     */
    public double getProb() {
        return prob;
    }

}
