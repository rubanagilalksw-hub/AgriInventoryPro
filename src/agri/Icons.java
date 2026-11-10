package agri;

import java.awt.*;
import java.awt.geom.*;

/** Small vector icons drawn with Java2D (no image files needed). */
public class Icons {
    public static void draw(Graphics2D g0, String name, double x, double y, double size, Color c) {
        Graphics2D g = (Graphics2D) g0.create();
        UI.aa(g);
        g.translate(x, y);
        double k = size / 20.0;
        g.scale(k, k);
        g.setColor(c);
        g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        switch (name) {
            case "dashboard":
                g.draw(new RoundRectangle2D.Double(2, 2, 6.5, 6.5, 2, 2)); g.draw(new RoundRectangle2D.Double(11.5, 2, 6.5, 6.5, 2, 2));
                g.draw(new RoundRectangle2D.Double(2, 11.5, 6.5, 6.5, 2, 2)); g.draw(new RoundRectangle2D.Double(11.5, 11.5, 6.5, 6.5, 2, 2)); break;
            case "products":
                g.draw(new RoundRectangle2D.Double(2.5, 6, 15, 11.5, 2, 2)); g.draw(new Line2D.Double(2.5, 6, 5, 2.5));
                g.draw(new Line2D.Double(17.5, 6, 15, 2.5)); g.draw(new Line2D.Double(5, 2.5, 15, 2.5)); g.draw(new Line2D.Double(10, 6, 10, 10)); break;
            case "farmers":
                g.draw(new Ellipse2D.Double(6.5, 2.5, 7, 7)); g.draw(new Arc2D.Double(3, 11, 14, 14, 0, 180, Arc2D.OPEN)); break;
            case "suppliers":
                g.draw(new RoundRectangle2D.Double(1.5, 5, 10, 8, 2, 2)); g.draw(new Path2D.Double() {{ moveTo(11.5, 8); lineTo(15.5, 8); lineTo(18.5, 11); lineTo(18.5, 13); lineTo(11.5, 13); }});
                g.fill(new Ellipse2D.Double(4, 13, 4, 4)); g.fill(new Ellipse2D.Double(13, 13, 4, 4)); break;
            case "queue":
                for (int i = 0; i < 3; i++) { g.draw(new Ellipse2D.Double(1.5 + i * 6.2, 3, 4.6, 4.6)); g.draw(new Arc2D.Double(0.8 + i * 6.2, 9, 6, 8, 0, 180, Arc2D.OPEN)); }
                g.draw(new Line2D.Double(2, 18, 18, 18)); break;
            case "stock":
                g.draw(new RoundRectangle2D.Double(3, 2.5, 14, 4.2, 2, 2)); g.draw(new RoundRectangle2D.Double(3, 8, 14, 4.2, 2, 2)); g.draw(new RoundRectangle2D.Double(3, 13.5, 14, 4.2, 2, 2)); break;
            case "seasons": {
                Path2D p = new Path2D.Double(); p.moveTo(3, 17); p.curveTo(2, 7, 9, 2, 17, 3); p.curveTo(18, 11, 12, 18, 3, 17); g.draw(p); g.draw(new Line2D.Double(3, 17, 11, 9)); break; }
            case "reports":
                g.draw(new Line2D.Double(2.5, 17.5, 17.5, 17.5)); g.draw(new RoundRectangle2D.Double(4, 10, 3, 7, 1, 1)); g.draw(new RoundRectangle2D.Double(8.5, 5, 3, 12, 1, 1)); g.draw(new RoundRectangle2D.Double(13, 8, 3, 9, 1, 1)); break;
            case "insights":
                g.draw(new Ellipse2D.Double(4.5, 1.5, 11, 11)); g.draw(new Line2D.Double(7.5, 15, 12.5, 15)); g.draw(new Line2D.Double(8.5, 17.8, 11.5, 17.8)); g.draw(new Line2D.Double(10, 5, 10, 9)); break;
            case "database":
                g.draw(new Ellipse2D.Double(3, 2, 14, 5)); g.draw(new Line2D.Double(3, 4.5, 3, 15)); g.draw(new Line2D.Double(17, 4.5, 17, 15));
                g.draw(new Arc2D.Double(3, 10, 14, 6, 180, 180, Arc2D.OPEN)); g.draw(new Arc2D.Double(3, 6, 14, 6, 180, 180, Arc2D.OPEN)); break;
            case "activity":
                g.draw(new Ellipse2D.Double(2.5, 2.5, 15, 15)); g.draw(new Line2D.Double(10, 6, 10, 10)); g.draw(new Line2D.Double(10, 10, 13, 12)); break;
            case "settings":
                g.draw(new Ellipse2D.Double(6, 6, 8, 8));
                for (int i = 0; i < 8; i++) { double a = Math.PI / 4 * i; g.draw(new Line2D.Double(10 + Math.cos(a) * 6.5, 10 + Math.sin(a) * 6.5, 10 + Math.cos(a) * 8.6, 10 + Math.sin(a) * 8.6)); } break;
            case "money":
                g.draw(new Ellipse2D.Double(2.5, 2.5, 15, 15)); g.draw(new Line2D.Double(10, 5.5, 10, 14.5)); g.draw(new Arc2D.Double(7, 6.5, 6, 3.5, 90, 180, Arc2D.OPEN)); g.draw(new Arc2D.Double(7, 10, 6, 3.5, -90, 180, Arc2D.OPEN)); break;
            case "alert":
                g.draw(new Path2D.Double() {{ moveTo(10, 2.5); lineTo(18, 17); lineTo(2, 17); closePath(); }}); g.draw(new Line2D.Double(10, 8, 10, 12)); g.fill(new Ellipse2D.Double(9.2, 13.6, 1.8, 1.8)); break;
            case "orders":
                g.draw(new RoundRectangle2D.Double(3.5, 3, 13, 15, 2, 2)); g.draw(new Line2D.Double(7, 8, 13, 8)); g.draw(new Line2D.Double(7, 11.5, 13, 11.5)); g.draw(new Line2D.Double(7, 15, 10.5, 15)); break;
            default:
                g.draw(new Ellipse2D.Double(3, 3, 14, 14));
        }
        g.dispose();
    }
}
