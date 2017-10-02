
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.*;
import javax.swing.UIManager.*;

/**
 * UserInterface Implements MouseListener for user input handling
 *
 * @author Sandra
 */
public class UserInterface implements MouseListener {

    private JFrame frame; // frame of the search engine, saved for restarting it
    private Path path; // for saving the path
    private String pathString; // saving result of recrusive search for file

    // background tasks
    private Indexing indexedCorpus = new Indexing(); // task for indexing
    private GeneratingTask gen; // task for generating label/ string    

    // indecis
    private PositionalInvertedIndex index;
    private KGramIndex kIndex;
    private SoundexIndex sIndex;

    // Parser
    private QueryParser parser;

    // Strings for easily changing the text of the label number.
    private final String docs = "Number of found Documents: ";
    private final String voc = "Number of Vocabulary found in corpus: ";

    // UI elements
    private JPanel foundDocArea = new JPanel(); // area which shows basicly fond documents
    private JTextField tQuery = new JTextField(); // query input
    private JButton bSubmit = new JButton("Submit"); // submit button
    private JPanel num = new JPanel(new FlowLayout()); // panel representing label and result for showing the number of documents/ vocabulary
    private JLabel number = new JLabel(""); // label for text before number
    private JLabel numberRes = new JLabel(); // results of text
    private JButton stem = new JButton("Stem"); // start stemming button
    private JButton newDic = new JButton("Index new directory"); // start new directory indexing
    private JButton all = new JButton("Print vocabulary"); // print all voc button
    private JDialog progressDialog = new JDialog(this.frame, "Please wait..."); // initialize a new dialog, this is the frame and "Indexing..." is the title
    private JComboBox combo = new JComboBox(); // combo box for search types (normal, author)    
    private List<JLabel> labels; // List of results that are shown in the foundDocArea
    private ImageIcon img = new ImageIcon(System.getProperty("user.dir") + "/icon.png"); // logo icon

    /**
     * Constructor for new User Interface for the search engine
     */
    public UserInterface() {
        // Change look and feel of swing components
        // resource: http://docs.oracle.com/javase/tutorial/uiswing/lookandfeel/nimbus.html
        try {
            for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | UnsupportedLookAndFeelException e) {
            System.err.println(e.toString());
        }
        this.progressDialog.setIconImage(img.getImage()); // set icon for process dialog
        this.labels = new ArrayList<>(); // initialize labels array
        this.frame = new JFrame(); // initialize frame
        path = Paths.get("C"); // default path will be documents
        chooseDirectory(); // let the user choose his directory he whants to index
        // add mouseListener for the buttons
        bSubmit.addMouseListener(this);
        stem.addMouseListener(this);
        newDic.addMouseListener(this);
        all.addMouseListener(this);
    }

