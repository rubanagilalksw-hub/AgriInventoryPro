package agri;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

/** Hand-made Bar and Pie (donut) charts - no library needed. */
public class Charts {

    public static Color[] palette() {
        return new Color[]{Theme.PRIMARY, Color.decode("#F59E0B"), Color.decode("#0EA5E9"), Color.decode("#8B5CF6"),
                Color.decode("#EF4444"), Color.decode("#14B8A6"), Color.decode("#EC4899"), Color.decode("#84CC16")};
    }

    static String shortNum(double v) {
        if (v >= 1_000_000) return String.format("%.1fM", v / 1_000_000);
        if (v >= 1_000) return String.format("%.0fk", v / 1_000);
        return String.valueOf((long) v);
    }

    public static class BarChart extends JComponent {
        private List<String> labels = new ArrayList<>();
        private List<Double> values = new ArrayList<>();
        private Color color;
        private double prog = 1;            // growth animation 0..1
        private javax.swing.Timer grow;

        public BarChart(Color c) { color = c; setPreferredSize(new Dimension(300, 220)); }

        public void setData(List<String> l, List<Double> v) {
            labels = l; values = v; prog = 0;
            if (grow != null) grow.stop();
            grow = new javax.swing.Timer(16, e -> { prog = Math.min(1, prog + 0.06); repaint(); if (prog >= 1) grow.stop(); });
            grow.start();
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create(); UI.aa(g2);
            int w = getWidth(), h = getHeight();
            int left = 52, right = 10, top = 14, bottom = 30;
            int pw = w - left - right, ph = h - top - bottom;
            double max = 0; for (double v : values) max = Math.max(max, v);
            if (max <= 0) max = 1;
            double mag = Math.pow(10, Math.floor(Math.log10(max)));
            double norm = max / mag;
            double niceMax = (norm <= 1 ? 1 : norm <= 2 ? 2 : norm <= 5 ? 5 : 10) * mag;

            g2.setFont(UI.font(Font.PLAIN, 11));
            FontMetrics fm = g2.getFontMetrics();
            for (int i = 0; i <= 4; i++) {
                int y = top + ph - (int) (ph * i / 4.0);
                g2.setColor(Theme.BORDER); g2.drawLine(left, y, w - right, y);
                String t = shortNum(niceMax * i / 4.0);
                g2.setColor(Theme.MUTED); g2.drawString(t, left - 8 - fm.stringWidth(t), y + 4);
            }
            int n = values.size();
            if (n == 0) { g2.setColor(Theme.MUTED); g2.drawString("No data", w / 2 - 30, h / 2); g2.dispose(); return; }
            double slot = (double) pw / n;
            int bw = (int) Math.min(48, slot * 0.6);
            for (int i = 0; i < n; i++) {
                int bh = (int) (ph * values.get(i) / niceMax * (1 - Math.pow(1 - prog, 3)));
                int x = left + (int) (slot * i + (slot - bw) / 2);
                int y = top + ph - bh;
                if (bh > 0) {
                    g2.setPaint(new GradientPaint(0, y, color, 0, y + bh, Theme.blend(color, Theme.CARD, 0.55f)));
                    g2.fill(new RoundRectangle2D.Double(x, y, bw, bh + 6, 8, 8));
                    g2.setColor(Theme.CARD); g2.fillRect(x - 1, top + ph, bw + 2, 8); // cut the bottom flat
                    if (bw >= 26 && values.get(i) > 0 && prog >= 1) {
                        String vt = shortNum(values.get(i));
                        g2.setColor(Theme.TEXT); g2.drawString(vt, x + (bw - fm.stringWidth(vt)) / 2, y - 4);
                    }
                }
                String lb = labels.get(i);
                if (n > 8 && (n - 1 - i) % 2 == 1) continue;
                g2.setColor(Theme.MUTED);
                g2.drawString(lb, (float) (left + slot * i + (slot - fm.stringWidth(lb)) / 2), h - 10);
            }
            g2.dispose();
        }
    }

    public static class PieChart extends JComponent {
        private List<String> labels = new ArrayList<>();
        private List<Double> values = new ArrayList<>();

        public PieChart() { setPreferredSize(new Dimension(300, 220)); }
        public void setData(List<String> l, List<Double> v) { labels = l; values = v; repaint(); }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create(); UI.aa(g2);
            int w = getWidth(), h = getHeight();
            double total = 0; for (double v : values) total += v;
            if (total <= 0) { g2.setColor(Theme.MUTED); g2.setFont(UI.font(Font.PLAIN, 12)); g2.drawString("No data", w / 2 - 30, h / 2); g2.dispose(); return; }
            int d = Math.min(h - 16, (int) (w * 0.5));
            int x0 = 8, y0 = (h - d) / 2;
            Color[] pal = palette();
            double start = 90;
            for (int i = 0; i < values.size(); i++) {
                double ext = -360.0 * values.get(i) / total;
                g2.setColor(pal[i % pal.length]);
                g2.fill(new Arc2D.Double(x0, y0, d, d, start, ext, Arc2D.PIE));
                start += ext;
            }
            g2.setColor(Theme.CARD);
            int hole = (int) (d * 0.56);
            g2.fillOval(x0 + (d - hole) / 2, y0 + (d - hole) / 2, hole, hole);
            // legend
            g2.setFont(UI.font(Font.PLAIN, 12));
            FontMetrics fm = g2.getFontMetrics();
            int lx = x0 + d + 18, ly = Math.max(14, (h - values.size() * 22) / 2);
            for (int i = 0; i < values.size(); i++) {
                g2.setColor(pal[i % pal.length]);
                g2.fillRoundRect(lx, ly + i * 22, 12, 12, 4, 4);
                g2.setColor(Theme.TEXT);
                String t = labels.get(i) + "  " + Math.round(values.get(i) * 100 / total) + "%";
                int maxW = w - lx - 22;
                while (fm.stringWidth(t) > maxW && t.length() > 6) t = t.substring(0, t.length() - 2);
                g2.drawString(t, lx + 20, ly + i * 22 + 11);
            }
            g2.dispose();
        }
    }
}
