package frontend;

import frontend.style.*;

import backend.*;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/** Where the money comes from and goes, with highlights for the chosen period. */
public final class FinancialInsightsUI extends PageSupport {
    private static final long serialVersionUID = 1L;
    private final JPanel body = Theme.clear(new BorderLayout(0, 12));
    private final JComponent range;
    private final int year = LocalDate.now().getYear();

    public FinancialInsightsUI(DashboardUI window) {
        super(window);
        range = Theme.segmented(new String[]{"This month", "Quarter", Integer.toString(year)}, 2, Theme.INK, index -> build(index));
        add(body);
        build(2);
    }

    @Override protected String subtitle() { return "Where the money comes from, where it goes, and how each month compares."; }
    @Override protected JComponent actions() { return range; }

    private void build(int choice) {
        LocalDate today = LocalDate.now();
        LocalDate start = choice == 0 ? monthStart()
            : choice == 1 ? LocalDate.of(year, (today.getMonthValue() - 1) / 3 * 3 + 1, 1) : LocalDate.of(year, 1, 1);
        LocalDate end = choice == 0 ? monthEnd() : choice == 1 ? start.plusMonths(3).minusDays(1) : LocalDate.of(year, 12, 31);
        List<FinancialRecord> all = service.getRecords();
        List<FinancialRecord> scope = ChartData.between(all, start, end);

        body.removeAll();
        JPanel top = Theme.clear(new BorderLayout(0, 12));
        top.add(Theme.equal(12, highlight("Revenue", scope, true), highlight("Expense", scope, false)), BorderLayout.NORTH);
        body.add(top, BorderLayout.NORTH);

        JPanel middle = Theme.clear(new BorderLayout(0, 12));
        middle.add(Theme.equal(12,
            chartCard("Revenue by Category", null, new ChartPanel(scope, ChartPanel.Mode.REVENUE_CATEGORIES, year), 170),
            chartCard("Expense by Category", null, new ChartPanel(scope, ChartPanel.Mode.EXPENSE_CATEGORIES, year), 170)), BorderLayout.NORTH);
        JPanel monthly = chartCard("Monthly Performance",
            "January – December " + year + " · " + today.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " to date",
            new ChartPanel(all, ChartPanel.Mode.MONTHLY, year), 150);
        JPanel legend = Theme.clear(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        legend.add(legendItem("Revenue", Theme.hatch(Theme.BRAND_MID)));
        legend.add(legendItem("Expenses", Theme.INK));
        ((JPanel) monthly.getComponent(0)).add(legend, BorderLayout.EAST);
        middle.add(monthly, BorderLayout.CENTER);
        body.add(middle);
        body.revalidate();
        body.repaint();
    }

    private JComponent legendItem(String name, Paint paint) {
        JLabel label = Theme.plain(name, 13, Theme.INK);
        label.setIcon(new Icon() {
            public int getIconWidth() { return 14; }
            public int getIconHeight() { return 14; }
            public void paintIcon(Component component, Graphics graphics, int x, int y) {
                Graphics2D pen = (Graphics2D) graphics.create();
                pen.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                pen.setPaint(paint);
                pen.fillRoundRect(x, y, 14, 14, 6, 6);
                pen.dispose();
            }
        });
        label.setIconTextGap(6);
        return label;
    }

    private JPanel chartCard(String title, String subtitle, JComponent chart, int height) {
        JPanel card = Theme.card();
        card.setLayout(new BorderLayout(0, 12));
        JPanel head = Theme.clear(new BorderLayout());
        JPanel words = Theme.clear(new GridLayout(subtitle == null ? 1 : 2, 1, 0, 2));
        words.add(Theme.label(title, 22, Theme.INK));
        if (subtitle != null) words.add(Theme.muted(subtitle, 14));
        head.add(words, BorderLayout.WEST);
        card.add(head, BorderLayout.NORTH);
        chart.setPreferredSize(new Dimension(300, height));
        if (subtitle != null) {
            Theme.RoundedPanel plot = new Theme.RoundedPanel(Theme.BACKGROUND);
            plot.radius = 22;
            plot.setLayout(new BorderLayout());
            plot.setBorder(new EmptyBorder(6, 10, 4, 10));
            plot.add(chart);
            card.add(plot);
        } else card.add(chart);
        return card;
    }

    private JPanel highlight(String type, List<FinancialRecord> scope, boolean revenue) {
        FinancialRecord best = null;
        for (FinancialRecord record : scope)
            if (record.getType().equals(type) && (best == null || record.getAmount().compareTo(best.getAmount()) > 0)) best = record;
        Theme.RoundedPanel card = Theme.hero(revenue ? Theme.BRAND : Theme.INK);
        card.setLayout(new BorderLayout(18, 0));
        card.setBorder(new EmptyBorder(14, 20, 14, 20));
        card.add(Theme.badge(revenue ? Icons.Kind.UP_RIGHT : Icons.Kind.DOWN_RIGHT, Color.WHITE, revenue ? Theme.BRAND : Theme.INK, 52), BorderLayout.WEST);
        JPanel text = Theme.clear(new GridLayout(2, 1, 0, 3));
        text.add(Theme.plain("Highest " + type, 15, new Color(232, 244, 238)));
        text.add(Theme.label(best == null ? "—" : Theme.money(best.getAmount()), 32, Color.WHITE));
        card.add(text);
        return card;
    }
}
