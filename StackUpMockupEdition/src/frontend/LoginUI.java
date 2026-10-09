package frontend;

import backend.*;
import java.awt.*;
import java.util.Arrays;
import javax.swing.*;

/** Split login: an illustrated teal introduction beside the working sign-in form. */
public final class LoginUI extends JFrame {
    private static final long serialVersionUID = 1L;

    public LoginUI(FinancialService service) {
        super("StackUp Financial Management System");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(800, 480);
        setMinimumSize(new Dimension(740, 450));
        setLocationRelativeTo(null);
        JPanel root = new Theme.SmoothPanel(new BorderLayout());
        root.setBackground(new Color(233,233,233));
        root.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        setContentPane(root);
        JPanel introduction = new Theme.RoundedPanel(new Color(246,246,246));
        introduction.setLayout(new BorderLayout(0,20));
        introduction.setPreferredSize(new Dimension(325, 420));
        introduction.setBorder(BorderFactory.createEmptyBorder(30, 26, 24, 26));
        JPanel words = Theme.clear(new GridLayout(3, 1, 0, 8));
        words.add(Theme.label("StackUp", 30, Theme.BLACK));
        words.add(Theme.label("Financial Management System", 13, Theme.MUTED));
        words.add(Theme.label("Clarity in every transaction.", 14, Theme.TEAL));
        introduction.add(words, BorderLayout.NORTH);
        introduction.add(new LoginIllustration());
        root.add(introduction, BorderLayout.WEST);

        boolean first = !service.hasAccounts();
        JPanel form = new Theme.RoundedPanel(Color.WHITE);
        form.setLayout(new BorderLayout(0,25));
        form.setBorder(BorderFactory.createEmptyBorder(48, 34, 44, 20));
        JPanel heading = Theme.clear(new GridLayout(2, 1, 0, 10));
        heading.add(Theme.label(first ? "Let's get started" : "Welcome back", 26, Theme.INK));
        JLabel description = new JLabel(first ? "Create your administrator account." : "Sign in to your StackUp account.");
        description.setForeground(Theme.MUTED);
        heading.add(description);
        form.add(heading, BorderLayout.NORTH);
        JTextField username = new JTextField();
        JPasswordField password = new JPasswordField();
        JPanel fields = Theme.clear(new GridLayout(2, 1, 0, 22));
        fields.add(Theme.field("Username", username));
        fields.add(Theme.field("Password", password));
        form.add(fields);
        JButton login = Theme.button(first ? "Create Account" : "Log In");
        login.setPreferredSize(new Dimension(320, 44));
        form.add(login, BorderLayout.SOUTH);
        root.add(form);
        login.addActionListener(event -> {
            char[] value = password.getPassword();
            try {
                if (first) service.createAccount(username.getText(), value, "Admin");
                service.login(username.getText(), value);
                new DashboardUI(service).setVisible(true);
                dispose();
            } catch (Exception exception) {
                JOptionPane.showMessageDialog(this, exception.getMessage());
            } finally {
                Arrays.fill(value, '\0');
                password.setText("");
            }
        });
        getRootPane().setDefaultButton(login);
    }

    /** Decorative ledger illustration, without sample transactions or amounts. */
    private static final class LoginIllustration extends JPanel {
        private static final long serialVersionUID = 1L;
        LoginIllustration() { setOpaque(false); }
        protected void paintComponent(Graphics graphics) {
            Graphics2D pen = (Graphics2D) graphics.create();
            pen.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int x = getWidth() / 2 - 88, y = getHeight() / 2 - 68;
            pen.setColor(new Color(255, 255, 255, 28));
            pen.fillRoundRect(x + 18, y - 16, 174, 136, 20, 20);
            pen.setColor(new Color(235, 255, 249));
            pen.fillRoundRect(x, y, 174, 136, 20, 20);
            pen.setColor(new Color(177, 226, 212));
            pen.fillRoundRect(x + 19, y + 20, 74, 9, 7, 7);
            for (int line = 0; line < 3; line++) {
                pen.setColor(new Color(219, 242, 234));
                pen.fillRoundRect(x + 18, y + 47 + line * 23, 138, 13, 6, 6);
                pen.setColor(Theme.TEAL);
                pen.fillOval(x + 23, y + 50 + line * 23, 7, 7);
            }
            pen.setColor(new Color(132, 226, 190));
            pen.fillOval(x + 133, y + 90, 57, 57);
            pen.setColor(new Color(0, 99, 87));
            pen.setStroke(new BasicStroke(2));
            pen.drawLine(x + 150, y + 117, x + 159, y + 126);
            pen.drawLine(x + 159, y + 126, x + 174, y + 107);
            pen.dispose();
        }
    }
}
