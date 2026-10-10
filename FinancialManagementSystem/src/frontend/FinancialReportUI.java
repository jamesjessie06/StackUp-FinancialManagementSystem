package frontend;

import backend.*;
import frontend.style.*;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public final class FinancialReportUI extends PageSupport {
    private static final long serialVersionUID = 1L;

    public FinancialReportUI(DashboardUI window) {
        super(window);
        DatePicker from = new DatePicker(), to = new DatePicker();
        from.setSelectedDate(monthStart());
        to.setSelectedDate(monthEnd());
        JButton generate = Theme.button("Generate Report");
        generate.setPreferredSize(new Dimension(190, 44));
        JPanel filters = Theme.card();
        filters.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL; c.weightx = 1; c.insets = new Insets(0, 0, 0, 16);
        c.gridx = 0; filters.add(Theme.field("From date", from), c);
        c.gridx = 1; filters.add(Theme.field("To date", to), c);
        c.gridx = 2; c.weightx = 0; c.insets = new Insets(0, 0, 0, 0); c.anchor = GridBagConstraints.SOUTH;
        filters.add(generate, c);
        JPanel body = Theme.clear(new BorderLayout(0, 12));
        JPanel top = Theme.clear(new BorderLayout(0, 12));
        top.add(filters, BorderLayout.NORTH);
        body.add(top, BorderLayout.NORTH);
        JPanel charts = Theme.clear(new BorderLayout());
        body.add(charts);
        add(body);

        Runnable update = () -> {
            try {
                LocalDate start = from.getSelectedDate(), end = to.getSelectedDate();
                if (start.isAfter(end)) throw new IllegalArgumentException("From date must be before To date.");
                List<FinancialRecord> records = ChartData.between(service.getRecords(), start, end);
                charts.removeAll();
                charts.add(chartCard("Revenue vs Expenses vs Profit",
                    new ChartPanel(records, ChartPanel.Mode.TOTALS, start.getYear())));
                body.revalidate();
                body.repaint();
            } catch (Exception exception) { error(exception); }
        };
        generate.addActionListener(event -> update.run());
        update.run();
    }

    @Override protected String subtitle() { return "Revenue, expenses and profit for any date range."; }

    private JPanel chartCard(String title, JComponent chart) {
        JPanel card = Theme.card();
        card.setLayout(new BorderLayout(0, 14));
        card.add(Theme.label(title, 22, Theme.INK), BorderLayout.NORTH);
        Theme.RoundedPanel plot = new Theme.RoundedPanel(Theme.BACKGROUND);
        plot.radius = 22;
        plot.setLayout(new BorderLayout());
        plot.setBorder(new EmptyBorder(8, 10, 6, 10));
        plot.add(chart);
        chart.setPreferredSize(new Dimension(300, 300));
        card.add(plot);
        return card;
    }



    private static List<Map.Entry<String, BigDecimal>> ranked(Map<String, BigDecimal> data) {
        List<Map.Entry<String, BigDecimal>> list = new ArrayList<>(data.entrySet());
        list.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        return list;
    }
}
