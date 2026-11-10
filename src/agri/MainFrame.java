package agri;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Sidebar + header + pages + status bar. */
public class MainFrame extends JFrame {
    private final CardLayout cards = new CardLayout();
    private final JPanel content = new Host(cards);
    private final Map<String, Page> pages = new LinkedHashMap<>();
    private final Map<String, NavItem> navs = new LinkedHashMap<>();
    private final JLabel title = UI.label("", 24, true, Theme.TEXT), sub = UI.muted("");
    private final JLabel statusRight = UI.label(" ", 12, false, Theme.MUTED);

    /** Page container: keeps a minimum page size and lets the user scroll when the screen is smaller. */
    private static class Host extends JPanel implements Scrollable {
        static final int MIN_W = 820, MIN_H = 540;
        Host(LayoutManager l) { super(l); setOpaque(false); }
        @Override public Dimension getPreferredSize() {
            Dimension d = super.getPreferredSize();
            for (Component c : getComponents()) if (c.isVisible()) d = c.getPreferredSize();   // size of the page that is shown
            return new Dimension(Math.max(d.width, MIN_W), Math.max(d.height, MIN_H));
        }
        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 24; }
        @Override public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return 160; }
        @Override public boolean getScrollableTracksViewportWidth() { return getParent() instanceof JViewport && getParent().getWidth() >= getPreferredSize().width; }
        @Override public boolean getScrollableTracksViewportHeight() { return getParent() instanceof JViewport && getParent().getHeight() >= getPreferredSize().height; }
    }

    private static class NavItem extends JButton {
        boolean active, hover;
        final String icon;
        NavItem(String text, String icon, boolean small) {
            super(text);
            this.icon = icon;
            setContentAreaFilled(false); setBorderPainted(false); setFocusPainted(false); setOpaque(false);
            setHorizontalAlignment(SwingConstants.LEFT);
            setFont(UI.font(Font.BOLD, small ? 13 : 14)); setForeground(Theme.SIDEBAR_TEXT);
            setBorder(new EmptyBorder(small ? 7 : 11, 54, small ? 7 : 11, 10));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, small ? 36 : 44));
            setAlignmentX(Component.LEFT_ALIGNMENT);
            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                public void mouseExited(MouseEvent e) { hover = false; repaint(); }
            });
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create(); UI.aa(g2);
            if (active) {
                g2.setPaint(new GradientPaint(0, 0, Theme.PRIMARY, getWidth(), 0, Theme.blend(Theme.PRIMARY, Color.BLACK, 0.72f)));
                g2.fillRoundRect(10, 2, getWidth() - 20, getHeight() - 4, 12, 12);
                g2.setColor(Color.WHITE); g2.fillRoundRect(10, 10, 4, getHeight() - 20, 4, 4);
            } else if (hover) {
                g2.setColor(new Color(255, 255, 255, 25)); g2.fillRoundRect(10, 2, getWidth() - 20, getHeight() - 4, 12, 12);
            }
            Icons.draw(g2, icon, 26, (getHeight() - 20) / 2.0, 20, active ? Color.WHITE : new Color(203, 213, 225));
            g2.dispose();
            setForeground(active ? Color.WHITE : Theme.SIDEBAR_TEXT);
            super.paintComponent(g);
        }
    }

    private final Map<String, String[]> meta = new HashMap<>();
    private final Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
    private final boolean small = screen.height < 800 || screen.width < 1280;

    private void reg(String key, String nav, String icon, String ttl, String subtitle, Page p) {
        pages.put(key, p);
        meta.put(key, new String[]{ttl, subtitle});
        content.add(p, key);
        NavItem n = new NavItem(nav, icon, small);
        n.addActionListener(e -> show(key));
        navs.put(key, n);
    }

    public MainFrame(String start, Rectangle bounds) {
        super("Agri Inventory Pro - Seed, Fertilizer & Pesticide Store");
        boolean admin = DataStore.current.isAdmin();
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        reg("dashboard", "Dashboard", "dashboard", "Dashboard", "Today's overview", new DashboardPage());
        reg("products", "Products", "products", "Products", "Seeds, Fertilizers, Pesticides and Tools" + (admin ? "" : "  (view only)"), new ProductsPage(admin));
        reg("farmers", "Farmers", "farmers", "Farmers", "Customer records, credit and purchase history", new FarmersPage(admin));
        if (admin) reg("suppliers", "Suppliers", "suppliers", "Suppliers", "Supplier records and pending payments", new SuppliersPage());
        reg("queue", "Order Queue", "queue", "Order Queue", "Array-based queue: enqueue, dequeue, peek, rear, isFull, cancel", new QueuePage());
        reg("stock", "Stock", "stock", "Stock Management", "Stock In/Out, low stock and expiry alerts", new StockPage(admin));
        reg("season", "Seasons", "seasons", "Season Planner", "Products by Rabi and Kharif season", new SeasonPage());
        reg("insights", "Smart Insights", "insights", "Smart Insights", "Reorder forecast and farmer credit risk", new InsightsPage());
        if (admin) reg("reports", "Reports", "reports", "Reports & Charts", "Sales, best selling, credit and alerts", new ReportsPage());
        if (admin) reg("activity", "Activity Log", "activity", "Activity Log", "Who changed what", new ActivityPage());
        reg("settings", "Settings", "settings", "Settings", "Theme, password, backup and users", new SettingsPage(admin));

        // ---- sidebar
        JPanel side = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setPaint(new GradientPaint(0, 0, Theme.SIDEBAR, 0, getHeight(), Theme.blend(Theme.SIDEBAR, Color.BLACK, 0.75f)));
                g2.fillRect(0, 0, getWidth(), getHeight()); g2.dispose();
            }
        };
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setPreferredSize(new Dimension(small ? 208 : 240, 0));
        side.setOpaque(false);
        JPanel brand = UI.transparent(new FlowLayout(FlowLayout.LEFT, 12, 0));
        brand.setBorder(new EmptyBorder(small ? 10 : 20, 14, small ? 6 : 14, 0));
        brand.add(new UI.Logo(42));
        JPanel bt = UI.transparent(new GridLayout(2, 1));
        bt.add(UI.label("Agri Inventory", 16, true, Color.WHITE)); bt.add(UI.label("Pro", 12, false, Theme.SIDEBAR_TEXT));
        brand.add(bt);
        brand.setMaximumSize(new Dimension(Integer.MAX_VALUE, small ? 62 : 82));
        brand.setAlignmentX(Component.LEFT_ALIGNMENT);
        side.add(brand);
        for (NavItem n : navs.values()) side.add(n);
        side.add(Box.createVerticalGlue());

        JPanel user = UI.transparent(new BorderLayout(0, 8));
        user.setBorder(new EmptyBorder(6, 18, small ? 10 : 18, 18));
        user.add(UI.label(DataStore.current.username, 14, true, Color.WHITE), BorderLayout.NORTH);
        user.add(UI.label("Role: " + DataStore.current.role, 12, false, Theme.SIDEBAR_TEXT), BorderLayout.CENTER);
        UI.Btn out = new UI.Btn("Logout", Color.decode("#EF4444"));
        out.addActionListener(e -> { if (UI.confirm(this, "Do you want to log out?")) Main.logout(); });
        user.add(out, BorderLayout.SOUTH);
        user.setMaximumSize(new Dimension(Integer.MAX_VALUE, small ? 104 : 130));
        user.setAlignmentX(Component.LEFT_ALIGNMENT);
        side.add(user);

        // ---- header
        JPanel head = UI.transparent(new BorderLayout());
        JPanel ht = UI.transparent(new GridLayout(2, 1, 0, 0));
        ht.add(title); ht.add(sub);
        head.add(ht, BorderLayout.WEST);
        JPanel hr = UI.transparent(new FlowLayout(FlowLayout.RIGHT, 12, 6));
        hr.add(UI.label(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy")), 13, true, Theme.MUTED));
        UI.Btn toggle = new UI.Btn(Theme.dark ? "Light Mode" : "Dark Mode", Theme.PRIMARY, true);
        toggle.addActionListener(e -> Main.changeTheme(!Theme.dark, Theme.accent));
        hr.add(toggle);
        head.add(hr, BorderLayout.EAST);

        // ---- status bar
        JPanel status = UI.transparent(new BorderLayout());
        JLabel dbL = UI.label("Agri Inventory Pro", 12, true, Theme.PRIMARY);
        status.add(dbL, BorderLayout.WEST);
        status.add(statusRight, BorderLayout.EAST);
        javax.swing.Timer clock = new javax.swing.Timer(1000, e -> updateStatus());
        clock.start();
        updateStatus();

        content.setOpaque(false);
        JPanel main = new JPanel(new BorderLayout(0, 14));
        main.setBackground(Theme.BG);
        main.setBorder(small ? new EmptyBorder(12, 16, 6, 16) : new EmptyBorder(20, 26, 10, 26));
        JScrollPane host = new JScrollPane(content, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        host.setBorder(null); host.setOpaque(false); host.getViewport().setOpaque(false);
        host.getVerticalScrollBar().setUI(new UI.SlimScroll()); host.getHorizontalScrollBar().setUI(new UI.SlimScroll());
        host.getVerticalScrollBar().setPreferredSize(new Dimension(10, 0)); host.getHorizontalScrollBar().setPreferredSize(new Dimension(0, 10));
        host.getVerticalScrollBar().setUnitIncrement(24);
        main.add(head, BorderLayout.NORTH); main.add(host, BorderLayout.CENTER); main.add(status, BorderLayout.SOUTH);

        JPanel root = new JPanel(new BorderLayout());
        root.add(side, BorderLayout.WEST); root.add(main, BorderLayout.CENTER);
        setContentPane(root);

        // the window always fits the screen (usable area, without the taskbar)
        if (bounds != null) setBounds(bounds); else setBounds(screen);
        setMinimumSize(new Dimension(Math.min(900, screen.width), Math.min(520, screen.height)));
        if (bounds == null) setExtendedState(JFrame.MAXIMIZED_BOTH);
        show(pages.containsKey(start) ? start : "dashboard");
    }

    private void updateStatus() {
        statusRight.setText("Queue: " + DataStore.queue.size() + "/" + DataStore.queue.capacity() + " pending   |   Alerts: " + Logic.alertCount()
                + "   |   " + DataStore.current.username + " (" + DataStore.current.role + ")   |   " + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
    }

    public void show(String key) {
        for (Map.Entry<String, NavItem> e : navs.entrySet()) { e.getValue().active = e.getKey().equals(key); e.getValue().repaint(); }
        title.setText(meta.get(key)[0]); sub.setText(meta.get(key)[1]);
        pages.get(key).refresh();
        cards.show(content, key);
        updateStatus();
    }
}
