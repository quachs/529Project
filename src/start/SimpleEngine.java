package start;

import start.UserInterface;
import java.io.*;
import javax.swing.SwingUtilities;

/**
 * A very simple engine to launch the user interface.
 * 
 */
public class SimpleEngine {

    public static void main(String[] args) throws IOException {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                UserInterface a = new UserInterface();
            }
        });
        
    }
}
