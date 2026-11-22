package main;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
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
 * Inbox - shows messages received by the currently logged-in user.
 *
 * <p>Message IDs are tracked in parallel with table rows so selected rows can
 * be marked as read.
 *
 * @author Gulshan Singh
 */
public class Inbox extends JFrame {

    private final DefaultTableModel model;
    private final JTable table;
    private final List<Integer> messageIds = new ArrayList<>();

    /** Creates new form Inbox */
    public Inbox() {
        setTitle("Inbox");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setBounds(100, 80, 720, 440);

        String[] cols = {"From", "Subject", "Message", "Sent", "Read"};
        model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(model);

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        JLabel title = new JLabel("Inbox");
        title.setFont(title.getFont().deriveFont(16.0f));
        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(e -> load());
        JButton mark = new JButton("Mark Selected Read");
        mark.addActionListener(e -> markRead());
        top.add(title);
        top.add(refresh);
        top.add(mark);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(top, BorderLayout.NORTH);
        getContentPane().add(new JScrollPane(table), BorderLayout.CENTER);

        load();
    }

    private void load() {
        model.setRowCount(0);
        messageIds.clear();
        int uid = Session.userId();
        if (uid <= 0) {
            return;
        }
        String sql = "SELECT m.message_id, f.name, m.subject, m.message_text, "
                + "m.sent_at, m.is_read "
                + "FROM messages m LEFT JOIN users f ON f.user_id = m.message_from "
                + "WHERE m.message_to = ? ORDER BY m.is_read ASC, m.sent_at DESC";
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            Connection conn = Connect.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, uid);
            rs = ps.executeQuery();
            while (rs.next()) {
                messageIds.add(rs.getInt(1));
                model.addRow(new Object[]{
                    rs.getString(2), rs.getString(3), rs.getString(4),
                    rs.getTimestamp(5), rs.getInt(6) == 1 ? "Yes" : "No"
                });
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                "Error loading inbox: " + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
        }
    }

    private void markRead() {
        int[] rows = table.getSelectedRows();
        if (rows.length == 0) {
            JOptionPane.showMessageDialog(this, "Select messages to mark as read.",
                "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        Connection conn = null;
        try {
            conn = Connect.getConnection();
            conn.setAutoCommit(false);
            PreparedStatement ps = conn.prepareStatement(
                "UPDATE messages SET is_read = 1 WHERE message_id = ?");
            for (int row : rows) {
                int modelRow = table.convertRowIndexToModel(row);
                ps.setInt(1, messageIds.get(modelRow));
                ps.executeUpdate();
            }
            ps.close();
            conn.commit();
            load();
        } catch (Exception ex) {
            try {
                if (conn != null) {
                    conn.rollback();
                }
            } catch (Exception ignore) {
            }
            JOptionPane.showMessageDialog(this,
                "Could not update messages: " + ex.getMessage(),
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
        java.awt.EventQueue.invokeLater(() -> new Inbox().setVisible(true));
    }
}
