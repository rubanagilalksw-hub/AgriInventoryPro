package agri;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;
import agri.Models.*;

/**
 * MAIN DSA FEATURE: the queue of farmers' orders.
 * Operations: enqueue, dequeue, peek (front), rear, size, isEmpty, isFull, cancel (remove).
 * The page also shows the queue as boxes (live) and as the raw circular ARRAY (array inspector).
 */
public class QueuePage extends Page {
    private final OrderQueue queue = DataStore.queue;
    private final QueueVisual visual = new QueueVisual();
    private final ArrayView arrayView = new ArrayView();
    private final JLabel stats = UI.label(" ", 13, false, Theme.MUTED);
    private final JLabel lastOp = UI.label("Last operation: -", 12, true, Theme.PRIMARY);
    private final DefaultTableModel pendModel = UI.model("Order #", "Position", "Farmer", "Priority", "Items", "Total (Rs)", "Est. wait (min)", "Date");
    private final UI.StyledTable pendTable = new UI.StyledTable(pendModel);
    private final DefaultTableModel histModel = UI.model("Order #", "Date", "Farmer", "Items", "Total (Rs)", "Paid (Rs)", "Balance (Rs)", "Priority", "Status", "By");
    private final UI.StyledTable histTable = new UI.StyledTable(histModel);
    private final DefaultTableModel opsModel = UI.model("Time", "Operation", "Order #", "Size", "Front idx", "Rear idx", "Details");
    private final JComboBox<String> modeBox = UI.combo(new String[]{"Priority Queue (Urgent first)", "Normal Queue (FIFO)"});
    private boolean loading = false;
    private javax.swing.Timer rushTimer;

