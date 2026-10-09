package frontend;

import backend.*;
import java.awt.*;
import java.math.BigDecimal;
import javax.swing.*;

public final class PaymentDialog {
    private PaymentDialog() {}
    public static boolean show(Component owner,FinancialService service) {
        JTextField description=new JTextField(),category=new JTextField(),amount=new JTextField();
        DatePicker date=new DatePicker();
        JPanel form=Theme.clear(new GridLayout(4,1,0,12));
        form.add(Theme.field("Description",description)); form.add(Theme.field("Category",category));
        form.add(Theme.field("Amount",amount)); form.add(Theme.field("Due date",date));
        form.setPreferredSize(new Dimension(390,290));
        while(JOptionPane.showConfirmDialog(owner,form,"Schedule Payment",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE)==JOptionPane.OK_OPTION) {
            try {
                service.schedule(new Payment(description.getText(),category.getText(),new BigDecimal(amount.getText()),date.getSelectedDate()));
                return true;
            } catch(Exception exception) { JOptionPane.showMessageDialog(owner,exception.getMessage()); }
        }
        return false;
    }
}
