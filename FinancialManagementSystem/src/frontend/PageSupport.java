package frontend;

import backend.*;
import frontend.style.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;

public abstract class PageSupport extends JPanel {
    private static final long serialVersionUID = 1L;
    static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);
    static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a", Locale.ENGLISH);
    public static final int STYLE_WHITE = 0, STYLE_GREEN = 1, STYLE_DARK = 2;

    protected final DashboardUI window;
    protected final FinancialService service;
    protected final boolean admin;

    protected PageSupport(DashboardUI window) {
        super(new BorderLayout(0, 12));
        setOpaque(false);
        this.window = window;
        service = window.service();
        admin = service.getCurrent().getRole().equals("Admin");
    }

    protected String title() { return null; }
    protected String subtitle() { return ""; }
    /** Optional buttons shown at the right of the heading. */
    protected JComponent actions() { return null; }


    protected JPanel titled(String title, JComponent body) {
        JPanel card = Theme.card();
        card.setLayout(new BorderLayout(0, 16));
        card.add(Theme.label(title, 18, Theme.INK), BorderLayout.NORTH);
        card.add(body);
        return card;
    }

    protected JPanel stat(String title, String value, String sub, int style, Icons.Kind icon) {
        return stat(title, value, sub, style, icon, style == STYLE_WHITE ? Theme.INK : Color.WHITE, 30);
    }

    protected JPanel stat(String title, String value, String sub, int style, Icons.Kind icon, Color valueColor, int size) {
        boolean solid = style != STYLE_WHITE;
        Theme.RoundedPanel card = style == STYLE_WHITE ? new Theme.RoundedPanel(Color.WHITE)
            : Theme.hero(style == STYLE_GREEN ? Theme.BRAND : Theme.INK);
        card.setLayout(new BorderLayout(0, 10));
        card.setBorder(new EmptyBorder(12, 18, 12, 18));
        JPanel head = Theme.clear(new BorderLayout());
        head.add(Theme.plain(title, 14, solid ? Color.WHITE : Theme.INK), BorderLayout.WEST);
        if (icon != null) head.add(Theme.badge(icon, solid ? new Color(255, 255, 255, 28) : Theme.SOFT,
            solid ? Color.WHITE : Theme.INK, 32), BorderLayout.EAST);
        card.add(head, BorderLayout.NORTH);
        JPanel foot = Theme.clear(new BorderLayout(0, 6));
        foot.add(Theme.label(value, size, valueColor), BorderLayout.CENTER);
        if (sub != null && !sub.isEmpty()) foot.add(Theme.plain(sub, 13, solid ? new Color(222, 240, 231) : Theme.MUTED), BorderLayout.SOUTH);
        JPanel bottom = Theme.clear(new BorderLayout());
        bottom.add(foot, BorderLayout.SOUTH);
        card.add(bottom);
        return card;
    }

    /** Puts a small footer row (such as a change pill) under the big figure of a {@link #stat} card. */
    protected void addFoot(JPanel card, JComponent foot) {
        JPanel bottom = (JPanel) card.getComponent(1);
        JComponent figure = (JComponent) bottom.getComponent(0);
        JPanel stack = Theme.clear(new BorderLayout(0, 8));
        stack.add(figure, BorderLayout.CENTER);
        stack.add(foot, BorderLayout.SOUTH);
        bottom.removeAll();
        bottom.add(stack, BorderLayout.SOUTH);
    }

    protected JComponent change(BigDecimal now, BigDecimal before, boolean onSolid, String suffix, boolean higherIsBetter) {
        JPanel row = Theme.clear(new FlowLayout(FlowLayout.LEFT, 8, 0));
        Color muted = onSolid ? new Color(222, 240, 231) : Theme.MUTED;
        if (before.signum() == 0) {
            return row;
        }
        double percent = now.subtract(before).multiply(BigDecimal.valueOf(100))
            .divide(before.abs(), 0, RoundingMode.HALF_UP).doubleValue();
        boolean up = percent >= 0;
        boolean good = up == higherIsBetter;
        Color bg = onSolid ? Color.WHITE : good ? Theme.MINT : Theme.SOFT;
        Color fg = onSolid ? Theme.BRAND : good ? Theme.BRAND : Theme.INK;
        if (onSolid && !good) fg = Theme.INK;
        row.add(pill(String.format(Locale.US, "%.0f%%", Math.abs(percent)),
            Icons.of(up ? Icons.Kind.UP_RIGHT : Icons.Kind.DOWN_RIGHT, fg, 13), bg, fg));
        row.add(Theme.plain(suffix, 13, muted));
        return row;
    }

    protected static JComponent pill(String text, Icon icon, Color background, Color foreground) {
        return new JComponent() {
            private static final long serialVersionUID = 1L;
            { setOpaque(false); setFont(Theme.font(Font.BOLD, 12)); }
            public Dimension getPreferredSize() {
                int width = getFontMetrics(getFont()).stringWidth(text) + 22 + (icon == null ? 0 : icon.getIconWidth() + 4);
                return new Dimension(width, 26);
            }
            protected void paintComponent(Graphics graphics) {
                Graphics2D pen = (Graphics2D) graphics.create();
                pen.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                pen.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                pen.setColor(background);
                pen.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                int x = 10;
                if (icon != null) { icon.paintIcon(this, pen, x, (getHeight() - icon.getIconHeight()) / 2); x += icon.getIconWidth() + 4; }
                pen.setColor(foreground);
                pen.setFont(getFont());
                FontMetrics metrics = pen.getFontMetrics();
                pen.drawString(text, x, (getHeight() + metrics.getAscent() - metrics.getDescent()) / 2);
                pen.dispose();
            }
        };
    }

    protected JPanel emptyState(String title, String subtitle) {
        JPanel panel = Theme.clear(new GridBagLayout());
        JPanel stack = Theme.clear(new GridLayout(3, 1, 0, 6));
        JPanel badge = Theme.clear(new FlowLayout(FlowLayout.CENTER, 0, 0));
        badge.add(Theme.badge(Icons.Kind.DOC, Theme.MINT, Theme.BRAND, 52));
        stack.add(badge);
        JLabel heading = Theme.label(title, 15, Theme.INK);
        heading.setHorizontalAlignment(SwingConstants.CENTER);
        stack.add(heading);
        JLabel detail = Theme.muted(subtitle, 13);
        detail.setHorizontalAlignment(SwingConstants.CENTER);
        stack.add(detail);
        panel.add(stack);
        return panel;
    }

    

    
    public static final class Desc {
        final String title, sub;
        public Desc(String title, String sub) { this.title = title; this.sub = sub; }
        public String toString() { return title; }
    }

    
    public static final class Tile {
        final LocalDate date;
        final String text;
        public Tile(LocalDate date, String text) { this.date = date; this.text = text; }
        public String toString() { return text; }
    }

    protected DefaultTableModel model(String... columns) {
        return new DefaultTableModel(columns, 0) {
            private static final long serialVersionUID = 1L;
            public boolean isCellEditable(int row, int column) { return false; }
        };
    }

    protected boolean isAdminUser(String name) {
        if (name == null) return false;
        if (name.equalsIgnoreCase(service.getCurrent().getUsername())) return admin;
        if (!admin) return false;
        for (Account account : service.getAccounts())
            if (account.getUsername().equalsIgnoreCase(name)) return account.getRole().equals("Admin");
        return false;
    }

    
    protected JTable table(DefaultTableModel model, String... kinds) {
        JTable table = new JTable(model);
        table.setRowHeight(46);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setFocusable(false);
        table.setOpaque(true);
        table.setBackground(Color.WHITE);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        table.setSelectionBackground(Theme.SELECTED);
        table.setSelectionForeground(Theme.INK);
        table.setRowSelectionAllowed(true);
        table.setFont(Theme.font(Font.PLAIN, 14));
        CellRenderer renderer = new CellRenderer(kinds);
        for (int column = 0; column < model.getColumnCount(); column++)
            table.getColumnModel().getColumn(column).setCellRenderer(renderer);
        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);
        header.setResizingAllowed(false);
        header.setPreferredSize(new Dimension(300, 40));
        header.setDefaultRenderer((t, value, selected, focus, row, column) -> {
            JLabel label = new JLabel(String.valueOf(value));
            label.setOpaque(true);
            label.setBackground(Theme.BACKGROUND);
            label.setForeground(Theme.MUTED);
            label.setFont(Theme.font(Font.PLAIN, 13));
            label.setBorder(new EmptyBorder(0, 18, 0, 8));
            return label;
        });
        return table;
    }

    protected void widths(JTable table, int... weights) {
        for (int i = 0; i < weights.length; i++) table.getColumnModel().getColumn(i).setPreferredWidth(weights[i]);
    }

    private final class CellRenderer implements TableCellRenderer {
        private final String[] kinds;
        CellRenderer(String[] kinds) { this.kinds = kinds; }
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                boolean focus, int row, int column) {
            String kind = column < kinds.length ? kinds[column] : "text";
            String text = value == null ? "" : value.toString();
            JPanel cell = new JPanel(new GridBagLayout());
            cell.setBackground(selected ? Theme.SELECTED : Color.WHITE);
            boolean last = row == table.getRowCount() - 1;
            cell.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, last ? 0 : 1, 0, Theme.LINE),
                new EmptyBorder(0, 18, 0, 8)));
            GridBagConstraints c = new GridBagConstraints();
            c.anchor = GridBagConstraints.WEST;
            c.weightx = 1;
            switch (kind) {
                case "check": {
                    boolean on = selected;
                    c.weightx = 0;
                    cell.add(new JComponent() {
                        private static final long serialVersionUID = 1L;
                        { setOpaque(false); }
                        public Dimension getPreferredSize() { return new Dimension(20, 20); }
                        protected void paintComponent(Graphics graphics) {
                            Graphics2D pen = (Graphics2D) graphics.create();
                            pen.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                            pen.setColor(on ? Theme.BRAND : Color.WHITE);
                            pen.fill(new RoundRectangle2D.Double(1, 1, 17, 17, 6, 6));
                            pen.setColor(on ? Theme.BRAND : new Color(120, 130, 125));
                            pen.draw(new RoundRectangle2D.Double(1, 1, 17, 17, 6, 6));
                            if (on) Icons.of(Icons.Kind.CHECK, Color.WHITE, 14).paintIcon(this, pen, 3, 3);
                            pen.dispose();
                        }
                    }, c);
                    break;
                }
                case "chip":
                    c.weightx = 0;
                    cell.add(new Theme.Chip(text), c);
                    c.weightx = 1; cell.add(Box.createGlue(), c);
                    break;
                case "user": {
                    c.weightx = 0;
                    cell.add(new Theme.Avatar(text, isAdminUser(text), 32), c);
                    c.insets = new Insets(0, 10, 0, 0); c.weightx = 1;
                    cell.add(Theme.plain(text, 14, Theme.INK), c);
                    break;
                }
                case "desc": {
                    Desc desc = (Desc) value;
                    JPanel two = Theme.clear(new GridLayout(2, 1, 0, 1));
                    two.add(Theme.plain(desc.title, 14, Theme.INK));
                    two.add(Theme.muted(desc.sub, 13));
                    cell.add(two, c);
                    break;
                }
                case "tile": {
                    Tile tile = (Tile) value;
                    c.weightx = 0;
                    cell.add(dateTile(tile.date), c);
                    c.insets = new Insets(0, 12, 0, 0); c.weightx = 1;
                    cell.add(Theme.plain(tile.text, 14, Theme.INK), c);
                    break;
                }
                case "money": {
                    JLabel label = Theme.label(text, 14, text.startsWith("+") ? Theme.BRAND : Theme.INK);
                    cell.add(label, c);
                    break;
                }
                case "bold":
                    cell.add(Theme.label(text, 14, Theme.INK), c);
                    break;
                case "muted":
                    cell.add(Theme.plain(text, 13, Theme.MUTED), c);
                    break;
                default:
                    cell.add(Theme.plain(text, 14, Theme.INK), c);
            }
            return cell;
        }
    }

    private JComponent dateTile(LocalDate date) {
        return new JComponent() {
            private static final long serialVersionUID = 1L;
            { setOpaque(false); }
            public Dimension getPreferredSize() { return new Dimension(44, 44); }
            protected void paintComponent(Graphics graphics) {
                Graphics2D pen = (Graphics2D) graphics.create();
                pen.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                pen.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                pen.setColor(Theme.SOFT);
                pen.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 14, 14));
                String month = date.getMonth().getDisplayName(java.time.format.TextStyle.SHORT, Locale.ENGLISH).toUpperCase(Locale.ROOT);
                pen.setColor(Theme.BRAND);
                pen.setFont(Theme.font(Font.BOLD, 10));
                pen.drawString(month, (getWidth() - pen.getFontMetrics().stringWidth(month)) / 2, 16);
                pen.setColor(Theme.INK);
                pen.setFont(Theme.font(Font.BOLD, 16));
                String day = Integer.toString(date.getDayOfMonth());
                pen.drawString(day, (getWidth() - pen.getFontMetrics().stringWidth(day)) / 2, 34);
                pen.dispose();
            }
        };
    }

    
    protected JPanel tableArea(JTable table, String message) {
        Theme.RoundedPanel box = new Theme.RoundedPanel(Color.WHITE);
        box.radius = 22;
        box.outlined = true;
        box.clip = true;
        box.setLayout(new BorderLayout());
        box.add(table.getTableHeader(), BorderLayout.NORTH);
        JPanel body = new JPanel(new BorderLayout());
        body.setBackground(Color.WHITE);
        body.add(table, BorderLayout.NORTH);
        JPanel empty = emptyState(message, "Entries will show up here as soon as they exist.");
        empty.setPreferredSize(new Dimension(100, 130));
        empty.setVisible(table.getRowCount() == 0);
        table.getModel().addTableModelListener(event -> empty.setVisible(table.getRowCount() == 0));
        body.add(empty);
        box.add(body);
        return box;
    }

    

    protected static String signed(FinancialRecord record) {
        return (record.signedAmount().signum() > 0 ? "+" : "−") + Theme.money(record.getAmount());
    }

    protected void error(Exception exception) {
        Theme.message(this, "Check input", exception.getMessage());
    }

    protected void record(String type, FinancialRecord existing) {
        if (Dialogs.record(this, service, type, existing)) window.refresh();
    }

    protected void schedule() {
        if (Dialogs.payment(this, service)) window.refresh();
    }

    
    protected static String dueText(Payment payment) {
        if (payment.isPaid()) return "Paid";
        long days = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), payment.getDue());
        if (days < 0) return "Due " + (-days) + " days ago";
        if (days == 0) return "Due today";
        if (days <= 7) return "Due in " + days + " day" + (days == 1 ? "" : "s");
        return "Scheduled";
    }

    protected static LocalDate monthStart() { return LocalDate.now().withDayOfMonth(1); }
    protected static LocalDate monthEnd() { return LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth()); }

    protected BigDecimal totalIn(String type, LocalDate from, LocalDate to) { return service.total(type, from, to); }


}
