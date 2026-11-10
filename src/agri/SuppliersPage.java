package agri;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import agri.Models.*;

/** Supplier CRUD + pending payment + items supplied by each supplier. */
public class SuppliersPage extends CrudPage {

    public SuppliersPage() {
        super(new String[]{"ID", "Supplier Name", "Phone", "Address", "Pending Payment (Rs)", "Products"}, true, true);
        UI.Btn items = UI.warn("Supplied Items");
        items.addActionListener(e -> supplied());
        UI.Btn pay = UI.success("Pay Supplier");
        pay.addActionListener(e -> pay());
        rightBar.add(items); rightBar.add(pay);
    }

    @Override protected List<Object[]> rows(String f) {
        List<Object[]> l = new ArrayList<>();
        for (Supplier s : DataStore.suppliers) {
            if (!f.isEmpty() && !(s.name.toLowerCase().contains(f) || s.address.toLowerCase().contains(f) || s.phone.contains(f))) continue;
            int n = 0; for (Product p : DataStore.products) if (p.supplierId == s.id) n++;
            l.add(new Object[]{s.id, s.name, s.phone, s.address, s.pending, n});
        }
        return l;
    }

    @Override protected Color rowColor(int r) {
        Supplier s = DataStore.supplier(((Number) model.getValueAt(r, 0)).intValue());
        return s != null && s.pending > 0 ? Theme.tint(Theme.WARN) : null;
    }

    private void pay() {
        Supplier s = DataStore.supplier(table.selectedId());
        if (s == null) { UI.showInfo(this, "Please select a supplier first."); return; }
        if (s.pending <= 0) { UI.showInfo(this, "This supplier has no pending payment."); return; }
        UI.TField amt = new UI.TField(String.valueOf((long) s.pending), 10);
        boolean ok = new UI.Form(this, "Pay Supplier")
                .add("Supplier", UI.label(s.name, 14, true, Theme.TEXT))
                .add("Pending Amount", UI.label(UI.money(s.pending), 14, true, Theme.DANGER))
                .add("Amount to pay (Rs)", amt).show(() -> {
                    try { double a = UI.toDouble(amt.getText()); if (a <= 0) return "The amount must be greater than 0."; if (a > s.pending) return "The amount cannot be more than the pending amount."; }
                    catch (Exception e) { return "Enter a valid amount."; }
                    return null; }, "Pay");
        if (!ok) return;
        double a = UI.toDouble(amt.getText());
        s.pending -= a;
        DataStore.log("Supplier payment: " + s.name + " Rs " + (long) a);
        DataStore.saveAll();
        refresh();
    }

    private void supplied() {
        Supplier s = DataStore.supplier(table.selectedId());
        if (s == null) { UI.showInfo(this, "Please select a supplier first."); return; }
        javax.swing.table.DefaultTableModel m = UI.model("Date", "Product", "Qty", "Cost (Rs)", "Note");
        for (int i = DataStore.stockLogs.size() - 1; i >= 0; i--) {
            StockLog l = DataStore.stockLogs.get(i);
            if ("IN".equals(l.type) && l.supplierId == s.id) m.addRow(new Object[]{l.date, DataStore.productName(l.productId), l.qty, l.cost, l.note});
        }
        JDialog d = new JDialog(SwingUtilities.getWindowAncestor(this), s.name + " - Supplied Items", Dialog.ModalityType.APPLICATION_MODAL);
        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBackground(Theme.BG); root.setBorder(javax.swing.BorderFactory.createEmptyBorder(16, 16, 16, 16));
        UI.Btn close = UI.ghost("Close"); close.addActionListener(e -> d.dispose());
        JPanel bp = UI.transparent(new FlowLayout(FlowLayout.RIGHT)); bp.add(close);
        JLabel hdr = UI.label("Goods received from " + s.name + " (Stock In records)", 16, true, Theme.TEXT);
        root.add(hdr, BorderLayout.NORTH); root.add(UI.scroll(new UI.StyledTable(m)), BorderLayout.CENTER); root.add(bp, BorderLayout.SOUTH);
        d.setContentPane(root); d.setSize(700, 420); d.setLocationRelativeTo(this); d.setVisible(true);
    }

    @Override protected void onAdd() { form(null); }
    @Override protected void onEdit(int id) { form(DataStore.supplier(id)); }

    @Override protected void onDelete(int id) {
        Supplier s = DataStore.supplier(id);
        if (s == null) return;
        if (s.pending > 0 && !UI.confirm(this, "This supplier is still owed Rs " + (long) s.pending + ". Delete anyway?")) return;
        if (s.pending <= 0 && !UI.confirm(this, "Delete '" + s.name + "'?")) return;
        for (Product p : DataStore.products) if (p.supplierId == id) p.supplierId = 0;
        DataStore.suppliers.remove(s);
        DataStore.log("Deleted supplier: " + s.name);
        DataStore.saveAll();
    }

    private void form(Supplier edit) {
        boolean isNew = edit == null;
        Supplier s = isNew ? new Supplier() : edit;
        UI.TField name = new UI.TField(isNew ? "" : s.name, 20), phone = new UI.TField(isNew ? "" : s.phone, 14), addr = new UI.TField(isNew ? "" : s.address, 24);
        boolean ok = new UI.Form(this, isNew ? "New Supplier" : "Edit Supplier")
                .add("Name", name).add("Phone", phone).add("Address", addr)
                .show(() -> name.getText().trim().isEmpty() ? "Enter the supplier name." : null, "Save");
        if (!ok) return;
        s.name = name.getText().trim(); s.phone = phone.getText().trim(); s.address = addr.getText().trim();
        if (isNew) { s.id = DataStore.nextSupplierId(); DataStore.suppliers.add(s); }
        DataStore.log((isNew ? "New supplier: " : "Updated supplier: ") + s.name);
        DataStore.saveAll();
    }
}
