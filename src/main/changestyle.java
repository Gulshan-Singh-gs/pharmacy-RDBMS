package main;

import java.awt.Color;
import javax.swing.JComponent;

/**
 * changestyle - small visual helper for applying common styles to Swing
 * components. Kept intentionally minimal; the GUI is otherwise built with
 * NetBeans forms.
 *
 * @author Gulshan Singh
 */
public final class changestyle {

    private changestyle() {
        // utility class
    }

    /** Applies a plain white background to a component. */
    public static void setWhiteBackground(JComponent component) {
        component.setBackground(Color.WHITE);
    }

    /** Applies a single background color to all given components. */
    public static void setBackground(JComponent[] components, Color color) {
        for (JComponent component : components) {
            component.setBackground(color);
        }
    }
}
