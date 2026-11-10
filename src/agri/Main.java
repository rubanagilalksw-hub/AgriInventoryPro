package agri;

import javax.swing.*;
import java.awt.*;

/** Application entry point (run this class). */
public class Main {
    private static MainFrame frame;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Splash::show);
        try {
            DataStore.load();                 // connects to the SQLite database (creates tables + sample data on first run)
        } catch (Throwable t) {
            t.printStackTrace();
            JOptionPane.showMessageDialog(null, "Could not open the database:\n" + t.getMessage(), "Database error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
        Theme.loadFromSettings();
        Runtime.getRuntime().addShutdownHook(new Thread(Db::close));
        SwingUtilities.invokeLater(() -> { Splash.hide(); new LoginFrame().setVisible(true); });
    }

    /** Opens the main window (after login or restore). */
    public static void showMain(String page) {
        Rectangle b = null;
        if (frame != null) { b = frame.getBounds(); frame.dispose(); }
        frame = new MainFrame(page, b);
        frame.setVisible(true);
    }

    /** Changes the theme: saves the setting and rebuilds the window. */
    public static void changeTheme(boolean dark, String accent) {
        DataStore.settings.setProperty("theme", dark ? "dark" : "light");
        DataStore.settings.setProperty("accent", accent);
        DataStore.saveSettings();
        Theme.apply(dark, accent);
        showMain("settings");
    }

    public static void logout() {
        DataStore.log("Logged out");
        DataStore.saveAll();
        DataStore.current = null;
        if (frame != null) { frame.dispose(); frame = null; }
        new LoginFrame().setVisible(true);
    }
}
