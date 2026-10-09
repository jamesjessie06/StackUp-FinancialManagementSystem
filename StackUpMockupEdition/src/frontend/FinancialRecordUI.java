package frontend;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public final class FinancialRecordUI extends PageSupport {
    private static final long serialVersionUID=1L;
    public FinancialRecordUI(DashboardUI window) {
        super(window);
        JPanel actions=Theme.clear(new GridLayout(1,2,12,0)); actions.setPreferredSize(new Dimension(760,152));
        for(String type:new String[]{"Revenue","Expense"}) {
            Color color=type.equals("Revenue")?Theme.GREEN:Theme.RED;
            Color pale=type.equals("Revenue")?new Color(222,247,232):new Color(255,226,232);
            JButton add=Theme.button("Add "+type,pale,color);
            add.setFont(new Font("Nimbus Sans",Font.BOLD,20));
            add.setIcon(Theme.icon(3,color,true)); add.setIconTextGap(12);
            add.setHorizontalAlignment(SwingConstants.LEFT);
            add.setBorder(BorderFactory.createEmptyBorder(15,22,15,22));
            add.addActionListener(event->record(type,null)); actions.add(add);
        }
        add(actions,BorderLayout.NORTH);
        DefaultTableModel recent=model("Description","Category","Amount","Date"); addRows(recent,service.getRecords());
        add(titled("Recent Records",tableArea(table(recent),"No records yet.")));
    }
}
