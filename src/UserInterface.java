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

/**
 * UserInterface Implements MouseListener for user input handling
 *
 * @author Sandra
 */
public class UserInterface implements MouseListener {

    // The frame of the search engine
    JFrame frame;
    // after the path is chosen it is saved here for calling the indexing method.
    private Path path;
    private String pathString;
    // Task where you can find positionalindex
    private Indexing indexedCorpus = new Indexing();
    GeneratingLabels gen;

    // initialize a new dialog, this is the frame and "Indexing..." is the title
    JDialog progressDialog = new JDialog(this.frame, "Please wait...");

    // indecis
    private PositionalInvertedIndex index;
    private KGramIndex kIndex;
    private SoundexIndex sIndex;

    // Parser
    private QueryParser_KQV parser;

    // Strings for easily changing the text of the label number.
    private final String docs = "Number of found Documents: ";
    private final String voc = "Number of Vocabulary found in corpus: ";

    // UI elements
    private JPanel foundDocArea = new JPanel();
    private JTextField tQuery = new JTextField();
    private JButton bSubmit = new JButton("Submit");
    private JPanel num = new JPanel(new FlowLayout());
    private JLabel number = new JLabel("");
    private JLabel numberRes = new JLabel();
    private JButton stem = new JButton("Stem");
    private JButton newDic = new JButton("Index new directory");
    private JButton all = new JButton("Print vocabulary");
    private JComboBox combo = new JComboBox();
    // List of results that are shown in the foundDocArea
    private List<JLabel> labels;

    // Constructor
    public UserInterface() throws IOException {
        this.labels = new ArrayList<>();
        this.index = indexedCorpus.getIndex();
        this.frame = new JFrame();
        path = Paths.get("C");
        createProgressBaer();
        // Let the user choose his directory
        chooseDirectory();
    }

