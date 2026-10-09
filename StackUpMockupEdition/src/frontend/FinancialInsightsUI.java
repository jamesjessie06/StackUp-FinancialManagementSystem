package frontend;

import backend.*;
import java.awt.*;
import java.time.LocalDate;
import javax.swing.*;

public final class FinancialInsightsUI extends PageSupport {
    private static final long serialVersionUID=1L;
    public FinancialInsightsUI(DashboardUI window) {
        super(window);
        JPanel top=Theme.clear(new GridLayout(1,2,12,0));top.setPreferredSize(new Dimension(700,65));
        for(String type:new String[]{"Revenue","Expense"}) {
            Color color=type.equals("Revenue")?Theme.GREEN:Theme.RED;
            JPanel highlight=new Theme.RoundedPanel(type.equals("Revenue")?new Color(222,247,232):new Color(255,226,232));
            highlight.setBorder(BorderFactory.createEmptyBorder(10,14,10,14));highlight.setLayout(new GridLayout(2,1));
            highlight.add(Theme.label("Highest "+type,12,color));
            FinancialRecord highest=null;
            for(FinancialRecord record:service.getRecords())if(record.getType().equals(type)&&(highest==null||record.getAmount().compareTo(highest.getAmount())>0))highest=record;
            JLabel detail=new JLabel(highest==null?"No records yet.":highest.getDescription()+" — "+Theme.money(highest.getAmount()));detail.setForeground(color);highlight.add(detail);top.add(highlight);
        }
        add(top,BorderLayout.NORTH);
        int year=LocalDate.now().getYear();
        JPanel body=Theme.clear(new GridLayout(2,1,12,12));
        JPanel categories=Theme.clear(new GridLayout(1,2,12,0));
        categories.add(titled("Revenue by Category",new ChartPanel(service.getRecords(),ChartPanel.Mode.REVENUE_CATEGORIES,year)));
        categories.add(titled("Expense by Category",new ChartPanel(service.getRecords(),ChartPanel.Mode.EXPENSE_CATEGORIES,year)));
        body.add(categories);body.add(titled("Monthly Performance",new ChartPanel(service.getRecords(),ChartPanel.Mode.MONTHLY,year)));add(body);
    }
}
