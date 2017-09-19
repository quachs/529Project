
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Query handling
 *
 * @author Sandra
 */
public class Query {

    String query;
    PositionalInvertedIndex index;

    public Query(String query, PositionalInvertedIndex index) {
        this.query = query;
        this.index = index;
    }

    public static void main(String[] args) {
        Query q = new Query("Test the \"query\" with this", new PositionalInvertedIndex());
        String[] a = q.checkQuery();
        for (String b : a) {
            System.out.println(b);
        }
    }

    public String[] getSubqueries() {
        // check if there are any OR querys
        if (this.query.contains("+")) {
            return this.query.split("+");
        }
        String[] res = new String[1];
        res[0] = this.query;
        return res;
    }

    /**
     * this method is for error handling the user input and showing some dialogs
     * it also handles the query
     *
     * @return the list of document names that fit the query
     */
    public String[] checkQuery() {
        String[] sub = getSubqueries();
        List<String> list = new ArrayList<String>();
        boolean h = false;
        for (String item : sub) {
            char[] test = item.toCharArray();
            String temp = "";
            for (char ch : item.toCharArray()) {
                if (ch != ' ' || ch != '\"' && !h) {
                    temp += ch;
                    break;
                }
                if (ch == ' ' && !h) {
                    if (temp != "") {
                        list.add(temp);
                        temp = "";
                    }
                    break;
                }
                if(ch == '\"' && !h){
                    h = true;
                    break;
                }
                if(h && ch != '\"'){
                    temp += ch;
                }
                if(h && ch == '\"'){
                    h = false;
                    list.add(temp);
                    temp = "";
                }
            }
            /*if (item.contains("\"")) {
                List<Integer> positions = new ArrayList<Integer>();
                for (char ch : item.toCharArray()) {
                    if (ch == '\"') {
                        positions.add(item.indexOf(ch));
                    }
                }
                for (int i = 0; i < positions.size() / 2; i = i + 2) {
                    list.add(item.substring(positions.get(i), positions.get(+1)));
                }
            }*/
        }

        return null;
    }

    /**
     * do the query
     *
     * @return the matching file names for that term
     */
    public String[] doQuery(String term) {
        // check if the term exists in the index
        if (Arrays.binarySearch(index.getDictionary(), term) >= 0) {
            // iterate postings list and print file name
            
        } else {

        }
        return null;
    }
}
