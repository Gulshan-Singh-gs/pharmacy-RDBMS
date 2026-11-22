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
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import javax.swing.table.DefaultTableModel;

/**
 * Search_Drug - searches drugs by name, barcode, or type.
 *
 * <p>Empty query lists all drugs; otherwise it filters with a LIKE match on
 * name, barcode, and type.
 *
 * @author Gulshan Singh
 */
public class Search_Drug extends JFrame {

    private final DefaultTableModel model;
    private final JTable table;
    private final JTextField tfQuery = new JTextField(18);

    /** Creates new form Search_Drug */
    public Search_Drug() {
        setTitle("Search Drug");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setBounds(100, 80, 720, 440);

        String[] cols = {"Barcode", "Name", "Type", "Dose", "Qty", "Place", "Company"};
        model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(model);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        JLabel lbl = new JLabel("Search:");
        JButton btn = new JButton("Search");
        btn.addActionListener(e -> search());
        tfQuery.addActionListener(e -> search());
        top.add(lbl);
        top.add(tfQuery);
        top.add(btn);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(top, BorderLayout.NORTH);
        getContentPane().add(new JScrollPane(table), BorderLayout.CENTER);

        search();
    }

    private void search() {
        model.setRowCount(0);
        String q = tfQuery.getText().trim();
        String sql;
        String like = null;
        if (q.isEmpty()) {
            sql = "SELECT d.barcode, d.name, d.type, d.dose, d.quantity, d.place, c.name "
                    + "FROM drugs d LEFT JOIN company c ON c.company_id = d.company_id "
                    + "ORDER BY d.name";
        } else {
            sql = "SELECT d.barcode, d.name, d.type, d.dose, d.quantity, d.place, c.name "
                    + "FROM drugs d LEFT JOIN company c ON c.company_id = d.company_id "
                    + "WHERE d.name LIKE ? OR d.barcode LIKE ? OR d.type LIKE ? "
                    + "ORDER BY d.name";
            like = "%" + q + "%";
        }
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            Connection conn = Connect.getConnection();
            ps = conn.prepareStatement(sql);
            if (like != null) {
                ps.setString(1, like);
                ps.setString(2, like);
                ps.setString(3, like);
            }
            rs = ps.executeQuery();
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4),
                    rs.getInt(5), rs.getString(6), rs.getString(7)
                });
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                "Error searching drugs: " + ex.getMessage(),
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
        java.awt.EventQueue.invokeLater(() -> new Search_Drug().setVisible(true));
    }
}
