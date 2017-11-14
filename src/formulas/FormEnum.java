package formulas;

/**
 * Enum for easier GUI setup and calling the right methods and classes.
 */
public enum FormEnum {
    DEFAULT, TFIDF, OKAPI, WACKY;

    /**
     * The ID represents the position of the type in the GUI component combo
     * box.
     *
     * @return The ID of the saved enum.
     */
    public int getID() {
        switch (this) {
            case WACKY:
                return 3;
            case TFIDF:
                return 1;
            case OKAPI:
                return 2;
            default:
                return 0;
        }
    }

    /**
     * Get the right type of enum by giving the ID. Also needed for working with
     * the combo box.
     *
     * @param id ID for the wanted type.
     * @return The Type for the ID.
     */
    public static FormEnum getFormByID(int id) {
        switch (id) {
            case 0:
                return FormEnum.DEFAULT;
            case 1:
                return FormEnum.TFIDF;
            case 2:
                return FormEnum.OKAPI;
            default:
                return FormEnum.WACKY;
        }
    }

    /**
     * Get the pretty form of the enum Type to print it in the combo box or to
     * the buttons at the beginning.
     *
     * @return The string of the type.
     */
    @Override
    public String toString() {
        switch (this) {
            case OKAPI:
                return "Okapi BM25";
            case TFIDF:
                return "Traditional";
            case WACKY:
                return "Wacky";
            default:
                return "Default";
        }
    }
}
