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
 * All_Deal - displays all sales deals/transactions.
 *
 * <p>Reads the {@code history_sales} view, one row per sold line item.
 *
 * @author Gulshan Singh
 */
public class All_Deal extends JFrame {

    private final DefaultTableModel model;
    private final JTable table;

    /** Creates new form All_Deal */
    public All_Deal() {
        setTitle("All Deals / Sales");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setBounds(100, 80, 760, 460);

        String[] cols = {
            "Cashier", "Barcode", "Name", "Type", "Dose", "Qty",
            "Price \u20b9", "Amount \u20b9", "Date", "Time"
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
        JLabel title = new JLabel("All Deals");
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
        String sql = "SELECT user_name, barcode, name, type, dose, quantity, price, "
                + "amount, date, time FROM history_sales "
                + "ORDER BY date DESC, time DESC";
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            Connection conn = Connect.getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4),
                    rs.getString(5), rs.getInt(6), rs.getBigDecimal(7),
                    rs.getBigDecimal(8), rs.getDate(9), rs.getTime(10)
                });
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                "Error loading deals: " + ex.getMessage(),
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
        java.awt.EventQueue.invokeLater(() -> new All_Deal().setVisible(true));
    }
}
