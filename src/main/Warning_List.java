package main;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.WindowConstants;
import javax.swing.table.DefaultTableModel;

/**
 * Warning_List - lists drug warnings (low stock). Reads the
 * {@code v_low_stock} view.
 *
 * @author Gulshan Singh
 */
public class Warning_List extends JFrame {

    private final DefaultTableModel model;
    private final JTable table;

    /** Creates new form Warning_List */
    public Warning_List() {
        setTitle("Low Stock Warnings");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setBounds(100, 80, 620, 420);

        String[] cols = {"Barcode", "Name", "Type", "Qty", "Place"};
        model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(model);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        JLabel title = new JLabel("Warnings (Low Stock)");
        title.setFont(title.getFont().deriveFont(16.0f));
        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(e -> load());
        top.add(title);
        top.add(refresh);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(top, BorderLayout.NORTH);
        getContentPane().add(new JScrollPane(table), BorderLayout.CENTER);

        load();
    }

    private void load() {
        model.setRowCount(0);
        String sql = "SELECT barcode, name, type, quantity, place "
                + "FROM v_low_stock ORDER BY quantity ASC";
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            Connection conn = Connect.getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getString(1), rs.getString(2), rs.getString(3),
                    rs.getInt(4), rs.getString(5)
                });
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                "Error loading warnings: " + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
        }
    }

    private static void closeQuietly(AutoCloseable resource) {
        if (resource != null) {
            try {
                resource.close();
            } catch (Exception ignore) {
                // releasing a closed resource is a non-event
            }
        }
    }

    /** @param args the command line arguments */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(() -> new Warning_List().setVisible(true));
    }
}
