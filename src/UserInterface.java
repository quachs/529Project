
import Helper.Formulars;
import Indexes.IndexingGUI;
import Retrivals.RetrievalGUI;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

/**
 * UserInterface Implements MouseListener for user input handling
 *
 * @author Sandra
 */
public class UserInterface extends JFrame {

    private ImageIcon img = new ImageIcon(System.getProperty("user.dir") + "/icon.png"); // logo icon
    public UserInterface() {
        // Change look and feel of swing components
        // resource: http://docs.oracle.com/javase/tutorial/uiswing/lookandfeel/nimbus.html
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException | IllegalAccessException
                | InstantiationException | UnsupportedLookAndFeelException e) {
            System.err.println(e.toString());
        }
        Object[] optionsBeginning = {"Indexing a corpus",
            "Process Queries"};
        int resultBeginning = JOptionPane.showOptionDialog(this,
                "What do you want to do?",
                "Search Engine Start",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE,
                img,
                optionsBeginning,
                optionsBeginning[1]);
        if (resultBeginning == 0) {
            new IndexingGUI();
        } else //Custom button text
        {
            Object[] optionsRetrival = {"Boolean Retrival",
                "Ranked Retrieval"};
            int resultRetrival = JOptionPane.showOptionDialog(this,
                    "Please choose retrival type",
                    "Retrival Type",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE,
                    img,
                    optionsRetrival,
                    optionsRetrival[1]);
            if (resultRetrival == 0) {
                new RetrievalGUI('b', null);
            } else {
                Object[] optionsForm = {"Default",
                "tf-idf","Okapi BM25","Wacky"};
                int resultFormular = JOptionPane.showOptionDialog(this,
                    "Please choose the type of formular",
                    "Formular",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE,
                    img,
                    optionsForm,
                    optionsForm[1]);
                new RetrievalGUI('r', Formulars.getFormByID(resultFormular));
            }
        }
    }
}
