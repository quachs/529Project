package classification;

/**
 * Class to represent mutual information term-class pair
 */
public class MutualInfo implements Comparable<MutualInfo> {

    private String term;
    private double score;

    public MutualInfo(String term, double score) {
        this.term = term;
        this.score = score;
    }

    /**
     * @return the term
     */
    public String getTerm() {
        return term;
    }

    /**
     * @return the mutualInfo
     */
    public double getScore() {
        return score;
    }

    public String toString() {
        return term + " : " + score;
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
