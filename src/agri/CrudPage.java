package agri;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/** Generic CRUD screen: search + table + Add / Edit / Delete buttons. */
public abstract class CrudPage extends Page {
    protected final DefaultTableModel model;
    protected final UI.StyledTable table;
    protected final UI.TField search = UI.field(18);
    protected final JPanel leftBar = UI.transparent(new FlowLayout(FlowLayout.LEFT, 10, 0));
    protected final JPanel rightBar = UI.transparent(new FlowLayout(FlowLayout.RIGHT, 8, 0));
    private final JLabel count = UI.muted("");

    protected abstract List<Object[]> rows(String filter);
    protected void onAdd() { }
    protected void onEdit(int id) { }
    protected void onDelete(int id) { }
    protected Color rowColor(int modelRow) { return null; }

    public CrudPage(String[] cols, boolean canEdit, boolean canDelete) {
        model = UI.model(cols);
        table = new UI.StyledTable(model);
        table.rowColor = this::rowColor;

        search.setToolTipText("Search...");
        JLabel sl = UI.label("Search:", 13, true, Theme.MUTED);
        leftBar.add(sl); leftBar.add(search);
        search.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { refresh(); }
            public void removeUpdate(DocumentEvent e) { refresh(); }
            public void changedUpdate(DocumentEvent e) { refresh(); }
        });

        if (canEdit) {
            UI.Btn add = UI.primary("+ Add New");
            add.addActionListener(e -> { onAdd(); refresh(); });
            UI.Btn edit = UI.info("Edit");
            edit.addActionListener(e -> { int id = table.selectedId(); if (id < 0) UI.showInfo(this, "Please select a row in the table first."); else { onEdit(id); refresh(); } });
            rightBar.add(edit); rightBar.add(add);
            table.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    if (e.getClickCount() == 2) { int id = table.selectedId(); if (id >= 0) { onEdit(id); refresh(); } }
                }
            });
        }
        if (canDelete) {
            UI.Btn del = UI.danger("Delete");
            del.addActionListener(e -> {
                int id = table.selectedId();
                if (id < 0) { UI.showInfo(this, "Please select a row in the table first."); return; }
                onDelete(id); refresh();
            });
            rightBar.add(del);
        }

        UI.Btn exp = UI.ghost("Export CSV");
        exp.addActionListener(e -> Export.csv(this, model, getClass().getSimpleName().replace("Page", "")));
        rightBar.add(exp, 0);

        JPanel top = UI.transparent(new BorderLayout(10, 0));
        top.add(leftBar, BorderLayout.WEST);
        top.add(rightBar, BorderLayout.EAST);
        JPanel bottom = UI.transparent(new BorderLayout());
        bottom.setBorder(new EmptyBorder(2, 4, 0, 0));
        bottom.add(count, BorderLayout.WEST);
        add(top, BorderLayout.NORTH);
        add(UI.scroll(table), BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
    }

    @Override public void refresh() {
        int sel = table.selectedId();
        model.setRowCount(0);
        List<Object[]> rs = rows(search.getText().trim().toLowerCase());
        for (Object[] r : rs) model.addRow(r);
        count.setText(rs.size() + " record(s)  |  Click a column header to sort");
        if (sel >= 0) for (int i = 0; i < model.getRowCount(); i++)
            if (((Number) model.getValueAt(i, 0)).intValue() == sel) {
                int v = table.convertRowIndexToView(i); table.setRowSelectionInterval(v, v); break;
            }
    }
}
