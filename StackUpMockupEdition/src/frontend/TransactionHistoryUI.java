package frontend;

import backend.*;
import java.awt.*;
import java.time.LocalDate;
import java.util.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public final class TransactionHistoryUI extends PageSupport {
    private static final long serialVersionUID=1L;
    public TransactionHistoryUI(DashboardUI window) {
        super(window);
        JComboBox<String> type=new JComboBox<>(new String[]{"All Transactions","Revenue","Expense"});
        JTextField search=new JTextField(); DatePicker from=new DatePicker(),to=new DatePicker();
        JButton apply=Theme.button("Apply"),reset=Theme.button("Reset",Color.WHITE,Theme.INK);
        JPanel controls=Theme.card();
        controls.setLayout(new BorderLayout(0,10));
        JPanel first=Theme.clear(new BorderLayout(10,0)); type.setPreferredSize(new Dimension(190,34));
        first.add(type,BorderLayout.WEST); first.add(search);
        JPanel second=Theme.clear(new GridLayout(1,4,10,0));
        second.add(Theme.field("From date",from)); second.add(Theme.field("To date",to));
        second.add(Theme.field(" ",apply)); second.add(Theme.field(" ",reset));
        controls.add(first,BorderLayout.NORTH); controls.add(second);
        add(controls,BorderLayout.NORTH);
        DefaultTableModel rows=model("Description","Category","Amount","Date","Recorded By");
        JTable table=table(rows); add(tableArea(table,"No transactions found."));
        java.util.List<FinancialRecord> visible=new ArrayList<>();
        Runnable refresh=()->{
            try {
                LocalDate start=from.getText().isBlank()?LocalDate.MIN:from.getSelectedDate();
                LocalDate end=to.getText().isBlank()?LocalDate.MAX:to.getSelectedDate();
                if(start.isAfter(end)) throw new IllegalArgumentException("From date must be before To date.");
                visible.clear(); rows.setRowCount(0);
                for(FinancialRecord record:service.getRecords()) if((type.getSelectedIndex()==0||record.getType().equals(type.getSelectedItem()))
                    && (record.getDescription()+" "+record.getCategory()).toLowerCase(Locale.ROOT).contains(search.getText().toLowerCase(Locale.ROOT))
                    && !record.getDate().toLocalDate().isBefore(start)&&!record.getDate().toLocalDate().isAfter(end)) {
                    visible.add(record); rows.addRow(new Object[]{record.getDescription(),record.getCategory(),Theme.money(record.getAmount()),record.getDate().toLocalDate(),record.getRecordedBy()});
                }
            } catch(Exception exception) { error(exception); }
        };
        apply.addActionListener(event->refresh.run()); search.addActionListener(event->refresh.run());
        type.addActionListener(event->refresh.run()); reset.addActionListener(event->{search.setText("");from.setSelectedDate(null);to.setSelectedDate(null);refresh.run();});
        if(admin) {
            JButton edit=Theme.button("Edit Selected"),delete=Theme.button("Delete Selected"); edit.setEnabled(false);delete.setEnabled(false);
            JPanel actions=Theme.clear(new FlowLayout(FlowLayout.RIGHT,6,0));actions.add(edit);actions.add(delete);controls.add(actions,BorderLayout.SOUTH);
            table.getSelectionModel().addListSelectionListener(event->{edit.setEnabled(table.getSelectedRow()>=0);delete.setEnabled(table.getSelectedRow()>=0);});
            edit.addActionListener(event->{int row=table.getSelectedRow();if(row>=0) record(visible.get(row).getType(),visible.get(row));});
            delete.addActionListener(event->{int row=table.getSelectedRow();if(row>=0&&JOptionPane.showConfirmDialog(this,"Delete this transaction?","Confirm",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION) {
                try {service.deleteRecord(visible.get(row).getId());refresh.run();}catch(Exception exception){error(exception);}
            }});
        }
        refresh.run();
    }
}