    public QueuePage() {
        pendTable.rowColor = r -> {
            Order o = DataStore.order(((Number) pendModel.getValueAt(r, 0)).intValue());
            if (o == null) return null;
            return o.priority == Order.URGENT ? Theme.tint(Theme.DANGER) : o.priority == Order.BULK ? Theme.tint(Theme.WARN) : null;
        };
        histTable.rowColor = r -> {
            String st = String.valueOf(histModel.getValueAt(r, 8));
            if ("CANCELLED".equals(st)) return Theme.tint(Theme.MUTED);
            return ((Number) histModel.getValueAt(r, 6)).doubleValue() > 0 ? Theme.tint(Theme.WARN) : null;
        };

        loading = true;
        modeBox.setSelectedIndex(queue.isPriorityMode() ? 0 : 1);
        loading = false;
        modeBox.addActionListener(e -> {
            if (loading) return;
            boolean pm = modeBox.getSelectedIndex() == 0;
            queue.setPriorityMode(pm);
            DataStore.settings.setProperty("queueMode", pm ? "priority" : "fifo");
            DataStore.saveSettings();
            record("mode", 0, "Mode changed to " + (pm ? "Priority Queue" : "FIFO Queue"), false);
            refresh();
        });

        UI.Btn nw = UI.primary("Enqueue");
        nw.addActionListener(e -> newOrder());
        UI.Btn serve = UI.success("Dequeue");
        nw.setToolTipText("enqueue(): add a new order at the REAR of the queue");
        serve.setToolTipText("dequeue(): serve the FRONT order");
        serve.addActionListener(e -> serve());
        UI.Btn peek = UI.info("Peek Front");
        peek.addActionListener(e -> peek(true));
        UI.Btn rear = UI.info("Peek Rear");
        rear.addActionListener(e -> peek(false));
        UI.Btn cancel = UI.danger("Cancel Order");
        cancel.addActionListener(e -> cancel());
        UI.Btn rush = UI.warn("Rush Hour Demo");
        rush.setToolTipText("Adds random orders one by one so you can watch enqueue, priority and Queue Overflow live");
        rush.addActionListener(e -> rushHour());

        UI.Card ctrl = new UI.Card(new BorderLayout(10, 8));
        JPanel left = UI.transparent(new GridLayout(2, 1, 0, 2));
        left.add(UI.label("Order Queue Manager", 17, true, Theme.TEXT));
        left.add(stats);
        JPanel btns = UI.transparent(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btns.add(nw); btns.add(serve); btns.add(peek); btns.add(rear); btns.add(cancel); btns.add(rush);
        JPanel modeP = UI.transparent(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        modeP.add(UI.label("Queue Type:", 13, true, Theme.MUTED)); modeP.add(modeBox);
        ctrl.add(left, BorderLayout.WEST); ctrl.add(modeP, BorderLayout.EAST); ctrl.add(btns, BorderLayout.SOUTH);

        UI.Card vis = new UI.Card(new BorderLayout(0, 4));
        vis.add(UI.label("Live Queue  (served from the FRONT, new orders join at the REAR)", 13, true, Theme.MUTED), BorderLayout.NORTH);
        JPanel stack = UI.transparent(new BorderLayout(0, 2));
        stack.add(visual, BorderLayout.CENTER);
        stack.add(arrayView, BorderLayout.SOUTH);
        JScrollPane vs = UI.plainScroll(stack);
        vs.setPreferredSize(new Dimension(100, 250));
        vis.add(vs, BorderLayout.CENTER);
        vis.add(lastOp, BorderLayout.SOUTH);

        JPanel north = UI.transparent(new BorderLayout(0, 12));
        north.add(ctrl, BorderLayout.NORTH); north.add(vis, BorderLayout.CENTER);

        UI.Tabs tabs = new UI.Tabs();
        tabs.addTab("Pending Orders", UI.scroll(pendTable), null);
        JPanel hist = UI.transparent(new BorderLayout(0, 10));
        UI.Btn view = UI.primary("View Bill");
        view.addActionListener(e -> {
            int id = histTable.selectedId();
            Order o = id < 0 ? null : DataStore.order(id);
            if (o == null) UI.showInfo(this, "Please select an order from the history first.");
            else if (!"COMPLETED".equals(o.status)) UI.showInfo(this, "A cancelled order has no bill.");
            else BillDialog.show(this, o);
        });
        JPanel hb = UI.transparent(new FlowLayout(FlowLayout.RIGHT, 0, 0)); hb.add(view);
        hist.add(hb, BorderLayout.NORTH); hist.add(UI.scroll(histTable), BorderLayout.CENTER);
        tabs.addTab("Order History & Bills", hist, null);
        tabs.addTab("Queue Operations Log", UI.scroll(new UI.StyledTable(opsModel)), null);

        add(north, BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);
    }

    /** Shows the text under the visual and stores the operation in the queue_ops table. */
    private void record(String op, int orderId, String text, boolean error) {
        lastOp.setForeground(error ? Theme.DANGER : Theme.PRIMARY);
        lastOp.setText("Last operation: " + text);
        DataStore.logQueue(op, orderId, text);
    }

    @Override public void refresh() {
        List<Order> list = queue.toList();
        visual.setList(list);
        arrayView.update(queue);
        String front = queue.isEmpty() ? "-" : "#" + queue.peek().id;
        String rear = queue.isEmpty() ? "-" : "#" + queue.rear().id;
        stats.setText("size " + queue.size() + "/" + queue.capacity() + "   |   Front " + front + "   |   Rear " + rear
                + "   |   isEmpty " + queue.isEmpty() + "   |   isFull " + queue.isFull());

        pendModel.setRowCount(0);
        int pos = 1;
        double wait = 0;
        for (Order o : list) {
            pendModel.addRow(new Object[]{o.id, pos++, DataStore.farmerName(o.farmerId), o.priorityName(), Logic.itemsSummary(o), o.total(), Math.round(wait), o.date});
            wait += Logic.serviceMinutes(o);
        }

        histModel.setRowCount(0);
        for (int i = DataStore.orders.size() - 1; i >= 0; i--) {
            Order o = DataStore.orders.get(i);
            if ("PENDING".equals(o.status)) continue;
            histModel.addRow(new Object[]{o.id, o.date, DataStore.farmerName(o.farmerId), Logic.itemsSummary(o), o.total(), o.paid, o.balance(), o.priorityName(), o.status, o.processedBy});
        }

        opsModel.setRowCount(0);
        try {
            List<String[]> rows = Db.query("SELECT op_time, operation, order_id, queue_size, front_idx, rear_idx, details FROM queue_ops ORDER BY id DESC LIMIT 200");
            for (int i = 1; i < rows.size(); i++) opsModel.addRow(rows.get(i));
        } catch (java.sql.SQLException ex) { ex.printStackTrace(); }
    }

    // ------------------------------------------------------------ enqueue
    private static class Item {
        final Product p;
        Item(Product p) { this.p = p; }
        @Override public String toString() { return p.name + "   [stock " + p.qty + " | Rs " + (long) p.price + "]"; }
    }

    private void newOrder() {
        if (queue.isFull()) {
            record("enqueue", 0, "enqueue() rejected: Queue Overflow, the queue is full (" + queue.size() + "/" + queue.capacity() + ")", true);
            UI.showError(this, "Queue Overflow!\n\nThe queue is full (" + queue.capacity() + " orders). Serve (dequeue) an order first, then add a new one.");
            refresh();
            return;
        }
        if (DataStore.farmers.isEmpty()) { UI.showError(this, "Please add at least one farmer on the Farmers page first."); return; }
        Vector<Item> pv = new Vector<>();
        for (Product p : DataStore.products) if (p.qty > 0) pv.add(new Item(p));
        if (pv.isEmpty()) { UI.showError(this, "No product is in stock."); return; }

        JDialog d = new JDialog(SwingUtilities.getWindowAncestor(this), "New Order (Enqueue)", Dialog.ModalityType.APPLICATION_MODAL);
        JComboBox<Farmer> fc = UI.combo(new Vector<>(DataStore.farmers));
        JComboBox<String> pc = UI.combo(new String[]{"Normal", "Bulk", "Urgent"});
        JComboBox<Item> prod = UI.combo(pv);
        UI.TField qty = new UI.TField("1", 5);
        qty.setPreferredSize(new Dimension(70, 38)); qty.setMinimumSize(new Dimension(70, 38));
        List<OrderItem> items = new ArrayList<>();
        DefaultTableModel im = UI.model("Product", "Qty", "Price", "Amount");
        UI.StyledTable it = new UI.StyledTable(im);
        JLabel total = UI.label("Total: Rs 0", 16, true, Theme.PRIMARY);
        JLabel err = UI.label(" ", 12, true, Theme.DANGER);

        Runnable reload = () -> {
            im.setRowCount(0);
            double t = 0;
            for (OrderItem x : items) { im.addRow(new Object[]{DataStore.productName(x.productId), x.qty, x.price, x.amount()}); t += x.amount(); }
            total.setText("Total: " + UI.money(t));
        };

        UI.Btn add = UI.info("Add Item");
        add.addActionListener(e -> {
            Product p = ((Item) prod.getSelectedItem()).p;
            int q;
            try { q = UI.toInt(qty.getText()); } catch (Exception ex) { err.setText("Enter the quantity as a valid number."); return; }
            if (q <= 0) { err.setText("Quantity must be greater than 0."); return; }
            OrderItem ex = null;
            for (OrderItem x : items) if (x.productId == p.id) ex = x;
            int have = ex == null ? 0 : ex.qty;
            if (have + q > p.qty) { err.setText("Only " + p.qty + " in stock: " + p.name); return; }
            if (ex == null) items.add(new OrderItem(p.id, q, p.price)); else ex.qty += q;
            err.setText(" "); qty.setText("1"); reload.run();
        });
        UI.Btn rem = UI.ghost("Remove Selected");
        rem.addActionListener(e -> {
            int r = it.getSelectedRow();
            if (r >= 0) { items.remove(it.convertRowIndexToModel(r)); reload.run(); }
        });
        UI.Btn cancel = UI.ghost("Cancel"); cancel.addActionListener(e -> d.dispose());
        UI.Btn go = UI.primary("Enqueue Order");
        go.addActionListener(e -> {
            if (items.isEmpty()) { err.setText("Add at least one item."); return; }
            Order o = new Order();
            o.id = DataStore.nextOrderId(); o.farmerId = ((Farmer) fc.getSelectedItem()).id; o.date = DataStore.now();
            String pr = (String) pc.getSelectedItem();
            o.priority = "Urgent".equals(pr) ? Order.URGENT : "Bulk".equals(pr) ? Order.BULK : Order.NORMAL;
            o.status = "PENDING";
            for (OrderItem x : items) o.items.add(new OrderItem(x.productId, x.qty, x.price));
            DataStore.orders.add(o);
            queue.enqueue(o);
            DataStore.log("Order #" + o.id + " added to the queue (" + o.priorityName() + ")");
            DataStore.saveAll();
            record("enqueue", o.id, "enqueue(Order #" + o.id + ") -> position " + queue.positionOf(o.id) + " of " + queue.size() + ", rear index = " + queue.rearIndex(), false);
            d.dispose();
            refresh();
        });

        JPanel top = UI.transparent(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 4, 4, 10); g.anchor = GridBagConstraints.WEST;
        g.gridx = 0; g.gridy = 0; top.add(UI.label("Farmer", 13, true, Theme.MUTED), g);
        g.gridx = 1; g.weightx = 1; g.fill = GridBagConstraints.HORIZONTAL; top.add(fc, g);
        g.gridx = 2; g.weightx = 0; g.fill = GridBagConstraints.NONE; top.add(UI.label("Priority", 13, true, Theme.MUTED), g);
        g.gridx = 3; top.add(pc, g);

        JPanel pick = UI.transparent(new GridBagLayout());
        g = new GridBagConstraints(); g.insets = new Insets(4, 4, 4, 8); g.anchor = GridBagConstraints.WEST;
        g.gridx = 0; pick.add(UI.label("Product", 13, true, Theme.MUTED), g);
        g.gridx = 1; g.weightx = 1; g.fill = GridBagConstraints.HORIZONTAL; pick.add(prod, g);
        g.gridx = 2; g.weightx = 0; g.fill = GridBagConstraints.NONE; pick.add(UI.label("Qty", 13, true, Theme.MUTED), g);
        g.gridx = 3; pick.add(qty, g);
        g.gridx = 4; pick.add(add, g);

        JPanel head = UI.transparent(new BorderLayout(0, 6));
        head.add(top, BorderLayout.NORTH); head.add(pick, BorderLayout.SOUTH);

        JPanel foot = UI.transparent(new BorderLayout(0, 6));
        JPanel row = UI.transparent(new BorderLayout());
        row.add(total, BorderLayout.WEST);
        JPanel fb = UI.transparent(new FlowLayout(FlowLayout.RIGHT, 8, 0)); fb.add(rem); fb.add(cancel); fb.add(go);
        row.add(fb, BorderLayout.EAST);
        foot.add(err, BorderLayout.NORTH); foot.add(row, BorderLayout.SOUTH);

        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBackground(Theme.CARD);
        root.setBorder(new javax.swing.border.CompoundBorder(new javax.swing.border.MatteBorder(4, 0, 0, 0, Theme.PRIMARY), new EmptyBorder(16, 20, 16, 20)));
        JPanel mid = UI.transparent(new BorderLayout(0, 10));
        mid.add(head, BorderLayout.NORTH); mid.add(UI.scroll(it), BorderLayout.CENTER);
        root.add(UI.label("New Order (Enqueue)", 18, true, Theme.TEXT), BorderLayout.NORTH);
        root.add(mid, BorderLayout.CENTER); root.add(foot, BorderLayout.SOUTH);
        d.setContentPane(root); Rectangle sc = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        d.setSize(Math.min(720, sc.width - 40), Math.min(560, sc.height - 50)); d.setLocationRelativeTo(this); d.setVisible(true);
    }

    // ------------------------------------------------------------ dequeue
    private void serve() {
        Order o = queue.peek();
        if (o == null) {
            record("dequeue", 0, "dequeue() rejected: Queue Underflow, the queue is empty", true);
            UI.showInfo(this, "Queue Underflow!\n\nThe queue is empty - there is no pending order to serve.");
            refresh();
            return;
        }
        Farmer f = DataStore.farmer(o.farmerId);
        UI.TField disc = new UI.TField("0", 10), paid = new UI.TField(String.valueOf((long) o.subTotal()), 10);
        boolean ok = new UI.Form(this, "Serve Order #" + o.id)
                .add("Farmer", UI.label(f == null ? "-" : f.name, 14, true, Theme.TEXT))
                .add("Items", UI.label("<html><body style='width:240px'>" + Logic.itemsSummary(o) + "</body></html>", 12, false, Theme.TEXT))
                .add("Sub Total", UI.label(UI.money(o.subTotal()), 14, true, Theme.PRIMARY))
                .add("Discount (Rs)", disc).add("Paid now (Rs)", paid)
                .show(() -> {
                    try {
                        double dv = UI.toDouble(disc.getText()), pv = UI.toDouble(paid.getText());
                        if (dv < 0 || pv < 0) return "Amounts cannot be negative.";
                        if (dv > o.subTotal()) return "The discount cannot be more than the sub total.";
                    } catch (Exception e) { return "Enter valid discount and payment amounts."; }
                    return null; }, "Serve & Bill");
        if (!ok) return;
        String err = DataStore.processOrder(o, UI.toDouble(disc.getText()), UI.toDouble(paid.getText()));
        if (err != null) {
            UI.showError(this, err + "\n\nThe order stays in the queue. Use Stock In or cancel the order.");
            return;
        }
        int oldFront = queue.frontIndex();
        queue.dequeue();
        record("dequeue", o.id, "dequeue() -> Order #" + o.id + " served, front index " + oldFront + " -> " + queue.frontIndex() + ", stock reduced automatically", false);
        refresh();
        BillDialog.show(this, o);
    }

    private void peek(boolean frontSide) {
        Order o = frontSide ? queue.peek() : queue.rear();
        String name = frontSide ? "peek() / front" : "rear()";
        if (o == null) {
            record(frontSide ? "peek" : "rear", 0, name + " -> the queue is empty", true);
            UI.showInfo(this, "The queue is empty.");
            refresh();
            return;
        }
        record(frontSide ? "peek" : "rear", o.id, name + " -> Order #" + o.id + " (not removed)", false);
        UI.showInfo(this, (frontSide ? "Next order (FRONT):" : "Last order (REAR):") + "\n\nOrder #" + o.id + "\nFarmer: " + DataStore.farmerName(o.farmerId)
                + "\nPriority: " + o.priorityName() + "\nItems: " + Logic.itemsSummary(o) + "\nTotal: " + UI.money(o.total())
                + "\n\nQueue size: " + queue.size() + " / " + queue.capacity());
        refresh();
    }

    private void cancel() {
        int id = pendTable.selectedId();
        if (id < 0) { UI.showInfo(this, "Select the order you want to cancel from the Pending Orders table."); return; }
        Order o = DataStore.order(id);
        if (o == null || !"PENDING".equals(o.status)) return;
        if (!UI.confirm(this, "Cancel Order #" + id + " (" + DataStore.farmerName(o.farmerId) + ")?")) return;
        queue.remove(id);
        o.status = "CANCELLED";
        DataStore.log("Order #" + id + " cancelled");
        DataStore.saveAll();
        record("remove", id, "remove(Order #" + id + ") -> cancelled and the gap was closed", false);
        refresh();
    }

    // ------------------------------------------------------------ rush hour demo
    private void rushHour() {
        if (rushTimer != null && rushTimer.isRunning()) return;
        final int[] n = {0};
        rushTimer = new javax.swing.Timer(550, e -> {
            if (n[0]++ >= 7) { rushTimer.stop(); return; }
            if (queue.isFull()) {
                record("enqueue", 0, "enqueue() rejected: Queue Overflow, the queue is full (" + queue.size() + "/" + queue.capacity() + ")", true);
                rushTimer.stop(); refresh(); return;
            }
            Order o = randomOrder();
            if (o == null) { rushTimer.stop(); return; }
            DataStore.orders.add(o);
            queue.enqueue(o);
            DataStore.log("Rush hour: order #" + o.id + " added to the queue (" + o.priorityName() + ")");
            DataStore.saveAll();
            record("enqueue", o.id, "enqueue(Order #" + o.id + ", " + o.priorityName() + ") -> position " + queue.positionOf(o.id) + " of " + queue.size(), false);
            refresh();
        });
        rushTimer.setInitialDelay(100);
        rushTimer.start();
    }

    private Order randomOrder() {
        if (DataStore.farmers.isEmpty()) return null;
        List<Product> avail = new ArrayList<>();
        for (Product p : DataStore.products) if (p.qty > 1) avail.add(p);
        if (avail.isEmpty()) return null;
        Random r = new Random();
        Order o = new Order();
        o.id = DataStore.nextOrderId();
        o.farmerId = DataStore.farmers.get(r.nextInt(DataStore.farmers.size())).id;
        o.date = DataStore.now();
        int pr = r.nextInt(10);
        o.priority = pr < 2 ? Order.URGENT : pr < 5 ? Order.BULK : Order.NORMAL;
        o.status = "PENDING";
        Collections.shuffle(avail, r);
        for (int i = 0; i < Math.min(avail.size(), 1 + r.nextInt(2)); i++) {
            Product p = avail.get(i);
            o.items.add(new OrderItem(p.id, 1 + r.nextInt(Math.min(4, p.qty)), p.price));
        }
        return o;
    }

    // ------------------------------------------------------------ visual 1: animated boxes
    private static class QueueVisual extends JPanel {
        private static final int BW = 172, BH = 74, GAP = 42, PADX = 22, TOP = 36;
        private static class Box { Order o; double x, y, a; boolean leaving; }
        private List<Box> boxes = new ArrayList<>();
        private final List<Box> leaving = new ArrayList<>();

        QueueVisual() {
            setOpaque(false);
            new javax.swing.Timer(16, e -> tick()).start();
        }

        void setList(List<Order> l) {
            List<Box> next = new ArrayList<>();
            for (int i = 0; i < l.size(); i++) {
                Box b = null;
                for (Box x : boxes) if (x.o.id == l.get(i).id) b = x;
                if (b == null) { b = new Box(); b.x = PADX + i * (BW + GAP) + 160; b.y = TOP; b.a = 0; }
                b.o = l.get(i);
                next.add(b);
            }
            for (Box old : boxes) if (!next.contains(old)) { old.leaving = true; leaving.add(old); }
            boxes = next;
            setPreferredSize(new Dimension(Math.max(500, PADX * 2 + l.size() * (BW + GAP)), 128));
            revalidate();
        }

        private void tick() {
            for (int i = 0; i < boxes.size(); i++) {
                Box b = boxes.get(i);
                b.x += (PADX + i * (BW + GAP) - b.x) * 0.2;
                b.a = Math.min(1, b.a + 0.07);
            }
            for (java.util.Iterator<Box> it = leaving.iterator(); it.hasNext(); ) {
                Box b = it.next();
                b.y -= 4; b.a -= 0.07;
                if (b.a <= 0) it.remove();
            }
            repaint();
        }

        private String clip(String s, FontMetrics fm, int w) {
            while (fm.stringWidth(s) > w && s.length() > 3) s = s.substring(0, s.length() - 2);
            return s;
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create(); UI.aa(g2);
            if (boxes.isEmpty() && leaving.isEmpty()) {
                g2.setFont(UI.font(Font.BOLD, 14)); g2.setColor(Theme.MUTED);
                g2.drawString("The queue is empty - no pending orders (isEmpty() = true)", 24, 70);
                g2.dispose(); return;
            }
            for (Box b : leaving) drawBox(g2, b, -1, false);
            for (int i = 0; i < boxes.size(); i++) drawBox(g2, boxes.get(i), i, i == boxes.size() - 1);
            g2.dispose();
        }

        private void drawBox(Graphics2D g2, Box b, int i, boolean last) {
            Order o = b.o;
            float a = (float) Math.max(0, Math.min(1, b.a));
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, a));
            int x = (int) b.x, y = (int) b.y;
            Color c = o.priority == Order.URGENT ? Theme.DANGER : o.priority == Order.BULK ? Theme.WARN : Theme.PRIMARY;
            g2.setColor(new Color(0, 0, 0, 40)); g2.fillRoundRect(x + 2, y + 4, BW, BH, 16, 16);
            g2.setPaint(new GradientPaint(0, y, Theme.tint(c), 0, y + BH, Theme.blend(Theme.tint(c), Theme.CARD, 0.7f)));
            g2.fillRoundRect(x, y, BW, BH, 16, 16);
            g2.setColor(c); g2.setStroke(new BasicStroke(2f)); g2.drawRoundRect(x, y, BW, BH, 16, 16);
            g2.fillOval(x + 10, y + 9, 26, 26);
            g2.setColor(Color.WHITE); g2.setFont(UI.font(Font.BOLD, 13));
            String pn = i < 0 ? "-" : String.valueOf(i + 1);
            g2.drawString(pn, x + 23 - g2.getFontMetrics().stringWidth(pn) / 2, y + 27);
            g2.setColor(Theme.TEXT); g2.setFont(UI.font(Font.BOLD, 14));
            g2.drawString("Order #" + o.id, x + 44, y + 28);
            g2.setFont(UI.font(Font.PLAIN, 12));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(clip(DataStore.farmerName(o.farmerId), fm, BW - 20), x + 12, y + 52);
            g2.setColor(Theme.MUTED);
            g2.drawString(o.priorityName() + "  |  " + UI.money(o.total()), x + 12, y + 68);
            if (i >= 0) {
                g2.setFont(UI.font(Font.BOLD, 11));
                boolean first = i == 0;
                String lbl = first && last ? "FRONT / REAR" : first ? "FRONT  (dequeue)" : last ? "REAR  (enqueue)" : null;
                if (lbl != null) { g2.setColor(Theme.PRIMARY); g2.drawString(lbl, x + 4, y - 13); g2.fillPolygon(new int[]{x + BW / 2 - 5, x + BW / 2 + 5, x + BW / 2}, new int[]{y - 9, y - 9, y - 2}, 3); }
                if (!last) {
                    int ax = x + BW + 6, bx = x + BW + GAP - 6, ay = y + BH / 2;
                    g2.setColor(Theme.MUTED); g2.setStroke(new BasicStroke(2f));
                    g2.drawLine(ax, ay, bx, ay);
                    g2.fillPolygon(new int[]{bx, bx - 8, bx - 8}, new int[]{ay, ay - 5, ay + 5}, 3);
                }
            }
            g2.setComposite(AlphaComposite.SrcOver);
        }
    }

