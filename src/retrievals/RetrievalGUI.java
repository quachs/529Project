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
 * Represents the GUI for the retrival part. It is a thread to show the progress
 * bar while it is created. The creation needs some time because of the many
 * things that have to be set at the beginning. It implements the mousListener
 * to react on buttons or label clicks. Tje ActionListener is for changing combo
 * box selections. The ThreadFinishedCallCack is to notify the GUI that the
 * background thread is finisched.
 */
public class RetrievalGUI extends Thread implements MouseListener, ActionListener, ThreadFinishedCallBack {

    private JFrame frame; // frame of the search engine, saved for successfull restarting it
    private String path; // for saving the path

    private ProgressDialog progressDialog = new ProgressDialog("Generating...");

    // indecis
    private DiskInvertedIndex dIndex;
    private KGramIndex kIndex;
    private DiskSoundexIndex sIndex;

    // Strings for easily changing the text of the label number.
    private final String docs = "Number of found Documents: ";
    private final String voc = "Size of Vocabulary found in corpus: ";

    // UI elements
    private JPanel foundDocArea = new JPanel(); // area which shows found documents
    private JTextField tQuery = new JTextField(); // query input
    private JButton bSubmit = new JButton("Submit"); // submit button
    private JPanel num = new JPanel(new FlowLayout()); // panel to display # of docus/vocab
    private JLabel number = new JLabel(""); // label for text before number
    private JLabel numberRes = new JLabel(); // results of text
    private JButton stem = new JButton("Stem"); // start stemming button
    private JLabel lComboTitel; // label for search types
    private JButton newDic = new JButton("Index new directory"); // start new directory indexing
    private JButton all = new JButton("Print vocabulary"); // print all vocab button

