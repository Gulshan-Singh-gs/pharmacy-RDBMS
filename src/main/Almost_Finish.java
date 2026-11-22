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
 * Almost_Finish - lists drugs nearing expiry (within the next 90 days).
 *
 * <p>Reads the {@code v_almost_expire} view.
 *
 * @author Gulshan Singh
 */
public class Almost_Finish extends JFrame {

    private final DefaultTableModel model;
    private final JTable table;

    /** Creates new form Almost_Finish */
    public Almost_Finish() {
        setTitle("Almost Expired (90 days)");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setBounds(100, 80, 640, 420);

        String[] cols = {"Product", "Code", "Expiry Date", "Qty Remaining"};
        model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(model);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        JLabel title = new JLabel("Almost Expired");
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
        String sql = "SELECT product_name, product_code, date_of_expiry, quantity_remain "
                + "FROM v_almost_expire";
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            Connection conn = Connect.getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getString(1), rs.getString(2), rs.getDate(3), rs.getInt(4)
                });
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                "Error loading almost-expired drugs: " + ex.getMessage(),
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
        java.awt.EventQueue.invokeLater(() -> new Almost_Finish().setVisible(true));
    }
}
