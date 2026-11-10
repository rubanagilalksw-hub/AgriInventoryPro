package agri;

import java.io.File;
import java.sql.*;
import java.util.*;

/**
 * DATABASE LAYER (JDBC + SQLite).
 *
 * All data lives in the relational database file  data/agri.db  (tables, primary keys, constraints).
 * No text files are used. The SQLite JDBC driver is bundled in lib/ so nothing has to be installed.
 *
 *   Tables: users, suppliers, products, farmers, orders, order_items, stock_log, activity_log,
 *           queue_ops, settings
 */
public class Db {
    public static final File DIR = new File(System.getProperty("user.dir"), "data");
    public static final File FILE = new File(DIR, "agri.db");
    private static Connection con;

    /** Opens (or creates) the database and makes sure all tables exist. Returns true when the DB was empty. */
    public static boolean open() throws SQLException {
        DIR.mkdirs();
        con = DriverManager.getConnection("jdbc:sqlite:" + FILE.getAbsolutePath());
        try (Statement st = con.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
            st.execute("PRAGMA journal_mode = WAL");
        }
        createSchema();
        try (Statement st = con.createStatement(); ResultSet r = st.executeQuery("SELECT COUNT(*) FROM users")) {
            return r.next() && r.getInt(1) == 0;
        }
    }

    public static Connection con() { return con; }

    public static void close() {
        try { if (con != null) con.close(); } catch (SQLException ignored) { }
        con = null;
    }

    private static void createSchema() throws SQLException {
        String[] ddl = {
            "CREATE TABLE IF NOT EXISTS users (" +
            " username TEXT PRIMARY KEY, pass_hash TEXT NOT NULL, role TEXT NOT NULL CHECK(role IN ('ADMIN','STAFF')))",

            "CREATE TABLE IF NOT EXISTS suppliers (" +
            " id INTEGER PRIMARY KEY, name TEXT NOT NULL, phone TEXT, address TEXT, pending REAL NOT NULL DEFAULT 0)",

            "CREATE TABLE IF NOT EXISTS products (" +
            " id INTEGER PRIMARY KEY, name TEXT NOT NULL, category TEXT NOT NULL, price REAL NOT NULL CHECK(price >= 0)," +
            " qty INTEGER NOT NULL CHECK(qty >= 0), min_qty INTEGER NOT NULL DEFAULT 0, expiry TEXT, season TEXT," +
            " supplier_id INTEGER)",

            "CREATE TABLE IF NOT EXISTS farmers (" +
            " id INTEGER PRIMARY KEY, name TEXT NOT NULL, phone TEXT, village TEXT, cnic TEXT, credit REAL NOT NULL DEFAULT 0)",

            "CREATE TABLE IF NOT EXISTS orders (" +
            " id INTEGER PRIMARY KEY, farmer_id INTEGER NOT NULL, order_date TEXT NOT NULL, priority INTEGER NOT NULL," +
            " status TEXT NOT NULL CHECK(status IN ('PENDING','COMPLETED','CANCELLED')), discount REAL NOT NULL DEFAULT 0," +
            " paid REAL NOT NULL DEFAULT 0, processed_by TEXT)",

            "CREATE TABLE IF NOT EXISTS order_items (" +
            " order_id INTEGER NOT NULL, product_id INTEGER NOT NULL, qty INTEGER NOT NULL, price REAL NOT NULL," +
            " PRIMARY KEY(order_id, product_id), FOREIGN KEY(order_id) REFERENCES orders(id) ON DELETE CASCADE)",

            "CREATE TABLE IF NOT EXISTS stock_log (" +
            " id INTEGER PRIMARY KEY, product_id INTEGER NOT NULL, type TEXT NOT NULL CHECK(type IN ('IN','OUT'))," +
            " qty INTEGER NOT NULL, log_date TEXT NOT NULL, note TEXT, supplier_id INTEGER, cost REAL NOT NULL DEFAULT 0)",

            "CREATE TABLE IF NOT EXISTS activity_log (" +
            " id INTEGER PRIMARY KEY AUTOINCREMENT, log_time TEXT NOT NULL, username TEXT, action TEXT NOT NULL)",

            "CREATE TABLE IF NOT EXISTS queue_ops (" +
            " id INTEGER PRIMARY KEY AUTOINCREMENT, op_time TEXT NOT NULL, operation TEXT NOT NULL, order_id INTEGER," +
            " queue_size INTEGER NOT NULL, front_idx INTEGER, rear_idx INTEGER, details TEXT)",

            "CREATE TABLE IF NOT EXISTS settings (key TEXT PRIMARY KEY, value TEXT)",

            "CREATE INDEX IF NOT EXISTS idx_orders_status ON orders(status)",
            "CREATE INDEX IF NOT EXISTS idx_items_product ON order_items(product_id)"
        };
        try (Statement st = con.createStatement()) { for (String s : ddl) st.execute(s); }
    }

