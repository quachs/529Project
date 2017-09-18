
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.*;

/**
 * UserInterface extends JFrame -> this means a JFrame element implements
 * ActionListener and MouseListener for user input handling
 *
 * TO-DO: Button: Stem Token -> calls simpleTokenStream and shows the reason
 * Button: New directory Button: Print all terms for corpus plus number of
 * documents found.
 *
 * @author Sandra
 */
public class UserInterface extends JFrame implements MouseListener {

    // after the path is chosen it is saved here for calling the indexing method.
    private Path path;

    // Task where you can find positionalindex
    private Indexing task = new Indexing();

    // Positional index is initialiezed in constructor
    private PositionalIndex index;

    // Strings for easily changing the text of the label number.
    private final String docs = "Number of found Documents: ";
    private final String voc = "Number of Vocabulary found in corpus: ";

    // UI elements
    private JPanel foundDocArea = new JPanel();
    JTextField tQuery = new JTextField();
    private JButton bSubmit = new JButton("Submit");
        JPanel num = new JPanel(new FlowLayout());
    private JLabel number = new JLabel("");
    private JLabel numberRes = new JLabel();
    private JButton stem = new JButton("Stem");
    private JButton newDic = new JButton("Index new directory");
    private JButton all = new JButton("Print vocabulary");

    // List of results that are shown in the foundDocArea
    private List<JLabel> labels = new ArrayList<>();

    public UserInterface() {
        this.index = task.getIndex();
        // Let the user choose his directory
        chooseDirectory();
    }

    public static void main(String[] args) {

    }

    private void createUI() {
        // TO-DO: add label for number of documents returned by the quers.
        JLabel lQuery = new JLabel("Enter the Query");

        bSubmit.addMouseListener(this);

        this.add(lQuery);
        this.add(tQuery);

        JPanel buttons = new JPanel(new FlowLayout());
        buttons.add(bSubmit);

        stem.addMouseListener(this);
        buttons.add(stem);

        newDic.addMouseListener(this);
        buttons.add(newDic);

        all.addMouseListener(this);
        buttons.add(all);

        this.add(buttons);

        this.add(foundDocArea);
        this.setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
        foundDocArea = new JPanel(new GridLayout(0, 1));
        JScrollPane jsp = new JScrollPane(foundDocArea);

        jsp.setPreferredSize(new Dimension(300, 300));
        jsp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        jsp.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

        this.add(jsp);

        num.add(number);
        num.add(numberRes);
        this.add(num);

        this.setTitle("My search engine");
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setMinimumSize(new Dimension(500, HEIGHT));
        this.pack();
        this.setLocationRelativeTo(null);
        this.setVisible(true);
    }

    /**
     * this method opens a file chooser dialog that the user can choose the
     * directory where his corpus is saved
     */
    public void chooseDirectory() {
        // initialize file chooser
        JFileChooser chooser = new JFileChooser();
        // for easier testing I chose the start directory where my created json files from homework 2 are saved
        chooser.setCurrentDirectory(new java.io.File("C:/Users/Sandra/Documents/NetBeansProjects/CECS429-Homework2/src/files"));
        // set the title of the dialog
        chooser.setDialogTitle("Choose Directory for corpus");
        // tell the file chooser that it only shows the directory that the user only is able to choose a directory and not a file
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        // chooser.showOpenDialog(null) opens the dialog in the middle of the screen, it is not depended by any frame or component
        // if user approves -> clicks "Open" ...
        if (JFileChooser.APPROVE_OPTION == chooser.showOpenDialog(null)) {
            // ... save the returend path
            path = Paths.get(chooser.getSelectedFile().getPath());
            // and start indexing. While indexing show a progress bar that the user can see that something happens
            showProgressBar();
        } else {
            // if any other key is pressed, the window closes
        }
    }

    /**
     * While Indexing processes show a progress bar
     */
    public void showProgressBar() {
        // initialize a new dialog, this is the frame and "Indexing..." is the title
        JDialog progressDialog = new JDialog(this, "Indexing...");
        // create a new panel
        JPanel contentPane = new JPanel();
        // set preferred size
        contentPane.setPreferredSize(new Dimension(300, 100));
        // initialize progress bar and add it to the panel
        JProgressBar bar = new JProgressBar(0, 100);
        bar.setIndeterminate(true);
        contentPane.add(bar);
        // add panel to the dialog
        progressDialog.setContentPane(contentPane);
        // with pack() you minimalize the size of the dialog
        progressDialog.pack();
        // sets the location to the center of the screen
        progressDialog.setLocationRelativeTo(null);
        // start the task idexing -> it is king of a thread
        task = new Indexing(path);
        // start the task        
        task.execute();
        // show the dialog
        progressDialog.setVisible(true);
        // wait till the task is finished -> set true when done() is finished
        while (!task.isDone()) {
        }
        // close the dialog
        progressDialog.dispose();
        // creat the view
        createUI();
    }

    /**
     * Handles the click event
     *
     * @param e: is the Mouse Event. with getSource() you know which button
     * throughs the event
     */
    @Override
    public void mouseClicked(MouseEvent e) {
        // Submit Button only needs to be clicked ones
        if (e.getClickCount() == 1) {
            // check if Submit is clicked
            if (e.getSource() == bSubmit) {
                // save the query
                String query = this.tQuery.getText();
                //q = new Query(query, index);
                //String[] res = q.checkQuery();
                // remove all existing elements in the panel 
                // if we don´t do this and submit the query twice we would get the result twice too
                this.foundDocArea.removeAll();
                this.labels = new ArrayList<JLabel>();
                // for test purpose I filled the array with test buttons
                // TO-DO: get the results of the query, and take the name of the file as name (including .txt,...) -> impotant for opening the file later!
                // TO-DO: try to get a list of title of the document
                for (int i = 0; i < 10; i++) {
                    JLabel lab = new JLabel("Label " + i);
                    this.labels.add(lab);
                    this.foundDocArea.add(lab);
                }
                this.number.setText(this.docs);
                this.numberRes.setText(labels.size() + "");
                this.num.setVisible(true);

                // add a listener for mouseclicks for every single button saved in the list 
                for (JLabel b : labels) {
                    b.addMouseListener(this);
                }
                // show panel where buttons are in
                this.foundDocArea.setVisible(true);
                // reload the view again by packing the frame              
                this.pack();
            }
            if (e.getSource() == stem) {
                // Stem the word that is input in the textfield
                this.foundDocArea.removeAll();
                this.foundDocArea.setVisible(false);
                this.num.setVisible(false);
                this.pack();
                // Save the result of stemming, call Simple Token Stream
                String result="Result";
                // Aufruf der statischen Methode showMessageDialog()
                JOptionPane.showMessageDialog(this, "Stemmed \"" + this.tQuery.getText()+"\" : "+result, "Result of stemming", JOptionPane.INFORMATION_MESSAGE);
            }
            if (e.getSource() == newDic){
                
            }
        }

        // every button with the title of the found document has to be dobble clicked for an action
        if (e.getClickCount() == 2) {
            // save the index of the button that was clicked
            int indx = labels.indexOf(e.getSource());
            // this shows that there is really an entry found -> double click submit can´t go in this
            if (indx >= 0) {
                // for testing: print the name of the button
                // TO-DO: Open the file that is saved there. This works with the saved path and the name of the button
                System.out.println(labels.get(indx).getText());
            }

        }
    }
    
    /**
     * all the other mouse click events aren´t used because we don´t need them
     *
     * @param e
     */
    @Override
    public void mousePressed(MouseEvent e) {

    }

    @Override
    public void mouseReleased(MouseEvent e) {

    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }
}
