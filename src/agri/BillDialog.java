package agri;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.util.List;
import agri.Models.*;

/** Shows, prints and saves a bill (invoice) + the order list dialog. */
public class BillDialog {

    private static String cut(String s, int n) { return s.length() <= n ? s : s.substring(0, n - 1) + "."; }

    public static String invoiceText(Order o) {
        Farmer f = DataStore.farmer(o.farmerId);
        String line = "--------------------------------------------------\n";
        StringBuilder sb = new StringBuilder();
        sb.append("              AGRI INVENTORY PRO\n");
        sb.append("        Seed & Fertilizer Store - INVOICE\n");
        sb.append(line);
        sb.append(String.format("Invoice #: %-12d Date: %s%n", o.id, o.date));
        sb.append("Farmer   : ").append(f == null ? "-" : f.name).append("\n");
        if (f != null) sb.append("Village  : ").append(f.village).append("   Ph: ").append(f.phone).append("\n");
        sb.append("Priority : ").append(o.priorityName()).append("   Status: ").append(o.status).append("\n");
        sb.append(line);
        sb.append(String.format("%-24s %4s %9s %9s%n", "Item", "Qty", "Price", "Amount"));
        sb.append(line);
        for (OrderItem i : o.items)
            sb.append(String.format("%-24s %4d %,9.0f %,9.0f%n", cut(DataStore.productName(i.productId), 24), i.qty, i.price, i.amount()));
        sb.append(line);
        sb.append(String.format("%-38s %,10.0f%n", "Sub Total:", o.subTotal()));
        sb.append(String.format("%-38s %,10.0f%n", "Discount:", o.discount));
        sb.append(String.format("%-38s %,10.0f%n", "TOTAL:", o.total()));
        sb.append(String.format("%-38s %,10.0f%n", "Paid:", o.paid));
        sb.append(String.format("%-38s %,10.0f%n", "Balance:", o.balance()));
        sb.append(line);
        if (f != null) sb.append(String.format("Farmer total credit: Rs %,.0f%n", f.credit));
        if (!o.processedBy.isEmpty()) sb.append("Served by: ").append(o.processedBy).append("\n");
        sb.append("\n           Thank you! Please visit again.\n");
        return sb.toString();
    }

    public static void show(Component parent, Order o) {
        Window w = parent == null ? null : SwingUtilities.getWindowAncestor(parent);
        JDialog d = new JDialog(w, "Bill #" + o.id, Dialog.ModalityType.APPLICATION_MODAL);
        JTextArea ta = new JTextArea(invoiceText(o));
        ta.setEditable(false); ta.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        ta.setBackground(Theme.CARD); ta.setForeground(Theme.TEXT); ta.setBorder(new EmptyBorder(14, 16, 14, 16));

        UI.Btn print = UI.primary("Print / PDF");
        print.setToolTipText("Choose 'Microsoft Print to PDF' in the print dialog to create a PDF");
        print.addActionListener(e -> {
            try { ta.print(); } catch (Exception ex) { UI.showError(d, "Printing failed: " + ex.getMessage()); }
        });
        UI.Btn save = UI.info("Save as Text");
        save.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setSelectedFile(new File("Bill_" + o.id + ".txt"));
            if (fc.showSaveDialog(d) == JFileChooser.APPROVE_OPTION)
                try { Files.write(fc.getSelectedFile().toPath(), invoiceText(o).getBytes("UTF-8")); UI.showOk(d, "Bill saved."); }
                catch (Exception ex) { UI.showError(d, ex.getMessage()); }
        });
        UI.Btn close = UI.ghost("Close"); close.addActionListener(e -> d.dispose());

        JPanel bp = UI.transparent(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        bp.add(close); bp.add(save); bp.add(print);
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.CARD);
        root.add(UI.plainScroll(ta), BorderLayout.CENTER);
        root.add(bp, BorderLayout.SOUTH);
        d.setContentPane(root);
        d.setSize(520, 600); d.setLocationRelativeTo(parent); d.setVisible(true);
    }

    public static void list(Component parent, String title, List<Order> orders) {
        Window w = parent == null ? null : SwingUtilities.getWindowAncestor(parent);
        JDialog d = new JDialog(w, title, Dialog.ModalityType.APPLICATION_MODAL);
        javax.swing.table.DefaultTableModel m = UI.model("Order #", "Date", "Items", "Total", "Paid", "Balance", "Status");
        for (Order o : orders)
            m.addRow(new Object[]{o.id, o.date, Logic.itemsSummary(o), o.total(), o.paid, o.balance(), o.status});
        UI.StyledTable t = new UI.StyledTable(m);
        UI.Btn view = UI.primary("View Bill");
        view.addActionListener(e -> {
            int id = t.selectedId();
            Order o = id < 0 ? null : DataStore.order(id);
            if (o == null) UI.showInfo(d, "Please select an order first."); else show(d, o);
        });
        UI.Btn close = UI.ghost("Close"); close.addActionListener(e -> d.dispose());
        JPanel bp = UI.transparent(new FlowLayout(FlowLayout.RIGHT, 8, 0)); bp.add(close); bp.add(view);
        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBackground(Theme.BG); root.setBorder(new EmptyBorder(16, 16, 16, 16));
        root.add(UI.label(title, 18, true, Theme.TEXT), BorderLayout.NORTH);
        root.add(UI.scroll(t), BorderLayout.CENTER); root.add(bp, BorderLayout.SOUTH);
        d.setContentPane(root); d.setSize(820, 460); d.setLocationRelativeTo(parent); d.setVisible(true);
    }
}
