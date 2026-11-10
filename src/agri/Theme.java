package agri;

import java.awt.Color;
import javax.swing.UIManager;

/** Light / Dark theme + accent colors. Can be changed in Settings. */
public class Theme {
    public static boolean dark = false;
    public static String accent = "Green";
    public static final String[] ACCENTS = {"Green", "Blue", "Orange", "Purple", "Red"};

    public static Color BG, CARD, TEXT, MUTED, BORDER, SIDEBAR, SIDEBAR_TEXT, INPUT, ALT,
            PRIMARY, PRIMARY_DARK, PRIMARY_SOFT,
            SUCCESS = Color.decode("#16A34A"), WARN = Color.decode("#F59E0B"),
            DANGER = Color.decode("#DC2626"), INFO = Color.decode("#0EA5E9");

    public static Color blend(Color a, Color b, float t) {
        return new Color(Math.round(a.getRed() * t + b.getRed() * (1 - t)),
                Math.round(a.getGreen() * t + b.getGreen() * (1 - t)),
                Math.round(a.getBlue() * t + b.getBlue() * (1 - t)));
    }

    /** A light version of any colour (used for row highlights). */
    public static Color tint(Color c) { return blend(c, CARD, dark ? 0.28f : 0.16f); }

    public static void apply(boolean isDark, String acc) {
        dark = isDark;
        accent = acc;
        switch (acc) {
            case "Blue":   PRIMARY = Color.decode("#2563EB"); PRIMARY_DARK = Color.decode("#1E3A8A"); break;
            case "Orange": PRIMARY = Color.decode("#EA580C"); PRIMARY_DARK = Color.decode("#7C2D12"); break;
            case "Purple": PRIMARY = Color.decode("#7C3AED"); PRIMARY_DARK = Color.decode("#3B0764"); break;
            case "Red":    PRIMARY = Color.decode("#DC2626"); PRIMARY_DARK = Color.decode("#7F1D1D"); break;
            default:       PRIMARY = Color.decode("#16A34A"); PRIMARY_DARK = Color.decode("#14532D"); accent = "Green";
        }
        if (dark) {
            BG = Color.decode("#0F172A"); CARD = Color.decode("#1E293B"); TEXT = Color.decode("#F1F5F9");
            MUTED = Color.decode("#94A3B8"); BORDER = Color.decode("#334155"); SIDEBAR = Color.decode("#020617");
            INPUT = Color.decode("#0F172A"); ALT = Color.decode("#243247");
        } else {
            BG = Color.decode("#F1F5F9"); CARD = Color.WHITE; TEXT = Color.decode("#0F172A");
            MUTED = Color.decode("#64748B"); BORDER = Color.decode("#E2E8F0"); SIDEBAR = PRIMARY_DARK;
            INPUT = Color.decode("#F8FAFC"); ALT = Color.decode("#F8FAFC");
        }
        SIDEBAR_TEXT = Color.decode("#E2E8F0");
        PRIMARY_SOFT = blend(PRIMARY, CARD, dark ? 0.35f : 0.18f);

        UIManager.put("Panel.background", BG);
        UIManager.put("ComboBox.background", INPUT);
        UIManager.put("ComboBox.foreground", TEXT);
        UIManager.put("ComboBox.selectionBackground", INPUT);
        UIManager.put("ComboBox.selectionForeground", TEXT);
        UIManager.put("ToolTip.background", CARD);
        UIManager.put("ToolTip.foreground", TEXT);
        UIManager.put("ToolTip.border", javax.swing.BorderFactory.createLineBorder(BORDER));
        UIManager.put("List.background", INPUT);
        UIManager.put("List.foreground", TEXT);
    }

    public static void loadFromSettings() {
        apply("dark".equals(DataStore.settings.getProperty("theme", "light")),
                DataStore.settings.getProperty("accent", "Green"));
    }
}