    // ------------------------------------------------------------------ generic helpers
    /** Runs a SELECT and returns the column names (first element) + rows as strings (for tables / viewers). */
    public static List<String[]> query(String sql, Object... args) throws SQLException {
        List<String[]> out = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 0; i < args.length; i++) ps.setObject(i + 1, args[i]);
            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData md = rs.getMetaData();
                int n = md.getColumnCount();
                String[] head = new String[n];
                for (int i = 0; i < n; i++) head[i] = md.getColumnLabel(i + 1);
                out.add(head);
                while (rs.next()) {
                    String[] row = new String[n];
                    for (int i = 0; i < n; i++) row[i] = rs.getString(i + 1) == null ? "" : rs.getString(i + 1);
                    out.add(row);
                }
            }
        }
        return out;
    }

    public static int count(String table) {
        try (Statement st = con.createStatement(); ResultSet r = st.executeQuery("SELECT COUNT(*) FROM " + table)) {
            return r.next() ? r.getInt(1) : 0;
        } catch (SQLException e) { return -1; }
    }

    public static List<String> tableNames() {
        List<String> l = new ArrayList<>();
        try (Statement st = con.createStatement();
             ResultSet r = st.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' ORDER BY name")) {
            while (r.next()) l.add(r.getString(1));
        } catch (SQLException ignored) { }
        return l;
    }

    public static String version() {
        try (Statement st = con.createStatement(); ResultSet r = st.executeQuery("SELECT sqlite_version()")) {
            return r.next() ? r.getString(1) : "?";
        } catch (SQLException e) { return "?"; }
    }

    // ------------------------------------------------------------------ settings
    public static void saveSettings(Properties p) {
        try (PreparedStatement ps = con.prepareStatement("INSERT INTO settings(key,value) VALUES(?,?) ON CONFLICT(key) DO UPDATE SET value=excluded.value")) {
            for (String k : p.stringPropertyNames()) { ps.setString(1, k); ps.setString(2, p.getProperty(k)); ps.addBatch(); }
            ps.executeBatch();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public static Properties loadSettings() {
        Properties p = new Properties();
        try (Statement st = con.createStatement(); ResultSet r = st.executeQuery("SELECT key, value FROM settings")) {
            while (r.next()) p.setProperty(r.getString(1), r.getString(2));
        } catch (SQLException e) { e.printStackTrace(); }
        return p;
    }

    // ------------------------------------------------------------------ queue operation history
    public static void logQueueOp(String op, int orderId, int size, int front, int rear, String details) {
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO queue_ops(op_time, operation, order_id, queue_size, front_idx, rear_idx, details) VALUES(?,?,?,?,?,?,?)")) {
            ps.setString(1, DataStore.now()); ps.setString(2, op);
            if (orderId > 0) ps.setInt(3, orderId); else ps.setNull(3, Types.INTEGER);
            ps.setInt(4, size); ps.setInt(5, front); ps.setInt(6, rear); ps.setString(7, details);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }
}
