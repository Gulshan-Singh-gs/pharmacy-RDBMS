package main;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;

/**
 * Buy_Drug - add a new drug to the inventory.
 *
 * <p>Captures the drugs master fields (see the {@code drugs} table) and
 * inserts a new row. Dates are entered as {@code yyyy-MM-dd}; cost/selling
 * price and quantity are validated before insert. The supplier company is
 * chosen from the existing companies.
 *
 * @author Gulshan Singh
 */
public class Buy_Drug extends JFrame {

    private final JTextField tfBarcode = new JTextField(15);
    private final JTextField tfName = new JTextField(15);
    private final JTextField tfType = new JTextField(15);
    private final JTextField tfDose = new JTextField(15);
    private final JTextField tfCode = new JTextField(15);
    private final JTextField tfCost = new JTextField(15);
    private final JTextField tfSell = new JTextField(15);
    private final JTextField tfProduction = new JTextField(15);
    private final JTextField tfExpiry = new JTextField(15);
    private final JTextField tfPlace = new JTextField(15);
    private final JTextField tfQty = new JTextField(15);
    private final JComboBox<String> cbCompany = new JComboBox<>();
    private final List<Integer> companyIds = new ArrayList<>();

    /** Creates new form Buy_Drug */
    public Buy_Drug() {
        setTitle("Add New Drug");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setBounds(120, 90, 520, 560);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(new EmptyBorder(12, 18, 12, 18));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 6, 5, 6);
        gbc.anchor = GridBagConstraints.WEST;

        int row = 0;
        addField(form, gbc, row++, "Barcode *", tfBarcode);
        addField(form, gbc, row++, "Name *", tfName);
        addField(form, gbc, row++, "Type", tfType);
        addField(form, gbc, row++, "Dose", tfDose);
        addField(form, gbc, row++, "Code", tfCode);
        addField(form, gbc, row++, "Cost price \u20b9", tfCost);
        addField(form, gbc, row++, "Selling price \u20b9", tfSell);
        addField(form, gbc, row++, "Production date (yyyy-MM-dd)", tfProduction);
        addField(form, gbc, row++, "Expiry date (yyyy-MM-dd)", tfExpiry);
        addField(form, gbc, row++, "Place", tfPlace);
        addField(form, gbc, row++, "Quantity", tfQty);

        // company row
        gbc.gridx = 0;
        gbc.gridy = row;
        form.add(new JLabel("Company"), gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        form.add(cbCompany, gbc);
        gbc.gridwidth = 1;
        row++;

        JButton save = new JButton("Add Drug");
        save.addActionListener(e -> save());
        gbc.gridx = 1;
        gbc.gridy = row;
        form.add(save, gbc);

        getContentPane().add(form);
        pack();

        loadCompanies();
    }

    private void addField(JPanel form, GridBagConstraints gbc, int row,
                          String label, JTextField field) {
        gbc.gridx = 0;
        gbc.gridy = row;
        form.add(new JLabel(label), gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        form.add(field, gbc);
        gbc.gridwidth = 1;
    }

    /** Loads companies into the combo box, preserving their ids. */
    private void loadCompanies() {
        companyIds.clear();
        cbCompany.removeAllItems();
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            Connection conn = Connect.getConnection();
            ps = conn.prepareStatement("SELECT company_id, name FROM company ORDER BY name");
            rs = ps.executeQuery();
            while (rs.next()) {
                companyIds.add(rs.getInt(1));
                cbCompany.addItem(rs.getString(2));
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                "Could not load companies: " + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
        }
    }

    /** Validates the fields and inserts a new drug. */
    private void save() {
        String barcode = tfBarcode.getText().trim();
        String name = tfName.getText().trim();
        if (barcode.isEmpty() || name.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Barcode and Name are required.", "Validation",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        BigDecimal cost;
        BigDecimal sell;
        int qty;
        try {
            cost = new BigDecimal(tfCost.getText().trim());
            sell = new BigDecimal(tfSell.getText().trim());
            qty = Integer.parseInt(tfQty.getText().trim());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this,
                "Cost, selling price and quantity must be numbers.", "Validation",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        Date production = parseDate(tfProduction.getText().trim());
        Date expiry = parseDate(tfExpiry.getText().trim());

        Integer companyId = companyIds.isEmpty() ? null : companyIds.get(cbCompany.getSelectedIndex());

        String sql = "INSERT INTO drugs (barcode, name, type, dose, code, "
                + "cost_price, selling_price, production_date, expiration_date, "
                + "place, quantity, company_id) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = null;
        try {
            Connection conn = Connect.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, barcode);
            ps.setString(2, name);
            ps.setString(3, emptyToNull(tfType.getText().trim()));
            ps.setString(4, emptyToNull(tfDose.getText().trim()));
            ps.setString(5, emptyToNull(tfCode.getText().trim()));
            ps.setBigDecimal(6, cost);
            ps.setBigDecimal(7, sell);
            ps.setDate(8, production);
            ps.setDate(9, expiry);
            ps.setString(10, emptyToNull(tfPlace.getText().trim()));
            ps.setInt(11, qty);
            if (companyId == null) {
                ps.setNull(12, java.sql.Types.INTEGER);
            } else {
                ps.setInt(12, companyId);
            }
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this,
                "Drug \"" + name + "\" added to inventory.", "Success",
                JOptionPane.INFORMATION_MESSAGE);
            clearForm();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                "Could not add drug: " + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            closeQuietly(ps);
        }
    }

    private static String emptyToNull(String value) {
        return value.isEmpty() ? null : value;
    }

    private static Date parseDate(String text) {
        if (text.isEmpty()) {
            return null;
        }
        try {
            return Date.valueOf(text);
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(null,
                "Invalid date '" + text + "' - use yyyy-MM-dd.", "Validation",
                JOptionPane.WARNING_MESSAGE);
            return null;
        }
    }

    private void clearForm() {
        for (JTextField f : new JTextField[]{
            tfBarcode, tfName, tfType, tfDose, tfCode, tfCost, tfSell,
            tfProduction, tfExpiry, tfPlace, tfQty
        }) {
            f.setText("");
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
        java.awt.EventQueue.invokeLater(() -> new Buy_Drug().setVisible(true));
    }
}
