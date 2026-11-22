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
 * Drug_List - views all drugs in the inventory.
 *
 * <p>Shows the drugs master in a read-only table, joined with the supplier
 * company name. Data is reloaded from the database on open and on Refresh.
 *
 * @author Gulshan Singh
 */
public class Drug_List extends JFrame {

    private final DefaultTableModel model;
    private final JTable table;

    /** Creates new form Drug_List */
    public Drug_List() {
        setTitle("Drug Inventory");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setBounds(100, 80, 820, 480);

        String[] cols = {
            "Barcode", "Name", "Type", "Dose", "Cost \u20b9",
            "Sell \u20b9", "Production", "Expiry", "Qty", "Place", "Company"
        };
        model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(model);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        JLabel title = new JLabel("Drug Inventory");
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

    /** Loads all drugs into the table. */
    private void load() {
        model.setRowCount(0);
        String sql = "SELECT d.barcode, d.name, d.type, d.dose, d.cost_price, "
                + "d.selling_price, d.production_date, d.expiration_date, "
                + "d.quantity, d.place, c.name "
                + "FROM drugs d LEFT JOIN company c ON c.company_id = d.company_id "
                + "ORDER BY d.name";
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            Connection conn = Connect.getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4),
                    rs.getBigDecimal(5), rs.getBigDecimal(6),
                    rs.getDate(7), rs.getDate(8),
                    rs.getInt(9), rs.getString(10), rs.getString(11)
                });
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                "Error loading drugs: " + ex.getMessage(),
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
        java.awt.EventQueue.invokeLater(() -> new Drug_List().setVisible(true));
    }
}
