package formulas;

/**
 *
 * @author Sandra
 */
public enum FormEnum {
    DEFAULT, TFIDF, OKAPI, WACKY;
    public int getID(){
        int result = 0;
        switch (this) {
            case WACKY:
                result = 3;
                break;
            case TFIDF:
                result = 1;
                break;
            case OKAPI:
                result = 2;
                break;
            default:
                result = 0;
                break;
        }
        return result;
    }
    public static FormEnum getFormByID(int id){
        FormEnum result = null;
        switch (id) {
            case 0:
                result = FormEnum.DEFAULT;
                break;
            case 1:
                result = FormEnum.TFIDF;
                break;
            case 2:
                result = FormEnum.OKAPI;
                break;
            default:
                result = FormEnum.WACKY;
                break;
        }
        return result;
    }
    @Override
    public String toString(){
        String result = "";
        switch (this) {
            case OKAPI:
                result = "Okapi BM25";
                break;
            case TFIDF:
                result = "tf-idf";
                break;
            case WACKY:
                result = "Wacky";
                break;
            default:
                result = "Dafault";
                break;
        }
        return result;
    }
}
