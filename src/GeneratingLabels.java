
import java.util.ArrayList;
import java.util.Date;
import javax.swing.*;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author Sandra
 */
public class GeneratingLabels extends SwingWorker<Void, Void> {

    long timer;
    ArrayList<JLabel> array = new ArrayList<JLabel>();
    ArrayList<Integer> docIds;
    String[] docArray;
    String res;
    ArrayList<String> fileNames;

    GeneratingLabels() {

    }

    GeneratingLabels(String[] docNames) {
        this.docArray = docNames;
        this.docIds = new ArrayList<Integer>();
    }

    GeneratingLabels(ArrayList<Integer> docIds, ArrayList<String> docNames) {
        this.docIds = docIds;
        this.fileNames = docNames;
        this.docArray = new String[0];
    }

    @Override
    protected Void doInBackground() throws Exception {
        setProgress(0);
        timer = new Date().getTime();
        if (this.docArray.length > 0 && this.docIds.size() ==0) {
            res = "";
                for (String s : docArray) {
                    res = res + s + "\n";
                }
        } else {
            for (int i = 0; i < this.docIds.size(); i++) {
                JLabel lab = new JLabel(this.fileNames.get(docIds.get(i)));
                array.add(lab);
            }
        }

        return null;
    }

    /**
     * method is called when doInBackgrond is finished -> process is finished
     */
    @Override
    public void done() {
        System.out.println("Time for Generating panel: " + (new Date().getTime() - timer));
    }

}
