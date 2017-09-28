import java.util.*;


/**
 * Class of generic methods to handle boolean retrieval operations
 * as well as positional intersection
 * 
 */
public final class ListMerge {
    
    /**
     * Get the intersection of two ordered lists
     * @param <T>
     * @param list1 list of comparable elements
     * @param list2 list of comparable elements
     * @return resulting list of intersecting list1 and list2
     */
     public static <T extends Comparable> List<T> intersectList(List<T> list1, List<T> list2){
        List<T> result = new ArrayList<T>();
        int i = 0;
        int j = 0;
        
        while( i < list1.size() && j < list2.size()){
            if(list1.get(i).compareTo(list2.get(j)) == 0){
                result.add(list1.get(i));
                i++;
                j++;
            }
            else if(list1.get(i).compareTo(list2.get(j)) < 0){ // list1 before list2
                i++; 
            }
            else{ // list2 before list1
                j++;
            }
        }
        
        return result;
}
    
     /**
     * OR the two given lists
     * @param <T>
     * @param list1
     * @param list2
     * @return resulting list of ORing list1 and list2 
     */
    public static <T extends Comparable> List<T> orList(List<T> list1, List<T> list2){
        List<T> result = new ArrayList<T>();
        int i = 0;
        int j = 0;
        
        while( i < list1.size() && j < list2.size()){
            if(list1.get(i).compareTo(list2.get(j)) == 0){
                result.add(list1.get(i));
                i++;
                j++;
            }
            else if(list1.get(i).compareTo(list2.get(j)) < 0){ // list1 before list2 
                result.add(list1.get(i));
                i++;
            }
            else{ // list2 before list1
                result.add(list2.get(j));
                j++;
            }
        }
        
        // append the longer list to the results
        if( i < list1.size()){
            while( i < list1.size()){
                result.add(list1.get(i));
                i++;
            }  
        }
        else if( j < list2.size()){
            while( j < list2.size()){
                result.add(list2.get(j));
                j++;
            }
        }
        
        return result;
    }
    
    /**
     * Positional intersection of two terms where the second term appears within
     * k positions after the first. Source: Introduction to Information
     * Retrieval (Figure 2.12)
     *
     * @param term1 positional postings list of the first term
     * @param term2 positional postings list of the second term
     * @param k max positions that term2 appears after term1
     * @return positional postings list from the intersection; the position
     * corresponds to term2
     */
    public static List<PositionalPosting> positionalIntersect
        (List<PositionalPosting> term1, List<PositionalPosting> term2, int k) {

        List<PositionalPosting> result = new ArrayList<PositionalPosting>();
        List<Integer> docs1 = new ArrayList<Integer>(); // term1 documents
        List<Integer> docs2 = new ArrayList<Integer>(); //term2 documents
        int i = 0; // term1 document index
        int j = 0; // term2 document index

        // load the docIDs to list
        for (PositionalPosting p : term1) {
            docs1.add(p.getDocumentID());
        }
        for (PositionalPosting p : term2) {
            docs2.add(p.getDocumentID());
        }

        // intersect the docs
        while (i < docs1.size() && j < docs2.size()) {
            // both terms appear in the doc
            if ((int) docs1.get(i) == docs2.get(j)) {
                List<Integer> candidate = new ArrayList<Integer>();
                List<Integer> pp1 = term1.get(i).getTermPositions(); // term1 positions
                List<Integer> pp2 = term2.get(j).getTermPositions(); // term2 positions
                int ii = 0; // term1 position index
                int jj = 0; // term2 position index

                // check if term2 appears within k positions after term1
                while (ii < pp1.size()) {
                    while (jj < pp2.size()) {
                        int relativePos = pp2.get(jj) - pp1.get(ii);
                        if (relativePos > 0 && relativePos <= k) {
                            // add term2 position to candidates
                            candidate.add(pp2.get(jj));
                        } else if (pp2.get(jj) > pp1.get(ii)) {
                            break;
                        }
                        jj++;
                    }
                    while (!candidate.isEmpty() && Math.abs(candidate.get(0) - pp1.get(ii)) > k) {
                        candidate.remove(0);
                    }
                    for (Integer pos : candidate) {
                        int currentIndex = result.size() - 1;
                        if (!result.isEmpty() && result.get(currentIndex).getDocumentID() == docs1.get(i)) {
                            // the query appears more than once in the doc
                            // add the position to existing posting
                            result.get(currentIndex).addPosition(pos);
                        } else { // add a new posting to the answer  
                            result.add(new PositionalPosting(docs1.get(i), pos));
                        }
                    }
                    ii++;
                }
                i++;
                j++;
            } else if (docs1.get(i) < docs2.get(j)) {
                i++;
            } else {
                j++;
            }
        }

        return result;
    }
}