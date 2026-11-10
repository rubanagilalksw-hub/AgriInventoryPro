package agri;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import agri.Models.*;

/** Stock status, expiry, seasons, queue wait-time estimate and the Smart Reorder forecast. */
public class Logic {
    public static final String OUT = "Out of stock", LOW = "Low stock", EXPIRING = "Expiring soon", EXPIRED = "Expired", OK = "OK";
    public static final int EXPIRY_WARN_DAYS = 30;

    /** Days left until expiry (Long.MAX_VALUE when the product has no expiry date). */
    public static long daysLeft(Product p) {
        if (p.expiry == null || p.expiry.trim().isEmpty()) return Long.MAX_VALUE;
        try { return ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.parse(p.expiry.trim())); }
        catch (Exception e) { return Long.MAX_VALUE; }
    }

    public static String status(Product p) {
        long d = daysLeft(p);
        if (d < 0) return EXPIRED;
        if (p.qty <= 0) return OUT;
        if (p.qty <= p.minQty) return LOW;
        if (d <= EXPIRY_WARN_DAYS) return EXPIRING;
        return OK;
    }

    public static boolean isAlert(Product p) { return !OK.equals(status(p)); }

    // ---------------- seasons
    public static String currentSeason() {
        int m = LocalDate.now().getMonthValue();
        return (m >= 5 && m <= 10) ? "Kharif" : "Rabi";
    }
    public static String nextSeason() { return "Rabi".equals(currentSeason()) ? "Kharif" : "Rabi"; }

    /** Days until the next season starts. */
    public static long daysToNextSeason() {
        LocalDate t = LocalDate.now();
        LocalDate start = "Rabi".equals(currentSeason()) ? LocalDate.of(t.getYear(), 5, 1) : LocalDate.of(t.getYear(), 11, 1);
        if (!start.isAfter(t)) start = start.plusYears(1);
        return ChronoUnit.DAYS.between(t, start);
    }

    public static String seasonRange(String s) { return "Rabi".equals(s) ? "Nov - Apr" : "Kharif".equals(s) ? "May - Oct" : "All year"; }

    // ---------------- sales helpers
    public static List<Order> completed() {
        List<Order> l = new ArrayList<>();
        for (Order o : DataStore.orders) if ("COMPLETED".equals(o.status)) l.add(o);
        return l;
    }

    public static double totalSales() { double t = 0; for (Order o : completed()) t += o.total(); return t; }
    public static double totalCredit() { double t = 0; for (Farmer f : DataStore.farmers) t += f.credit; return t; }
    public static int alertCount() { int n = 0; for (Product p : DataStore.products) if (isAlert(p)) n++; return n; }

    public static double farmerPurchases(int farmerId) {
        double t = 0;
        for (Order o : completed()) if (o.farmerId == farmerId) t += o.total();
        return t;
    }

    public static String itemsSummary(Order o) {
        StringBuilder sb = new StringBuilder();
        for (OrderItem i : o.items) { if (sb.length() > 0) sb.append(", "); sb.append(DataStore.productName(i.productId)).append(" x").append(i.qty); }
        return sb.toString();
    }

    // ---------------- queue: estimated service time of one order (3 min + 1.5 min per extra item)
    public static double serviceMinutes(Order o) { return 3 + 1.5 * Math.max(0, o.items.size() - 1); }

    // ---------------- UNIQUE FEATURE 1: Smart Reorder Advisor
    public static final int LEAD_DAYS = 14, SAFETY_DAYS = 7, HISTORY_DAYS = 30;

    public static class Forecast {
        public Product product;
        public int sold30;            // units sold in the last 30 days (from the database)
        public double perDay;         // average daily demand
        public double seasonBoost;    // 1.0 or 1.5 when the product's season starts soon
        public double daysLeft;       // days the current stock will last (999 = no demand)
        public int reorderQty;        // suggested quantity to order now
        public String urgency;        // CRITICAL / HIGH / MEDIUM / OK
    }

    /** Uses an SQL GROUP BY query on order_items + orders to measure demand, then predicts stock-outs. */
    public static List<Forecast> forecasts() {
        Map<Integer, Integer> sold = new HashMap<>();
        String since = LocalDate.now().minusDays(HISTORY_DAYS).toString();
        try {
            List<String[]> rows = Db.query(
                "SELECT oi.product_id, SUM(oi.qty) FROM order_items oi JOIN orders o ON o.id = oi.order_id " +
                "WHERE o.status = 'COMPLETED' AND substr(o.order_date, 1, 10) >= ? GROUP BY oi.product_id", since);
            for (int i = 1; i < rows.size(); i++) sold.put(Integer.parseInt(rows.get(i)[0]), Integer.parseInt(rows.get(i)[1]));
        } catch (SQLException e) { e.printStackTrace(); }

        String next = nextSeason();
        boolean seasonSoon = daysToNextSeason() <= 30;
        List<Forecast> out = new ArrayList<>();
        for (Product p : DataStore.products) {
            Forecast f = new Forecast();
            f.product = p;
            f.sold30 = sold.getOrDefault(p.id, 0);
            f.seasonBoost = (seasonSoon && next.equals(p.season)) ? 1.5 : 1.0;
            f.perDay = f.sold30 / (double) HISTORY_DAYS * f.seasonBoost;
            f.daysLeft = f.perDay <= 0 ? 999 : p.qty / f.perDay;
            double target = f.perDay * (LEAD_DAYS + SAFETY_DAYS);
            f.reorderQty = (int) Math.max(0, Math.ceil(Math.max(target, p.minQty * 2.0) - p.qty));
            if (p.qty <= 0 || f.daysLeft < 7) f.urgency = "CRITICAL";
            else if (f.daysLeft < LEAD_DAYS) f.urgency = "HIGH";
            else if (f.daysLeft < 21 || p.qty <= p.minQty) f.urgency = "MEDIUM";
            else f.urgency = "OK";
            out.add(f);
        }
        out.sort((a, b) -> {
            int c = Integer.compare(rank(a.urgency), rank(b.urgency));
            return c != 0 ? c : Double.compare(a.daysLeft, b.daysLeft);
        });
        return out;
    }

    private static int rank(String u) { return "CRITICAL".equals(u) ? 0 : "HIGH".equals(u) ? 1 : "MEDIUM".equals(u) ? 2 : 3; }

    // ---------------- UNIQUE FEATURE 2: farmer credit risk score
    public static class Risk { public Farmer farmer; public double purchases, ratio; public int score; public String level; public long daysSince; }

    public static List<Risk> creditRisks() {
        List<Risk> out = new ArrayList<>();
        for (Farmer f : DataStore.farmers) {
            Risk r = new Risk();
            r.farmer = f;
            r.purchases = farmerPurchases(f.id);
            r.ratio = r.purchases <= 0 ? (f.credit > 0 ? 1 : 0) : Math.min(1.0, f.credit / r.purchases);
            String last = "";
            for (Order o : completed()) if (o.farmerId == f.id && o.date.compareTo(last) > 0) last = o.date;
            r.daysSince = last.isEmpty() ? 999 : ChronoUnit.DAYS.between(LocalDate.parse(last.substring(0, 10)), LocalDate.now());
            double amountPart = Math.min(1.0, f.credit / 150000.0);                     // size of the debt
            double agePart = Math.min(1.0, r.daysSince / 60.0);                         // how long since the last purchase
            r.score = (int) Math.round(100 * (0.5 * r.ratio + 0.35 * amountPart + 0.15 * agePart));
            r.level = f.credit <= 0 ? "LOW" : r.score >= 55 ? "HIGH" : r.score >= 30 ? "MEDIUM" : "LOW";
            out.add(r);
        }
        out.sort((a, b) -> Integer.compare(b.score, a.score));
        return out;
    }
}
