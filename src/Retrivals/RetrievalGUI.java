package Retrivals;

import Threads.GeneratingTask;
import Helper.DisplayJson;
import Helper.Formulars;
import Helper.PorterStemmer;
import Helper.ProgressDialog;
import Helper.Subquery;
import Indexes.KGramIndex;
import Indexes.SoundexIndex;
import Indexes.diskPart.DiskInvertedIndex;
import Retrivals.rankedRetrival.RankedItem;
import Threads.ThreadFinishedCallBack;
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

public class RetrievalGUI implements MouseListener, ActionListener, ThreadFinishedCallBack {

    private JFrame frame; // frame of the search engine, saved for restarting it
    private String path; // for saving the path

    private ProgressDialog progressDialog = new ProgressDialog("Generating...");

    // indecis
    private DiskInvertedIndex dIndex;
    private KGramIndex kIndex;
    private SoundexIndex sIndex;

    // Strings for easily changing the text of the label number.
    private final String docs = "Number of found Documents: ";
    private final String voc = "Number of Vocabulary found in corpus: ";

    // UI elements
    private JPanel foundDocArea = new JPanel(); // area which shows found documents
    private JTextField tQuery = new JTextField(); // query input
    private JButton bSubmit = new JButton("Submit"); // submit button
    private JPanel num = new JPanel(new FlowLayout()); // panel to display # of docus/vocab
    private JLabel number = new JLabel(""); // label for text before number
    private JLabel numberRes = new JLabel(); // results of text
    private JButton stem = new JButton("Stem"); // start stemming button
    private JLabel lComboTitel; // label for search types
    //private JButton newDic = new JButton("Index new directory"); // start new directory indexing
    private JButton all = new JButton("Print vocabulary"); // print all vocab button

    // initialize dialog with "Indexing..." as title
    private JComboBox comboSearchOrForms = new JComboBox(); // combo box for search types (normal, author)    
    private JComboBox comboRetrivalType = new JComboBox(); // combo box for retrival types (boolean/ ranked)
    private List<JLabel> labels; // List of results that are shown in the foundDocArea
    private ImageIcon img = new ImageIcon(System.getProperty("user.dir") + "/icon.png"); // logo icon

    private Formulars form;
    private GeneratingTask task;

