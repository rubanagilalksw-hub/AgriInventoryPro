package agri;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;

public class Widgets {
    /** Dashboard statistic card: coloured bar, icon badge and an animated count-up value. */
    public static class StatCard extends UI.Card {
        private final JLabel value = UI.label("0", 20, true, Theme.TEXT);
        private final Color accent;
        private final String icon;
        private Timer anim;

        public StatCard(String label, Color accent) { this(label, accent, null); }

        public StatCard(String label, Color accent, String icon) {
            super(new BorderLayout(0, 2));
            this.accent = accent;
            this.icon = icon;
            setBorder(javax.swing.BorderFactory.createEmptyBorder(14, 22, 14, 12));
            add(UI.label(label, 12, true, Theme.MUTED), BorderLayout.NORTH);
            add(value, BorderLayout.CENTER);
        }

        /** Sets the text; numbers (also "Rs 1,200") count up smoothly. */
        public void setValue(String v) {
            if (anim != null) anim.stop();
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("^(\\D*)([\\d,]+)(.*)$").matcher(v);
            if (!m.matches() || m.group(2).replace(",", "").isEmpty()) { value.setText(v); return; }
            final String pre = m.group(1), suf = m.group(3);
            final long target = Long.parseLong(m.group(2).replace(",", ""));
            final boolean comma = m.group(2).contains(",");
            final long[] step = {0};
            anim = new Timer(16, null);
            anim.addActionListener(e -> {
                step[0]++;
                double t = Math.min(1, step[0] / 30.0);
                long cur = Math.round(target * (1 - Math.pow(1 - t, 3)));
                value.setText(pre + (comma ? String.format("%,d", cur) : String.valueOf(cur)) + suf);
                if (t >= 1) anim.stop();
            });
            anim.start();
        }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create(); UI.aa(g2);
            g2.setColor(accent); g2.fillRoundRect(9, 14, 5, getHeight() - 31, 5, 5);
            if (icon != null) {
                int d = 30, x = getWidth() - d - 10, y = 10;
                g2.setColor(Theme.blend(accent, Theme.CARD, Theme.dark ? 0.30f : 0.16f));
                g2.fill(new Ellipse2D.Double(x, y, d, d));
                Icons.draw(g2, icon, x + 6, y + 6, 18, accent);
            }
            g2.dispose();
        }
    }

    public static UI.Card titled(String title, JComponent body) {
        UI.Card c = new UI.Card(new BorderLayout(0, 8));
        c.add(UI.label(title, 14, true, Theme.TEXT), BorderLayout.NORTH);
        c.add(body, BorderLayout.CENTER);
        return c;
    }
}
