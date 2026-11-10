package agri;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.util.*;
import java.util.List;
import agri.Models.*;

/** Overview: stat cards, charts, alerts and recent orders. */
public class DashboardPage extends Page {
    private final Widgets.StatCard cProd = new Widgets.StatCard("PRODUCTS", Theme.PRIMARY, "products"),
            cSales = new Widgets.StatCard("TOTAL SALES", Theme.SUCCESS, "money"),
            cPend = new Widgets.StatCard("PENDING", Theme.INFO, "queue"),
            cAlert = new Widgets.StatCard("ALERTS", Theme.DANGER, "alert"),
            cFarm = new Widgets.StatCard("FARMERS", Theme.WARN, "farmers"),
            cUd = new Widgets.StatCard("CREDIT", Color.decode("#8B5CF6"), "orders");
    private final Charts.BarChart bar = new Charts.BarChart(Theme.PRIMARY);
    private final Charts.PieChart pie = new Charts.PieChart();
    private final JPanel alerts = UI.transparent(new GridLayout(0, 1, 0, 6));
    private final DefaultTableModel recentM = UI.model("Order #", "Date", "Farmer", "Total (Rs)");

    public DashboardPage() {
        JPanel cards = UI.transparent(new GridLayout(1, 6, 12, 12));
        cards.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override public void componentResized(java.awt.event.ComponentEvent e) {
                int cols = cards.getWidth() < 940 ? 3 : 6;
                if (((GridLayout) cards.getLayout()).getColumns() != cols) { cards.setLayout(new GridLayout(0, cols, 12, 12)); cards.revalidate(); }
            }
        });
        cards.add(cProd); cards.add(cSales); cards.add(cPend); cards.add(cAlert); cards.add(cFarm); cards.add(cUd);

        JPanel mid = UI.transparent(new GridLayout(1, 2, 14, 0));
        mid.add(Widgets.titled("Sales - last 7 days (Rs)", bar));
        mid.add(Widgets.titled("Category-wise Stock Value", pie));

        JPanel al = UI.transparent(new BorderLayout()); al.add(alerts, BorderLayout.NORTH);
        JPanel low = UI.transparent(new GridLayout(1, 2, 14, 0));
        low.add(Widgets.titled("Alerts (needs attention)", UI.plainScroll(al)));
        low.add(Widgets.titled("Recent Orders", UI.scroll(new UI.StyledTable(recentM))));

        JPanel center = UI.transparent(new GridLayout(2, 1, 0, 14));
        center.add(mid); center.add(low);
        add(cards, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);
    }

    private JLabel alertLine(String text, Color c) {
        JLabel l = UI.label(text, 13, false, Theme.TEXT);
        l.setIconTextGap(6);
        l.setForeground(Theme.TEXT);
        l.setBorder(BorderFactory.createMatteBorder(0, 3, 0, 0, c));
        l.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 3, 0, 0, c), BorderFactory.createEmptyBorder(2, 8, 2, 0)));
        return l;
    }

    @Override public void refresh() {
        cProd.setValue(String.valueOf(DataStore.products.size()));
        cSales.setValue(UI.money(Logic.totalSales()));
        cPend.setValue(String.valueOf(DataStore.queue.size()));
        cAlert.setValue(String.valueOf(Logic.alertCount()));
        cFarm.setValue(String.valueOf(DataStore.farmers.size()));
        cUd.setValue(UI.money(Logic.totalCredit()));

        // 7 day sales
        LinkedHashMap<String, Double> days = new LinkedHashMap<>();
        for (int i = 6; i >= 0; i--) days.put(LocalDate.now().minusDays(i).toString(), 0.0);
        for (Order o : Logic.completed()) { String k = o.date.substring(0, 10); if (days.containsKey(k)) days.put(k, days.get(k) + o.total()); }
        List<String> l = new ArrayList<>(); List<Double> v = new ArrayList<>();
        for (Map.Entry<String, Double> e : days.entrySet()) { l.add(e.getKey().substring(5)); v.add(e.getValue()); }
        bar.setData(l, v);

        // category stock value
        LinkedHashMap<String, Double> cat = new LinkedHashMap<>();
        for (Product p : DataStore.products) cat.merge(p.category, p.qty * p.price, Double::sum);
        pie.setData(new ArrayList<>(cat.keySet()), new ArrayList<>(cat.values()));

        // alerts
        alerts.removeAll();
        int n = 0;
        for (Product p : DataStore.products) {
            String s = Logic.status(p);
            if (s.equals(Logic.OK)) continue;
            Color c = (s.equals(Logic.OUT) || s.equals(Logic.EXPIRED)) ? Theme.DANGER : Theme.WARN;
            String extra = s.equals(Logic.EXPIRING) ? " (" + Logic.daysLeft(p) + " days left)" : s.equals(Logic.LOW) ? " (only " + p.qty + " left)" : "";
            alerts.add(alertLine(p.name + "  -  " + s + extra, c));
            if (++n >= 9) break;
        }
        if (n == 0) alerts.add(alertLine("Everything is fine - no alerts", Theme.SUCCESS));
        if (Logic.totalCredit() > 0) alerts.add(alertLine("Credit outstanding in the market: " + UI.money(Logic.totalCredit()), Theme.INFO));
        alerts.revalidate(); alerts.repaint();

        recentM.setRowCount(0);
        int c = 0;
        for (int i = DataStore.orders.size() - 1; i >= 0 && c < 12; i--, c++) {
            Order o = DataStore.orders.get(i);
            recentM.addRow(new Object[]{o.id, o.date.substring(5, 10), DataStore.farmerName(o.farmerId), o.total()});
        }
    }
}
