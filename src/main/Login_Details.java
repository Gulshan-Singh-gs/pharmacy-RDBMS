package main;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

/**
 * Login_Details - shows the login audit trail from the login_log table.
 *
 * @author Gulshan Singh
 */
public class Login_Details extends javax.swing.JFrame {

    private static final String[] COLUMNS = {"Name", "Type", "Date", "Time"};

    private DefaultTableModel model;
    private JTable jTable;

    /** Creates new form Login_Details */
    public Login_Details() {
        initComponents();
        loadLog();
    }

    private void initComponents() {
        setTitle("Login Details");
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setBounds(150, 100, 620, 460);

        model = new DefaultTableModel(COLUMNS, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        jTable = new JTable(model);

        JButton reload = new JButton("Reload");
        reload.addActionListener(e -> loadLog());
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(reload);

        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(new EmptyBorder(8, 8, 8, 8));
        root.add(top, BorderLayout.NORTH);
        root.add(new JScrollPane(jTable), BorderLayout.CENTER);
        setContentPane(root);
        pack();
    }

    private void loadLog() {
        model.setRowCount(0);
        Connection conn;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = Connect.getConnection();
            ps = conn.prepareStatement(
                "SELECT name, type, login_date, login_time FROM login_log ORDER BY login_id DESC");
            rs = ps.executeQuery();
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getString("name"),
                    rs.getString("type"),
                    rs.getDate("login_date"),
                    rs.getTime("login_time")
                });
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                "Could not load login history: " + ex.getMessage(),
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
                // ignore
            }
        }
    }
}
