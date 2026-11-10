package agri;

import javax.swing.*;
import javax.swing.table.TableModel;
import java.awt.Component;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/** Exports any table to a CSV file (opens in Excel). */
public class Export {
    private static String esc(Object v) {
        String s = v == null ? "" : (v instanceof Double ? String.valueOf(Math.round((Double) v * 100) / 100.0) : String.valueOf(v));
        return (s.contains(",") || s.contains("\"") || s.contains("\n")) ? "\"" + s.replace("\"", "\"\"") + "\"" : s;
    }

    public static void csv(Component parent, TableModel m, String defaultName) {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File(defaultName + "_" + DataStore.today() + ".csv"));
        if (fc.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) return;
        StringBuilder sb = new StringBuilder("\uFEFF");
        for (int c = 0; c < m.getColumnCount(); c++) sb.append(c > 0 ? "," : "").append(esc(m.getColumnName(c)));
        sb.append("\r\n");
        for (int r = 0; r < m.getRowCount(); r++) {
            for (int c = 0; c < m.getColumnCount(); c++) sb.append(c > 0 ? "," : "").append(esc(m.getValueAt(r, c)));
            sb.append("\r\n");
        }
        try {
            Files.write(fc.getSelectedFile().toPath(), sb.toString().getBytes(StandardCharsets.UTF_8));
            UI.showOk(parent, m.getRowCount() + " rows exported to:\n" + fc.getSelectedFile().getAbsolutePath());
            DataStore.log("Exported CSV: " + fc.getSelectedFile().getName());
        } catch (Exception ex) { UI.showError(parent, "Export failed: " + ex.getMessage()); }
    }
}
