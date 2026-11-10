package agri;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import agri.Models.*;

/** Farmer (customer) CRUD + credit (udhaar) + purchase history. */
public class FarmersPage extends CrudPage {

    public FarmersPage(boolean admin) {
        super(new String[]{"ID", "Name", "Phone", "Village", "CNIC", "Credit (Rs)", "Total Purchases (Rs)"}, true, admin);
        UI.Btn pay = UI.success("Receive Payment");
        pay.addActionListener(e -> receive());
        UI.Btn hist = UI.warn("Purchase History");
        hist.addActionListener(e -> {
            Farmer f = DataStore.farmer(table.selectedId());
            if (f == null) { UI.showInfo(this, "Please select a farmer first."); return; }
            List<Order> l = new ArrayList<>();
            for (Order o : DataStore.orders) if (o.farmerId == f.id && !"PENDING".equals(o.status)) l.add(o);
            Collections.reverse(l);
            BillDialog.list(this, f.name + " - Purchase History", l);
        });
        rightBar.add(hist); rightBar.add(pay);
    }

    @Override protected List<Object[]> rows(String f) {
        List<Object[]> l = new ArrayList<>();
        for (Farmer x : DataStore.farmers) {
            if (!f.isEmpty() && !(x.name.toLowerCase().contains(f) || x.village.toLowerCase().contains(f) || x.phone.contains(f) || x.cnic.contains(f))) continue;
            l.add(new Object[]{x.id, x.name, x.phone, x.village, x.cnic, x.credit, Logic.farmerPurchases(x.id)});
        }
        return l;
    }

    @Override protected Color rowColor(int r) {
        Farmer f = DataStore.farmer(((Number) model.getValueAt(r, 0)).intValue());
        return f != null && f.credit > 0 ? Theme.tint(Theme.WARN) : null;
    }

    private void receive() {
        Farmer f = DataStore.farmer(table.selectedId());
        if (f == null) { UI.showInfo(this, "Please select a farmer first."); return; }
        if (f.credit <= 0) { UI.showInfo(this, f.name + " has no outstanding credit."); return; }
        UI.TField amt = new UI.TField(String.valueOf((long) f.credit), 10);
        boolean ok = new UI.Form(this, "Receive Payment")
                .add("Farmer", UI.label(f.name, 14, true, Theme.TEXT))
                .add("Current Credit", UI.label(UI.money(f.credit), 14, true, Theme.DANGER))
                .add("Amount (Rs)", amt).show(() -> {
                    try { double a = UI.toDouble(amt.getText()); if (a <= 0) return "The amount must be greater than 0."; if (a > f.credit) return "The amount cannot be more than the credit."; }
                    catch (Exception e) { return "Enter a valid amount."; }
                    return null; }, "Receive");
        if (!ok) return;
        double a = UI.toDouble(amt.getText());
        f.credit -= a;
        DataStore.log("Credit received: " + f.name + " Rs " + (long) a);
        DataStore.saveAll();
        UI.showOk(this, UI.money(a) + " received. Remaining credit: " + UI.money(f.credit));
        refresh();
    }

    @Override protected void onAdd() { form(null); }
    @Override protected void onEdit(int id) { form(DataStore.farmer(id)); }

    @Override protected void onDelete(int id) {
        Farmer f = DataStore.farmer(id);
        if (f == null) return;
        for (Order o : DataStore.orders) if ("PENDING".equals(o.status) && o.farmerId == id) { UI.showError(this, "This farmer has a pending order."); return; }
        if (f.credit > 0 && !UI.confirm(this, f.name + " still owes Rs " + (long) f.credit + ". Delete anyway?")) return;
        if (f.credit <= 0 && !UI.confirm(this, "Delete '" + f.name + "'?")) return;
        DataStore.farmers.remove(f);
        DataStore.log("Deleted farmer: " + f.name);
        DataStore.saveAll();
    }

    private void form(Farmer edit) {
        boolean isNew = edit == null;
        Farmer f = isNew ? new Farmer() : edit;
        UI.TField name = new UI.TField(isNew ? "" : f.name, 20), phone = new UI.TField(isNew ? "" : f.phone, 14),
                village = new UI.TField(isNew ? "" : f.village, 20), cnic = new UI.TField(isNew ? "" : f.cnic, 16);
        cnic.setToolTipText("13 digits, e.g. 43201-1234567-1");
        boolean ok = new UI.Form(this, isNew ? "New Farmer" : "Edit Farmer")
                .add("Name", name).add("Phone", phone).add("Village", village).add("CNIC", cnic)
                .show(() -> {
                    if (name.getText().trim().isEmpty()) return "Enter the farmer name.";
                    String ph = phone.getText().replaceAll("[^0-9]", "");
                    if (!ph.isEmpty() && (ph.length() < 10 || ph.length() > 12)) return "The phone number must have 10-12 digits.";
                    String cn = cnic.getText().replaceAll("[^0-9]", "");
                    if (!cn.isEmpty() && cn.length() != 13) return "The CNIC must have 13 digits.";
                    return null; }, "Save");
        if (!ok) return;
        f.name = name.getText().trim(); f.phone = phone.getText().trim(); f.village = village.getText().trim(); f.cnic = cnic.getText().trim();
        if (isNew) { f.id = DataStore.nextFarmerId(); DataStore.farmers.add(f); }
        DataStore.log((isNew ? "New farmer: " : "Updated farmer: ") + f.name);
        DataStore.saveAll();
    }
}
