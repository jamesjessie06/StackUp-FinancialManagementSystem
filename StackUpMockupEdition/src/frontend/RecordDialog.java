package frontend;

import backend.*;
import java.awt.*;
import java.math.BigDecimal;
import java.time.*;
import javax.swing.*;

public final class RecordDialog {
    private RecordDialog() {}
    public static boolean show(Component owner,FinancialService service,String type,FinancialRecord existing) {
        JTextField description=new JTextField(existing==null?"":existing.getDescription());
        JTextField category=new JTextField(existing==null?"":existing.getCategory());
        JTextField amount=new JTextField(existing==null?"":existing.getAmount().toPlainString());
        DatePicker date=new DatePicker();
        if(existing!=null) date.setSelectedDate(existing.getDate().toLocalDate());
        JPanel form=Theme.clear(new GridLayout(4,1,0,12));
        form.add(Theme.field("Description",description)); form.add(Theme.field("Category",category));
        form.add(Theme.field("Amount",amount)); form.add(Theme.field("Date",date));
        form.setPreferredSize(new Dimension(390,290));
        while(JOptionPane.showConfirmDialog(owner,form,"Record Transaction",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE)==JOptionPane.OK_OPTION) {
            try {
                LocalDateTime time=date.getSelectedDate().atTime(existing==null?LocalTime.now().withNano(0):existing.getDate().toLocalTime());
                BigDecimal value=new BigDecimal(amount.getText().trim());
                String author=existing==null?service.getCurrent().getUsername():existing.getRecordedBy();
                FinancialRecord record=type.equals("Revenue")?new Revenue(description.getText(),category.getText(),value,time,author):new Expense(description.getText(),category.getText(),value,time,author);
                if(existing==null) service.addRecord(record); else service.editRecord(existing.getId(),record);
                return true;
            } catch(Exception exception) { JOptionPane.showMessageDialog(owner,exception.getMessage()); }
        }
        return false;
    }
}
