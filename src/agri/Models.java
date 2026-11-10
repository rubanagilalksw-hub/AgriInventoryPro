package agri;

import java.util.ArrayList;
import java.util.List;

/** All data classes (models). */
public class Models {

    public static class User {
        public String username, passHash, role; // role: ADMIN / STAFF
        public User(String u, String p, String r) { username = u; passHash = p; role = r; }
        public boolean isAdmin() { return "ADMIN".equals(role); }
    }

    public static class Product {
        public int id, qty, minQty, supplierId;
        public String name, category, expiry, season; // expiry: yyyy-MM-dd or empty
        public double price;
        @Override public String toString() { return name; }
    }

    public static class Farmer {
        public int id;
        public String name, phone, village, cnic;
        public double credit; // credit (udhaar)
        @Override public String toString() { return name; }
    }

    public static class Supplier {
        public int id;
        public String name, phone, address;
        public double pending; // amount we still owe the supplier
        @Override public String toString() { return name; }
    }

    public static class OrderItem {
        public int productId, qty;
        public double price;
        public OrderItem(int p, int q, double pr) { productId = p; qty = q; price = pr; }
        public double amount() { return qty * price; }
    }

    public static class Order {
        public static final int URGENT = 1, BULK = 2, NORMAL = 3;
        public int id, farmerId, priority = NORMAL;
        public String date, status = "PENDING", processedBy = "";
        public double discount, paid;
        public List<OrderItem> items = new ArrayList<>();

        public double subTotal() { double t = 0; for (OrderItem i : items) t += i.amount(); return t; }
        public double total() { return Math.max(0, subTotal() - discount); }
        public double balance() { return Math.max(0, total() - paid); }
        public String priorityName() { return priority == URGENT ? "Urgent" : priority == BULK ? "Bulk" : "Normal"; }
    }

    public static class StockLog {
        public int id, productId, qty, supplierId;
        public double cost;
        public String type, date, note; // IN / OUT
    }

    public static class Activity {
        public String time, user, action;
        public Activity(String t, String u, String a) { time = t; user = u; action = a; }
    }
}
