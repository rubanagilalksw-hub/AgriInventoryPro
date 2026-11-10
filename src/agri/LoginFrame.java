package agri;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;

/** Login screen with an animated farm-themed side panel. */
public class LoginFrame extends JFrame {

    /** Left panel: gradient sky, sun, floating bubbles and swaying wheat. */
    private static class Hero extends JPanel {
        private double t = 0;
        Hero() {
            super(null);
            new Timer(30, e -> { t += 0.03; repaint(); }).start();
        }

        private void wheat(Graphics2D g, double x, double base, double h, double phase, float alpha) {
            double sway = Math.sin(t + phase) * 9;
            g.setColor(new Color(255, 236, 160, (int) (alpha * 255)));
            g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            Path2D stem = new Path2D.Double();
            stem.moveTo(x, base);
            stem.quadTo(x + sway * 0.3, base - h * 0.5, x + sway, base - h);
            g.draw(stem);
            for (int i = 0; i < 6; i++) {                       // grains on both sides of the stem
                double f = 0.55 + i * 0.075;
                double px = x + sway * f * f * 1.05, py = base - h * f;
                AffineTransform old = g.getTransform();
                g.translate(px, py);
                g.rotate(-0.7 + sway * 0.01); g.fill(new Ellipse2D.Double(-9, -3, 10, 5));
                g.setTransform(old); g.translate(px, py);
                g.rotate(0.7 + sway * 0.01); g.fill(new Ellipse2D.Double(-1, -3, 10, 5));
                g.setTransform(old);
            }
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create(); UI.aa(g);
            int w = getWidth(), h = getHeight();
            g.setPaint(new GradientPaint(0, 0, Theme.PRIMARY_DARK, w, h, Theme.PRIMARY));
            g.fillRect(0, 0, w, h);
            // sun
            g.setPaint(new RadialGradientPaint(new Point2D.Double(w - 90, 95), 130, new float[]{0f, 1f},
                    new Color[]{new Color(255, 240, 170, 170), new Color(255, 240, 170, 0)}));
            g.fill(new Ellipse2D.Double(w - 220, -35, 260, 260));
            g.setColor(new Color(255, 244, 190, 230)); g.fill(new Ellipse2D.Double(w - 120, 65, 60, 60));
            // floating bubbles
            for (int i = 0; i < 9; i++) {
                double bx = (i * 83 + 40) % (w - 30), by = h - ((t * 24 + i * 71) % (h + 60));
                g.setColor(new Color(255, 255, 255, 22 + (i % 3) * 8));
                g.fill(new Ellipse2D.Double(bx, by, 22 + (i % 4) * 14, 22 + (i % 4) * 14));
            }
            // hills
            g.setColor(new Color(0, 0, 0, 38));
            Path2D hill = new Path2D.Double(); hill.moveTo(0, h - 90); hill.curveTo(w * 0.3, h - 170, w * 0.6, h - 40, w, h - 120); hill.lineTo(w, h); hill.lineTo(0, h); hill.closePath();
            g.fill(hill);
            // wheat
            for (int i = 0; i < 14; i++) wheat(g, 18 + i * (w - 36) / 13.0, h + 4, 110 + (i * 37 % 50), i * 0.7, i % 2 == 0 ? 0.85f : 0.55f);
            g.dispose();
        }
    }

    public LoginFrame() {
        super("Agri Inventory Pro - Login");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        Hero left = new Hero();
        left.setPreferredSize(new Dimension(420, 0));
        UI.Logo logo = new UI.Logo(84); logo.setBounds(48, 92, 84, 84);
        JLabel t1 = UI.label("Agri Inventory Pro", 30, true, Color.WHITE); t1.setBounds(48, 196, 380, 40);
        JLabel t2 = UI.label("Seed, Fertilizer & Pesticide Store", 15, false, new Color(255, 255, 255, 225)); t2.setBounds(48, 238, 380, 24);
        JLabel t3 = UI.label("Inventory  \u2022  Order Queue  \u2022  Billing  \u2022  Reports", 13, false, new Color(255, 255, 255, 200)); t3.setBounds(48, 274, 380, 22);
        JLabel t4 = UI.label("Smart stock forecasting for Rabi and Kharif seasons", 13, false, new Color(255, 255, 255, 200)); t4.setBounds(48, 298, 390, 22);
        left.add(logo); left.add(t1); left.add(t2); left.add(t3); left.add(t4);

        JPanel right = new JPanel(new GridBagLayout());
        right.setBackground(Theme.CARD);
        JPanel form = UI.transparent(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0; g.fill = GridBagConstraints.HORIZONTAL; g.weightx = 1; g.insets = new Insets(4, 0, 4, 0);

        UI.TField user = UI.field(20); UI.PField pass = new UI.PField(20);
        user.setPreferredSize(new Dimension(320, 40)); pass.setPreferredSize(new Dimension(320, 40));
        JLabel err = UI.label(" ", 12, true, Theme.DANGER);
        JCheckBox show = new JCheckBox("Show password");
        show.setOpaque(false); show.setForeground(Theme.MUTED); show.setFont(UI.font(Font.PLAIN, 12)); show.setFocusPainted(false);
        char echo = pass.getEchoChar();
        show.addActionListener(e -> pass.setEchoChar(show.isSelected() ? (char) 0 : echo));
        UI.Btn login = UI.primary("Login");
        login.setPreferredSize(new Dimension(320, 42));

        Runnable doLogin = () -> {
            Models.User u = DataStore.login(user.getText(), new String(pass.getPassword()));
            if (u == null) { err.setText("Wrong username or password."); return; }
            DataStore.current = u;
            DataStore.log("Logged in");
            dispose();
            Main.showMain("dashboard");
        };
        login.addActionListener(e -> doLogin.run());
        pass.addActionListener(e -> doLogin.run());
        user.addActionListener(e -> pass.requestFocus());

        g.gridy = 0; form.add(UI.label("Welcome back!", 28, true, Theme.TEXT), g);
        g.gridy = 1; g.insets = new Insets(0, 0, 22, 0); form.add(UI.muted("Log in to your account"), g);
        g.insets = new Insets(4, 0, 4, 0);
        g.gridy = 2; form.add(UI.label("Username", 13, true, Theme.MUTED), g);
        g.gridy = 3; form.add(user, g);
        g.gridy = 4; g.insets = new Insets(10, 0, 4, 0); form.add(UI.label("Password", 13, true, Theme.MUTED), g);
        g.gridy = 5; g.insets = new Insets(4, 0, 4, 0); form.add(pass, g);
        g.gridy = 6; form.add(show, g);
        g.gridy = 7; form.add(err, g);
        g.gridy = 8; g.insets = new Insets(6, 0, 10, 0); form.add(login, g);
        right.add(form);

        JPanel root = new JPanel(new BorderLayout());
        root.add(left, BorderLayout.WEST); root.add(right, BorderLayout.CENTER);
        setContentPane(root);
        Rectangle sc = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        setSize(Math.min(940, sc.width - 20), Math.min(560, sc.height - 20)); setResizable(false); setLocationRelativeTo(null);
        getRootPane().setDefaultButton(login);
    }
}
