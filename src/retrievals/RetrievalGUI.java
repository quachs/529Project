package retrievals;

import threads.GeneratingTask;
import helper.DisplayJson;
import formulas.FormEnum;
import helper.PorterStemmer;
import helper.ProgressDialog;
import helper.SpellingCorrection;
import indexes.KGramIndex;
import indexes.diskPart.DiskSoundexIndex;
import indexes.diskPart.DiskInvertedIndex;
import retrievals.rankedRetrieval.RankedDocument;
import threads.ThreadFinishedCallBack;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import start.UserInterface;

/**
 * Represents the GUI for the retrieval part of our project. It is a thread to show the progress
 * bar while it is created. The creation needs some time because of the many
 * things that have to be set at the beginning. It implements MouseListener
 * to react on buttons or label clicks. The ActionListener is for changing combo
 * box selections. The ThreadFinishedCallCack is to notify the GUI that the
 * background thread is finished.
 */
public class RetrievalGUI extends Thread implements MouseListener, ActionListener, ThreadFinishedCallBack {

    private JFrame frame; // Frame of the search engine, saved for successfullly restarting it
    private String path; // For saving the path

    private ProgressDialog progressDialog = new ProgressDialog("Generating...");

    // Indeces
    private DiskInvertedIndex dIndex;
    private KGramIndex kIndex;
    private DiskSoundexIndex sIndex;

    // Strings for easily changing the text of the label number.
    private final String docs = "Number of found Documents: ";
    private final String voc = "Size of Vocabulary found in corpus: ";

    // UI elements
    private JPanel foundDocArea = new JPanel(); // Area which shows found documents
    private JTextField tQuery = new JTextField(); // Query input
    private JButton bSubmit = new JButton("Submit"); // Submit button
    private JPanel num = new JPanel(new FlowLayout()); // Panel to display # of docs/vocab
    private JLabel number = new JLabel(""); // Label for text before number
    private JLabel numberRes = new JLabel(); // Results of text
    private JButton stem = new JButton("Stem"); // Start stemming button
    private JLabel lComboTitel; // Label for search types
    private JButton newDic = new JButton("Index new directory"); // Start new directory indexing
    private JButton all = new JButton("Print vocabulary"); // Button to print entire vocabulary

    // Initialize dialog with "Indexing..." as title
    private JComboBox comboSearchOrForms = new JComboBox(); // Combo box for search types (normal, author)    
    private JComboBox comboRetrievalType = new JComboBox(); // Combo box for retrieval types (boolean/ranked)
    private List<JLabel> labels; // List of results that are shown in the foundDocArea
    private ImageIcon img = new ImageIcon(System.getProperty("user.dir") + "/icon.png"); // Logo icon

    private FormEnum form;
    private GeneratingTask task;
    private char retr;

    private ThreadFinishedCallBack finishCreatingGUI;

    /**
     * Constructor for new User Interface for the search engine
     */
    public RetrievalGUI(char retr, FormEnum form, String path, ThreadFinishedCallBack finish) {
        this.finishCreatingGUI = finish;
        this.dIndex = new DiskInvertedIndex(path);
        this.labels = new ArrayList<>(); // Initialize labels array
        this.frame = new JFrame(); // Initialize frame
        this.form = form;
        this.path = path;
        this.retr = retr;
        this.start();
    }

    @Override
    public void run() {
        // Add MouseListener for the buttons
        bSubmit.addMouseListener(this);
        stem.addMouseListener(this);
        newDic.addMouseListener(this);
        all.addMouseListener(this);
        // Get the soundex index for creating the combo box right
        sIndex = new DiskSoundexIndex(path);
        // Deserialize the k-gram index
        this.kIndex = new KGramIndex();
        createKGramIndex();
        createUI(retr, form);
    }

    private void createKGramIndex() {
        try {
            FileInputStream fileIn = new FileInputStream(path + "/Indexes/kGramIndex.bin");
            ObjectInputStream objectIn = new ObjectInputStream(fileIn);
            this.kIndex = (KGramIndex) objectIn.readObject();
            objectIn.close();
            fileIn.close();
        } catch (IOException | ClassNotFoundException ex) {
            System.out.println(ex.toString());
        }
    }

