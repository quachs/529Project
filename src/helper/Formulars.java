/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package helper;

/**
 *
 * @author Sandra
 */
public enum Formulars {
    DEFAULT, TFIDF, OKAPI, WACKY;

    public double getWqt(int N, int dft) {
        double result = 0;
        switch (this) {
            case WACKY:
                result = Math.log(((double) (N - dft)) / (double) dft);
                break;
            case TFIDF:
                result = Math.log(((double) N) / ((double) dft));
                break;
            case OKAPI:
                result = Math.log((((double) (N - dft)) + 0.5) / ((double) dft) + 0.5);
                break;
            default:
                result = Math.log(((double) N) / ((double) dft) + 1);
                break;
        }
        return result;
    }

    public double getWdt(int tftd, int ave) {
        double result = 0;
        switch (this) {
            case WACKY:
                result = (1 + Math.log(tftd)) / (1 + Math.log(ave));
                break;
            case TFIDF:
                result = tftd;
                break;
            case OKAPI:
                result = 2.2 * tftd;
                break;
            default:
                result = 1 + Math.log(tftd);
                break;
        }
        return result;
    }
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
    public static Formulars getFormByID(int id){
        Formulars result = null;
        switch (id) {
            case 0:
                result = Formulars.DEFAULT;
                break;
            case 1:
                result = Formulars.TFIDF;
                break;
            case 2:
                result = Formulars.OKAPI;
                break;
            default:
                result = Formulars.WACKY;
                break;
        }
        return result;
    }
}
