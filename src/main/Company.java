package main;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
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
 * Company - views and adds companies/suppliers.
 *
 * @author Gulshan Singh
 */
public class Company extends JFrame {

    private final DefaultTableModel model;
    private final JTable table;

    /** Creates new form Company */
    public Company() {
        setTitle("Companies / Suppliers");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setBounds(100, 80, 640, 420);

        String[] cols = {"ID", "Name", "Address", "Phone"};
        model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(model);

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        JLabel title = new JLabel("Companies");
        title.setFont(title.getFont().deriveFont(16.0f));
        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(e -> load());
        JButton add = new JButton("Add Company");
        add.addActionListener(e -> addCompany());
        top.add(title);
        top.add(refresh);
        top.add(add);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(top, BorderLayout.NORTH);
        getContentPane().add(new JScrollPane(table), BorderLayout.CENTER);

        load();
    }

    private void load() {
        model.setRowCount(0);
        String sql = "SELECT company_id, name, address, phone FROM company ORDER BY name";
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            Connection conn = Connect.getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4)
                });
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                "Error loading companies: " + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
        }
    }

    private void addCompany() {
        JTextField tfName = new JTextField(18);
        JTextField tfAddress = new JTextField(18);
        JTextField tfPhone = new JTextField(18);
        JPanel p = new JPanel(new GridLayout(0, 2, 6, 6));
        p.add(new JLabel("Name *"));
        p.add(tfName);
        p.add(new JLabel("Address"));
        p.add(tfAddress);
        p.add(new JLabel("Phone"));
        p.add(tfPhone);

        int opt = JOptionPane.showConfirmDialog(this, p, "Add Company",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (opt != JOptionPane.OK_OPTION) {
            return;
        }
        String name = tfName.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Company name is required.",
                "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String sql = "INSERT INTO company (name, address, phone) VALUES (?, ?, ?)";
        PreparedStatement ps = null;
        try {
            Connection conn = Connect.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, name);
            ps.setString(2, nullIfEmpty(tfAddress.getText()));
            ps.setString(3, nullIfEmpty(tfPhone.getText()));
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Company added.", "Success",
                JOptionPane.INFORMATION_MESSAGE);
            load();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                "Could not add company: " + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            closeQuietly(ps);
        }
    }

    private static String nullIfEmpty(String value) {
        String t = value.trim();
        return t.isEmpty() ? null : t;
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
        java.awt.EventQueue.invokeLater(() -> new Company().setVisible(true));
    }
}
