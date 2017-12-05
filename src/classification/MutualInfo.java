package classification;

/**
 * Class to represent mutual information term-class pair
 */
public class MutualInfo implements Comparable<MutualInfo> {

    private String term;
    private Authors author;
    private double score;

    public MutualInfo(String term, Authors author, double score) {
        this.term = term;
        this.author = author;
        this.score = score;
    }

    /**
     * @return the term
     */
    public String getTerm() {
        return term;
    }

    /**
     * @return the author
     */
    public Authors getAuthor() {
        return author;
    }

    /**
     * @return the mutualInfo
     */
    public double getScore() {
        return score;
    }

    public String toString() {
        return author + ":" + term + " -> " + score;
    }

    public int compareTo(MutualInfo other) {
        if (other.score < this.score) {
            return -1;
        }
        if (this.score == other.score) {
            return 0;
        } else {
            return 1;
        }

    }

}
