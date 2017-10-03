import java.util.ArrayList;
import java.util.Date;
import javax.swing.*;
/**
 * Task to process in backround the walking trough all vocabulary of the corpus.
 * @author Sandra
 */
public class GeneratingTask extends SwingWorker<Void, Void> {

    private long timer; // time for printing how long task took
    private ArrayList<JLabel> labels; // arraylist of labels
    private ArrayList<Integer> docIds; // list of doc IDs
    private String[] docArray; 
    private String res; // result string for all voc
    private ArrayList<String> fileNames; // array of names of documents

    /**
     * With this parameter we know that we don´t have to create labels.
     * A string for representing it in an text area is enough.
     * This saves time!
     * @param docNames Array of vocabulary
     */
    public GeneratingTask(String[] docArray) {
        this.docArray = docArray;
        this.docIds = new ArrayList<Integer>();
        this.labels = new ArrayList<JLabel>();
    }

    /**
     * With this parameter we need to create labels to later add mouse click listener
     * Documents can be open
     * @param docIds List of document IDs
     * @param docNames List of all names of all documents
     */
    public GeneratingTask(ArrayList<Integer> docIds, ArrayList<String> docNames) {
        this.docIds = docIds;
        this.fileNames = docNames;
        this.docArray = new String[0];
        this.labels = new ArrayList<JLabel>();
    }

    /**
     * Do in Background process
     * @return nothing
     * @throws Exception 
     */
    @Override
    protected Void doInBackground() throws Exception {
        timer = new Date().getTime(); // start timer for printing out how long process took
        if (this.docArray.length > 0 && this.docIds.size() == 0) { // check if we only need to create a String
            res = "";
            for (String s : docArray) { 
                res = res + s + "\n"; // add every single voc with a linebreak to the result string
            }
        } else { // otherwhise we have to create the labels
            for (int i = 0; i < this.docIds.size(); i++) { // go through all found doc IDs
                JLabel lab = new JLabel(this.fileNames.get(docIds.get(i))); // create new label with name of found doc
                labels.add(lab); // add it to our list
            }
        }
        return null; // return null to show that we are done
    }

    /**
     * method is called when doInBackgrond is finished -> process is finished
     */
    @Override
    public void done() {
        System.out.println("Time for Generating process: " + (new Date().getTime() - timer)); // print time that process took
    }

    // Getter
    public ArrayList<JLabel> getArray() {
        return labels;
    }

    public String getRes() {
        return res;
    }
    
}