    // initialize dialog with "Indexing..." as title
    private JComboBox comboSearchOrForms = new JComboBox(); // combo box for search types (normal, author)    
    private JComboBox comboRetrievalType = new JComboBox(); // combo box for retrival types (boolean/ ranked)
    private List<JLabel> labels; // List of results that are shown in the foundDocArea
    private ImageIcon img = new ImageIcon(System.getProperty("user.dir") + "/icon.png"); // logo icon

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
        this.labels = new ArrayList<>(); // initialize labels array
        this.frame = new JFrame(); // initialize frame
        this.form = form;
        this.path = path;
        this.retr = retr;
        this.start();
    }

    @Override
    public void run() {
        // add mouseListener for the buttons
        bSubmit.addMouseListener(this);
        stem.addMouseListener(this);
        newDic.addMouseListener(this);
        all.addMouseListener(this);
        // get the soundex index for creating the combo box right
        sIndex = new DiskSoundexIndex(path);
        // deserialize the k-gram index
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
     * Create the UI for processing queries and other user wishes
     */
    private void createUI(char retr, FormEnum form) {

        // Set Layout to box Layout - vertical orientation of objects
        this.frame.setLayout(new BoxLayout(frame.getContentPane(), BoxLayout.Y_AXIS));
        // Components that do not need to be used or changed again
        lComboTitel = new JLabel("Choose search type"); // create label that explains what combo box options
        JLabel lComboRetrieval = new JLabel("Choose retrieval type"); // create label that explains what combo box options
        JLabel lQuery = new JLabel("Enter the Query"); // create label for showing what to enter in the text box
        JPanel buttons = new JPanel(new FlowLayout()); // create panel fo all buttons

        this.foundDocArea = new JPanel(new GridLayout(0, 1)); // initialize foundDocArea with one doc per line

        // Modify components
        lQuery.setAlignmentX(Component.CENTER_ALIGNMENT); // component in the center

        this.comboRetrievalType.addItem("Boolean Retrieval");
        this.comboRetrievalType.addItem("Ranked Retrieval");
        if (retr == 'b') { // b is boolean retrival, is firt entry in combobox.
            this.comboRetrievalType.setSelectedIndex(0); // set seletion to boolean
            this.lComboTitel.setText("Choose search type");
            this.comboSearchOrForms.removeAllItems(); // clear combo (important after new directory is processed)
            this.comboSearchOrForms.addItem("Normal search"); // normal search is always available as search type
            if (sIndex.getTermCount() > 0) {
                this.comboSearchOrForms.addItem("Search by author"); // author search only if there is any author saved in sIndex
            }
        } else {
            this.comboRetrievalType.setSelectedIndex(1); // seat seletion to ranked
            this.lComboTitel.setText("Choose formula for ranked retrieval");
            this.comboSearchOrForms.removeAllItems(); // clear combo
            this.comboSearchOrForms.addItem(FormEnum.DEFAULT);
            this.comboSearchOrForms.addItem(FormEnum.TFIDF);
            this.comboSearchOrForms.addItem(FormEnum.OKAPI);
            this.comboSearchOrForms.addItem(FormEnum.WACKY);
            this.comboSearchOrForms.setSelectedIndex(form.getID());
        }
        this.comboRetrievalType.addActionListener(this);

        // Create combo boxes next to each other
        JPanel combos = new JPanel(new GridLayout(2, 2));
        // add all components to frame
        combos.add(lComboRetrieval);
        combos.add(lComboTitel);
        combos.add(comboRetrievalType);
        combos.add(comboSearchOrForms);

        // add components to panel buttons
        buttons.add(bSubmit);
        buttons.add(stem);
        buttons.add(newDic);
        buttons.add(all);

        this.frame.add(combos);
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
        this.finishCreatingGUI.notifyThreadFinished();
    }

    /**
     * Handles the click event
     *
     * @param e: is the Mouse Event. with getSource() you know which button
     * throws the event
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
                // if this is not done and submit the query twice, this would return the results twice too
                this.foundDocArea.removeAll();
                this.foundDocArea.repaint();

                String query = this.tQuery.getText(); // save the query
                // check if user typed something in...
                if (query.length() > 0) {
                    // if yes start the retrival for the selected user choice
                    if (this.comboRetrievalType.getSelectedIndex() == 0) { // 0 = boolean
                        booleanRetrieval(query);
                    } else {
                        rankedRetrieval(query);
                    }
                } else { // there is no query entered - let the user know
                    labels = new ArrayList<JLabel>();
                    this.num.setVisible(false);
                    this.foundDocArea.add(new JLabel("Please enter a term!"));
                    // show panel where buttons are in
                    this.foundDocArea.setVisible(true);
                    // reload the view again by packing the frame
                    this.frame.pack();
                    this.progressDialog.setVisible(false);
                }

            }
            if (e.getSource() == stem) { // stemming is clicked
                // Stem the word that is input in the textfield and show it in a dialog
                this.foundDocArea.removeAll();
                this.foundDocArea.setVisible(false);
                this.num.setVisible(false);
                this.frame.pack();
                if (this.tQuery.getText().split(" ").length > 1) {
                    JOptionPane.showMessageDialog(this.frame, "Please enter only one term for stemming",
                            "Result of stemming", JOptionPane.INFORMATION_MESSAGE, this.img);
                } else {
                    // Save the result of stemming, call Simple Token Stream
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
                    // close everything
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

        // every button with the title of the found document has to be double clicked for an action
        if (e.getClickCount() == 2) {
            // save the index of the button that was clicked
            int indx = labels.indexOf(e.getSource());
            // this shows that there is really an entry found -> double click submit can´t go in this
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
     * Boolean retrival is submitted. Start the proccess in a background thread
     */
    private void booleanRetrieval(String query) {
        progressDialog.setVisible(true); // close the dialog
        // generate the task depending on the user input for normal or author search
        if (this.comboSearchOrForms.getSelectedIndex() == 0) {
            task = new GeneratingTask(query, dIndex, false, kIndex, sIndex, this);
        } else {
            task = new GeneratingTask(query, dIndex, true, kIndex, sIndex, this);
        }
        // create and start the thread
        Thread t = new Thread(task);
        t.start();
    }

    /**
     * Ranked retrival is submitted. Start the proccess in a background thread
     */
    private void rankedRetrieval(String query) {
        progressDialog.setVisible(true); // show the progress dialog
        form = FormEnum.getFormByID(comboSearchOrForms.getSelectedIndex()); // save the formular that is selected by the user 

        // if we have less then 10 documents in the corpus we show maximum the size of documents otherwise we only show the best ten results
        if (dIndex.getFileNames().size() < 10) {
            task = new GeneratingTask(dIndex, kIndex, query, dIndex.getFileNames().size(), this, form);
        } else {
            task = new GeneratingTask(dIndex, kIndex, query, 10, this, form);
        }
        // create and start the thread;
        Thread t = new Thread(task);
        t.start();
    }

    /**
     * Change the items of the second combo box depending on the selection of
     * the first one. For boolean retrival the user can choose between normal
     * and author search if the documents are saving documents. The combo box
     * though stays visible, maybe we want to add different search types later.
     * For ranked retrival is the user able to choose between the four diffrent
     * types of formulars explained in the given paper.
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (this.comboRetrievalType.getSelectedIndex() == 0) {
            //this.comboRetrievalType.setSelectedIndex(0);
            this.lComboTitel.setText("Choose search type: ");
            this.comboSearchOrForms.removeAllItems(); // clear combo (important after new directory is processed)
            this.comboSearchOrForms.addItem("Normal search"); // normal search is always available as search type
            if (sIndex.getTermCount() > 0) {
                this.comboSearchOrForms.addItem("Search by author"); // author search only if there is any author saved in sIndex
            }
        } else {
            //this.comboRetrievalType.setSelectedIndex(1);
            this.lComboTitel.setText("Choose formular for ranked retrieval");
            this.comboSearchOrForms.removeAllItems(); // clear combo
            this.comboSearchOrForms.removeAllItems(); // clear combo
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
        this.foundDocArea.removeAll(); // remove everything from the foundDocArea that we never print out stuff twice.
        this.labels = new ArrayList<JLabel>(); // the same with the labels
        // go trough the three different opportunities of a thread. We either want to print all vocabulary, print the boolean results or the ranked one.
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
                boolean number = true;
                String modified = spellingCorrection();
                if (modified != null) {
                    this.tQuery.setText(modified);
                    booleanRetrieval(modified);
                    return;
                }
                if (results.get(0).equals("No documents found.")) {
                    this.foundDocArea.add(new JLabel("No documents found."));
                    number = false;
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
                this.num.setVisible(number);
                break;
            default: // equals ranked retrival
                RankedDocument[] res = task.getResultsRank();
                String modifiedQuery = spellingCorrection();
                if (modifiedQuery != null) {
                    this.tQuery.setText(modifiedQuery);
                    rankedRetrieval(modifiedQuery);
                    return;
                }
                if (res == null) {
                    JLabel l = new JLabel("No documents found!");
                    this.foundDocArea.add(l);
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
                break;
        }
        this.progressDialog.setVisible(false);
        // show panel where buttons are in
        this.foundDocArea.setVisible(true);
        // reload the view again by packing the frame
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
            if (modifiedQuery != null) {
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
