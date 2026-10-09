package frontend;

import backend.*;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

/** Reference-inspired card grid showing only actual system data. */
public final class DashboardPage extends PageSupport {
    private static final long serialVersionUID = 1L;
    public DashboardPage(DashboardUI window) {
        super(window);
        setLayout(new BorderLayout(14,14));
        BigDecimal revenue=service.total("Revenue",LocalDate.MIN,LocalDate.MAX);
        BigDecimal expense=service.total("Expense",LocalDate.MIN,LocalDate.MAX);
        JPanel left=Theme.clear(new BorderLayout(0,14));left.setPreferredSize(new Dimension(275,480));
        JPanel balance=metric("Net balance",revenue.subtract(expense),false);
        JPanel actions=Theme.clear(new GridLayout(1,2,8,0));
        JButton history=Theme.button("History",Theme.BLACK,Color.WHITE);history.addActionListener(e->window.navigate("Transaction History"));actions.add(history);
        JButton next=Theme.button(admin?"Reports":"Record",new Color(243,244,244),Theme.BLACK);next.addActionListener(e->window.navigate(admin?"Financial Report":"Financial Record"));actions.add(next);balance.add(actions,BorderLayout.SOUTH);
        balance.setPreferredSize(new Dimension(275,190));left.add(balance,BorderLayout.NORTH);
        JPanel payments=titled("Upcoming payments",emptyState("No pending payments","Scheduled items appear here.",4));
        long pending=service.getPayments().stream().filter(v->!v.isPaid()).count();
        if(pending>0)payments=titled("Upcoming payments",Theme.label(pending+" pending payment(s)",20,Theme.TEAL));
        left.add(payments);
        JPanel status=new AccentPanel(new BorderLayout(0,12),true);status.setBorder(BorderFactory.createEmptyBorder(20,18,20,18));status.add(Theme.label("StackUp",21,Color.WHITE),BorderLayout.NORTH);status.add(Theme.label(service.getRecords().size()+" transactions recorded",13,Color.WHITE));status.setPreferredSize(new Dimension(275,125));left.add(status,BorderLayout.SOUTH);add(left,BorderLayout.WEST);
        JPanel main=Theme.clear(new BorderLayout(14,14));JPanel upper=Theme.clear(new GridLayout(1,2,14,0));upper.setPreferredSize(new Dimension(700,270));
        JPanel metrics=Theme.card();metrics.setLayout(new GridLayout(2,2,10,10));
        metrics.add(metric("Total revenue",revenue,true));metrics.add(metric("Total expenses",expense,false));metrics.add(metric("Total profit",revenue.subtract(expense),false));metrics.add(metric("Today's revenue",service.total("Revenue",LocalDate.now(),LocalDate.now()),false));upper.add(metrics);
        upper.add(titled("Monthly cash flow",new ChartPanel(service.getRecords(),ChartPanel.Mode.CASH_FLOW,LocalDate.now().getYear())));main.add(upper,BorderLayout.NORTH);
        DefaultTableModel recent=model("Description","Category","Amount","Date");java.util.List<FinancialRecord> records=service.getRecords();addRows(recent,records.subList(Math.max(0,records.size()-5),records.size()));main.add(titled("Recent transactions",tableArea(table(recent),"No transactions recorded yet.")));add(main);
    }
    private JPanel metric(String title,BigDecimal value,boolean accent){
        JPanel card=new Theme.RoundedPanel(accent?Theme.TEAL:new Color(246,246,246));card.setLayout(new BorderLayout(0,12));card.setBorder(BorderFactory.createEmptyBorder(14,12,14,12));
        JPanel heading=Theme.clear(new BorderLayout());JLabel name=new JLabel(title);name.setFont(new Font("Nimbus Sans",Font.PLAIN,11));name.setForeground(accent?Color.WHITE:Theme.BLACK);heading.add(name);heading.add(new JLabel(Theme.icon(1,accent?Color.WHITE:Theme.MUTED,false)),BorderLayout.EAST);card.add(heading,BorderLayout.NORTH);card.add(Theme.label(Theme.money(value),21,accent?Color.WHITE:Theme.BLACK));return card;
    }
}
