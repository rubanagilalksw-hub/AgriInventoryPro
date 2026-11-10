package agri;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;
import java.util.List;
import agri.Models.*;

/** Sales (daily/monthly), best selling, stock alerts and farmer credit reports. The sales and best-selling reports use SQL queries. */
public class ReportsPage extends Page {
    private final JComboBox<String> period = UI.combo(new String[]{"Daily (last 14 days)", "Monthly (last 6 months)"});
    private final Charts.BarChart salesChart = new Charts.BarChart(Theme.PRIMARY);
    private final DefaultTableModel salesM = UI.model("Period", "Orders", "Sales (Rs)", "Collected (Rs)", "Credit (Rs)");
    private final Charts.PieChart bestPie = new Charts.PieChart();
    private final DefaultTableModel bestM = UI.model("Rank", "Product", "Qty Sold", "Revenue (Rs)");
    private final DefaultTableModel alertM = UI.model("Product", "Category", "Qty", "Min Qty", "Expiry", "Status");
    private final DefaultTableModel udM = UI.model("ID", "Farmer", "Village", "Phone", "Credit (Rs)");
    private final Charts.BarChart udChart = new Charts.BarChart(Theme.WARN);
    private final JLabel udTotal = UI.label("", 15, true, Theme.DANGER);
    private final UI.StyledTable alertT = new UI.StyledTable(alertM);

    public ReportsPage() {
        UI.Tabs tabs = new UI.Tabs();

        JPanel sales = UI.transparent(new BorderLayout(0, 12));
        JPanel bar = UI.transparent(new FlowLayout(FlowLayout.LEFT, 10, 0));
        bar.add(UI.label("Report:", 13, true, Theme.MUTED)); bar.add(period);
        UI.Btn exp = UI.info("Export CSV");
        exp.addActionListener(e -> Export.csv(this, salesM, "SalesReport"));
        bar.add(exp);
        period.addActionListener(e -> refresh());
        salesChart.setPreferredSize(new Dimension(400, 230));
        JPanel mid = UI.transparent(new GridLayout(2, 1, 0, 12));
        mid.add(Widgets.titled("Sales Chart (Rs)", salesChart)); mid.add(UI.scroll(new UI.StyledTable(salesM)));
        sales.add(bar, BorderLayout.NORTH); sales.add(mid, BorderLayout.CENTER);
        tabs.addTab("Sales Report", sales, null);

        JPanel best = UI.transparent(new GridLayout(1, 2, 14, 0));
        best.add(Widgets.titled("Top Products (by revenue)", bestPie)); best.add(UI.scroll(new UI.StyledTable(bestM)));
        tabs.addTab("Best Selling", best, null);

        alertT.rowColor = r -> {
            String s = String.valueOf(alertM.getValueAt(r, 5));
            return (s.equals(Logic.OUT) || s.equals(Logic.EXPIRED)) ? Theme.tint(Theme.DANGER) : Theme.tint(Theme.WARN);
        };
        tabs.addTab("Low Stock & Expiry", UI.scroll(alertT), null);

        JPanel ud = UI.transparent(new BorderLayout(0, 12));
        JPanel udMid = UI.transparent(new GridLayout(1, 2, 14, 0));
        udMid.add(UI.scroll(new UI.StyledTable(udM))); udMid.add(Widgets.titled("Highest outstanding credit (Rs)", udChart));
        ud.add(udTotal, BorderLayout.NORTH); ud.add(udMid, BorderLayout.CENTER);
        tabs.addTab("Farmer Credit", ud, null);

        add(tabs, BorderLayout.CENTER);
    }

    @Override public void refresh() {
        try {
            // ---- sales: SQL GROUP BY on the orders + order_items tables
            boolean daily = period.getSelectedIndex() == 0;
            int len = daily ? 10 : 7;                                   // substr length: yyyy-MM-dd or yyyy-MM
            LinkedHashMap<String, double[]> map = new LinkedHashMap<>(); // orders, sales, paid
            LocalDate today = LocalDate.now();
            if (daily) for (int i = 13; i >= 0; i--) map.put(today.minusDays(i).toString(), new double[3]);
            else for (int i = 5; i >= 0; i--) map.put(today.minusMonths(i).toString().substring(0, 7), new double[3]);
            List<String[]> rows = Db.query(
                "SELECT substr(o.order_date, 1, " + len + ") AS period, COUNT(*) AS orders, " +
                "SUM((SELECT COALESCE(SUM(oi.qty * oi.price), 0) FROM order_items oi WHERE oi.order_id = o.id) - o.discount) AS sales, " +
                "SUM(o.paid) AS paid FROM orders o WHERE o.status = 'COMPLETED' GROUP BY period ORDER BY period");
            for (int i = 1; i < rows.size(); i++) {
                double[] a = map.get(rows.get(i)[0]);
                if (a != null) { a[0] = Double.parseDouble(rows.get(i)[1]); a[1] = Double.parseDouble(rows.get(i)[2]); a[2] = Double.parseDouble(rows.get(i)[3]); }
            }
            salesM.setRowCount(0);
            List<String> lb = new ArrayList<>(); List<Double> vl = new ArrayList<>();
            for (Map.Entry<String, double[]> e : map.entrySet()) {
                double[] a = e.getValue();
                salesM.addRow(new Object[]{e.getKey(), (int) a[0], a[1], a[2], a[1] - a[2]});
                lb.add(daily ? e.getKey().substring(5) : e.getKey()); vl.add(a[1]);
            }
            salesChart.setData(lb, vl);

            // ---- best selling: SQL JOIN + GROUP BY
            List<String[]> best = Db.query(
                "SELECT p.name, SUM(oi.qty) AS qty, SUM(oi.qty * oi.price) AS revenue FROM order_items oi " +
                "JOIN orders o ON o.id = oi.order_id JOIN products p ON p.id = oi.product_id " +
                "WHERE o.status = 'COMPLETED' GROUP BY p.id, p.name ORDER BY revenue DESC");
            bestM.setRowCount(0);
            List<String> pl = new ArrayList<>(); List<Double> pv = new ArrayList<>();
            for (int i = 1; i < best.size(); i++) {
                String[] r = best.get(i);
                bestM.addRow(new Object[]{i, r[0], (int) Double.parseDouble(r[1]), Double.parseDouble(r[2])});
                if (i <= 7) { pl.add(r[0]); pv.add(Double.parseDouble(r[2])); }
            }
            bestPie.setData(pl, pv);
        } catch (SQLException ex) { ex.printStackTrace(); }

        // ---- alerts
        alertM.setRowCount(0);
        for (Product p : DataStore.products) if (Logic.isAlert(p))
            alertM.addRow(new Object[]{p.name, p.category, p.qty, p.minQty, p.expiry.isEmpty() ? "-" : p.expiry, Logic.status(p)});

        // ---- credit
        udM.setRowCount(0);
        List<Farmer> fs = new ArrayList<>();
        for (Farmer f : DataStore.farmers) if (f.credit > 0) fs.add(f);
        fs.sort((a, b) -> Double.compare(b.credit, a.credit));
        List<String> ul = new ArrayList<>(); List<Double> uv = new ArrayList<>();
        for (Farmer f : fs) {
            udM.addRow(new Object[]{f.id, f.name, f.village, f.phone, f.credit});
            if (ul.size() < 6) { ul.add(f.name.split(" ")[0]); uv.add(f.credit); }
        }
        udChart.setData(ul, uv);
        udTotal.setText("Total outstanding credit: " + UI.money(Logic.totalCredit()) + "   |   " + fs.size() + " farmers");
    }
}