    // ------------------------------------------------------------ visual 2: the real array
    private static class ArrayView extends JPanel {
        private static final int CW = 66, CH = 40, PADX = 22;
        private OrderQueue q;
        ArrayView() { setOpaque(false); setPreferredSize(new Dimension(600, 124)); }
        void update(OrderQueue q) {
            this.q = q;
            setPreferredSize(new Dimension(PADX * 2 + 90 + q.capacity() * (CW + 6), 124));
            revalidate(); repaint();
        }
        @Override protected void paintComponent(Graphics g) {
            if (q == null) return;
            Graphics2D g2 = (Graphics2D) g.create(); UI.aa(g2);
            int y = 30;
            g2.setFont(UI.font(Font.BOLD, 12)); g2.setColor(Theme.MUTED);
            g2.drawString("data[ ]", PADX, y + 25);
            int x0 = PADX + 56;
            for (int i = 0; i < q.capacity(); i++) {
                int x = x0 + i * (CW + 6);
                boolean act = q.isActive(i);
                Order o = q.slot(i);
                Color c = o == null ? Theme.BORDER : o.priority == Order.URGENT ? Theme.DANGER : o.priority == Order.BULK ? Theme.WARN : Theme.PRIMARY;
                g2.setColor(act ? Theme.tint(c) : Theme.blend(Theme.CARD, Theme.BG, 0.6f));
                g2.fillRoundRect(x, y, CW, CH, 10, 10);
                g2.setColor(act ? c : Theme.BORDER); g2.setStroke(new BasicStroke(act ? 2f : 1.2f));
                g2.drawRoundRect(x, y, CW, CH, 10, 10);
                g2.setFont(UI.font(Font.BOLD, 12));
                String t = o == null ? "null" : "#" + o.id;
                g2.setColor(act ? Theme.TEXT : Theme.MUTED);
                g2.drawString(t, x + (CW - g2.getFontMetrics().stringWidth(t)) / 2, y + 20);
                if (!act && o != null) { g2.setFont(UI.font(Font.PLAIN, 10)); g2.drawString("(old)", x + CW / 2 - 12, y + 33); }
                g2.setFont(UI.font(Font.PLAIN, 11)); g2.setColor(Theme.MUTED);
                String ix = "[" + i + "]";
                g2.drawString(ix, x + (CW - g2.getFontMetrics().stringWidth(ix)) / 2, y + CH + 14);
            }
            // pointers
            if (!q.isEmpty() || q.rearIndex() >= 0) {
                pointer(g2, x0 + q.frontIndex() * (CW + 6) + CW / 2, y - 3, "FRONT", Theme.SUCCESS, true);
                if (q.rearIndex() >= 0) pointer(g2, x0 + q.rearIndex() * (CW + 6) + CW / 2, y + CH + 18, "REAR", Theme.INFO, false);
            }
            g2.dispose();
        }
        private void pointer(Graphics2D g2, int cx, int y, String label, Color c, boolean above) {
            g2.setColor(c); g2.setFont(UI.font(Font.BOLD, 11));
            g2.drawString(label, cx - g2.getFontMetrics().stringWidth(label) / 2, above ? y - 10 : y + 22);
            if (above) g2.fillPolygon(new int[]{cx - 5, cx + 5, cx}, new int[]{y - 7, y - 7, y}, 3);
            else g2.fillPolygon(new int[]{cx - 5, cx + 5, cx}, new int[]{y + 7, y + 7, y}, 3);
        }
    }
}
