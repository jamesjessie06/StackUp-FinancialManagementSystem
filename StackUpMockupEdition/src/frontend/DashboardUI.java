package frontend;

import backend.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.*;

/** Fresh desktop shell built from the approved mockup. */
public final class DashboardUI extends JFrame {
    private static final long serialVersionUID=1L;
    private final FinancialService service;
    private final boolean admin;
    private final JPanel workspace=Theme.clear(new BorderLayout(0,14));
    private final Map<String,JButton> menu=new LinkedHashMap<>();
    private String current="Dashboard";
    public DashboardUI(FinancialService service) {
        super("StackUp Financial Management System");this.service=service;admin=service.getCurrent().getRole().equals("Admin");
        setDefaultCloseOperation(EXIT_ON_CLOSE);setSize(1200,730);setMinimumSize(new Dimension(1040,650));setLocationRelativeTo(null);
        JPanel outer=new JPanel(new BorderLayout());outer.setBackground(new Color(233,233,233));
        outer.setBorder(BorderFactory.createEmptyBorder(24,24,24,24));setContentPane(outer);
        JPanel root=new Theme.RoundedPanel(Theme.BACKGROUND);root.setLayout(new BorderLayout(16,18));
        root.setBorder(BorderFactory.createEmptyBorder(16,14,16,14));outer.add(root);
        String[] pages=admin?new String[]{"Dashboard","Transaction History","Financial Report","Financial Insights","Activity Log","Accounts"}:new String[]{"Dashboard","Financial Record","Transaction History","Payments"};
        JPanel top=Theme.clear(new BorderLayout(22,0));
        JLabel brand=Theme.label("StackUp",17,Theme.BLACK);brand.setIcon(Theme.icon(2,Theme.TEAL,true));top.add(brand,BorderLayout.WEST);
        JPanel tabs=new Theme.RoundedPanel(Color.WHITE);tabs.setLayout(new FlowLayout(FlowLayout.CENTER,3,3));
        for(String page:pages){JButton button=Theme.button(page,Color.WHITE,Theme.MUTED);button.setFont(new Font("Nimbus Sans",Font.PLAIN,10));button.addActionListener(event->open(page));menu.put(page,button);tabs.add(button);}
        top.add(tabs);
        JLabel account=Theme.label(service.getCurrent().getUsername()+"  ·  "+service.getCurrent().getRole(),11,Theme.BLACK);account.setIcon(Theme.icon(3,Theme.TEAL,true));top.add(account,BorderLayout.EAST);root.add(top,BorderLayout.NORTH);
        JPanel sidebar=new Theme.RoundedPanel(Color.WHITE);sidebar.setPreferredSize(new Dimension(44,550));sidebar.setLayout(new BoxLayout(sidebar,BoxLayout.Y_AXIS));sidebar.setBorder(BorderFactory.createEmptyBorder(14,3,10,3));
        int index=0;
        for(String page:pages){JButton icon=Theme.button("",Color.WHITE,Theme.MUTED);icon.setIcon(Theme.icon(index++,Theme.BLACK,false));icon.setToolTipText(page);icon.setMaximumSize(new Dimension(38,40));icon.addActionListener(event->open(page));sidebar.add(icon);sidebar.add(Box.createVerticalStrut(6));}
        sidebar.add(Box.createVerticalGlue());JButton logout=Theme.button("↪",Color.WHITE,Theme.BLACK);logout.setToolTipText("Logout");logout.setMaximumSize(new Dimension(38,40));sidebar.add(logout);
        logout.addActionListener(event->{if(JOptionPane.showConfirmDialog(this,"Log out?","Logout",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION){service.logout();new LoginUI(service).setVisible(true);dispose();}});
        root.add(sidebar,BorderLayout.WEST);root.add(workspace);showPage("Dashboard");
    }
    public FinancialService service() { return service; }
    public void refresh() { showPage(current); }
    public void navigate(String page) { open(page); }
    private void open(String page) {
        if(current.equals(page)) JOptionPane.showMessageDialog(this,page+" is already open.","Page Already Open",JOptionPane.WARNING_MESSAGE);
        else showPage(page);
    }
    private void showPage(String page) {
        current=page;workspace.removeAll();for(Map.Entry<String,JButton> entry:menu.entrySet())entry.getValue().setBackground(entry.getKey().equals(page)?Theme.BLACK:Color.WHITE);
        for(Map.Entry<String,JButton> entry:menu.entrySet())entry.getValue().setForeground(entry.getKey().equals(page)?Color.WHITE:Theme.MUTED);
        JPanel header=Theme.clear(new BorderLayout());
        header.setBorder(BorderFactory.createEmptyBorder(0,0,10,0));header.add(Theme.label(page.equals("Dashboard")?"Good day, "+service.getCurrent().getUsername():page,26,Theme.BLACK),BorderLayout.WEST);
        workspace.add(header,BorderLayout.NORTH);
        JPanel screen;
        switch(page) {
            case "Financial Record":screen=new FinancialRecordUI(this);break;
            case "Transaction History":screen=new TransactionHistoryUI(this);break;
            case "Financial Report":screen=new FinancialReportUI(this);break;
            case "Financial Insights":screen=new FinancialInsightsUI(this);break;
            case "Activity Log":screen=new ActivityLogUI(this);break;
            case "Accounts":screen=new AccountsUI(this);break;
            case "Payments":screen=new PaymentsUI(this);break;
            default:screen=new DashboardPage(this);
        }
        workspace.add(screen);workspace.revalidate();workspace.repaint();
    }
}
