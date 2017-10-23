
package Helper;

import java.awt.Dimension;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;

public class ProgressDialog {// initialize dialog with "Indexing..." as title
    private JDialog progressDialog; 
    
    public ProgressDialog(){
        progressDialog = new JDialog(new JFrame(), "Please wait...");
        createProgressBar();
    }
    public ProgressDialog(String message){
        progressDialog = new JDialog(new JFrame(), message);
        createProgressBar();
    }
    
    /**
     * Create the Progress Bar
     */
    private void createProgressBar() {        
        JPanel contentPane = new JPanel(); // create a new panel        
        contentPane.setPreferredSize(new Dimension(300, 100)); // set preferred size
        // initialize progress bar and add it to the panel
        JProgressBar bar = new JProgressBar(SwingConstants.HORIZONTAL);
        bar.setIndeterminate(true);
        contentPane.add(bar);        
        progressDialog.setContentPane(contentPane); // add panel to the dialog        
        progressDialog.pack(); // pack the dialog you minimalize the size of it        
        progressDialog.setLocationRelativeTo(null); // set the location to the center of the screen
    }
    
    public void setVisible(boolean v){
        this.progressDialog.setVisible(v);
    }
}
