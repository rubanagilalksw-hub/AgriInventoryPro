package agri;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;

/** Small start-up splash screen with an animated progress bar. */
public class Splash {
    private static JWindow w;
    private static Timer t;

    public static void show() {
        if (Theme.PRIMARY == null) Theme.apply(false, "Green");
        w = new JWindow();
        final double[] p = {0};
        JPanel panel = new JPanel(null) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create(); UI.aa(g2);
                g2.setPaint(new GradientPaint(0, 0, Color.decode("#14532D"), getWidth(), getHeight(), Color.decode("#16A34A")));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 28, 28);
                g2.setColor(new Color(255, 255, 255, 25));
                g2.fill(new Ellipse2D.Double(-60, getHeight() - 120, 220, 220));
                g2.fill(new Ellipse2D.Double(getWidth() - 120, -70, 220, 220));
                g2.setColor(Color.WHITE); g2.setFont(new Font("Segoe UI", Font.BOLD, 30));
                g2.drawString("Agri Inventory Pro", 100, 118);
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 14)); g2.setColor(new Color(255, 255, 255, 210));
                g2.drawString("Loading your data...", 100, 144);
                g2.setColor(new Color(255, 255, 255, 60)); g2.fillRoundRect(100, 172, 300, 8, 8, 8);
                g2.setColor(Color.WHITE); g2.fillRoundRect(100, 172, (int) (300 * Math.min(1, p[0])), 8, 8, 8);
                g2.dispose();
            }
        };
        panel.setOpaque(false);
        UI.Logo logo = new UI.Logo(56); logo.setBounds(30, 84, 56, 56); panel.add(logo);
        w.setBackground(new Color(0, 0, 0, 0));
        w.setContentPane(panel);
        w.setSize(450, 240);
        w.setLocationRelativeTo(null);
        w.setVisible(true);
        t = new Timer(20, e -> { p[0] += 0.02; panel.repaint(); });
        t.start();
    }

    public static void hide() {
        if (t != null) t.stop();
        if (w != null) w.dispose();
    }
}
