package frontend;

import backend.*;
import java.awt.*;
import java.util.ArrayList;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public final class PaymentsUI extends PageSupport {
    private static final long serialVersionUID=1L;
    public PaymentsUI(DashboardUI window) {
        super(window);
        JButton upcoming=Theme.button("Upcoming Payments"),paid=Theme.button("Paid Payments",Color.WHITE,Theme.INK),schedule=Theme.button("Schedule Payment");
        JPanel top=Theme.clear(new BorderLayout());JPanel tabs=Theme.clear(new FlowLayout(FlowLayout.LEFT,6,0));tabs.add(upcoming);tabs.add(paid);
        top.add(tabs,BorderLayout.WEST);top.add(schedule,BorderLayout.EAST);add(top,BorderLayout.NORTH);
        DefaultTableModel rows=model("Description","Category","Amount","Due Date","Status");JTable table=table(rows);add(tableArea(table,"No payments in this list yet."));
        java.util.List<Payment> visible=new ArrayList<>();boolean[] showPaid={false};
        Runnable refresh=()->{
            visible.clear();rows.setRowCount(0);
            for(Payment payment:service.getPayments())if(payment.isPaid()==showPaid[0]) {
                visible.add(payment);rows.addRow(new Object[]{payment.getDescription(),payment.getCategory(),Theme.money(payment.getAmount()),payment.getDue(),payment.isPaid()?"Paid":"Upcoming"});
            }
        };
        upcoming.addActionListener(event->{showPaid[0]=false;upcoming.setBackground(Theme.TEAL);upcoming.setForeground(Color.WHITE);paid.setBackground(Color.WHITE);paid.setForeground(Theme.INK);refresh.run();});
        paid.addActionListener(event->{showPaid[0]=true;paid.setBackground(Theme.TEAL);paid.setForeground(Color.WHITE);upcoming.setBackground(Color.WHITE);upcoming.setForeground(Theme.INK);refresh.run();});
        schedule.addActionListener(event->schedule());
        JButton pay=Theme.button("Mark Selected Paid");pay.setEnabled(false);
        table.getSelectionModel().addListSelectionListener(event->pay.setEnabled(table.getSelectedRow()>=0&&!showPaid[0]));
        pay.addActionListener(event->{int row=table.getSelectedRow();if(row>=0&&JOptionPane.showConfirmDialog(this,"Record this payment as an expense?","Confirm Payment",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION) {
            try{service.pay(visible.get(row).getId());refresh.run();}catch(Exception exception){error(exception);}
        }});
        JPanel actions=Theme.clear(new FlowLayout(FlowLayout.RIGHT,0,0));actions.add(pay);add(actions,BorderLayout.SOUTH);refresh.run();
    }
}
