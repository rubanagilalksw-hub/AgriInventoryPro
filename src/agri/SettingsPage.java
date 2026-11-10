package agri;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import agri.Models.*;

/** Theme, password, backup/restore and users. */
public class SettingsPage extends Page {
    private final boolean admin;
    private final DefaultTableModel um = UI.model("Username", "Role");
    private final UI.StyledTable ut = new UI.StyledTable(um);

    public SettingsPage(boolean admin) {
        this.admin = admin;
        UI.Tabs tabs = new UI.Tabs();
        tabs.addTab("Appearance / Theme", appearance(), null);
        tabs.addTab("Change Password", password(), null);
        if (admin) {
            tabs.addTab("Backup & Restore", backup(), null);
            tabs.addTab("Users", users(), null);
        }
        tabs.addTab("About", about(), null);
        add(tabs, BorderLayout.CENTER);
    }

    private JPanel wrap(JComponent c) {
        JPanel p = UI.transparent(new FlowLayout(FlowLayout.LEFT, 0, 0));
        p.add(c);
        return p;
    }

    // ---------------------------------------------------------------- theme
    private static class Swatch extends JComponent {
        private final String name; private final boolean selected;
        Swatch(String name, boolean sel, Runnable r) {
            this.name = name; selected = sel;
            setPreferredSize(new Dimension(46, 46));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText(name);
            addMouseListener(new MouseAdapter() { public void mouseClicked(MouseEvent e) { r.run(); } });
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create(); UI.aa(g2);
            Color c = "Blue".equals(name) ? Color.decode("#2563EB") : "Orange".equals(name) ? Color.decode("#EA580C")
                    : "Purple".equals(name) ? Color.decode("#7C3AED") : "Red".equals(name) ? Color.decode("#DC2626") : Color.decode("#16A34A");
            g2.setColor(c); g2.fillOval(5, 5, 36, 36);
            if (selected) { g2.setColor(Theme.TEXT); g2.setStroke(new BasicStroke(2.5f)); g2.drawOval(2, 2, 42, 42);
                g2.setColor(Color.WHITE); g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(15, 24, 21, 30); g2.drawLine(21, 30, 32, 17); }
            g2.dispose();
        }
    }

