package agri;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import agri.Models.*;

/** Product CRUD + search + category filter + sort (header click). */
public class ProductsPage extends CrudPage {
    static final String[] CATS = {"Seeds", "Fertilizers", "Pesticides", "Farm Tools"};
    static final String[] SEASONS = {"Rabi", "Kharif", "All Season"};
    private final JComboBox<String> catFilter = UI.combo(new String[]{"All Categories", "Seeds", "Fertilizers", "Pesticides", "Farm Tools"});

    public ProductsPage(boolean admin) {
        super(new String[]{"ID", "Product Name", "Category", "Price (Rs)", "Qty", "Min Qty", "Expiry", "Season", "Supplier", "Status"}, admin, admin);
        leftBar.add(catFilter);
        catFilter.addActionListener(e -> refresh());
    }

    @Override protected List<Object[]> rows(String f) {
        List<Object[]> l = new ArrayList<>();
        String cat = (String) catFilter.getSelectedItem();
        for (Product p : DataStore.products) {
            if (!"All Categories".equals(cat) && !p.category.equals(cat)) continue;
            if (!f.isEmpty() && !(p.name.toLowerCase().contains(f) || p.category.toLowerCase().contains(f))) continue;
            l.add(new Object[]{p.id, p.name, p.category, p.price, p.qty, p.minQty, p.expiry.isEmpty() ? "-" : p.expiry,
                    p.season, DataStore.supplierName(p.supplierId), Logic.status(p)});
        }
        return l;
    }

    @Override protected Color rowColor(int r) {
        Object id = model.getValueAt(r, 0);
        Product p = DataStore.product(((Number) id).intValue());
        if (p == null) return null;
        String s = Logic.status(p);
        if (s.equals(Logic.OUT) || s.equals(Logic.EXPIRED)) return Theme.tint(Theme.DANGER);
        if (s.equals(Logic.LOW) || s.equals(Logic.EXPIRING)) return Theme.tint(Theme.WARN);
        return null;
    }

    @Override protected void onAdd() { form(null); }
    @Override protected void onEdit(int id) { form(DataStore.product(id)); }

    @Override protected void onDelete(int id) {
        Product p = DataStore.product(id);
        if (p == null) return;
        for (Order o : DataStore.orders) if ("PENDING".equals(o.status))
            for (OrderItem i : o.items) if (i.productId == id) { UI.showError(this, "This product is used in a pending order. Serve or cancel that order first."); return; }
        if (!UI.confirm(this, "Delete '" + p.name + "'?")) return;
        DataStore.products.remove(p);
        DataStore.log("Deleted product: " + p.name);
        DataStore.saveAll();
    }

    private void form(Product edit) {
        boolean isNew = edit == null;
        Product p = isNew ? new Product() : edit;
        UI.TField name = new UI.TField(isNew ? "" : p.name, 20);
        JComboBox<String> cat = UI.combo(CATS);
        JComboBox<String> season = UI.combo(SEASONS);
        UI.TField price = new UI.TField(isNew ? "" : String.valueOf((long) p.price), 10);
        UI.TField qty = new UI.TField(isNew ? "0" : String.valueOf(p.qty), 10);
        UI.TField min = new UI.TField(isNew ? "10" : String.valueOf(p.minQty), 10);
        UI.TField exp = new UI.TField(isNew ? "" : p.expiry, 10);
        exp.setToolTipText("Format: yyyy-MM-dd (can be left empty)");
        Supplier none = new Supplier(); none.id = 0; none.name = "(None)";
        Vector<Supplier> sv = new Vector<>(); sv.add(none); sv.addAll(DataStore.suppliers);
        JComboBox<Supplier> sup = UI.combo(sv);
        if (!isNew) { cat.setSelectedItem(p.category); season.setSelectedItem(p.season); sup.setSelectedItem(DataStore.supplier(p.supplierId) == null ? none : DataStore.supplier(p.supplierId)); }

        UI.Form f = new UI.Form(this, isNew ? "New Product" : "Edit Product")
                .add("Name", name).add("Category", cat).add("Price (Rs)", price).add("Quantity", qty)
                .add("Min Qty (alert)", min).add("Expiry (yyyy-MM-dd)", exp).add("Season", season).add("Supplier", sup);
        boolean ok = f.show(() -> {
            if (name.getText().trim().isEmpty()) return "Enter the product name.";
            try { if (UI.toDouble(price.getText()) < 0) return "Price cannot be negative."; } catch (Exception e) { return "Enter the price as a valid number."; }
            try { if (UI.toInt(qty.getText()) < 0) return "Quantity cannot be negative."; } catch (Exception e) { return "Enter the quantity as a valid number."; }
            try { if (UI.toInt(min.getText()) < 0) return "Min quantity cannot be negative."; } catch (Exception e) { return "Enter the min quantity as a valid number."; }
            if (!UI.validDate(exp.getText())) return "The expiry date must use the format yyyy-MM-dd (e.g. 2027-03-15).";
            return null;
        }, "Save");
        if (!ok) return;
        p.name = name.getText().trim(); p.category = (String) cat.getSelectedItem(); p.season = (String) season.getSelectedItem();
        p.price = UI.toDouble(price.getText()); p.qty = UI.toInt(qty.getText()); p.minQty = UI.toInt(min.getText());
        p.expiry = exp.getText().trim(); p.supplierId = ((Supplier) sup.getSelectedItem()).id;
        if (isNew) { p.id = DataStore.nextProductId(); DataStore.products.add(p); }
        DataStore.log((isNew ? "New product: " : "Updated product: ") + p.name);
        DataStore.saveAll();
    }
}
