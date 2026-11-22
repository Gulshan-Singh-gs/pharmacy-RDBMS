package main;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;

/**
 * User - admin user management: list, add, update, delete users and set
 * passwords. Uses salted SHA-256 hashes (never stores plaintext passwords).
 *
 * @author Gulshan Singh
 */
public class User extends javax.swing.JFrame {

    private static final String[] COLUMNS = {"ID", "Name", "DOB", "Address", "Phone", "Salary"};

    private JTextField txtName;
    private JTextField txtDOB;
    private JTextField txtAddress;
    private JTextField txtPhone;
    private JTextField txtSalary;
    private JPasswordField txtPassword;
    private JTable jTable;
    private DefaultTableModel model;

    public User() {
        initComponents();
        loadUsers();
    }

    private void initComponents() {
        setTitle("User Administration");
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setBounds(120, 90, 780, 560);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(new TitledBorder("User Details"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.anchor = GridBagConstraints.WEST;

        txtName = new JTextField(20);
        txtDOB = new JTextField(20);
        txtAddress = new JTextField(20);
        txtPhone = new JTextField(20);
        txtSalary = new JTextField(20);
        txtPassword = new JPasswordField(20);

        addRow(form, gbc, 0, "Name *", txtName);
        addRow(form, gbc, 1, "DOB (yyyy-MM-dd)", txtDOB);
        addRow(form, gbc, 2, "Address", txtAddress);
        addRow(form, gbc, 3, "Phone", txtPhone);
        addRow(form, gbc, 4, "Salary", txtSalary);
        addRow(form, gbc, 5, "Password (blank = default)", txtPassword);

        JButton btnLoad = new JButton("Reload");
        JButton btnAdd = new JButton("Add");
        JButton btnUpdate = new JButton("Update");
        JButton btnDelete = new JButton("Delete");
        JButton btnClear = new JButton("Clear");

        btnLoad.addActionListener(e -> loadUsers());
        btnAdd.addActionListener(e -> addUser());
        btnUpdate.addActionListener(e -> updateUser());
        btnDelete.addActionListener(e -> deleteUser());
        btnClear.addActionListener(e -> clearForm());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 6));
        buttons.add(btnLoad);
        buttons.add(btnAdd);
        buttons.add(btnUpdate);
        buttons.add(btnDelete);
        buttons.add(btnClear);

        model = new DefaultTableModel(COLUMNS, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        jTable = new JTable(model);
        jTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        jTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                populateFromSelection();
            }
        });

        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.add(form, BorderLayout.NORTH);
        root.add(new JScrollPane(jTable), BorderLayout.CENTER);
        root.add(buttons, BorderLayout.SOUTH);
        setContentPane(root);
        pack();
    }

    private void addRow(JPanel panel, GridBagConstraints gbc, int row,
                        String label, JTextField field) {
        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel(label), gbc);
        gbc.gridx = 1;
        panel.add(field, gbc);
    }

    private void loadUsers() {
        model.setRowCount(0);
        Connection conn;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = Connect.getConnection();
            ps = conn.prepareStatement(
                "SELECT user_id, name, dob, address, phone, salary FROM users ORDER BY user_id");
            rs = ps.executeQuery();
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt("user_id"),
                    rs.getString("name"),
                    rs.getDate("dob"),
                    rs.getString("address"),
                    rs.getString("phone"),
                    rs.getBigDecimal("salary")
                });
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Could not load users: " + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
        }
    }

    private void populateFromSelection() {
        int row = jTable.getSelectedRow();
        if (row < 0) {
            return;
        }
        txtName.setText(stringAt(row, 1));
        txtDOB.setText(objAt(row, 2) == null ? "" : objAt(row, 2).toString());
        txtAddress.setText(stringAt(row, 3));
        txtPhone.setText(stringAt(row, 4));
        txtSalary.setText(objAt(row, 5) == null ? "" : objAt(row, 5).toString());
        txtPassword.setText("");
    }

    private void clearForm() {
        txtName.setText("");
        txtDOB.setText("");
        txtAddress.setText("");
        txtPhone.setText("");
        txtSalary.setText("");
        txtPassword.setText("");
        jTable.clearSelection();
    }

    private void addUser() {
        String name = txtName.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Name is required.", "Add User",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        Date dob = parseDate(txtDOB.getText().trim());
        BigDecimal salary = parseSalary(txtSalary.getText().trim());
        if (salary == null) {
            return;
        }

        Connection conn;
        PreparedStatement ps = null;
        try {
            conn = Connect.getConnection();
            ps = conn.prepareStatement("SELECT user_id FROM users WHERE name = ?");
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    JOptionPane.showMessageDialog(this, "A user with this name already exists.",
                        "Add User", JOptionPane.WARNING_MESSAGE);
                    return;
                }
            }
            closeQuietly(ps);

            String password = new String(txtPassword.getPassword());
            boolean defaultPwd = password.isEmpty();
            if (defaultPwd) {
                password = "user123";
            }
            String salt = PasswordUtil.randomSaltHex();
            String hash = PasswordUtil.hash(salt, password);
            int mustChange = defaultPwd ? 1 : 0;

            ps = conn.prepareStatement(
                "INSERT INTO users (name, dob, address, phone, salary, password_hash, salt, must_change_password) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
            ps.setString(1, name);
            ps.setDate(2, dob);
            ps.setString(3, txtAddress.getText().trim());
            ps.setString(4, txtPhone.getText().trim());
            ps.setBigDecimal(5, salary);
            ps.setString(6, hash);
            ps.setString(7, salt);
            ps.setInt(8, mustChange);
            ps.executeUpdate();

            JOptionPane.showMessageDialog(this,
                defaultPwd ? "User added. Default password: user123" : "User added.",
                "Add User", JOptionPane.INFORMATION_MESSAGE);
            loadUsers();
            clearForm();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Could not add user: " + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            closeQuietly(ps);
        }
    }

    private void updateUser() {
        int row = jTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a user to update.", "Update User",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        int userId = Integer.parseInt(objAt(row, 0).toString());
        String name = txtName.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Name is required.", "Update User",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        Date dob = parseDate(txtDOB.getText().trim());
        BigDecimal salary = parseSalary(txtSalary.getText().trim());
        if (salary == null) {
            return;
        }

        Connection conn;
        PreparedStatement ps = null;
        try {
            conn = Connect.getConnection();
            String password = new String(txtPassword.getPassword());
            if (password.isEmpty()) {
                ps = conn.prepareStatement(
                    "UPDATE users SET name = ?, dob = ?, address = ?, phone = ?, salary = ? "
                    + "WHERE user_id = ?");
                ps.setString(1, name);
                ps.setDate(2, dob);
                ps.setString(3, txtAddress.getText().trim());
                ps.setString(4, txtPhone.getText().trim());
                ps.setBigDecimal(5, salary);
                ps.setInt(6, userId);
            } else {
                String salt = PasswordUtil.randomSaltHex();
                String hash = PasswordUtil.hash(salt, password);
                ps = conn.prepareStatement(
                    "UPDATE users SET name = ?, dob = ?, address = ?, phone = ?, salary = ?, "
                    + "password_hash = ?, salt = ?, must_change_password = 0 WHERE user_id = ?");
                ps.setString(1, name);
                ps.setDate(2, dob);
                ps.setString(3, txtAddress.getText().trim());
                ps.setString(4, txtPhone.getText().trim());
                ps.setBigDecimal(5, salary);
                ps.setString(6, hash);
                ps.setString(7, salt);
                ps.setInt(8, userId);
            }
            ps.executeUpdate();

            JOptionPane.showMessageDialog(this, "User updated.", "Update User",
                JOptionPane.INFORMATION_MESSAGE);
            loadUsers();
            clearForm();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Could not update user: " + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            closeQuietly(ps);
        }
    }

    private void deleteUser() {
        int row = jTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a user to delete.", "Delete User",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        int userId = Integer.parseInt(objAt(row, 0).toString());
        String name = stringAt(row, 1);
        int choice = JOptionPane.showConfirmDialog(this,
            "Delete user \"" + name + "\"?", "Delete User",
            JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        Connection conn;
        PreparedStatement ps = null;
        try {
            conn = Connect.getConnection();
            ps = conn.prepareStatement("DELETE FROM users WHERE user_id = ?");
            ps.setInt(1, userId);
            ps.executeUpdate();
            loadUsers();
            clearForm();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                "Could not delete user (may be referenced by records): " + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            closeQuietly(ps);
        }
    }

    private static Date parseDate(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        try {
            return Date.valueOf(text);
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(null,
                "Invalid date \"" + text + "\". Use yyyy-MM-dd.", "Input error",
                JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }

    private static BigDecimal parseSalary(String text) {
        if (text == null || text.isEmpty()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(text);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(null,
                "Invalid salary \"" + text + "\".", "Input error",
                JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }

    private Object objAt(int row, int col) {
        return model.getValueAt(row, col);
    }

    private String stringAt(int row, int col) {
        Object v = model.getValueAt(row, col);
        return v == null ? "" : v.toString();
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