    private JPanel appearance() {
        UI.Card c = new UI.Card(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0; g.anchor = GridBagConstraints.WEST; g.insets = new Insets(6, 0, 6, 0);
        g.gridy = 0; c.add(UI.label("Theme Mode", 16, true, Theme.TEXT), g);
        g.gridy = 1; c.add(UI.muted("Choose Light or Dark mode - applied immediately"), g);

        JPanel modes = UI.transparent(new FlowLayout(FlowLayout.LEFT, 10, 0));
        UI.Btn light = new UI.Btn("Light Mode", Theme.PRIMARY, Theme.dark);
        UI.Btn dark = new UI.Btn("Dark Mode", Theme.PRIMARY, !Theme.dark);
        light.addActionListener(e -> Main.changeTheme(false, Theme.accent));
        dark.addActionListener(e -> Main.changeTheme(true, Theme.accent));
        modes.add(light); modes.add(dark);
        g.gridy = 2; g.insets = new Insets(6, 0, 18, 0); c.add(modes, g);

        g.gridy = 3; g.insets = new Insets(6, 0, 6, 0); c.add(UI.label("Accent Color", 16, true, Theme.TEXT), g);
        JPanel sw = UI.transparent(new FlowLayout(FlowLayout.LEFT, 6, 0));
        for (String a : Theme.ACCENTS) sw.add(new Swatch(a, a.equals(Theme.accent), () -> Main.changeTheme(Theme.dark, a)));
        g.gridy = 4; c.add(sw, g);
        return wrap(c);
    }

    // ---------------------------------------------------------------- password
    private JPanel password() {
        UI.PField oldP = new UI.PField(18), newP = new UI.PField(18), conf = new UI.PField(18);
        JLabel msg = UI.label(" ", 12, true, Theme.DANGER);
        UI.Btn go = UI.primary("Update Password");
        go.addActionListener(e -> {
            String o = new String(oldP.getPassword()), n = new String(newP.getPassword()), c = new String(conf.getPassword());
            if (!n.equals(c)) { msg.setForeground(Theme.DANGER); msg.setText("The new password and the confirmation do not match."); return; }
            String err = DataStore.changePassword(o, n);
            if (err != null) { msg.setForeground(Theme.DANGER); msg.setText(err); return; }
            oldP.setText(""); newP.setText(""); conf.setText("");
            msg.setForeground(Theme.SUCCESS); msg.setText("Password changed successfully!");
        });
        UI.Card c = new UI.Card(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0; g.gridy = 0; g.gridwidth = 2; g.anchor = GridBagConstraints.WEST; g.insets = new Insets(4, 0, 12, 0);
        c.add(UI.label("Change Password", 16, true, Theme.TEXT), g);
        g.gridwidth = 1; g.insets = new Insets(6, 0, 6, 14);
        String[] ls = {"Old Password", "New Password", "Confirm New Password"};
        JComponent[] fs = {oldP, newP, conf};
        for (int i = 0; i < 3; i++) {
            g.gridx = 0; g.gridy = i + 1; c.add(UI.label(ls[i], 13, true, Theme.MUTED), g);
            g.gridx = 1; c.add(fs[i], g);
        }
        g.gridx = 1; g.gridy = 4; g.insets = new Insets(12, 0, 4, 0); c.add(go, g);
        g.gridx = 0; g.gridy = 5; g.gridwidth = 2; c.add(msg, g);
        return wrap(c);
    }

    // ---------------------------------------------------------------- backup
    private JPanel backup() {
        UI.Btn b = UI.primary("Create Backup");
        b.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setSelectedFile(new File("AgriBackup_" + DataStore.today() + ".db"));
            if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
            try { DataStore.backup(fc.getSelectedFile()); UI.showOk(this, "Backup created:\n" + fc.getSelectedFile().getAbsolutePath()); }
            catch (Exception ex) { UI.showError(this, "Backup failed: " + ex.getMessage()); }
        });
        UI.Btn r = UI.warn("Restore Backup");
        r.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
            if (!UI.confirm(this, "All current data will be replaced by the backup. Continue?")) return;
            try { DataStore.restore(fc.getSelectedFile()); UI.showOk(this, "Restore complete. The application is refreshing."); Main.showMain("dashboard"); }
            catch (Exception ex) { UI.showError(this, "Restore failed: " + ex.getMessage()); }
        });
        UI.Card c = new UI.Card(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0; g.anchor = GridBagConstraints.WEST; g.insets = new Insets(6, 0, 6, 0);
        g.gridy = 0; c.add(UI.label("Backup & Restore", 16, true, Theme.TEXT), g);
        g.gridy = 1; c.add(UI.muted("Save a backup copy of all your data (products, farmers, orders...) to one file, or restore it later."), g);
        JPanel row = UI.transparent(new FlowLayout(FlowLayout.LEFT, 10, 0)); row.add(b); row.add(r);
        g.gridy = 2; g.insets = new Insets(12, 0, 6, 0); c.add(row, g);
        g.gridy = 3; c.add(UI.muted("Data location: " + Db.FILE.getAbsolutePath()), g);
        return wrap(c);
    }

    // ---------------------------------------------------------------- users
    private JPanel users() {
        JPanel p = UI.transparent(new BorderLayout(0, 12));
        UI.TField un = UI.field(12); UI.PField pw = new UI.PField(12);
        JComboBox<String> role = UI.combo(new String[]{"STAFF", "ADMIN"});
        UI.Btn add = UI.primary("+ User Add");
        add.addActionListener(e -> {
            String err = DataStore.addUser(un.getText(), new String(pw.getPassword()), (String) role.getSelectedItem());
            if (err != null) UI.showError(this, err); else { un.setText(""); pw.setText(""); refresh(); }
        });
        UI.Btn del = UI.danger("Delete User");
        del.addActionListener(e -> {
            int r = ut.getSelectedRow();
            if (r < 0) { UI.showInfo(this, "Please select a user first."); return; }
            String name = (String) um.getValueAt(ut.convertRowIndexToModel(r), 0);
            if (name.equalsIgnoreCase(DataStore.current.username)) { UI.showError(this, "You cannot delete your own account."); return; }
            if (!UI.confirm(this, "Delete user '" + name + "'?")) return;
            DataStore.users.removeIf(u -> u.username.equals(name));
            DataStore.log("Deleted user: " + name); DataStore.saveAll(); refresh();
        });
        JPanel bar = UI.transparent(new FlowLayout(FlowLayout.LEFT, 10, 0));
        bar.add(UI.label("Username", 13, true, Theme.MUTED)); bar.add(un);
        bar.add(UI.label("Password", 13, true, Theme.MUTED)); bar.add(pw);
        bar.add(role); bar.add(add); bar.add(del);
        p.add(bar, BorderLayout.NORTH); p.add(UI.scroll(ut), BorderLayout.CENTER);
        return p;
    }

    private JPanel about() {
        UI.Card c = new UI.Card(new GridLayout(0, 1, 0, 6));
        c.add(UI.label("Agri Inventory Pro", 20, true, Theme.TEXT));
        c.add(UI.muted("Seed, Fertilizer & Pesticide Store Management System"));
        c.add(UI.muted("Java Swing Desktop Application  |  Array-based Queue + CRUD"));
        c.setPreferredSize(new Dimension(520, 170));
        return wrap(c);
    }

    @Override public void refresh() {
        um.setRowCount(0);
        if (admin) for (User u : DataStore.users) um.addRow(new Object[]{u.username, u.role});
    }
}
