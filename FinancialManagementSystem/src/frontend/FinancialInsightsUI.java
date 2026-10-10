package frontend;

import backend.*;
import frontend.style.*;
import java.awt.*;
import java.time.LocalDate;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public final class FinancialInsightsUI extends PageSupport {
    private static final long serialVersionUID = 1L;
    private final JPanel body = Theme.clear(new BorderLayout(0, 12));
    private final int year = LocalDate.now().getYear();

    public FinancialInsightsUI(DashboardUI window) {
        super(window);
        add(body);
        build();
    }

    @Override protected String subtitle() { return "Where the money comes from, where it goes, and how each month compares."; }

    private void build() {
        LocalDate start = LocalDate.of(year, 1, 1);
        LocalDate end = LocalDate.of(year, 12, 31);
        List<FinancialRecord> all = service.getRecords();
        List<FinancialRecord> scope = ChartData.between(all, start, end);

        body.removeAll();

        JPanel middle = Theme.clear(new BorderLayout(0, 12));
        middle.add(Theme.equal(12,
            chartCard("Revenue by Category", null, new ChartPanel(scope, ChartPanel.Mode.REVENUE_CATEGORIES, year), 210),
            chartCard("Expense by Category", null, new ChartPanel(scope, ChartPanel.Mode.EXPENSE_CATEGORIES, year), 210)), BorderLayout.NORTH);
        JPanel monthly = chartCard("Monthly Performance",
            "Revenue and expenses for each month of " + year + " - hover a month to see the amounts",
            new ChartPanel(all, ChartPanel.Mode.MONTHLY, year), 200);
        JPanel legend = Theme.clear(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        legend.add(legendItem("Revenue", ChartPanel.REVENUE_LINE));
        legend.add(legendItem("Expenses", ChartPanel.EXPENSE_LINE));
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

}