    /**
     * Constructor for new User Interface for the search engine
     */
    public RetrievalGUI(char retr, Formulars form, String path) {
        this.dIndex = new DiskInvertedIndex(path);
        this.labels = new ArrayList<>(); // initialize labels array
        this.frame = new JFrame(); // initialize frame
        this.form = form;
        this.path = path;

        // add mouseListener for the buttons
        bSubmit.addMouseListener(this);
        stem.addMouseListener(this);
        //newDic.addMouseListener(this);
        all.addMouseListener(this);
        /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // get the soundex index for creating the combo box right
        sIndex = new SoundexIndex();
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
    private void createUI(char retr, Formulars form) {

        // Set Layout to box Layout - vertical orientation of objects
        this.frame.setLayout(new BoxLayout(frame.getContentPane(), BoxLayout.Y_AXIS));
        // Components that do not need to be used or changed again
        lComboTitel = new JLabel("Choose search type"); // create label that explains what combo box options
        JLabel lComboRetrival = new JLabel("Choose retrival type"); // create label that explains what combo box options
        JLabel lQuery = new JLabel("Enter the Query"); // create label for showing what to enter in the text box
        JPanel buttons = new JPanel(new FlowLayout()); // create panel fo all buttons

        this.foundDocArea = new JPanel(new GridLayout(0, 1)); // initialize foundDocArea with one doc per line

        // Modify components
        lQuery.setAlignmentX(Component.CENTER_ALIGNMENT); // component in the center

        this.comboRetrivalType.addItem("Boolean Retrival");
        this.comboRetrivalType.addItem("Ranked Retrival");
        if (retr == 'b') {
            this.comboRetrivalType.setSelectedIndex(0);
            this.lComboTitel.setText("Choose search type");
            this.comboSearchOrForms.removeAllItems(); // clear combo (important after new directory is processed)
            this.comboSearchOrForms.addItem("Normal search"); // normal search is always available as search type
            if (sIndex.getTermCount() > 0) {
                this.comboSearchOrForms.addItem("Search by author"); // author search only if there is any author saved in sIndex
            }
        } else {
            this.comboRetrivalType.setSelectedIndex(1);
            this.lComboTitel.setText("Choose formular for ranked retrival");
            this.comboSearchOrForms.removeAllItems(); // clear combo
            this.comboSearchOrForms.addItem("Dafault");
            this.comboSearchOrForms.addItem("tf-idf");
            this.comboSearchOrForms.addItem("Okapi BM25");
            this.comboSearchOrForms.addItem("Wacky");
            this.comboSearchOrForms.setSelectedIndex(form.getID());
        }
        this.comboRetrivalType.addActionListener(this);

        // Create combo boxes next to each other
        JPanel combos = new JPanel(new GridLayout(2, 2));
        // add all components to frame
        combos.add(lComboRetrival);
        combos.add(lComboTitel);
        combos.add(comboRetrivalType);
        combos.add(comboSearchOrForms);

        // add components to panel buttons
        buttons.add(bSubmit);
        buttons.add(stem);
        //buttons.add(newDic);
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

                // Put all the calculation into the background thread.
                //GeneratingTask t = new GeneratingTask();
                if (this.comboRetrivalType.getSelectedIndex() == 0) {
                    booleanRetrival();
                } else {
                    rankedRetrival();
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
            /*if (e.getSource() == newDic) {
                int res = JOptionPane.showConfirmDialog(this.frame, "Do you really want to index a new directory?", 
                        "Index new directory", 2, JOptionPane.INFORMATION_MESSAGE, this.img);
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
            }*/
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
                findFile(labels.get(indx).getText(), new File(path));
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

    private void findFile(String name, File file) {
        File[] list = file.listFiles();
        if (list != null) {
            for (File fil : list) {
                if (fil.isDirectory()) {
                    findFile(name, fil);
                } else if (name.equalsIgnoreCase(fil.getName())) {
                    File p = fil.getParentFile();
                    //pathString = p.getAbsolutePath();
                }
            }
        }
    }

    private void booleanRetrival() {
        progressDialog.setVisible(true); // close the dialog
        String query = this.tQuery.getText(); // save the query
        if (query.length() > 0) {
            if (this.comboSearchOrForms.getSelectedIndex() == 0) {
                task = new GeneratingTask(query, dIndex, false, kIndex, sIndex, this);
            } else {
                task = new GeneratingTask(query, dIndex, false, kIndex, sIndex, this);
            }
            Thread t = new Thread(task);
            t.start();
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

    private void rankedRetrival() {
        progressDialog.setVisible(false); // close the dialog
        String query = this.tQuery.getText(); // save the query
        if (query.length() > 0) {
            Subquery s = new Subquery();
            s.addLiteral(query);
            if (dIndex.getFileNames().size() < 10) {
                task = new GeneratingTask(dIndex, kIndex, s, dIndex.getFileNames().size(), this);
            } else {
                task = new GeneratingTask(dIndex, kIndex, s, 10, this);
            }
            Thread t = new Thread(task);
            t.start();
        } else { // there is no query entered - let the user know
            labels = new ArrayList<JLabel>();
            this.num.setVisible(false);
            this.foundDocArea.add(new JLabel("Please enter a term!"));
            // show panel where buttons are in
            this.foundDocArea.setVisible(true);
            // reload the view again by packing the frame
            this.frame.pack();
        }
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

    @Override
    public void actionPerformed(ActionEvent e) {
        if (this.comboRetrivalType.getSelectedIndex() == 0) {
            this.comboRetrivalType.setSelectedIndex(0);
            this.lComboTitel.setText("Choose search type");
            this.comboSearchOrForms.removeAllItems(); // clear combo (important after new directory is processed)
            this.comboSearchOrForms.addItem("Normal search"); // normal search is always available as search type
            if (sIndex.getTermCount() > 0) {
                this.comboSearchOrForms.addItem("Search by author"); // author search only if there is any author saved in sIndex
            }
        } else {
            this.comboRetrivalType.setSelectedIndex(1);
            this.lComboTitel.setText("Choose formular for ranked retrival");
            this.comboSearchOrForms.removeAllItems(); // clear combo
            this.comboSearchOrForms.addItem("Dafault");
            this.comboSearchOrForms.addItem("tf-idf");
            this.comboSearchOrForms.addItem("Okapi BM25");
            this.comboSearchOrForms.addItem("Wacky");
        }
    }

    @Override
    public void notifyThreadFinished() {
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
                boolean num = true;
                for (String s : results) {
                    JLabel l = new JLabel(s);
                    if (l.getText().equals("No document found.")) {
                        this.foundDocArea.add(l);
                        num = false;
                        break;
                    }
                    l.addMouseListener(this);
                    this.labels.add(l);
                    this.foundDocArea.add(l);
                }
                this.number.setText(this.docs);
                this.numberRes.setText(this.labels.size() + "");
                this.num.setVisible(num);
                break;
            default:
                RankedItem[] res = task.getResultsRank();
                /*int max = 0;
                for(String s : dIndex.getFileNames()){
                    if(s.length() > max){
                        max=s.length();
                    }
                }*/
                if (res == null) {
                    JLabel l = new JLabel("No document found!");
                    this.foundDocArea.add(l);
                } else {
                    for (RankedItem item : res) {
                        //int a = max - dIndex.getFileNames().get(item.getDocumentID()).length();
                        String output = String.format("%.2f: %s", item.getA_d(),dIndex.getFileNames().get(item.getDocumentID()));
                        JLabel l = new JLabel(output);
                        l.addMouseListener(this);
                        this.labels.add(l);
                        this.foundDocArea.add(l);
                    }
                    this.number.setText("Ranking for the best 10 documents");
                    this.numberRes.setText("");
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
}
