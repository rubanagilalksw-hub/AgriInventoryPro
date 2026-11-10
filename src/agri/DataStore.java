package agri;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import agri.Models.*;

/**
 * Central data store.
 *
 * The lists below are an in-memory cache of the database tables (fast for the UI).
 * Every change is written to the SQLite database through JDBC (see Db.java):
 *   - load()    : SELECT every table into the lists
 *   - saveAll() : one transaction that INSERT/UPDATEs changed rows and DELETEs removed rows
 */
public class DataStore {

    public static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static List<User> users = new ArrayList<>();
    public static List<Product> products = new ArrayList<>();
    public static List<Farmer> farmers = new ArrayList<>();
    public static List<Supplier> suppliers = new ArrayList<>();
    public static List<Order> orders = new ArrayList<>();
    public static List<StockLog> stockLogs = new ArrayList<>();
    public static List<Activity> activities = new ArrayList<>();
    public static Properties settings = new Properties();
    public static OrderQueue queue = new OrderQueue();
    public static User current;

    // ---------------------------------------------------------------- helpers
    public static String now() { return LocalDateTime.now().format(DT); }
    public static String today() { return LocalDate.now().toString(); }

    public static String hash(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            StringBuilder sb = new StringBuilder();
            for (byte b : md.digest(s.getBytes(StandardCharsets.UTF_8))) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) { return s; }
    }

    private static String nz(String s) { return s == null ? "" : s; }

    // ---------------------------------------------------------------- load (SELECT)
    public static void load() {
        try {
            boolean empty = Db.open();
            if (empty) seedAndSave();
            readAll();
        } catch (SQLException e) {
            throw new RuntimeException("Database error: " + e.getMessage(), e);
        }
    }

    private static void readAll() throws SQLException {
        users.clear(); products.clear(); farmers.clear(); suppliers.clear();
        orders.clear(); stockLogs.clear(); activities.clear();
        Connection c = Db.con();
        try (Statement st = c.createStatement()) {
            ResultSet r = st.executeQuery("SELECT username, pass_hash, role FROM users ORDER BY rowid");
            while (r.next()) users.add(new User(r.getString(1), r.getString(2), r.getString(3)));

            r = st.executeQuery("SELECT id, name, phone, address, pending FROM suppliers ORDER BY id");
            while (r.next()) {
                Supplier s = new Supplier();
                s.id = r.getInt(1); s.name = r.getString(2); s.phone = nz(r.getString(3)); s.address = nz(r.getString(4)); s.pending = r.getDouble(5);
                suppliers.add(s);
            }
            r = st.executeQuery("SELECT id, name, category, price, qty, min_qty, expiry, season, supplier_id FROM products ORDER BY id");
            while (r.next()) {
                Product p = new Product();
                p.id = r.getInt(1); p.name = r.getString(2); p.category = r.getString(3); p.price = r.getDouble(4); p.qty = r.getInt(5);
                p.minQty = r.getInt(6); p.expiry = nz(r.getString(7)); p.season = nz(r.getString(8)); p.supplierId = r.getInt(9);
                products.add(p);
            }
            r = st.executeQuery("SELECT id, name, phone, village, cnic, credit FROM farmers ORDER BY id");
            while (r.next()) {
                Farmer f = new Farmer();
                f.id = r.getInt(1); f.name = r.getString(2); f.phone = nz(r.getString(3)); f.village = nz(r.getString(4));
                f.cnic = nz(r.getString(5)); f.credit = r.getDouble(6);
                farmers.add(f);
            }
            Map<Integer, Order> byId = new HashMap<>();
            r = st.executeQuery("SELECT id, farmer_id, order_date, priority, status, discount, paid, processed_by FROM orders ORDER BY id");
            while (r.next()) {
                Order o = new Order();
                o.id = r.getInt(1); o.farmerId = r.getInt(2); o.date = r.getString(3); o.priority = r.getInt(4); o.status = r.getString(5);
                o.discount = r.getDouble(6); o.paid = r.getDouble(7); o.processedBy = nz(r.getString(8));
                orders.add(o); byId.put(o.id, o);
            }
            r = st.executeQuery("SELECT order_id, product_id, qty, price FROM order_items ORDER BY order_id, rowid");
            while (r.next()) {
                Order o = byId.get(r.getInt(1));
                if (o != null) o.items.add(new OrderItem(r.getInt(2), r.getInt(3), r.getDouble(4)));
            }
            r = st.executeQuery("SELECT id, product_id, type, qty, log_date, note, supplier_id, cost FROM stock_log ORDER BY id");
            while (r.next()) {
                StockLog l = new StockLog();
                l.id = r.getInt(1); l.productId = r.getInt(2); l.type = r.getString(3); l.qty = r.getInt(4); l.date = r.getString(5);
                l.note = nz(r.getString(6)); l.supplierId = r.getInt(7); l.cost = r.getDouble(8);
                stockLogs.add(l);
            }
            r = st.executeQuery("SELECT log_time, username, action FROM activity_log ORDER BY id DESC LIMIT 500");
            while (r.next()) activities.add(new Activity(r.getString(1), nz(r.getString(2)), r.getString(3)));
        }
        settings = Db.loadSettings();
        rebuildQueue();
    }

    public static void rebuildQueue() {
        List<Order> pend = new ArrayList<>();
        for (Order o : orders) if ("PENDING".equals(o.status)) pend.add(o);
        pend.sort((a, b) -> Integer.compare(a.id, b.id));
        int cap = Math.max(OrderQueue.DEFAULT_CAPACITY, pend.size());
        queue = new OrderQueue(cap);
        queue.setPriorityMode("priority".equals(settings.getProperty("queueMode", "priority")));
        for (Order o : pend) queue.enqueue(o);
    }

    // ---------------------------------------------------------------- save (INSERT / UPDATE / DELETE)
    private static void deleteMissing(Connection c, String table, String keyCol, Set<String> keep) throws SQLException {
        List<String> existing = new ArrayList<>();
        try (Statement st = c.createStatement(); ResultSet r = st.executeQuery("SELECT " + keyCol + " FROM " + table)) {
            while (r.next()) existing.add(r.getString(1));
        }
        try (PreparedStatement ps = c.prepareStatement("DELETE FROM " + table + " WHERE " + keyCol + " = ?")) {
            for (String k : existing) if (!keep.contains(k)) { ps.setString(1, k); ps.addBatch(); }
            ps.executeBatch();
        }
    }

    /** Writes the whole in-memory state to the database inside ONE transaction (all or nothing). */
    public static synchronized void saveAll() {
        Connection c = Db.con();
        try {
            c.setAutoCommit(false);

            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO users(username, pass_hash, role) VALUES(?,?,?) ON CONFLICT(username) DO UPDATE SET pass_hash=excluded.pass_hash, role=excluded.role")) {
                for (User u : users) { ps.setString(1, u.username); ps.setString(2, u.passHash); ps.setString(3, u.role); ps.addBatch(); }
                ps.executeBatch();
            }
            Set<String> keep = new HashSet<>();
            for (User u : users) keep.add(u.username);
            deleteMissing(c, "users", "username", keep);

            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO suppliers(id, name, phone, address, pending) VALUES(?,?,?,?,?) ON CONFLICT(id) DO UPDATE SET " +
                    "name=excluded.name, phone=excluded.phone, address=excluded.address, pending=excluded.pending")) {
                for (Supplier s : suppliers) {
                    ps.setInt(1, s.id); ps.setString(2, s.name); ps.setString(3, s.phone); ps.setString(4, s.address); ps.setDouble(5, s.pending);
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            keep = new HashSet<>();
            for (Supplier s : suppliers) keep.add(String.valueOf(s.id));
            deleteMissing(c, "suppliers", "id", keep);

            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO products(id, name, category, price, qty, min_qty, expiry, season, supplier_id) VALUES(?,?,?,?,?,?,?,?,?) " +
                    "ON CONFLICT(id) DO UPDATE SET name=excluded.name, category=excluded.category, price=excluded.price, qty=excluded.qty, " +
                    "min_qty=excluded.min_qty, expiry=excluded.expiry, season=excluded.season, supplier_id=excluded.supplier_id")) {
                for (Product p : products) {
                    ps.setInt(1, p.id); ps.setString(2, p.name); ps.setString(3, p.category); ps.setDouble(4, p.price); ps.setInt(5, p.qty);
                    ps.setInt(6, p.minQty); ps.setString(7, p.expiry); ps.setString(8, p.season); ps.setInt(9, p.supplierId);
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            keep = new HashSet<>();
            for (Product p : products) keep.add(String.valueOf(p.id));
            deleteMissing(c, "products", "id", keep);

            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO farmers(id, name, phone, village, cnic, credit) VALUES(?,?,?,?,?,?) ON CONFLICT(id) DO UPDATE SET " +
                    "name=excluded.name, phone=excluded.phone, village=excluded.village, cnic=excluded.cnic, credit=excluded.credit")) {
                for (Farmer f : farmers) {
                    ps.setInt(1, f.id); ps.setString(2, f.name); ps.setString(3, f.phone); ps.setString(4, f.village); ps.setString(5, f.cnic); ps.setDouble(6, f.credit);
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            keep = new HashSet<>();
            for (Farmer f : farmers) keep.add(String.valueOf(f.id));
            deleteMissing(c, "farmers", "id", keep);

            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO orders(id, farmer_id, order_date, priority, status, discount, paid, processed_by) VALUES(?,?,?,?,?,?,?,?) " +
                    "ON CONFLICT(id) DO UPDATE SET farmer_id=excluded.farmer_id, order_date=excluded.order_date, priority=excluded.priority, " +
                    "status=excluded.status, discount=excluded.discount, paid=excluded.paid, processed_by=excluded.processed_by")) {
                for (Order o : orders) {
                    ps.setInt(1, o.id); ps.setInt(2, o.farmerId); ps.setString(3, o.date); ps.setInt(4, o.priority); ps.setString(5, o.status);
                    ps.setDouble(6, o.discount); ps.setDouble(7, o.paid); ps.setString(8, o.processedBy);
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            keep = new HashSet<>();
            for (Order o : orders) keep.add(String.valueOf(o.id));
            deleteMissing(c, "orders", "id", keep);          // order_items are removed by ON DELETE CASCADE
            try (Statement st = c.createStatement()) { st.execute("DELETE FROM order_items"); }
            try (PreparedStatement ps = c.prepareStatement("INSERT INTO order_items(order_id, product_id, qty, price) VALUES(?,?,?,?)")) {
                for (Order o : orders) for (OrderItem i : o.items) {
                    ps.setInt(1, o.id); ps.setInt(2, i.productId); ps.setInt(3, i.qty); ps.setDouble(4, i.price);
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT OR IGNORE INTO stock_log(id, product_id, type, qty, log_date, note, supplier_id, cost) VALUES(?,?,?,?,?,?,?,?)")) {
                for (StockLog l : stockLogs) {
                    ps.setInt(1, l.id); ps.setInt(2, l.productId); ps.setString(3, l.type); ps.setInt(4, l.qty); ps.setString(5, l.date);
                    ps.setString(6, l.note); ps.setInt(7, l.supplierId); ps.setDouble(8, l.cost);
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            Db.saveSettings(settings);
            c.commit();
        } catch (SQLException e) {
            try { c.rollback(); } catch (SQLException ignored) { }
            e.printStackTrace();
        } finally {
            try { c.setAutoCommit(true); } catch (SQLException ignored) { }
        }
    }

    public static void saveSettings() { Db.saveSettings(settings); }

    // ---------------------------------------------------------------- lookups
    public static Product product(int id) { for (Product p : products) if (p.id == id) return p; return null; }
    public static Farmer farmer(int id) { for (Farmer f : farmers) if (f.id == id) return f; return null; }
    public static Supplier supplier(int id) { for (Supplier s : suppliers) if (s.id == id) return s; return null; }
    public static Order order(int id) { for (Order o : orders) if (o.id == id) return o; return null; }
    public static String productName(int id) { Product p = product(id); return p == null ? "(deleted)" : p.name; }
    public static String farmerName(int id) { Farmer f = farmer(id); return f == null ? "(deleted)" : f.name; }
    public static String supplierName(int id) { Supplier s = supplier(id); return s == null ? "-" : s.name; }

    public static int nextProductId() { int m = 0; for (Product x : products) m = Math.max(m, x.id); return m + 1; }
    public static int nextFarmerId() { int m = 0; for (Farmer x : farmers) m = Math.max(m, x.id); return m + 1; }
    public static int nextSupplierId() { int m = 0; for (Supplier x : suppliers) m = Math.max(m, x.id); return m + 1; }
    public static int nextOrderId() { int m = 1000; for (Order x : orders) m = Math.max(m, x.id); return m + 1; }
    public static int nextLogId() { int m = 0; for (StockLog x : stockLogs) m = Math.max(m, x.id); return m + 1; }

    // ---------------------------------------------------------------- activity log + queue history (INSERT right away)
    public static void log(String action) {
        String user = current == null ? "system" : current.username;
        activities.add(0, new Activity(now(), user, action));
        if (activities.size() > 500) activities.remove(activities.size() - 1);
        try (PreparedStatement ps = Db.con().prepareStatement("INSERT INTO activity_log(log_time, username, action) VALUES(?,?,?)")) {
            ps.setString(1, now()); ps.setString(2, user); ps.setString(3, action);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    /** Records one queue operation (enqueue / dequeue / peek / ...) in the queue_ops table. */
    public static void logQueue(String op, int orderId, String details) {
        Db.logQueueOp(op, orderId, queue.size(), queue.frontIndex(), queue.rearIndex(), details);
    }

    // ---------------------------------------------------------------- authentication
    public static User login(String u, String p) {
        for (User x : users) if (x.username.equalsIgnoreCase(u.trim()) && x.passHash.equals(hash(p))) return x;
        return null;
    }

    public static String changePassword(String oldP, String newP) {
        if (!current.passHash.equals(hash(oldP))) return "The old password is incorrect.";
        if (newP.length() < 4) return "The new password must be at least 4 characters long.";
        current.passHash = hash(newP);
        log("Changed password");
        saveAll();
        return null;
    }

    public static String addUser(String u, String p, String role) {
        if (u.trim().isEmpty() || p.length() < 4) return "Enter a username and a password of at least 4 characters.";
        for (User x : users) if (x.username.equalsIgnoreCase(u.trim())) return "This username already exists.";
        users.add(new User(u.trim(), hash(p), role));
        log("Created user: " + u.trim() + " (" + role + ")");
        saveAll();
        return null;
    }

    // ---------------------------------------------------------------- business logic
    /** Stock In: goods received from a supplier. */
    public static void addStock(Product p, int qty, Supplier sup, double cost, double paidNow, String note) {
        p.qty += qty;
        StockLog l = new StockLog();
        l.id = nextLogId(); l.productId = p.id; l.type = "IN"; l.qty = qty; l.date = now();
        l.note = note; l.supplierId = sup == null ? 0 : sup.id; l.cost = cost;
        stockLogs.add(l);
        if (sup != null) sup.pending += Math.max(0, cost - paidNow);
        log("Stock In: " + p.name + " x" + qty);
        saveAll();
    }

    /** Serves an order (after dequeue): reduces stock, creates the bill and updates the farmer's credit. Returns an error text or null. */
    public static String processOrder(Order o, double discount, double paid) {
        for (OrderItem it : o.items) {
            Product p = product(it.productId);
            if (p == null) return "Product not found: " + it.productId;
            if (p.qty < it.qty) return "Not enough stock: " + p.name + " (available " + p.qty + ", needed " + it.qty + ")";
        }
        o.discount = Math.max(0, Math.min(discount, o.subTotal()));
        o.paid = Math.max(0, Math.min(paid, o.total()));
        for (OrderItem it : o.items) {
            Product p = product(it.productId);
            p.qty -= it.qty;
            StockLog l = new StockLog();
            l.id = nextLogId(); l.productId = p.id; l.type = "OUT"; l.qty = it.qty; l.date = now();
            l.note = "Order #" + o.id;
            stockLogs.add(l);
        }
        o.status = "COMPLETED";
        o.processedBy = current == null ? "" : current.username;
        Farmer f = farmer(o.farmerId);
        if (f != null) f.credit += o.balance();
        log("Order #" + o.id + " served (Rs " + (long) o.total() + ")");
        saveAll();
        return null;
    }

    // ---------------------------------------------------------------- backup / restore (database file)
    public static void backup(File target) throws IOException {
        saveAll();
        try {
            Files.deleteIfExists(target.toPath());
            try (Statement st = Db.con().createStatement()) {
                st.execute("VACUUM INTO '" + target.getAbsolutePath().replace("'", "''") + "'");
            }
        } catch (SQLException e) { throw new IOException(e.getMessage(), e); }
        log("Created database backup");
    }

    public static void restore(File backupFile) throws IOException {
        try (InputStream in = new FileInputStream(backupFile)) {
            byte[] head = new byte[16];
            if (in.read(head) < 16 || !new String(head, StandardCharsets.US_ASCII).startsWith("SQLite format 3"))
                throw new IOException("This is not a valid SQLite backup file.");
        }
        String me = current == null ? null : current.username;
        Db.close();
        Files.copy(backupFile.toPath(), Db.FILE.toPath(), StandardCopyOption.REPLACE_EXISTING);
        new File(Db.FILE.getPath() + "-wal").delete();
        new File(Db.FILE.getPath() + "-shm").delete();
        try { Db.open(); readAll(); } catch (SQLException e) { throw new IOException(e.getMessage(), e); }
        current = null;
        if (me != null) for (User u : users) if (u.username.equals(me)) current = u;
        log("Restored database from backup");
    }

    // ---------------------------------------------------------------- sample data (first run only)
    private static void seedAndSave() throws SQLException {
        users.clear(); products.clear(); farmers.clear(); suppliers.clear(); orders.clear(); stockLogs.clear();
        users.add(new User("admin", hash("admin123"), "ADMIN"));
        users.add(new User("staff", hash("staff123"), "STAFF"));

        String[][] sup = {
            {"Punjab Seed Corporation", "0300-1112233", "Multan Road, Lahore"},
            {"Green Valley Agro", "0321-4445566", "Main Bazaar, Sukkur"},
            {"Sindh Agri Traders", "0333-7778899", "Station Road, Shikarpur"}};
        for (String[] x : sup) {
            Supplier s = new Supplier(); s.id = nextSupplierId(); s.name = x[0]; s.phone = x[1]; s.address = x[2];
            suppliers.add(s);
        }
        suppliers.get(0).pending = 45000; suppliers.get(2).pending = 18500;

        // name, category, price, qty, min qty, expiry (days from today, -1 = none), season, supplier
        Object[][] pr = {
            {"Wheat Seed Akbar-19 (40kg)", "Seeds", 6800, 120, 30, 300, "Rabi", 1},
            {"Wheat Seed Faisalabad-2008 (40kg)", "Seeds", 6500, 85, 30, 280, "Rabi", 1},
            {"Mustard/Canola Seed (5kg)", "Seeds", 2400, 40, 15, 200, "Rabi", 1},
            {"Sunflower Hybrid Seed (2kg)", "Seeds", 3800, 8, 15, 18, "Rabi", 1},
            {"BT Cotton Seed (4kg)", "Seeds", 4500, 150, 40, 120, "Kharif", 2},
            {"Super Basmati Rice Seed (10kg)", "Seeds", 2800, 60, 20, 150, "Kharif", 2},
            {"Maize Hybrid Seed (10kg)", "Seeds", 9500, 25, 20, 160, "Kharif", 2},
            {"Urea Fertilizer (50kg)", "Fertilizers", 4400, 200, 50, -1, "All Season", 3},
            {"DAP Fertilizer (50kg)", "Fertilizers", 12500, 70, 40, -1, "Rabi", 3},
            {"Potash SOP (50kg)", "Fertilizers", 9000, 0, 15, -1, "Kharif", 3},
            {"Zinc Sulphate (10kg)", "Fertilizers", 2200, 35, 10, 400, "All Season", 3},
            {"Imidacloprid Insecticide (250ml)", "Pesticides", 1400, 14, 20, 25, "Kharif", 2},
            {"Glyphosate Weedicide (1L)", "Pesticides", 1800, 55, 20, 500, "All Season", 2},
            {"Clodinafop Wheat Weedicide (160g)", "Pesticides", 1600, 48, 20, 350, "Rabi", 1},
            {"Knapsack Sprayer 16L", "Farm Tools", 4500, 12, 5, -1, "All Season", 3},
            {"Sickle (Darati)", "Farm Tools", 450, 60, 20, -1, "Rabi", 3},
            {"Hand Hoe (Kassi)", "Farm Tools", 900, 5, 10, -1, "All Season", 3}};
        for (Object[] x : pr) {
            Product p = new Product();
            p.id = nextProductId(); p.name = (String) x[0]; p.category = (String) x[1]; p.price = (Integer) x[2];
            p.qty = (Integer) x[3]; p.minQty = (Integer) x[4]; int dd = (Integer) x[5];
            p.expiry = dd < 0 ? "" : LocalDate.now().plusDays(dd).toString();
            p.season = (String) x[6]; p.supplierId = (Integer) x[7];
            products.add(p);
        }

        String[][] fr = {
            {"Ghulam Rasool", "0301-2345678", "Chak 12, Shikarpur", "43201-1234567-1"},
            {"Muhammad Aslam", "0312-9876543", "Lakhi Ghulam Shah", "43201-7654321-3"},
            {"Abdul Qadir Jatoi", "0345-1122334", "Garhi Yasin", "43202-2233445-5"},
            {"Haji Allah Bux", "0300-5566778", "Khanpur", "43203-9988776-7"},
            {"Imran Memon", "0333-4455667", "Jacobabad Road", "43201-5544332-9"}};
        for (String[] x : fr) {
            Farmer f = new Farmer(); f.id = nextFarmerId(); f.name = x[0]; f.phone = x[1]; f.village = x[2]; f.cnic = x[3];
            farmers.add(f);
        }

        // 45 completed sales spread over the last 30 days (for charts and the Smart Insights forecast)
        Random rnd = new Random(7);
        for (int k = 0; k < 45; k++) {
            Order o = new Order();
            o.id = nextOrderId();
            o.farmerId = 1 + rnd.nextInt(farmers.size());
            o.date = LocalDate.now().minusDays(rnd.nextInt(30)).toString() + " " + String.format("%02d:%02d", 9 + rnd.nextInt(9), rnd.nextInt(60));
            o.priority = 1 + rnd.nextInt(3);
            int n = 1 + rnd.nextInt(3);
            for (int j = 0; j < n; j++) {
                Product p = products.get(rnd.nextInt(products.size()));
                boolean dup = false;
                for (OrderItem ex : o.items) if (ex.productId == p.id) dup = true;
                if (!dup) o.items.add(new OrderItem(p.id, 1 + rnd.nextInt(6), p.price));
            }
            o.status = "COMPLETED"; o.processedBy = "admin";
            o.discount = rnd.nextInt(3) == 0 ? 200 : 0;
            o.paid = rnd.nextInt(3) == 0 ? Math.round(o.total() * 0.6) : o.total();
            farmer(o.farmerId).credit += o.balance();
            orders.add(o);
        }
        orders.sort((a, b) -> a.date.compareTo(b.date));
        int id = 1001;
        for (Order o : orders) o.id = id++;

        // 4 pending orders so that the queue is not empty on first run (different priorities)
        int[][] pend = {{1, 3, 1, 4, 2, 2}, {3, 1, 8, 2, 9, 1}, {2, 2, 5, 20, 8, 10}, {4, 3, 6, 5, 13, 2}};
        for (int[] x : pend) {
            Order o = new Order();
            o.farmerId = x[0]; o.priority = x[1]; o.status = "PENDING"; o.date = now();
            o.items.add(new OrderItem(x[2], x[3], product(x[2]).price));
            o.items.add(new OrderItem(x[4], x[5], product(x[4]).price));
            o.id = orders.get(orders.size() - 1).id + 1;
            orders.add(o);
        }
        // stock movement history for the completed orders
        for (Order o : orders) if ("COMPLETED".equals(o.status))
            for (OrderItem it : o.items) {
                StockLog l = new StockLog();
                l.id = nextLogId(); l.productId = it.productId; l.type = "OUT"; l.qty = it.qty; l.date = o.date; l.note = "Order #" + o.id;
                stockLogs.add(l);
            }
        settings.setProperty("queueMode", "priority");
        saveAll();
        try (PreparedStatement ps = Db.con().prepareStatement("INSERT INTO activity_log(log_time, username, action) VALUES(?,?,?)")) {
            ps.setString(1, now()); ps.setString(2, "system"); ps.setString(3, "Database created and sample data inserted");
            ps.executeUpdate();
        }
    }
}