    /**
     * Open a file chooser dialog for choosing the directory the user wants to
     * index
     */
    private void chooseDirectory() {
        JFileChooser chooser = new JFileChooser(); // initialize file chooser        
        chooser.setCurrentDirectory(new java.io.File(path.toString())); // start with the saved path        
        chooser.setDialogTitle("Choose Directory for corpus"); // set the title of the dialog
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY); // tell the file chooser that it only shows the directory that the user only is able to choose a directory and not a file
        // resChooser saves result of dialog: int for JFileChooser options (APPROVE_OPTION, ABORT,...)
        int resChooser = chooser.showOpenDialog(null); // chooser.showOpenDialog(null) opens the dialog in the middle of the screen, it is not depended by any frame or component
        // if user approves -> clicks "Open" ...
        if (JFileChooser.APPROVE_OPTION == resChooser) {
            path = Paths.get(chooser.getSelectedFile().getPath()); // ... save the returend path,
            createProgressBar(); // create the progress bar
            indexing(); // and start indexing. While indexing show a progress bar that the user can see that something happens
        } else {
            System.exit(0); // Close the system if user presses x or cancle
        }
    }

    /**
     * While Indexing processes show a progress bar
     */
    private void indexing() {
        progressDialog.setVisible(true); // show the dialog        
        indexedCorpus = new Indexing(path); // start the task idexing
        indexedCorpus.execute(); // start the task  
        // wait till the task is finished -> set true when done() is finished
        while (!indexedCorpus.isDone()) {
        }        
        progressDialog.setVisible(false); // close the dialog
        saveIndecies(); // save all created indecis        
        createUI(); // creat the view
    }

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

    /**
     * Save all indexis created in the background task indexing
     */
    private void saveIndecies() {
        kIndex = indexedCorpus.getKgIndex();
        sIndex = indexedCorpus.getsIndex();
        index = indexedCorpus.getIndex();
    }

    /**
     * Create the UI for processing queries and other user wishes
     */     
    private void createUI() {
        this.frame.setLayout(new BoxLayout(frame.getContentPane(), BoxLayout.Y_AXIS)); // Set Layout to box Layout - vertical orientation of objects
        // Components that not need to be used or changed again
        JLabel lCombo = new JLabel("Choose search type"); // create label for explaining what you can choose in combo box
        JLabel lQuery = new JLabel("Enter the Query"); // create label for showing what to enter in the following text view
        JPanel buttons = new JPanel(new FlowLayout()); // create panel fo all buttons
        
        this.foundDocArea = new JPanel(new GridLayout(0, 1)); // initialize foundDocArea with layout that each label is in one line

        // Modify components
        lCombo.setAlignmentX(Component.CENTER_ALIGNMENT); // component in the center
        lQuery.setAlignmentX(Component.CENTER_ALIGNMENT); // component in the center
        this.combo.removeAllItems(); // clear combo (important after new directory is processed
        this.combo.addItem("Normal search"); // normal search is always available as search type
        if (sIndex.getTermCount() > 0) {
            this.combo.addItem("Search by author"); // author search only if there is any author saved in sIndex
        }

        // add components to panel buttons
        buttons.add(bSubmit);
        buttons.add(stem);
        buttons.add(newDic);
        buttons.add(all);

        // add all components to frame
        this.frame.add(lCombo);
        this.frame.add(combo);
        this.frame.add(lQuery);
        this.frame.add(tQuery);
        this.frame.add(buttons);
        this.frame.add(foundDocArea);

        // add scrolbar        
        JScrollPane jsp = new JScrollPane(foundDocArea);
        jsp.setPreferredSize(new Dimension(300, 300));
        jsp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        jsp.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        this.frame.add(jsp);

        // add num panel
        num.add(number);
        num.add(numberRes);
        this.frame.add(num);

        // setup frame
        this.frame.setTitle("My search engine");
        this.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.frame.setMinimumSize(new Dimension(500, frame.HEIGHT));
        this.frame.setIconImage(img.getImage());
        this.frame.pack();
        this.frame.setLocationRelativeTo(null);
        this.frame.setVisible(true);
    }

    /**
     * Checks if there are found documents
     * @param foundDocs List of documents found after query process
     */
    private void checkResults(List foundDocs) {
        if (foundDocs != null && foundDocs.size() > 0) {            
            generatingLabels(null, (ArrayList<Integer>) foundDocs); // .. yes than generate the labels
        } else {            
            this.foundDocArea.add(new JLabel("No document found.")); // .. no print that there are no documents
        }
    }

    /**
     * Create a String if we only get a list of vocabulary that should be printed
     * or create labels for showing the query results
     * @param docsArray Array for vocabulary
     * @param docIDList List for labels
     */
    private void generatingLabels(String[] docsArray, ArrayList<Integer> docIDList) {
        if (docIDList == null) {
            this.gen = new GeneratingTask(docsArray); // create new task with array
        } else {
            this.gen = new GeneratingTask(docIDList, (ArrayList<String>) indexedCorpus.getFileNames()); // create new task with docIds and names
        }
        progressDialog.setVisible(true); // show the dialog        
        gen.execute(); // start the task
        // wait till the task is finished -> set true when done() is finished
        while (!gen.isDone()) {
        }
        this.labels = gen.getArray(); // save created labels and add all of them to the area for found docs, by creating only a string the arraylist will be empty - no elements to add to the area
        for (JLabel a : this.labels) {
            this.foundDocArea.add(a);
        }
        this.frame.repaint();
        this.frame.pack();        
        progressDialog.setVisible(false); // close the dialog
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
                // first clear everything to refresh the view
                num.setVisible(false);                
                // remove all existing elements in the panel 
                // if we don´t do this and submit the query twice we would get the result twice too
                this.foundDocArea.removeAll();
                this.foundDocArea.repaint();
                
                parser = new QueryParser(index, kIndex); // create the parser                
                String query = this.tQuery.getText(); // save the query
                if (query.length() > 0) { // if there is any query entered                    
                    List<Integer> foundDocs; // create a list to save found documents                    
                    this.labels = new ArrayList<>(); // initialize list of labels new - important for more than one submit action
                    // check if combobox is selected for normal search
                    if ("Normal search".equals(combo.getSelectedItem().toString())) {                        
                        foundDocs = parser.getDocumentList(query);// .. yes than parse the query and save the resilt IDs                        
                        checkResults(foundDocs); // check if there are any results
                    } else { // .. or not normal search == author search                       
                        foundDocs = QueryProcessor.authorQuery(query, sIndex); // save DocIds for author search                                    
                        checkResults(foundDocs); // check if there are any results        
                    }                    
                    this.number.setText(this.docs); // set text for found documents
                    this.numberRes.setText(labels.size() + ""); // save size of documents
                    this.num.setVisible(true); // make num panel visible
                    // add a listener for mouseclicks for every single button saved in the list 
                    for (JLabel b : labels) {
                        b.addMouseListener(this);
                    }
                } else { // there is no query entered - let the user know
                    labels = new ArrayList<JLabel>();
                    this.num.setVisible(false);
                    this.foundDocArea.add(new JLabel("Please enter a term!"));
                }                
                this.foundDocArea.setVisible(true); // show panel where buttons are in
                this.frame.pack(); // reload the view again by packing the frame         
            }
            if (e.getSource() == stem) { // stemming is clicked
                // Stem the word that is input in the textfield and show it in a dialog
                this.foundDocArea.removeAll();
                this.foundDocArea.setVisible(false);
                this.num.setVisible(false);
                this.frame.pack();
                if (this.tQuery.getText().split(" ").length > 1) {
                    JOptionPane.showMessageDialog(this.frame, "Please enter only one term for stemming", "Result of stemming", JOptionPane.INFORMATION_MESSAGE, this.img);
                } else {
                    // Save the result of stemming, call Simple Token Stream
                    String result = PorterStemmer.getStem(this.tQuery.getText());
                    // Aufruf der statischen Methode showMessageDialog()
                    JOptionPane.showMessageDialog(this.frame, "Stemmed \"" + this.tQuery.getText() + "\" : " + result, "Result of stemming", JOptionPane.INFORMATION_MESSAGE, this.img);
                }
            }
            if (e.getSource() == newDic) {
                int res = JOptionPane.showConfirmDialog(this.frame, "Do you really want to index a new directory?", "Index new directory", 2, JOptionPane.INFORMATION_MESSAGE, this.img);
                if (res == 0) {
                    this.frame.setVisible(false);
                    this.frame.dispose();
                    // close everything
                    // Task where you can find positionalindex
                    indexedCorpus = new Indexing();
                    foundDocArea.removeAll();
                    labels = new ArrayList<>();
                    num.setVisible(false);
                    tQuery.setText("");
                    this.frame = new JFrame();
                    chooseDirectory();
                }
            }
            if (e.getSource() == all) {
                this.foundDocArea.removeAll();
                this.labels = new ArrayList<>();
                this.num.setVisible(true);
                generatingLabels(index.getDictionary(), null);
                JTextArea label = new JTextArea();
                label.setEditable(false);
                String res = gen.getRes();
                label.setText(res);
                label.setCaretPosition(0);
                this.foundDocArea.add(label);
                this.number.setText(this.voc);
                this.numberRes.setText(index.getTermCount() + "");
                // show panel where buttons are in
                this.foundDocArea.setVisible(true);
                // reload the view again by packing the frame
                this.frame.pack();

            }
        }

        // every button with the title of the found document has to be dobble clicked for an action
        if (e.getClickCount() == 2) {
            // save the index of the button that was clicked
            int indx = labels.indexOf(e.getSource());
            // this shows that there is really an entry found -> double click submit can´t go in this
            if (indx >= 0) {
                findFile(labels.get(indx).getText(), new File(path.toUri()));
                String p = pathString + "/" + labels.get(indx).getText();
                File file = new File(p);
                try {
                    new DisplayJson(file);
                } catch (IOException ex) {
                    Logger.getLogger(UserInterface.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    private void findFile(String name, File file) {
        File[] list = file.listFiles();
        if (list != null) {
            for (File fil : list) {
                if (fil.isDirectory()) {
                    findFile(name, fil);
                } else if (name.equalsIgnoreCase(fil.getName())) {
                    File p = fil.getParentFile();
                    pathString = p.getAbsolutePath();
                }
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