    /**
     * Create the UI for processing queries and performing other user tasks
     */
    private void createUI(char retr, FormEnum form) {

        // Set Layout to box Layout - vertical orientation of objects
        this.frame.setLayout(new BoxLayout(frame.getContentPane(), BoxLayout.Y_AXIS));
        // Components that do not need to be used or changed again
        lComboTitel = new JLabel("Choose search type"); // Create label that explains what combo box options
        JLabel lComboRetrieval = new JLabel("Choose retrieval type"); // Create label that explains what combo box options
        JLabel lQuery = new JLabel("Enter the Query"); // Create label for showing what to enter in the text box
        JPanel buttons = new JPanel(new FlowLayout()); // Create panel for all buttons

        this.foundDocArea = new JPanel(new GridLayout(0, 1)); // Initialize foundDocArea with one doc per line

        // Modify components
        lQuery.setAlignmentX(Component.CENTER_ALIGNMENT); // Component in the center

        this.comboRetrievalType.addItem("Boolean Retrieval");
        this.comboRetrievalType.addItem("Ranked Retrieval");
        if (retr == 'b') { // "b" is for boolean retrival, the firt entry in the combobox.
            this.comboRetrievalType.setSelectedIndex(0); // Set seletion to boolean
            this.lComboTitel.setText("Choose search type");
            this.comboSearchOrForms.removeAllItems(); // Clear combo (important after new directory is processed)
            this.comboSearchOrForms.addItem("Normal search"); // Normal search is always available as search type
            if (sIndex.getTermCount() > 0) {
                this.comboSearchOrForms.addItem("Search by author"); // Author search only if there are authors saved in sIndex
            }
        } else {
            this.comboRetrievalType.setSelectedIndex(1); // Set selection to ranked
            this.lComboTitel.setText("Choose formula for ranked retrieval");
            this.comboSearchOrForms.removeAllItems(); // Clear combo
            this.comboSearchOrForms.addItem(FormEnum.DEFAULT);
            this.comboSearchOrForms.addItem(FormEnum.TFIDF);
            this.comboSearchOrForms.addItem(FormEnum.OKAPI);
            this.comboSearchOrForms.addItem(FormEnum.WACKY);
            this.comboSearchOrForms.setSelectedIndex(form.getID());
        }
        this.comboRetrievalType.addActionListener(this);

        // Create combo boxes next to each other
        JPanel combos = new JPanel(new GridLayout(2, 2));
        // Add all components to frame
        combos.add(lComboRetrieval);
        combos.add(lComboTitel);
        combos.add(comboRetrievalType);
        combos.add(comboSearchOrForms);

        // Add components to panel buttons
        buttons.add(bSubmit);
        buttons.add(stem);
        buttons.add(newDic);
        buttons.add(all);

        this.frame.add(combos);
        this.frame.add(lQuery);
        this.frame.add(tQuery);
        this.frame.add(buttons);
        this.frame.add(foundDocArea);

        // Add scrolbar        
        JScrollPane jsp = new JScrollPane(foundDocArea);
        jsp.setPreferredSize(new Dimension(300, 300));
        jsp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        jsp.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        this.frame.add(jsp);

        // Add num panel
        num.add(number);
        num.add(numberRes);
        this.frame.add(num);

        // Setup frame
        this.frame.setTitle("My search engine");
        this.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.frame.setMinimumSize(new Dimension(500, frame.HEIGHT));
        this.frame.setIconImage(img.getImage());
        this.frame.pack();
        this.frame.setLocationRelativeTo(null);
        this.frame.setVisible(true);
        this.finishCreatingGUI.notifyThreadFinished();
    }

