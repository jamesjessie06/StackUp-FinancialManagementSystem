package frontend;

import backend.*;
import java.awt.*;
import java.util.Arrays;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public final class AccountsUI extends PageSupport {
    private static final long serialVersionUID=1L;
    public AccountsUI(DashboardUI window) {
        super(window);
        JTextField username=new JTextField();JPasswordField password=new JPasswordField();
        JComboBox<String> role=new JComboBox<>(new String[]{"Employee","Admin"});
        JButton save=Theme.button("Save Account");
        JPanel form=Theme.clear(new GridLayout(4,1,0,12));
        form.add(Theme.field("Username",username));form.add(Theme.field("Password (8+ characters)",password));form.add(Theme.field("Role",role));form.add(save);
        JPanel create=Theme.clear(new BorderLayout(0,18));create.add(form,BorderLayout.NORTH);
        JPanel rolePreview=new AccentPanel(new BorderLayout(0,10),false);
        rolePreview.setBorder(BorderFactory.createEmptyBorder(16,16,16,16));
        rolePreview.setPreferredSize(new Dimension(230,112));
        rolePreview.add(Theme.label("Account access",13,Theme.TEAL),BorderLayout.NORTH);
        JLabel permission=new JLabel("<html>Employee<br><br>Record transactions and manage payments.</html>");
        permission.setForeground(Theme.MUTED);
        rolePreview.add(permission);
        role.addActionListener(event -> permission.setText(role.getSelectedItem().equals("Admin")
            ? "<html>Admin<br><br>Review reports, manage records and accounts.</html>"
            : "<html>Employee<br><br>Record transactions and manage payments.</html>"));
        create.add(rolePreview,BorderLayout.SOUTH);
        JPanel left=titled("Create Account",create);left.setPreferredSize(new Dimension(285,400));add(left,BorderLayout.WEST);
        DefaultTableModel rows=model("Username","Role");JTable accounts=table(rows);
        Runnable refresh=()->{rows.setRowCount(0);for(Account account:service.getAccounts())rows.addRow(new Object[]{account.getUsername(),account.getRole()});};refresh.run();
        save.addActionListener(event->{char[] value=password.getPassword();try{service.createAccount(username.getText(),value,(String)role.getSelectedItem());username.setText("");refresh.run();JOptionPane.showMessageDialog(this,"Account created.");}catch(Exception exception){error(exception);}finally{Arrays.fill(value,'\0');password.setText("");}});
        JTextField selected=new JTextField();selected.setEditable(false);JButton reset=Theme.button("Reset Employee Password");reset.setEnabled(false);
        accounts.getSelectionModel().addListSelectionListener(event->{int row=accounts.getSelectedRow();selected.setText(row>=0?rows.getValueAt(row,0).toString():"");reset.setEnabled(row>=0&&"Employee".equals(rows.getValueAt(row,1)));});
        reset.addActionListener(event->{JPasswordField replacement=new JPasswordField();if(JOptionPane.showConfirmDialog(this,Theme.field("New password",replacement),"Reset Employee Password",JOptionPane.OK_CANCEL_OPTION)==JOptionPane.OK_OPTION){char[] value=replacement.getPassword();try{service.resetEmployeePassword(selected.getText(),value);JOptionPane.showMessageDialog(this,"Password updated.");}catch(Exception exception){error(exception);}finally{Arrays.fill(value,'\0');replacement.setText("");}}});
        JPanel right=Theme.clear(new BorderLayout(0,12));right.add(titled("Registered Accounts",tableArea(accounts,"No registered accounts.")));
        JPanel resetForm=Theme.clear(new BorderLayout(0,10));resetForm.add(selected);resetForm.add(reset,BorderLayout.SOUTH);
        JPanel resetCard=titled("Reset Employee Password",resetForm);resetCard.setPreferredSize(new Dimension(400,135));right.add(resetCard,BorderLayout.SOUTH);add(right);
    }
}
