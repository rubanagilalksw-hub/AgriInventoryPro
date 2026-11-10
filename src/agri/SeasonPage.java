package agri;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.*;
import agri.Models.*;

/** Rabi / Kharif season-wise products and stock suggestions. */
public class SeasonPage extends Page {
    private final Widgets.StatCard cCur = new Widgets.StatCard("CURRENT SEASON", Theme.PRIMARY),
            cNext = new Widgets.StatCard("NEXT SEASON", Theme.INFO), cCnt = new Widgets.StatCard("PRODUCTS IN THIS FILTER", Theme.WARN);
    private final DefaultTableModel m = UI.model("ID", "Product", "Category", "Season", "Qty", "Min Qty", "Status");
    private final UI.StyledTable t = new UI.StyledTable(m);
    private final JComboBox<String> filter = UI.combo(new String[]{"Rabi", "Kharif", "All Season", "All Products"});
    private final JTextArea tips = new JTextArea();

    public SeasonPage() {
        t.rowColor = r -> {
            String s = String.valueOf(m.getValueAt(r, 6));
            if (s.equals(Logic.OUT) || s.equals(Logic.EXPIRED)) return Theme.tint(Theme.DANGER);
            if (s.equals(Logic.LOW) || s.equals(Logic.EXPIRING)) return Theme.tint(Theme.WARN);
            return null;
        };
        filter.setSelectedItem(Logic.currentSeason());
        filter.addActionListener(e -> refresh());

        JPanel cards = UI.transparent(new GridLayout(1, 3, 14, 0));
        cards.add(cCur); cards.add(cNext); cards.add(cCnt);

        JPanel bar = UI.transparent(new FlowLayout(FlowLayout.LEFT, 10, 0));
        bar.add(UI.label("Season filter:", 13, true, Theme.MUTED)); bar.add(filter);
        JPanel left = UI.transparent(new BorderLayout(0, 10));
        left.add(bar, BorderLayout.NORTH); left.add(UI.scroll(t), BorderLayout.CENTER);

        tips.setEditable(false); tips.setLineWrap(true); tips.setWrapStyleWord(true); tips.setOpaque(false);
        tips.setFont(UI.font(Font.PLAIN, 13)); tips.setForeground(Theme.TEXT);
        UI.Card tipCard = Widgets.titled("Stock Suggestions (before the season)", UI.plainScroll(tips));
        tipCard.setPreferredSize(new Dimension(340, 100));

        add(cards, BorderLayout.NORTH);
        add(left, BorderLayout.CENTER);
        add(tipCard, BorderLayout.EAST);
    }

    @Override public void refresh() {
        String cur = Logic.currentSeason(), next = Logic.nextSeason();
        long days = Logic.daysToNextSeason();
        cCur.setValue(cur + "  (" + Logic.seasonRange(cur) + ")");
        cNext.setValue(next + "  - in " + days + " days");
        String f = (String) filter.getSelectedItem();
        m.setRowCount(0);
        int n = 0;
        for (Product p : DataStore.products) {
            if (!"All Products".equals(f) && !p.season.equals(f)) continue;
            m.addRow(new Object[]{p.id, p.name, p.category, p.season, p.qty, p.minQty, Logic.status(p)});
            n++;
        }
        cCnt.setValue(String.valueOf(n));

        String target = days <= 60 ? next : cur;
        StringBuilder sb = new StringBuilder();
        if (days <= 60) sb.append(next).append(" season starts in ").append(days).append(" days. Get ready:\n\n");
        else sb.append("The ").append(cur).append(" season is running now. Review of this season's products:\n\n");
        int c = 0;
        for (Product p : DataStore.products) {
            if (!p.season.equals(target)) continue;
            if (p.qty < p.minQty * 2) {
                int need = Math.max(1, p.minQty * 3 - p.qty);
                sb.append("\u2022 ").append(p.name).append("\n   in stock ").append(p.qty).append(", suggestion: order ").append(need).append(" more\n");
                c++;
            }
        }
        if (c == 0) sb.append("Stock of all ").append(target).append(" products is fine.");
        tips.setText(sb.toString());
        tips.setCaretPosition(0);
    }
}