    /**
     * Handles the click event
     *
     * @param e: is the MouseEvent; with getSource(), you know which button
     * throws the event
     */
    @Override
    public void mouseClicked(MouseEvent e) {
        // Submit Button only needs to be clicked ones
        if (e.getClickCount() == 1) {
            // Check if Submit is clicked
            if (e.getSource() == bSubmit) {
                // Clear everything in order to refresh the view
                num.setVisible(false);
                // Remove all existing elements in the panel 
                // If this is not done and submit the query twice, this would return the results twice too
                this.foundDocArea.removeAll();
                this.foundDocArea.repaint();

                String query = this.tQuery.getText(); // Save the query
                // Check if user input is non-empty
                if (query.length() > 0) {
                    // If yes, start the retrival for the selected user choice
                    if (this.comboRetrievalType.getSelectedIndex() == 0) { // 0 = boolean
                        booleanRetrieval(query);
                    } else {
                        rankedRetrieval(query);
                    }
                } else { // There is no query entered - let the user know
                    labels = new ArrayList<JLabel>();
                    this.num.setVisible(false);
                    this.foundDocArea.add(new JLabel("Please enter a term!"));
                    // Show panel where buttons are in
                    this.foundDocArea.setVisible(true);
                    // Reload the view again by packing the frame
                    this.frame.pack();
                    this.progressDialog.setVisible(false);
                }

            }
            if (e.getSource() == stem) { // stemming is clicked
                // Stem the word that is input in the text field; display in a dialog box
                this.foundDocArea.removeAll();
                this.foundDocArea.setVisible(false);
                this.num.setVisible(false);
                this.frame.pack();
                if (this.tQuery.getText().split(" ").length > 1) {
                    JOptionPane.showMessageDialog(this.frame, "Please enter only one term for stemming",
                            "Result of stemming", JOptionPane.INFORMATION_MESSAGE, this.img);
                } else {
                    // Save the result of stemming, call SimpleTokenStream
                    String result = PorterStemmer.getStem(this.tQuery.getText());
                    JOptionPane.showMessageDialog(this.frame, "Stemmed \"" + this.tQuery.getText()
                            + "\" : " + result, "Result of stemming", JOptionPane.INFORMATION_MESSAGE, this.img);
                }
            }
            if (e.getSource() == newDic) {
                int res = JOptionPane.showConfirmDialog(this.frame, "Do you really want to index a new directory?",
                        "Index new directory", 2, JOptionPane.INFORMATION_MESSAGE, this.img);
                if (res == 0) {
                    this.frame.setVisible(false);
                    this.frame.dispose();
                    // Close everything
                    foundDocArea.removeAll();
                    labels = new ArrayList<>();
                    num.setVisible(false);
                    tQuery.setText("");
                    this.frame = new JFrame();
                    UserInterface u = new UserInterface();
                }
            }
            if (e.getSource() == all) {
                this.foundDocArea.removeAll();
                this.num.setVisible(false);
                this.progressDialog.setVisible(true);
                task = new GeneratingTask(dIndex.getDictionary(), this);
                Thread thread = new Thread(task);
                thread.start();
            }
        }

        // Every button with the title of the found document has to be 
        // double clicked to be displayed.
        if (e.getClickCount() == 2) {
            // Save the index of the button that was clicked
            int indx = labels.indexOf(e.getSource());
            // This shows that there is really an entry found -> double click submit can´t go in this
            if (indx >= 0) {
                String text = labels.get(indx).getText();
                if (text.contains(":")) {
                    String[] array = text.split(" ");
                    text = array[1];
                }
                String p = path + "/" + text;
                File file = new File(p);
                try {
                    new DisplayJson(file);
                } catch (IOException ex) {
                    Logger.getLogger(RetrievalGUI.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    /**
     * Boolean retrieval is submitted. Start the process in a background thread
     */
    private void booleanRetrieval(String query) {
        progressDialog.setVisible(true); // close the dialog
        // Generate the task depending on the user input for normal or author search
        if (this.comboSearchOrForms.getSelectedIndex() == 0) {
            task = new GeneratingTask(query, dIndex, false, kIndex, sIndex, this);
        } else {
            task = new GeneratingTask(query, dIndex, true, kIndex, sIndex, this);
        }
        // Create and start the thread
        Thread t = new Thread(task);
        t.start();
    }

    /**
     * Ranked retrieval is submitted. Start the process in a background thread
     */
    private void rankedRetrieval(String query) {
        progressDialog.setVisible(true); // Show the progress dialog
        form = FormEnum.getFormByID(comboSearchOrForms.getSelectedIndex()); // Save the user-selected formula

        // If there are less then 10 documents in the corpus, only attempt to display up to the size of corpus.
        if (dIndex.getFileNames().size() < 10) {
            task = new GeneratingTask(dIndex, kIndex, query, dIndex.getFileNames().size(), this, form);
        } else {
            task = new GeneratingTask(dIndex, kIndex, query, 10, this, form);
        }
        // Create and start the thread;
        Thread t = new Thread(task);
        t.start();
    }

    /**
     * Change the items of the second combo box depending on the selection of
     * the first one. For boolean retrieval, the user can choose between normal
     * and author search if the documents are saving authors. It is possible
     * to add different search types to the combo box, if desired.
     * For ranked retrieval, the user is able to choose between the four different
     * types of formulas explained in the given milestone prompt.
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (this.comboRetrievalType.getSelectedIndex() == 0) {
            //this.comboRetrievalType.setSelectedIndex(0);
            this.lComboTitel.setText("Choose search type: ");
            this.comboSearchOrForms.removeAllItems(); // Clear combo (important after new directory is processed)
            this.comboSearchOrForms.addItem("Normal search"); // Normal search is always available as search type
            if (sIndex.getTermCount() > 0) {
                this.comboSearchOrForms.addItem("Search by author"); // Author search only if there are authors saved in sIndex
            }
        } else {
            //this.comboRetrievalType.setSelectedIndex(1);
            this.lComboTitel.setText("Choose formular for ranked retrieval");
            this.comboSearchOrForms.removeAllItems(); // Clear combo
            this.comboSearchOrForms.removeAllItems(); // Clear combo
            this.comboSearchOrForms.addItem(FormEnum.DEFAULT);
            this.comboSearchOrForms.addItem(FormEnum.TFIDF);
            this.comboSearchOrForms.addItem(FormEnum.OKAPI);
            this.comboSearchOrForms.addItem(FormEnum.WACKY);
            this.form = FormEnum.getFormByID(this.comboSearchOrForms.getSelectedIndex());
        }
    }

    /**
     * Is called when the thread is finished.
     */
    @Override
    public void notifyThreadFinished() {
        this.foundDocArea.removeAll(); // Remove everything from the foundDocArea so that we don't display old results
        this.labels = new ArrayList<JLabel>(); // The same with the labels
        // Go trough the three different opportunities of a thread. We either want to print all vocabulary, print the boolean results or the ranked one.
        switch (task.getOpportunities()) {
            case ALL:
                JTextArea label = new JTextArea();
                label.setText(task.getAllTerms());
                label.setEditable(false);
                label.setCaretPosition(0);
                this.foundDocArea.add(label);
                this.number.setText(this.voc);
                this.numberRes.setText(dIndex.getTermCount() + "");
                num.setVisible(true);
                break;
            case BOOLEAN:
                ArrayList<String> results = task.getResultsBool();
                if (results.get(0).equals("No documents found.")) {
                    this.foundDocArea.add(new JLabel("No documents found."));
                    this.num.setVisible(false);
                } else {

                    for (String s : results) {
                        JLabel l = new JLabel(s);
                        l.addMouseListener(this);
                        this.labels.add(l);
                        this.foundDocArea.add(l);
                    }
                }
                this.number.setText(this.docs);
                this.numberRes.setText(this.labels.size() + "");
                String modified = spellingCorrection();    
                if (modified != null) {
                    this.tQuery.setText(modified);
                    booleanRetrieval(modified);
                    return;
                }
                break;
            default: // Ranked retrival
                RankedDocument[] res = task.getResultsRank();            
                if (res == null) {
                    JLabel l = new JLabel("No documents found!");
                    this.foundDocArea.add(l);
                    this.num.setVisible(false);
                } else {
                    for (RankedDocument item : res) {
                        String output = String.format("%.6f: %s", item.getAccumulatedScore(), dIndex.getFileNames().get(item.getDocID()));
                        JLabel l = new JLabel(output);
                        l.addMouseListener(this);
                        this.labels.add(l);
                        this.foundDocArea.add(l);
                    }
                    this.number.setText("Ranking for the best 10 documents, but found number: ");
                    this.numberRes.setText(task.getRank().getSizeOfFoundDocs() + "");
                    this.num.setVisible(true);
                }
                String modifiedQuery = spellingCorrection();    
                if (modifiedQuery != null) {
                    this.tQuery.setText(modifiedQuery);
                    rankedRetrieval(modifiedQuery);
                    return;
                }
                break;
        }
        this.progressDialog.setVisible(false);
        // Show panel containing buttons
        this.foundDocArea.setVisible(true);
        // Reload the view again by packing the frame
        this.frame.pack();
    }

    /**
     * For the spelling correction check if the query needs to be modified. If
     * yes, ask the user if he wants to use the new spelling correction. If he
     * wants then return the modified query, otherwise return null
     */
    private String spellingCorrection() {
        SpellingCorrection spellCorrect = new SpellingCorrection(this.tQuery.getText(), dIndex, kIndex);
        if (spellCorrect.needCorrection()) {
            String modifiedQuery = spellCorrect.getModifiedQuery();
            modifiedQuery= modifiedQuery.trim();
            if (!modifiedQuery.equals(this.tQuery.getText())) {
                Object[] options = {"Yes", "No"};
                int pane = JOptionPane.showOptionDialog(this.frame,
                        "Did you mean: " + modifiedQuery,
                        "Spelling correction needed",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.PLAIN_MESSAGE,
                        img, options, options[1]);
                if (pane == 0) { // Yes
                    return modifiedQuery;
                }
            }
        }
        return null;
    }

    /**
     * all the other mouse click events are not used
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