    /**
     * this method opens a file chooser dialog that the user can choose the
     * directory where his corpus is saved
     */
    private void chooseDirectory() throws IOException {
        // initialize file chooser
        JFileChooser chooser = new JFileChooser();
        // for easier testing I chose the start directory where my created json files from homework 2 are saved
        chooser.setCurrentDirectory(new java.io.File(path.toString()));
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
            indexing();
        } else {
            // if any other key is pressed, the window closes
        }
    }

    /**
     * While Indexing processes show a progress bar
     *
     * @throws java.io.IOException
     */
    public void indexing() throws IOException {

        // show the dialog
        progressDialog.setVisible(true);
        // start the task idexing -> it is king of a thread
        indexedCorpus = new Indexing(path);
        // start the task  
        //task.doInBackground();
        indexedCorpus.execute();
        // wait till the task is finished -> set true when done() is finished
        while (!indexedCorpus.isDone()) {
        }
        this.index = indexedCorpus.getIndex();
        // close the dialog
        progressDialog.setVisible(false);
        // save created indecis
        saveIndecies();
        // creat the view
        createUI();
    }

    public void createProgressBaer() {
        // create a new panel
        JPanel contentPane = new JPanel();
        // set preferred size
        contentPane.setPreferredSize(new Dimension(300, 100));
        // initialize progress bar and add it to the panel
        JProgressBar bar = new JProgressBar(SwingConstants.HORIZONTAL);
        bar.setIndeterminate(true);
        contentPane.add(bar);
        // add panel to the dialog
        progressDialog.setContentPane(contentPane);
        // with pack() you minimalize the size of the dialog
        progressDialog.pack();
        // sets the location to the center of the screen
        progressDialog.setLocationRelativeTo(null);
        progressDialog.setVisible(false);
    }

    private void saveIndecies() {
        kIndex = indexedCorpus.getKgIndex();
        //parser = new QueryParser_KQV(index, kIndex);
        sIndex = indexedCorpus.getsIndex();
        index = indexedCorpus.getIndex();
    }

    // After indexing create the UI
    private void createUI() {
        // create all Components
        this.frame.setLayout(new BoxLayout(frame.getContentPane(), BoxLayout.Y_AXIS));
        JLabel lQuery = new JLabel("Enter the Query");
        JPanel buttons = new JPanel(new FlowLayout());
        foundDocArea = new JPanel(new GridLayout(0, 1));

        // Modify components
        this.combo.removeAllItems();
        this.combo.addItem("Normal search");
        if (sIndex.getTermCount() > 0) {
            this.combo.addItem("Search by author");
        }

        // add components to subpanel
        buttons.add(bSubmit);
        buttons.add(stem);
        buttons.add(newDic);
        buttons.add(all);

        // add all components to frame
        this.frame.add(combo);
        this.frame.add(lQuery);
        this.frame.add(tQuery);
        this.frame.add(buttons);
        this.frame.add(foundDocArea);

        // add mouseListener
        bSubmit.addMouseListener(this);
        stem.addMouseListener(this);
        newDic.addMouseListener(this);
        all.addMouseListener(this);

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
        this.frame.pack();
        this.frame.setLocationRelativeTo(null);
        this.frame.setVisible(true);
    }

    private void checkResults(List foundDocs) {
        if (foundDocs != null && foundDocs.size() > 0) {
            // .. yes than go through them and add them to the label list
            generatingLabels(null, (ArrayList<Integer>) foundDocs);
        } else {
            // .. no print that there are no documents
            this.foundDocArea.add(new JLabel("No document found."));
        }
    }

    private void generatingLabels(String[] docsArray, ArrayList<Integer> docIDList) {
        if (docIDList == null) {
            this.gen = new GeneratingLabels(docsArray);
        } else {
            this.gen = new GeneratingLabels(docIDList, (ArrayList<String>) indexedCorpus.getFileNames());
        }
        // show the dialog
        progressDialog.setVisible(true);
        // start the task  
        gen.execute();
        // wait till the task is finished -> set true when done() is finished
        while (!gen.isDone()) {
        }
        this.labels = gen.getArray();
        for (JLabel a : this.labels) {
            this.foundDocArea.add(a);
        }
        this.frame.repaint();
        this.frame.pack();
        // close the dialog
        progressDialog.setVisible(false);
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
                num.setVisible(false);
                parser = new QueryParser_KQV(index, kIndex);
                // save the query
                String query = this.tQuery.getText();
                // remove all existing elements in the panel 
                // if we don´t do this and submit the query twice we would get the result twice too
                this.foundDocArea.removeAll();
                this.foundDocArea.repaint();
                if (query.length() > 0) {
                    // create a list to save found documents
                    List<Integer> foundDocs;
                    // initialize list of labels new - important for more than one submit action
                    this.labels = new ArrayList<>();
                    // check if combobox is selected for normal search
                    if ("Normal search".equals(combo.getSelectedItem().toString())) {
                        // .. yes than parse the query and save the resilt IDs
                        foundDocs = parser.getDocumentList(query);
                        // check if there are any results
                        checkResults(foundDocs);
                    } else { // .. not normal search == author search
                        // save DocIds for author search
                        foundDocs = QueryProcessor.authorQuery(query, sIndex);
                        // check if there are any results                    
                        checkResults(foundDocs);
                    }
                    // set text for found documents
                    this.number.setText(this.docs);
                    // save size of documents
                    this.numberRes.setText(labels.size() + "");
                    // make num panel visible
                    this.num.setVisible(true);
                    // add a listener for mouseclicks for every single button saved in the list 
                    for (JLabel b : labels) {
                        b.addMouseListener(this);
                    }
                } else {
                    labels = new ArrayList<JLabel>();
                    this.num.setVisible(false);
                    this.foundDocArea.add(new JLabel("Please enter a term!"));
                }
                // show panel where buttons are in
                this.foundDocArea.setVisible(true);
                // reload the view again by packing the frame              
                this.frame.pack();
            }
            if (e.getSource() == stem) {
                // Stem the word that is input in the textfield
                this.foundDocArea.removeAll();
                this.foundDocArea.setVisible(false);
                this.num.setVisible(false);
                this.frame.pack();
                // Save the result of stemming, call Simple Token Stream
                String result = PorterStemmer.getStem(this.tQuery.getText());
                // Aufruf der statischen Methode showMessageDialog()
                JOptionPane.showMessageDialog(this.frame, "Stemmed \"" + this.tQuery.getText() + "\" : " + result, "Result of stemming", JOptionPane.INFORMATION_MESSAGE);
            }
            if (e.getSource() == newDic) {
                String res = JOptionPane.showInputDialog("Choosing index directory will start at the following path:", this.tQuery.getText());
                if (res != null) {
                    if (res.length() > 0) {
                        String directory = res;
                        path = Paths.get(directory);
                    }
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
                    try {
                        chooseDirectory();
                    } catch (IOException ex) {
                        Logger.getLogger(UserInterface.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
            if (e.getSource() == all) {
                this.foundDocArea.removeAll();
                this.labels = new ArrayList<>();
                this.num.setVisible(true);
                generatingLabels(index.getDictionary(), null);
                JTextArea label = new JTextArea();
                label.setEditable(false);
                label.setCaretPosition(0);
                String res = gen.getRes();
                label.setText(res);
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
public void findFile(String name,File file)
    {
        File[] list = file.listFiles();
        if(list!=null)
        for (File fil : list)
        {
            if (fil.isDirectory())
            {
                findFile(name,fil);
            }
            else if (name.equalsIgnoreCase(fil.getName()))
            {
                File p = fil.getParentFile();
                pathString = p.getAbsolutePath();
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
