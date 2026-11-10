package agri;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.plaf.basic.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;
import java.util.function.*;

/** Reusable, good-looking Swing components: buttons, cards, fields, tables, dialogs. */
public class UI {

    public static Font font(int style, int size) { return new Font("Segoe UI", style, size); }

    public static String money(double v) { return "Rs " + String.format("%,.0f", v); }

    public static void aa(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }

    // ------------------------------------------------------------ labels
    public static JLabel label(String t, int size, boolean bold, Color c) {
        JLabel l = new JLabel(t);
        l.setFont(font(bold ? Font.BOLD : Font.PLAIN, size));
        l.setForeground(c);
        return l;
    }
    public static JLabel title(String t) { return label(t, 22, true, Theme.TEXT); }
    public static JLabel muted(String t) { return label(t, 12, false, Theme.MUTED); }

    public static JPanel transparent(LayoutManager lm) { JPanel p = new JPanel(lm); p.setOpaque(false); return p; }

    // ------------------------------------------------------------ button
    public static class Btn extends JButton {
        private Color base;
        private boolean outline, hover, press;

        public Btn(String text, Color base) { this(text, base, false); }

        public Btn(String text, Color base, boolean outline) {
            super(text);
            this.base = base; this.outline = outline;
            setContentAreaFilled(false); setBorderPainted(false); setFocusPainted(false); setOpaque(false);
            setFont(font(Font.BOLD, 13));
            setForeground(outline ? base : Color.WHITE);
            setBorder(new EmptyBorder(9, 18, 9, 18));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                public void mouseExited(MouseEvent e) { hover = false; press = false; repaint(); }
                public void mousePressed(MouseEvent e) { press = true; repaint(); }
                public void mouseReleased(MouseEvent e) { press = false; repaint(); }
            });
        }

        public void setStyle(Color base, boolean outline) {
            this.base = base; this.outline = outline;
            setForeground(outline ? base : Color.WHITE);
            repaint();
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            aa(g2);
            int w = getWidth(), h = getHeight();
            Color c = !isEnabled() ? Theme.blend(Theme.MUTED, Theme.CARD, 0.45f)
                    : press ? base.darker() : hover ? Theme.blend(Color.WHITE, base, 0.12f) : base;
            if (outline) {
                if (hover && isEnabled()) { g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 35)); g2.fillRoundRect(0, 0, w - 1, h - 1, 12, 12); }
                g2.setColor(c); g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(1, 1, w - 3, h - 3, 12, 12);
            } else {
                g2.setColor(c); g2.fillRoundRect(0, 0, w, h, 12, 12);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    public static Btn primary(String t) { return new Btn(t, Theme.PRIMARY); }
    public static Btn success(String t) { return new Btn(t, Theme.SUCCESS); }
    public static Btn danger(String t) { return new Btn(t, Theme.DANGER); }
    public static Btn info(String t) { return new Btn(t, Theme.INFO); }
    public static Btn warn(String t) { return new Btn(t, Theme.WARN); }
    public static Btn ghost(String t) { return new Btn(t, Theme.MUTED, true); }

    // ------------------------------------------------------------ card
    public static class Card extends JPanel {
        public Card(LayoutManager lm) { super(lm); setOpaque(false); setBorder(new EmptyBorder(16, 18, 16, 18)); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            aa(g2);
            int w = getWidth(), h = getHeight();
            g2.setColor(new Color(0, 0, 0, Theme.dark ? 60 : 16));
            g2.fillRoundRect(1, 2, w - 2, h - 3, 18, 18);
            g2.setColor(Theme.CARD);
            g2.fillRoundRect(0, 0, w - 2, h - 3, 18, 18);
            g2.setColor(Theme.BORDER);
            g2.drawRoundRect(0, 0, w - 3, h - 4, 18, 18);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // ------------------------------------------------------------ inputs
    public static class RoundBorder extends AbstractBorder {
        private final Color color; private final int arc;
        public RoundBorder(Color c, int arc) { color = c; this.arc = arc; }
        @Override public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g.create(); aa(g2);
            g2.setColor(color); g2.drawRoundRect(x, y, w - 1, h - 1, arc, arc); g2.dispose();
        }
        @Override public Insets getBorderInsets(Component c) { return new Insets(1, 1, 1, 1); }
    }

    private static void paintInputBg(Graphics g, JComponent c) {
        Graphics2D g2 = (Graphics2D) g.create(); aa(g2);
        g2.setColor(Theme.INPUT);
        g2.fillRoundRect(0, 0, c.getWidth() - 1, c.getHeight() - 1, 10, 10);
        g2.dispose();
    }

    private static void styleInput(JComponent f) {
        f.setFont(font(Font.PLAIN, 13));
        f.setForeground(Theme.TEXT);
        f.setOpaque(false);
        if (f instanceof javax.swing.text.JTextComponent) {
            ((javax.swing.text.JTextComponent) f).setCaretColor(Theme.TEXT);
            ((javax.swing.text.JTextComponent) f).setSelectionColor(Theme.PRIMARY_SOFT);
            ((javax.swing.text.JTextComponent) f).setSelectedTextColor(Theme.TEXT);
        }
        Border normal = new CompoundBorder(new RoundBorder(Theme.BORDER, 10), new EmptyBorder(7, 10, 7, 10));
        Border focus = new CompoundBorder(new RoundBorder(Theme.PRIMARY, 10), new EmptyBorder(7, 10, 7, 10));
        f.setBorder(normal);
        f.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) { f.setBorder(focus); f.repaint(); }
            public void focusLost(FocusEvent e) { f.setBorder(normal); f.repaint(); }
        });
    }

    public static class TField extends JTextField {
        public TField(int cols) { super(cols); styleInput(this); }
        public TField(String text, int cols) { this(cols); setText(text); }
        @Override protected void paintComponent(Graphics g) { paintInputBg(g, this); super.paintComponent(g); }
    }

    public static class PField extends JPasswordField {
        public PField(int cols) { super(cols); styleInput(this); }
        @Override protected void paintComponent(Graphics g) { paintInputBg(g, this); super.paintComponent(g); }
    }

    public static TField field(int cols) { return new TField(cols); }

    public static <T> JComboBox<T> combo(T[] items) { JComboBox<T> c = new JComboBox<>(items); styleCombo(c); return c; }
    public static <T> JComboBox<T> combo(Vector<T> items) { JComboBox<T> c = new JComboBox<>(items); styleCombo(c); return c; }

    private static void styleCombo(JComboBox<?> c) {
        c.setUI(new BasicComboBoxUI() {
            @Override protected JButton createArrowButton() {
                JButton b = new JButton("\u25BE");
                b.setBorder(new EmptyBorder(0, 6, 0, 8)); b.setContentAreaFilled(false); b.setFocusPainted(false);
                b.setForeground(Theme.MUTED); b.setFont(font(Font.BOLD, 14));
                return b;
            }
            @Override public void paintCurrentValueBackground(Graphics g, Rectangle b, boolean hasFocus) { }
        });
        c.setOpaque(false);
        c.setFont(font(Font.PLAIN, 13));
        c.setBackground(Theme.INPUT);
        c.setForeground(Theme.TEXT);
        c.setBorder(new CompoundBorder(new RoundBorder(Theme.BORDER, 10), new EmptyBorder(3, 6, 3, 2)));
        c.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> l, Object v, int i, boolean sel, boolean foc) {
                JLabel r = (JLabel) super.getListCellRendererComponent(l, v, i, false, false);
                r.setBorder(new EmptyBorder(6, 8, 6, 8));
                boolean hot = i >= 0 && sel;
                r.setOpaque(i >= 0);
                r.setBackground(hot ? Theme.PRIMARY_SOFT : Theme.INPUT);
                r.setForeground(Theme.TEXT);
                return r;
            }
        });
    }

    // ------------------------------------------------------------ table
    public static DefaultTableModel model(String... cols) {
        return new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
            @Override public Class<?> getColumnClass(int c) {
                for (int r = 0; r < getRowCount(); r++) { Object v = getValueAt(r, c); if (v != null) return v.getClass(); }
                return Object.class;
            }
        };
    }

    public static class StyledTable extends JTable {
        public Function<Integer, Color> rowColor; // model row -> highlight color (null = normal)
        private final Border pad = new EmptyBorder(0, 10, 0, 10);

        public StyledTable(DefaultTableModel m) {
            super(m);
            setRowHeight(34); setShowGrid(false); setIntercellSpacing(new Dimension(0, 0));
            setFillsViewportHeight(true); setFont(font(Font.PLAIN, 13));
            setForeground(Theme.TEXT); setBackground(Theme.CARD);
            setSelectionBackground(Theme.PRIMARY_SOFT); setSelectionForeground(Theme.TEXT);
            setAutoCreateRowSorter(true);
            setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
            m.addTableModelListener(e -> { if (!fitQueued) { fitQueued = true; SwingUtilities.invokeLater(() -> { fitQueued = false; fit(); }); } });
            JTableHeader h = getTableHeader();
            h.setReorderingAllowed(false);
            h.setPreferredSize(new Dimension(0, 38));
            h.setDefaultRenderer(new DefaultTableCellRenderer() {
                @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
                    JLabel l = (JLabel) super.getTableCellRendererComponent(t, v, s, f, r, c);
                    String txt = String.valueOf(v);
                    java.util.List<? extends RowSorter.SortKey> keys = t.getRowSorter() == null ? null : t.getRowSorter().getSortKeys();
                    if (keys != null && !keys.isEmpty() && keys.get(0).getColumn() == t.convertColumnIndexToModel(c))
                        txt += keys.get(0).getSortOrder() == SortOrder.ASCENDING ? "  \u25B2" : "  \u25BC";
                    l.setText(txt);
                    l.setBackground(Theme.PRIMARY); l.setForeground(Color.WHITE);
                    l.setFont(font(Font.BOLD, 12)); l.setBorder(pad); l.setOpaque(true);
                    l.setHorizontalAlignment(SwingConstants.LEFT);
                    return l;
                }
            });
        }

        @Override public Component prepareRenderer(TableCellRenderer r, int row, int col) {
            Component c = super.prepareRenderer(r, row, col);
            if (c instanceof JComponent) ((JComponent) c).setBorder(pad);
            if (c instanceof JLabel) ((JLabel) c).setHorizontalAlignment(SwingConstants.LEFT);
            if (isRowSelected(row)) { c.setBackground(Theme.PRIMARY_SOFT); }
            else {
                Color rc = rowColor == null ? null : rowColor.apply(convertRowIndexToModel(row));
                c.setBackground(rc != null ? rc : (row % 2 == 0 ? Theme.CARD : Theme.ALT));
            }
            c.setForeground(Theme.TEXT);
            return c;
        }

        private boolean fitQueued = false;

        /** Sets every column width to fit its content. */
        public void fit() {
            FontMetrics fm = getFontMetrics(getFont()), hf = getFontMetrics(font(Font.BOLD, 12));
            for (int c = 0; c < getColumnCount(); c++) {
                int w = hf.stringWidth(String.valueOf(getColumnName(c))) + 44;
                for (int r = 0; r < Math.min(getRowCount(), 80); r++) {
                    Object v = getValueAt(r, c);
                    String s = v instanceof Double ? String.format("%,.0f", (Double) v) : String.valueOf(v);
                    w = Math.max(w, fm.stringWidth(s) + 40);
                }
                getColumnModel().getColumn(c).setPreferredWidth(Math.min(w, 420));
            }
        }

        /** Returns the first column (ID) of the selected row, or -1. */
        public int selectedId() {
            int r = getSelectedRow();
            if (r < 0) return -1;
            Object v = getModel().getValueAt(convertRowIndexToModel(r), 0);
            return v instanceof Number ? ((Number) v).intValue() : -1;
        }
    }

    public static class SlimScroll extends BasicScrollBarUI {
        @Override protected void configureScrollBarColors() { thumbColor = Theme.BORDER; trackColor = Theme.CARD; }
        @Override protected JButton createDecreaseButton(int o) { return zero(); }
        @Override protected JButton createIncreaseButton(int o) { return zero(); }
        private JButton zero() { JButton b = new JButton(); b.setPreferredSize(new Dimension(0, 0)); return b; }
        @Override protected void paintTrack(Graphics g, JComponent c, Rectangle r) { g.setColor(Theme.CARD); g.fillRect(r.x, r.y, r.width, r.height); }
        @Override protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
            Graphics2D g2 = (Graphics2D) g.create(); aa(g2);
            g2.setColor(Theme.blend(Theme.MUTED, Theme.CARD, 0.5f));
            g2.fillRoundRect(r.x + 2, r.y + 2, r.width - 4, r.height - 4, 8, 8);
            g2.dispose();
        }
    }

    public static JScrollPane scroll(JComponent c) {
        JScrollPane sp = new JScrollPane(c);
        sp.setBorder(new RoundBorder(Theme.BORDER, 12));
        sp.getViewport().setBackground(Theme.CARD);
        sp.setBackground(Theme.CARD);
        sp.getVerticalScrollBar().setUI(new SlimScroll());
        sp.getHorizontalScrollBar().setUI(new SlimScroll());
        sp.getVerticalScrollBar().setPreferredSize(new Dimension(10, 0));
        sp.getHorizontalScrollBar().setPreferredSize(new Dimension(0, 10));
        sp.getVerticalScrollBar().setUnitIncrement(16);
        return sp;
    }

    public static JScrollPane plainScroll(JComponent c) {
        JScrollPane sp = scroll(c);
        sp.setBorder(null); sp.setOpaque(false); sp.getViewport().setOpaque(false);
        return sp;
    }

    // ------------------------------------------------------------ tabs
    public static class Tabs extends JPanel {
        private final CardLayout cl = new CardLayout();
        private final JPanel body = new JPanel(cl);
        private final JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        private final Map<String, Btn> btns = new LinkedHashMap<>();
        private final Map<String, Runnable> hooks = new HashMap<>();

        public Tabs() {
            super(new BorderLayout(0, 12));
            setOpaque(false); body.setOpaque(false); bar.setOpaque(false);
            add(bar, BorderLayout.NORTH); add(body, BorderLayout.CENTER);
        }
        public void addTab(String name, JComponent comp, Runnable onShow) {
            Btn b = new Btn(name, Theme.MUTED, true);
            b.addActionListener(e -> select(name));
            btns.put(name, b); bar.add(b); body.add(comp, name); hooks.put(name, onShow);
            if (btns.size() == 1) select(name);
        }
        public void select(String name) {
            cl.show(body, name);
            for (Map.Entry<String, Btn> e : btns.entrySet()) e.getValue().setStyle(e.getKey().equals(name) ? Theme.PRIMARY : Theme.MUTED, !e.getKey().equals(name));
            Runnable r = hooks.get(name);
            if (r != null) r.run();
        }
    }

    // ------------------------------------------------------------ logo (leaf)
    public static class Logo extends JComponent {
        private final int size;
        public Logo(int size) { this.size = size; setPreferredSize(new Dimension(size, size)); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create(); aa(g2);
            g2.setPaint(new GradientPaint(0, 0, Theme.blend(Color.WHITE, Theme.PRIMARY, 0.25f), size, size, Theme.PRIMARY_DARK));
            g2.fillOval(0, 0, size - 1, size - 1);
            double s = size;
            Path2D leaf = new Path2D.Double();
            leaf.moveTo(s * .27, s * .74);
            leaf.curveTo(s * .20, s * .35, s * .45, s * .22, s * .78, s * .24);
            leaf.curveTo(s * .80, s * .55, s * .62, s * .78, s * .27, s * .74);
            g2.setColor(Color.WHITE); g2.fill(leaf);
            g2.setColor(Theme.PRIMARY_DARK); g2.setStroke(new BasicStroke((float) (s * .035), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(new Line2D.Double(s * .27, s * .74, s * .64, s * .38));
            g2.dispose();
        }
    }

    // ------------------------------------------------------------ dialogs
    private static Window win(Component c) { return c == null ? null : SwingUtilities.getWindowAncestor(c); }

    private static boolean msg(Component parent, String title, String text, Color accent, boolean ask) {
        JDialog d = new JDialog(win(parent), title, Dialog.ModalityType.APPLICATION_MODAL);
        final boolean[] res = {false};
        JPanel root = new JPanel(new BorderLayout(0, 16));
        root.setBackground(Theme.CARD);
        root.setBorder(new CompoundBorder(new MatteBorder(4, 0, 0, 0, accent), new EmptyBorder(20, 24, 18, 24)));
        JLabel l = new JLabel("<html><body style='width:300px'>" + text.replace("&", "&amp;").replace("<", "&lt;").replace("\n", "<br>") + "</body></html>");
        l.setFont(font(Font.PLAIN, 14)); l.setForeground(Theme.TEXT);
        JPanel bp = transparent(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        if (ask) {
            Btn no = ghost("No"); no.addActionListener(e -> d.dispose());
            Btn yes = new Btn("Yes", accent); yes.addActionListener(e -> { res[0] = true; d.dispose(); });
            bp.add(no); bp.add(yes);
        } else {
            Btn ok = new Btn("OK", accent); ok.addActionListener(e -> d.dispose()); bp.add(ok);
            d.getRootPane().setDefaultButton(ok);
        }
        root.add(label(title, 17, true, Theme.TEXT), BorderLayout.NORTH);
        root.add(l, BorderLayout.CENTER); root.add(bp, BorderLayout.SOUTH);
        d.setContentPane(root); d.pack(); d.setLocationRelativeTo(parent); d.setVisible(true);
        return res[0];
    }

    public static void showInfo(Component p, String t) { msg(p, "Information", t, Theme.INFO, false); }
    public static void showOk(Component p, String t) { msg(p, "Success", t, Theme.SUCCESS, false); }
    public static void showError(Component p, String t) { msg(p, "Error", t, Theme.DANGER, false); }
    public static boolean confirm(Component p, String t) { return msg(p, "Confirm", t, Theme.DANGER, true); }

    /** Form dialog: labels + inputs with validation. */
    public static class Form {
        private final JDialog d;
        private final JPanel grid = transparent(new GridBagLayout());
        private final JLabel err = label(" ", 12, true, Theme.DANGER);
        private int row = 0;
        private boolean ok = false;

        public Form(Component parent, String title) {
            d = new JDialog(win(parent), title, Dialog.ModalityType.APPLICATION_MODAL);
            this.parent = parent; this.title = title;
        }
        private final Component parent; private final String title;

        public Form add(String lbl, JComponent c) {
            GridBagConstraints g = new GridBagConstraints();
            g.gridy = row++; g.insets = new Insets(5, 0, 5, 12); g.anchor = GridBagConstraints.WEST;
            grid.add(label(lbl, 13, true, Theme.MUTED), g);
            g.gridx = 1; g.weightx = 1; g.fill = GridBagConstraints.HORIZONTAL; g.insets = new Insets(5, 0, 5, 0);
            if (c.getPreferredSize().width < 260) c.setPreferredSize(new Dimension(260, c.getPreferredSize().height));
            grid.add(c, g);
            return this;
        }

        public boolean show(Supplier<String> validator, String saveText) {
            JPanel root = new JPanel(new BorderLayout(0, 12));
            root.setBackground(Theme.CARD);
            root.setBorder(new CompoundBorder(new MatteBorder(4, 0, 0, 0, Theme.PRIMARY), new EmptyBorder(18, 24, 16, 24)));
            Btn cancel = ghost("Cancel"); cancel.addActionListener(e -> d.dispose());
            Btn save = primary(saveText); 
            save.addActionListener(e -> {
                String m = validator == null ? null : validator.get();
                if (m != null) { err.setText(m); d.pack(); } else { ok = true; d.dispose(); }
            });
            JPanel bp = transparent(new FlowLayout(FlowLayout.RIGHT, 8, 0)); bp.add(cancel); bp.add(save);
            JPanel south = transparent(new BorderLayout(0, 8)); south.add(err, BorderLayout.NORTH); south.add(bp, BorderLayout.SOUTH);
            root.add(label(title, 18, true, Theme.TEXT), BorderLayout.NORTH);
            root.add(grid, BorderLayout.CENTER); root.add(south, BorderLayout.SOUTH);
            d.setContentPane(root); d.getRootPane().setDefaultButton(save);
            d.pack(); d.setLocationRelativeTo(parent); d.setVisible(true);
            return ok;
        }
    }

    // ------------------------------------------------------------ parsing helpers
    public static int toInt(String s) throws NumberFormatException { return Integer.parseInt(s.trim()); }
    public static double toDouble(String s) throws NumberFormatException { return Double.parseDouble(s.trim().replace(",", "")); }
    public static boolean validDate(String s) {
        if (s.trim().isEmpty()) return true;
        try { java.time.LocalDate.parse(s.trim()); return true; } catch (Exception e) { return false; }
    }
}
