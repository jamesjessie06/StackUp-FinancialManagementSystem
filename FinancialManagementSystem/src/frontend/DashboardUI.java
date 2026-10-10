package frontend;

import backend.*;
import frontend.style.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public final class DashboardUI extends JFrame {
    private static final long serialVersionUID = 1L;
    private final FinancialService service;
    private final boolean admin;
    private final JPanel workspace = Theme.clear(new BorderLayout(0, 10));
    private final Map<String, RailButton> menu = new LinkedHashMap<>();
    private String current = "Dashboard";

    public DashboardUI(FinancialService service) {
        super("StackUp Financial Management System");
        this.service = service;
        admin = service.getCurrent().getRole().equals("Admin");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        java.awt.image.BufferedImage icon = loadLogo();
        if (icon != null) setIconImage(icon);
        Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        setSize(Math.min(1500, screen.width * 97 / 100), screen.height);
        setMinimumSize(new Dimension(1120, 700));
        setLocationRelativeTo(null);
        JPanel outer = new JPanel(new BorderLayout());
        outer.setBackground(Theme.FRAME);
        outer.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        setContentPane(outer);
        Theme.RoundedPanel root = new Theme.RoundedPanel(Theme.BACKGROUND);
        root.radius = 38;
        root.setLayout(new BorderLayout(14, 12));
        root.setBorder(BorderFactory.createEmptyBorder(14, 20, 16, 20));
        outer.add(root);

        root.add(topBar(), BorderLayout.NORTH);
        root.add(rail(), BorderLayout.WEST);
        root.add(workspace);
        showPage("Dashboard");
    }

    public FinancialService service() { return service; }
    public void refresh() { showPage(current); }
    public void navigate(String page) { open(page); }

    private String[] pages() {
        return admin
            ? new String[]{"Dashboard", "Reminders", "Transaction History", "Financial Report", "Financial Insights", "Activity Log", "Accounts"}
            : new String[]{"Dashboard", "Reminders", "Financial Record", "Payments", "Transaction History"};
    }

    private static Icons.Kind iconFor(String page) {
        switch (page) {
            case "Financial Record": return Icons.Kind.PLUS_CIRCLE;
            case "Payments": return Icons.Kind.CLOCK;
            case "Reminders": return Icons.Kind.BELL;
            case "Transaction History": return Icons.Kind.DOC;
            case "Financial Report": return Icons.Kind.BARS;
            case "Financial Insights": return Icons.Kind.PIE;
            case "Activity Log": return Icons.Kind.PULSE;
            case "Accounts": return Icons.Kind.USERS;
            default: return Icons.Kind.HOME;
        }
    }


    private static java.awt.image.BufferedImage loadLogo() {
        try { return javax.imageio.ImageIO.read(Theme.asset("logo.png")); }
        catch (Exception exception) { return null; }
    }

    private JComponent topBar() {
        JPanel bar = Theme.clear(new BorderLayout());
        Theme.RoundedPanel brand = new Theme.RoundedPanel(Color.BLACK);
        brand.radius = 999;
        brand.setLayout(new BorderLayout());
        brand.add(new JComponent() {
            private static final long serialVersionUID = 1L;
            private final java.awt.image.BufferedImage logo = loadLogo();
            public Dimension getPreferredSize() {
                if (logo == null) return new Dimension(120, 46);
                return new Dimension(logo.getWidth() * 46 / logo.getHeight(), 46);
            }
            protected void paintComponent(Graphics graphics) {
                Graphics2D pen = (Graphics2D) graphics.create();
                pen.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                pen.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                pen.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                if (logo != null) pen.drawImage(logo, 0, 0, getWidth(), getHeight(), null);
                pen.dispose();
            }
        });
        brand.setBorder(new EmptyBorder(0, 26, 0, 26));
        bar.add(brand, BorderLayout.WEST);

        JPanel right = Theme.clear(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        right.add(userPill());
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private Theme.RoundedPanel pill() {
        Theme.RoundedPanel panel = new Theme.RoundedPanel(Color.WHITE);
        panel.radius = 999;
        return panel;
    }

    private JComponent userPill() {
        Theme.RoundedPanel pill = pill();
        pill.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 6));
        Account account = service.getCurrent();
        pill.add(new Theme.Avatar(account.getUsername(), false, 34));
        JPanel text = Theme.clear(new GridLayout(2, 1, 0, 0));
        JLabel who = Theme.label(account.getUsername(), 14, Theme.INK), what = Theme.muted(account.getRole(), 12);
        text.add(who);
        text.add(what);
        text.setPreferredSize(new Dimension(Math.max(who.getPreferredSize().width, what.getPreferredSize().width) + 14, 36));
        pill.add(text);
        pill.setBorder(new EmptyBorder(0, 0, 0, 8));
        return pill;
    }

    private void logout() {
        if (Theme.confirm(this, Icons.Kind.LOGOUT, "Log out?", "You will need to sign in again to keep using StackUp.", "Yes, log out", "Stay signed in")) {
            service.logout();
            new LoginUI(service).setVisible(true);
            dispose();
        }
    }

    

    private JComponent rail() {
        JPanel column = Theme.clear(new BorderLayout());
        Theme.RoundedPanel navigation = pill();
        navigation.radius = 36;
        navigation.setLayout(new BoxLayout(navigation, BoxLayout.Y_AXIS));
        navigation.setBorder(new EmptyBorder(6, 6, 6, 6));
        for (String page : pages()) {
            RailButton button = new RailButton(iconFor(page), page);
            button.addActionListener(event -> open(page));
            menu.put(page, button);
            navigation.add(button);
            navigation.add(Box.createVerticalStrut(2));
        }
        navigation.remove(navigation.getComponentCount() - 1);
        Theme.RoundedPanel help = pill();
        help.radius = 36;
        help.setLayout(new BoxLayout(help, BoxLayout.Y_AXIS));
        help.setBorder(new EmptyBorder(6, 6, 6, 6));
        RailButton logout = new RailButton(Icons.Kind.LOGOUT, "Logout");
        logout.addActionListener(event -> logout());
        help.add(logout);
        JPanel top = Theme.clear(new BorderLayout(0, 10));
        top.add(navigation, BorderLayout.NORTH);
        top.add(help, BorderLayout.CENTER);
        JPanel holder = Theme.clear(new BorderLayout());
        holder.add(top, BorderLayout.NORTH);
        column.add(holder, BorderLayout.NORTH);
        return column;
    }

    private static final class RailButton extends JButton {
        private static final long serialVersionUID = 1L;
        private final Icons.Kind kind;
        private boolean active;
        RailButton(Icons.Kind kind, String name) {
            this.kind = kind;
            setName(name);
            setToolTipText(name);
            setPreferredSize(new Dimension(42, 42));
            setMaximumSize(new Dimension(42, 42));
            setMinimumSize(new Dimension(42, 42));
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorder(BorderFactory.createEmptyBorder());
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setAlignmentX(Component.CENTER_ALIGNMENT);
        }
        void setActive(boolean value) { active = value; repaint(); }
        protected void paintComponent(Graphics graphics) {
            Graphics2D pen = (Graphics2D) graphics.create();
            pen.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (active) { pen.setColor(Theme.INK); pen.fillOval(0, 0, getWidth() - 1, getHeight() - 1); }
            else if (getModel().isRollover()) { pen.setColor(Theme.SOFT); pen.fillOval(0, 0, getWidth() - 1, getHeight() - 1); }
            Icon icon = Icons.of(kind, active ? Color.WHITE : Theme.INK, 20);
            icon.paintIcon(this, pen, (getWidth() - 20) / 2, (getHeight() - 20) / 2);
            pen.dispose();
        }
    }

    // --------------------------------------------------------------- pages

    private void open(String page) {
        if (current.equals(page))
            Theme.alreadyOpen(this, page);
        else showPage(page);
    }

    private String greeting() {
        int hour = java.time.LocalTime.now().getHour();
        String part = hour < 12 ? "Good morning" : hour < 18 ? "Good afternoon" : "Good evening";
        return part + ", " + service.getCurrent().getUsername();
    }

    private void showPage(String page) {
        current = page;
        workspace.removeAll();
        for (Map.Entry<String, RailButton> entry : menu.entrySet()) entry.getValue().setActive(entry.getKey().equals(page));
        PageSupport screen;
        switch (page) {
            case "Financial Record": screen = new FinancialRecordUI(this); break;
            case "Transaction History": screen = new TransactionHistoryUI(this); break;
            case "Financial Report": screen = new FinancialReportUI(this); break;
            case "Financial Insights": screen = new FinancialInsightsUI(this); break;
            case "Activity Log": screen = new ActivityLogUI(this); break;
            case "Accounts": screen = new AccountsUI(this); break;
            case "Payments": screen = new PaymentsUI(this); break;
            case "Reminders": screen = new RemindersUI(this); break;
            default: screen = new DashboardPage(this);
        }
        String title = screen.title() != null ? screen.title() : page.equals("Dashboard") ? greeting() : page;
        JPanel header = Theme.clear(new BorderLayout(16, 0));
        JPanel words = Theme.clear(new BorderLayout(0, 4));
        words.add(Theme.label(title, 30, Theme.INK), BorderLayout.NORTH);
        words.add(Theme.plain(screen.subtitle(), 14, Theme.MUTED));
        header.add(words, BorderLayout.WEST);
        JComponent actions = screen.actions();
        if (actions != null) {
            JPanel holder = Theme.clear(new GridBagLayout());
            holder.add(actions);
            header.add(holder, BorderLayout.EAST);
        }
        workspace.add(header, BorderLayout.NORTH);
        workspace.add(scroller(screen));
        workspace.revalidate();
        workspace.repaint();
    }

    private JComponent scroller(JComponent screen) {
        JPanel body = new ScrollBody();
        body.add(screen);
        JScrollPane scroll = new JScrollPane(body, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(24);
        return scroll;
    }

    private static final class ScrollBody extends JPanel implements Scrollable {
        private static final long serialVersionUID = 1L;
        ScrollBody() { super(new BorderLayout()); setOpaque(false); }
        public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        public int getScrollableUnitIncrement(Rectangle area, int orientation, int direction) { return 24; }
        public int getScrollableBlockIncrement(Rectangle area, int orientation, int direction) { return Math.max(60, area.height - 60); }
        public boolean getScrollableTracksViewportWidth() { return true; }
        public boolean getScrollableTracksViewportHeight() {
            return getParent() instanceof JViewport && getParent().getHeight() > getPreferredSize().height;
        }
    }
}
