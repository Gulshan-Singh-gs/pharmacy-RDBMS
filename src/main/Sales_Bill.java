package main;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComboBox;
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
 * Sales_Bill - creates a sales bill.
 *
 * <p>Pick a drug and a quantity, add lines to the bill, then Complete Sale: a
 * {@code sales} header and {@code sale_items} lines are inserted and stock is
 * decremented inside one transaction.
 *
 * @author Gulshan Singh
 */
public class Sales_Bill extends JFrame {

    private static class BillLine {
        final String barcode;
        final String name;
        final String type;
        final String dose;
        final int qty;
        final BigDecimal price;
        final BigDecimal amount;

        BillLine(String b, String n, String t, String d, int q, BigDecimal p) {
            barcode = b;
            name = n;
            type = t;
            dose = d;
            qty = q;
            price = p;
            amount = p.multiply(BigDecimal.valueOf(q));
        }
    }

    private final List<BillLine> bill = new ArrayList<>();
    private final DefaultTableModel billModel;
    private final JTable billTable;
    private final JLabel totalLabel = new JLabel("Total: \u20b9 0.00");

    private final JComboBox<String> cbDrug = new JComboBox<>();
    private final JTextField tfQty = new JTextField(6);

    // data aligned with combo indices
    private final List<String> barcodes = new ArrayList<>();
    private final List<String> names = new ArrayList<>();
    private final List<String> types = new ArrayList<>();
    private final List<String> doses = new ArrayList<>();
    private final List<BigDecimal> prices = new ArrayList<>();
    private final List<Integer> available = new ArrayList<>();

    /** Creates new form Sales_Bill */
    public Sales_Bill() {
        setTitle("Sales Bill");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setBounds(100, 80, 780, 520);

        billModel = new DefaultTableModel(new String[]{
            "Barcode", "Name", "Type", "Dose", "Qty", "Price \u20b9", "Amount \u20b9"
        }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        billTable = new JTable(billModel);
        billTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        JPanel picker = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        picker.add(new JLabel("Drug:"));
        cbDrug.setPreferredSize(new java.awt.Dimension(380, 26));
        picker.add(cbDrug);
        picker.add(new JLabel("Qty:"));
        picker.add(tfQty);
        JButton addBtn = new JButton("Add to Bill");
        addBtn.addActionListener(e -> addLine());
        picker.add(addBtn);
        JButton removeBtn = new JButton("Remove Selected");
        removeBtn.addActionListener(e -> removeSelected());
        picker.add(removeBtn);

        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 6));
        south.add(totalLabel);
        JButton complete = new JButton("Complete Sale");
        complete.addActionListener(e -> completeSale());
        south.add(complete);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(picker, BorderLayout.NORTH);
        getContentPane().add(new JScrollPane(billTable), BorderLayout.CENTER);
        getContentPane().add(south, BorderLayout.SOUTH);

