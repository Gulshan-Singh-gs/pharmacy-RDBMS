package main;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Main - application entry point.
 *
 * <p>Initializes the visual theme (JTattoo AluminiumLookAndFeel with a safe
 * fallback to the cross-platform look and feel) and launches the login screen.
 *
 * @author Gulshan Singh
 */
public class Main {

    /**
     * Application entry point.
     *
     * @param args the command line arguments (unused)
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                initLookAndFeel();
                new Login().setVisible(true);
            }
        });
    }

    /**
     * Applies the preferred look and feel. Falls back gracefully to the
     * cross-platform look and feel if JTattoo is not on the classpath.
     */
    private static void initLookAndFeel() {
        try {
            UIManager.setLookAndFeel("com.jtattoo.plaf.aluminium.AluminiumLookAndFeel");
        } catch (Exception ex) {
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (Exception ignore) {
                // keep the default look and feel
            }
        }
    }
}
