package agri;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import agri.Models.*;

/** Stock In / Out, low-stock, expiry and out-of-stock alerts. */
public class StockPage extends Page {
    private final boolean admin;
    private final Widgets.StatCard cOut = new Widgets.StatCard("OUT OF STOCK", Theme.DANGER),
            cLow = new Widgets.StatCard("LOW STOCK", Theme.WARN),
            cExp = new Widgets.StatCard("EXPIRING (30 days)", Theme.INFO),
            cExd = new Widgets.StatCard("EXPIRED", Theme.DANGER);
    private final DefaultTableModel sm = UI.model("ID", "Product", "Category", "Qty", "Min Qty", "Expiry", "Days Left", "Status");
    private final UI.StyledTable st = new UI.StyledTable(sm);
    private final DefaultTableModel lm = UI.model("Date", "Product", "Type", "Qty", "Supplier", "Cost (Rs)", "Note");
    private final UI.StyledTable lt = new UI.StyledTable(lm);
    private final JComboBox<String> filter = UI.combo(new String[]{"All", Logic.OUT, Logic.LOW, Logic.EXPIRING, Logic.EXPIRED, Logic.OK});

    public StockPage(boolean admin) {
        this.admin = admin;
        st.rowColor = r -> {
            String s = String.valueOf(sm.getValueAt(r, 7));
            if (s.equals(Logic.OUT) || s.equals(Logic.EXPIRED)) return Theme.tint(Theme.DANGER);
            if (s.startsWith("Low") || s.equals(Logic.EXPIRING)) return Theme.tint(Theme.WARN);
            return null;
        };
        lt.rowColor = r -> "IN".equals(lm.getValueAt(r, 2)) ? Theme.tint(Theme.SUCCESS) : null;
        st.getColumnModel().getColumn(6).setCellRenderer(new DefaultTableCellRenderer() {
            @Override protected void setValue(Object v) { setText(v instanceof Number && ((Number) v).intValue() >= 99999 ? "-" : String.valueOf(v)); }
        });

        JPanel cards = UI.transparent(new GridLayout(1, 4, 14, 0));
        cards.add(cOut); cards.add(cLow); cards.add(cExp); cards.add(cExd);

        JPanel bar = UI.transparent(new BorderLayout());
        JPanel l = UI.transparent(new FlowLayout(FlowLayout.LEFT, 10, 0));
        l.add(UI.label("Filter:", 13, true, Theme.MUTED)); l.add(filter);
        filter.addActionListener(e -> refresh());
        bar.add(l, BorderLayout.WEST);
        if (admin) {
            UI.Btn in = UI.success("+ Stock In (goods received)");
            in.addActionListener(e -> stockIn());
            JPanel r = UI.transparent(new FlowLayout(FlowLayout.RIGHT, 0, 0)); r.add(in);
            bar.add(r, BorderLayout.EAST);
        }
        JPanel status = UI.transparent(new BorderLayout(0, 10));
        status.add(bar, BorderLayout.NORTH); status.add(UI.scroll(st), BorderLayout.CENTER);

        UI.Tabs tabs = new UI.Tabs();
        tabs.addTab("Stock Status & Alerts", status, null);
        tabs.addTab("Stock Movement Log", UI.scroll(lt), null);
        add(cards, BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);
    }

    @Override public void refresh() {
        int out = 0, low = 0, exp = 0, exd = 0;
        for (Product p : DataStore.products) {
            String s = Logic.status(p);
            long dl = Logic.daysLeft(p);
            if (s.equals(Logic.OUT)) out++; else if (s.equals(Logic.LOW)) low++;
            if (dl < 0) exd++; else if (dl <= Logic.EXPIRY_WARN_DAYS) exp++;
        }
        cOut.setValue(String.valueOf(out)); cLow.setValue(String.valueOf(low)); cExp.setValue(String.valueOf(exp)); cExd.setValue(String.valueOf(exd));

        String f = (String) filter.getSelectedItem();
        sm.setRowCount(0);
        for (Product p : DataStore.products) {
            String s = Logic.status(p);
            long d = Logic.daysLeft(p);
            boolean match = "All".equals(f) || s.equals(f) || (Logic.EXPIRING.equals(f) && d >= 0 && d <= Logic.EXPIRY_WARN_DAYS);
            if (!match) continue;
            sm.addRow(new Object[]{p.id, p.name, p.category, p.qty, p.minQty, p.expiry.isEmpty() ? "-" : p.expiry, d == Long.MAX_VALUE ? 99999 : (int) d, (s.equals(Logic.LOW) && d >= 0 && d <= Logic.EXPIRY_WARN_DAYS) ? "Low stock + Expiring" : s});
        }
        lm.setRowCount(0);
        for (int i = DataStore.stockLogs.size() - 1; i >= 0; i--) {
            StockLog g = DataStore.stockLogs.get(i);
            lm.addRow(new Object[]{g.date, DataStore.productName(g.productId), g.type, g.qty, DataStore.supplierName(g.supplierId), g.cost, g.note});
        }
    }

    private void stockIn() {
        if (DataStore.products.isEmpty()) { UI.showError(this, "Please add a product first."); return; }
        JComboBox<Product> pc = UI.combo(new Vector<>(DataStore.products));
        Supplier none = new Supplier(); none.id = 0; none.name = "(None)";
        Vector<Supplier> sv = new Vector<>(); sv.add(none); sv.addAll(DataStore.suppliers);
        JComboBox<Supplier> sc = UI.combo(sv);
        Runnable sync = () -> { Supplier s = DataStore.supplier(((Product) pc.getSelectedItem()).supplierId); sc.setSelectedItem(s == null ? none : s); };
        pc.addActionListener(e -> sync.run()); sync.run();
        int sel = st.selectedId();
        if (sel >= 0) pc.setSelectedItem(DataStore.product(sel));
        UI.TField qty = new UI.TField("10", 8), cost = new UI.TField("0", 8), paid = new UI.TField("0", 8), note = new UI.TField("", 18);
        boolean ok = new UI.Form(this, "Stock In - Goods Received")
                .add("Product", pc).add("Supplier", sc).add("Quantity", qty)
                .add("Total Cost (Rs)", cost).add("Paid now (Rs)", paid).add("Note", note)
                .show(() -> {
                    try {
                        if (UI.toInt(qty.getText()) <= 0) return "Quantity must be greater than 0.";
                        double c = UI.toDouble(cost.getText()), p = UI.toDouble(paid.getText());
                        if (c < 0 || p < 0) return "Amounts cannot be negative.";
                        if (p > c) return "The paid amount cannot be more than the total cost.";
                    } catch (Exception e) { return "Enter a valid quantity and amounts."; }
                    return null; }, "Add Stock");
        if (!ok) return;
        Supplier s = (Supplier) sc.getSelectedItem();
        DataStore.addStock((Product) pc.getSelectedItem(), UI.toInt(qty.getText()), s.id == 0 ? null : s,
                UI.toDouble(cost.getText()), UI.toDouble(paid.getText()), note.getText().trim().isEmpty() ? "Stock In" : note.getText().trim());
        UI.showOk(this, "Stock added.");
        refresh();
    }
}
