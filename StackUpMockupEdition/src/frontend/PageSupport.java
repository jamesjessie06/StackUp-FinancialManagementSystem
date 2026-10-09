package frontend;

import backend.*;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

/** Shared display helpers; business rules remain in the backend. */
public abstract class PageSupport extends JPanel {
    private static final long serialVersionUID=1L;
    protected final DashboardUI window;
    protected final FinancialService service;
    protected final boolean admin;
    protected PageSupport(DashboardUI window) {
        super(new BorderLayout(12,12)); setOpaque(false);
        this.window=window; service=window.service();
        admin=service.getCurrent().getRole().equals("Admin");
    }
    protected JPanel emptyState(String title, String subtitle, int icon) {
        JPanel panel = Theme.clear(new GridBagLayout());
        JPanel stack = Theme.clear(new GridLayout(3, 1, 0, 8));
        JLabel badge = new JLabel(Theme.icon(icon, Theme.TEAL, true), SwingConstants.CENTER);
        stack.add(badge);
        JLabel heading = Theme.label(title, 14, Theme.INK);
        heading.setHorizontalAlignment(SwingConstants.CENTER);
        stack.add(heading);
        JLabel detail = new JLabel(subtitle, SwingConstants.CENTER);
        detail.setFont(new Font("Nimbus Sans", Font.PLAIN, 11));
        detail.setForeground(Theme.MUTED);
        stack.add(detail);
        panel.add(stack);
        return panel;
    }

    protected DefaultTableModel model(String... columns) {
        return new DefaultTableModel(columns,0) {
            private static final long serialVersionUID=1L;
            public boolean isCellEditable(int row,int column) { return false; }
        };
    }
    protected JTable table(DefaultTableModel model) {
        JTable table=new JTable(model); table.setRowHeight(30); table.setFont(new Font("DejaVu Sans",Font.PLAIN,11));
        table.setShowVerticalLines(false); table.setGridColor(Theme.LINE);
        table.setSelectionBackground(new Color(220,246,240));
        table.setSelectionForeground(Theme.INK); table.setFillsViewportHeight(true);
        table.getTableHeader().setBackground(new Color(240,242,245));
        table.getTableHeader().setFont(new Font("Nimbus Sans",Font.BOLD,10));
        table.getTableHeader().setPreferredSize(new Dimension(300, 28));
        javax.swing.table.DefaultTableCellRenderer header = new javax.swing.table.DefaultTableCellRenderer();
        header.setBackground(new Color(240,242,245));
        header.setForeground(Theme.INK);
        header.setFont(new Font("Nimbus Sans", Font.BOLD, 10));
        header.setBorder(BorderFactory.createEmptyBorder(7,8,7,8));
        table.getTableHeader().setDefaultRenderer(header);
        return table;
    }
    protected JPanel tableArea(JTable table,String message) {
        JPanel area=Theme.clear(new BorderLayout());
        JScrollPane scroll=new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(Theme.LINE));
        if(table.getRowCount()==0) {
            JPanel viewport=new JPanel(new BorderLayout()); viewport.setBackground(Color.WHITE);
            JLabel empty=new JLabel(message,SwingConstants.CENTER) {
                private static final long serialVersionUID = 1L;
                protected void paintComponent(Graphics graphics) {
                    Graphics2D pen = (Graphics2D) graphics.create();
                    pen.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    pen.setColor(new Color(242, 250, 246));
                    int diameter = Math.min(130, getHeight() - 20);
                    pen.fillOval((getWidth() - diameter) / 2, (getHeight() - diameter) / 2 - 10, diameter, diameter);
                    pen.dispose();
                    super.paintComponent(graphics);
                }
            };
            empty.setIcon(Theme.icon(1, Theme.TEAL, true));
            empty.setHorizontalTextPosition(SwingConstants.CENTER);
            empty.setVerticalTextPosition(SwingConstants.BOTTOM);
            empty.setIconTextGap(12);
            empty.setFont(new Font("Nimbus Sans",Font.PLAIN,12));
            empty.setForeground(Theme.MUTED); viewport.add(empty);
            table.putClientProperty("emptyLabel",empty);
            // JTable itself stays in the hierarchy for selection, refresh and tests.
            table.setLayout(new BorderLayout()); table.add(empty);
            table.getModel().addTableModelListener(event -> empty.setVisible(table.getRowCount()==0));
        }
        area.add(scroll); return area;
    }
    protected JPanel titled(String title,JComponent body) {
        JPanel card=Theme.card(); card.setLayout(new BorderLayout(0,10));
        card.add(Theme.label(title,15,Theme.INK),BorderLayout.NORTH); card.add(body); return card;
    }
    protected JPanel summary(LocalDate from,LocalDate to,boolean vertical) {
        JPanel row=Theme.clear(new GridLayout(vertical?3:1,vertical?1:3,10,10));
        BigDecimal revenue=service.total("Revenue",from,to),expense=service.total("Expense",from,to);
        BigDecimal[] values={revenue,expense,revenue.subtract(expense)};
        String[] names={"Revenue","Expenses","Profit"};
        Color[] accents={Theme.GREEN,Theme.RED,Theme.BLUE};
        Color[] fills={new Color(248,255,251),new Color(255,249,250),new Color(249,251,255)};
        for(int i=0;i<3;i++) {
            JPanel card=new Theme.RoundedPanel(fills[i]);
            card.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
            card.setLayout(new BorderLayout(10,0));
            JLabel icon=new JLabel(Theme.icon(i==2?2:1,accents[i],true));
            card.add(icon,BorderLayout.WEST);
            JPanel text=Theme.clear(new GridLayout(2,1,0,3));
            text.add(new JLabel(names[i])); text.add(Theme.label(Theme.money(values[i]),24,accents[i]));
            card.add(text); row.add(card);
        }
        row.setPreferredSize(new Dimension(vertical?190:600,vertical?230:78)); return row;
    }
    protected void error(Exception exception) { JOptionPane.showMessageDialog(this,exception.getMessage(),"Check input",JOptionPane.ERROR_MESSAGE); }
    protected void record(String type,FinancialRecord existing) {
        if(RecordDialog.show(this,service,type,existing)) window.refresh();
    }
    protected void schedule() {
        if(PaymentDialog.show(this,service)) window.refresh();
    }
    protected void addRows(DefaultTableModel model,List<FinancialRecord> records) {
        for(FinancialRecord record:records) model.addRow(new Object[]{record.getDescription(),record.getCategory(),Theme.money(record.getAmount()),record.getDate().toLocalDate()});
    }
}
