package agri;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.*;
import java.util.List;
import agri.Models.*;

/**
 * UNIQUE FEATURES
 *  1. Smart Reorder Advisor  - predicts when each product will run out (demand measured with an SQL query)
 *                              and suggests how much to order now.
 *  2. Farmer Credit Risk     - scores every farmer's credit (udhaar) so risky customers are visible.
 */
public class InsightsPage extends Page {
    private final Widgets.StatCard cCrit = new Widgets.StatCard("CRITICAL ITEMS", Theme.DANGER),
            cHigh = new Widgets.StatCard("HIGH PRIORITY", Theme.WARN),
            cUnits = new Widgets.StatCard("UNITS TO ORDER", Theme.INFO),
            cCost = new Widgets.StatCard("ESTIMATED PURCHASE COST", Theme.PRIMARY);
    private final DefaultTableModel fm = UI.model("Product", "Stock", "Sold 30d", "Per Day", "Days Left", "Boost", "Reorder", "Cost (Rs)", "Supplier", "Urgency");
    private final UI.StyledTable ft = new UI.StyledTable(fm);
    private final DefaultTableModel rm = UI.model("Farmer", "Village", "Credit (Rs)", "Total Purchases (Rs)", "Credit / Purchases", "Days Since Last Purchase", "Risk Score", "Risk Level");
    private final UI.StyledTable rt = new UI.StyledTable(rm);
    private final Charts.BarChart riskChart = new Charts.BarChart(Theme.DANGER);

    public InsightsPage() {
        ft.rowColor = r -> {
            String u = String.valueOf(fm.getValueAt(r, 9));
            return "CRITICAL".equals(u) ? Theme.tint(Theme.DANGER) : "HIGH".equals(u) ? Theme.tint(Theme.WARN) : "MEDIUM".equals(u) ? Theme.tint(Theme.INFO) : null;
        };
        rt.rowColor = r -> {
            String u = String.valueOf(rm.getValueAt(r, 7));
            return "HIGH".equals(u) ? Theme.tint(Theme.DANGER) : "MEDIUM".equals(u) ? Theme.tint(Theme.WARN) : null;
        };

        JPanel cards = UI.transparent(new GridLayout(1, 4, 14, 0));
        cards.add(cCrit); cards.add(cHigh); cards.add(cUnits); cards.add(cCost);

        JPanel how = UI.transparent(new BorderLayout(10, 0));
        JLabel info = UI.label("<html>Demand = units sold in the last " + Logic.HISTORY_DAYS + " days / " + Logic.HISTORY_DAYS
                + ".  x1.5 if the product's season starts within 30 days.  Reorder = demand x (" + Logic.LEAD_DAYS + " delivery + "
                + Logic.SAFETY_DAYS + " safety days) - stock.</html>", 12, false, Theme.MUTED);
        UI.Btn exp = UI.info("Export CSV");
        exp.addActionListener(e -> Export.csv(this, fm, "SmartReorder"));
        how.add(info, BorderLayout.CENTER); how.add(exp, BorderLayout.EAST);
        JPanel reorder = UI.transparent(new BorderLayout(0, 10));
        reorder.add(how, BorderLayout.NORTH); reorder.add(UI.scroll(ft), BorderLayout.CENTER);

        JPanel risk = UI.transparent(new GridLayout(1, 2, 14, 0));
        risk.add(UI.scroll(rt));
        risk.add(Widgets.titled("Credit risk score (0-100, higher = riskier)", riskChart));

        UI.Tabs tabs = new UI.Tabs();
        tabs.addTab("Smart Reorder Advisor", reorder, null);
        tabs.addTab("Farmer Credit Risk", risk, null);
        add(cards, BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);
    }

    @Override public void refresh() {
        fm.setRowCount(0);
        int crit = 0, high = 0, units = 0;
        double cost = 0;
        for (Logic.Forecast f : Logic.forecasts()) {
            Product p = f.product;
            double c = f.reorderQty * p.price * 0.85;           // estimated buying price = 85% of the selling price
            if ("CRITICAL".equals(f.urgency)) crit++;
            if ("HIGH".equals(f.urgency)) high++;
            units += f.reorderQty; cost += c;
            fm.addRow(new Object[]{p.name, p.qty, f.sold30, Math.round(f.perDay * 100) / 100.0,
                    f.daysLeft >= 999 ? "no demand" : String.valueOf(Math.round(f.daysLeft)), f.seasonBoost > 1 ? "x1.5" : "-",
                    f.reorderQty, Math.round(c), DataStore.supplierName(p.supplierId), f.urgency});
        }
        cCrit.setValue(String.valueOf(crit)); cHigh.setValue(String.valueOf(high));
        cUnits.setValue(String.valueOf(units)); cCost.setValue(UI.money(cost));

        rm.setRowCount(0);
        List<String> lb = new ArrayList<>(); List<Double> vl = new ArrayList<>();
        for (Logic.Risk r : Logic.creditRisks()) {
            rm.addRow(new Object[]{r.farmer.name, r.farmer.village, r.farmer.credit, r.purchases, Math.round(r.ratio * 100) + "%",
                    r.daysSince >= 999 ? "never" : String.valueOf(r.daysSince), r.score, r.level});
            lb.add(r.farmer.name.split(" ")[0]); vl.add((double) r.score);
        }
        riskChart.setData(lb, vl);
    }
}