        loadDrugs();
    }

    private void loadDrugs() {
        barcodes.clear();
        names.clear();
        types.clear();
        doses.clear();
        prices.clear();
        available.clear();
        cbDrug.removeAllItems();
        String sql = "SELECT barcode, name, type, dose, selling_price, quantity "
                + "FROM drugs WHERE quantity > 0 ORDER BY name";
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            Connection conn = Connect.getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();
            while (rs.next()) {
                barcodes.add(rs.getString(1));
                names.add(rs.getString(2));
                types.add(rs.getString(3));
                doses.add(rs.getString(4));
                prices.add(rs.getBigDecimal(5));
                available.add(rs.getInt(6));
                cbDrug.addItem(rs.getString(2) + " (" + rs.getString(1)
                    + ") - \u20b9" + rs.getBigDecimal(5));
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                "Could not load drugs: " + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
        }
    }

    private void rebuildBillTable() {
        billModel.setRowCount(0);
        BigDecimal total = BigDecimal.ZERO;
        for (BillLine line : bill) {
            billModel.addRow(new Object[]{
                line.barcode, line.name, line.type, line.dose,
                line.qty, line.price, line.amount
            });
            total = total.add(line.amount);
        }
        totalLabel.setText("Total: \u20b9 " + total.toPlainString());
    }

    private void addLine() {
        int idx = cbDrug.getSelectedIndex();
        if (idx < 0) {
            JOptionPane.showMessageDialog(this, "Select a drug first.",
                "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int qty;
        try {
            qty = Integer.parseInt(tfQty.getText().trim());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Quantity must be a number.",
                "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (qty <= 0) {
            JOptionPane.showMessageDialog(this, "Quantity must be positive.",
                "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int already = 0;
        for (BillLine line : bill) {
            if (line.barcode.equals(barcodes.get(idx))) {
                already += line.qty;
            }
        }
        if (already + qty > available.get(idx)) {
            JOptionPane.showMessageDialog(this,
                "Not enough stock. Available: " + available.get(idx),
                "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        bill.add(new BillLine(barcodes.get(idx), names.get(idx), types.get(idx),
            doses.get(idx), qty, prices.get(idx)));
        rebuildBillTable();
        tfQty.setText("");
    }

    private void removeSelected() {
        int[] rows = billTable.getSelectedRows();
        List<Integer> doomed = new ArrayList<>();
        for (int row : rows) {
            doomed.add(billTable.convertRowIndexToModel(row));
        }
        doomed.sort((a, b) -> b - a);
        for (int i : doomed) {
            bill.remove(i);
        }
        rebuildBillTable();
    }

    private void completeSale() {
        if (bill.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Bill is empty.",
                "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int uid = Session.userId();
        if (uid <= 0) {
            JOptionPane.showMessageDialog(this, "Not logged in.",
                "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        BigDecimal total = BigDecimal.ZERO;
        for (BillLine line : bill) {
            total = total.add(line.amount);
        }
        Connection conn = null;
        try {
            conn = Connect.getConnection();
            conn.setAutoCommit(false);

            PreparedStatement psHeader = conn.prepareStatement(
                "INSERT INTO sales (user_id, sale_date, sale_time, total_amount) "
                + "VALUES (?, CURDATE(), CURTIME(), ?)",
                Statement.RETURN_GENERATED_KEYS);
            psHeader.setInt(1, uid);
            psHeader.setBigDecimal(2, total);
            psHeader.executeUpdate();
            ResultSet keys = psHeader.getGeneratedKeys();
            int saleId = -1;
            if (keys.next()) {
                saleId = keys.getInt(1);
            }
            keys.close();
            psHeader.close();

            for (BillLine line : bill) {
                PreparedStatement psItem = conn.prepareStatement(
                    "INSERT INTO sale_items (sale_id, barcode, name, type, dose, "
                    + "quantity, price, amount) VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
                psItem.setInt(1, saleId);
                psItem.setString(2, line.barcode);
                psItem.setString(3, line.name);
                psItem.setString(4, line.type);
                psItem.setString(5, line.dose);
                psItem.setInt(6, line.qty);
                psItem.setBigDecimal(7, line.price);
                psItem.setBigDecimal(8, line.amount);
                psItem.executeUpdate();
                psItem.close();

                PreparedStatement psStock = conn.prepareStatement(
                    "UPDATE drugs SET quantity = quantity - ? "
                    + "WHERE barcode = ? AND quantity >= ?");
                psStock.setInt(1, line.qty);
                psStock.setString(2, line.barcode);
                psStock.setInt(3, line.qty);
                int affected = psStock.executeUpdate();
                psStock.close();
                if (affected == 0) {
                    throw new SQLException("Insufficient stock for " + line.name);
                }
            }

            conn.commit();
            JOptionPane.showMessageDialog(this,
                "Sale completed. Total: \u20b9 " + total.toPlainString(),
                "Success", JOptionPane.INFORMATION_MESSAGE);
            bill.clear();
            rebuildBillTable();
            loadDrugs();
        } catch (Exception ex) {
            try {
                if (conn != null) {
                    conn.rollback();
                }
            } catch (Exception ignore) {
            }
            JOptionPane.showMessageDialog(this,
                "Sale failed: " + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            try {
                if (conn != null) {
                    conn.setAutoCommit(true);
                }
            } catch (Exception ignore) {
            }
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
        java.awt.EventQueue.invokeLater(() -> new Sales_Bill().setVisible(true));
    }
}
