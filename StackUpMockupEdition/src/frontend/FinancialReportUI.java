package frontend;

import backend.*;
import java.awt.*;
import java.time.LocalDate;
import javax.swing.*;

public final class FinancialReportUI extends PageSupport {
    private static final long serialVersionUID=1L;
    public FinancialReportUI(DashboardUI window) {
        super(window);
        DatePicker from=new DatePicker(),to=new DatePicker(); from.setSelectedDate(LocalDate.now());to.setSelectedDate(LocalDate.now());
        JButton generate=Theme.button("Generate Report");
        JPanel top=Theme.clear(new BorderLayout(0,12));
        JPanel filters=Theme.clear(new GridLayout(1,3,12,0));
        filters.add(Theme.field("From date",from)); filters.add(Theme.field("To date",to));filters.add(Theme.field(" ",generate));
        top.add(filters,BorderLayout.NORTH);
        JPanel totals=Theme.clear(new BorderLayout());top.add(totals);add(top,BorderLayout.NORTH);
        JPanel charts=Theme.clear(new GridLayout(1,2,12,0));add(charts);
        Runnable update=()->{
            try {
                LocalDate start=from.getSelectedDate(),end=to.getSelectedDate();
                if(start.isAfter(end))throw new IllegalArgumentException("From date must be before To date.");
                java.util.List<FinancialRecord> records=ChartData.between(service.getRecords(),start,end);
                totals.removeAll();totals.add(summary(start,end,false));charts.removeAll();
                charts.add(titled("Revenue vs Expenses vs Profit",new ChartPanel(records,ChartPanel.Mode.TOTALS,start.getYear())));
                charts.add(titled("Expense Category Breakdown",new ChartPanel(records,ChartPanel.Mode.EXPENSE_CATEGORIES,start.getYear())));
                revalidate();repaint();
            }catch(Exception exception){error(exception);}
        };
        generate.addActionListener(event->update.run());update.run();
    }
}
