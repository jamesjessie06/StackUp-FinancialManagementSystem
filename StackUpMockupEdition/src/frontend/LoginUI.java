package frontend;

import frontend.style.*;

import backend.*;
import java.awt.*;
import java.util.Arrays;
import javax.swing.*;

/** Just a username, a password and one button. */
public final class LoginUI extends JFrame {
    private static final long serialVersionUID = 1L;

    public LoginUI(FinancialService service) {
        super("StackUp Financial Management System");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        try { setIconImage(javax.imageio.ImageIO.read(Theme.asset("logo.png"))); } catch (Exception ignored) { }
        setSize(540, 660);
        setMinimumSize(new Dimension(500, 620));
        setLocationRelativeTo(null);
        JPanel outer = new JPanel(new GridBagLayout());
        outer.setBackground(Theme.FRAME);
        setContentPane(outer);

        boolean first = !service.hasAccounts();
        Theme.RoundedPanel card = new Theme.RoundedPanel(Color.WHITE);
        card.radius = 34;
        card.setLayout(new GridBagLayout());
        card.setBorder(BorderFactory.createEmptyBorder(30, 40, 34, 40));
        card.setPreferredSize(new Dimension(440, 540));
        JTextField username = Theme.input("e.g. maria");
        Theme.RoundedPassword password = new Theme.RoundedPassword();
        password.setPlaceholder(first ? "At least 8 characters" : "Your password");
        JButton login = Theme.button(first ? "Create Account" : "Log In");
        login.setPreferredSize(new Dimension(100, 48));

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL;
        c.gridy = 0; c.insets = new Insets(0, 0, 28, 0); card.add(new LogoPanel(), c);
        c.gridy = 1; c.insets = new Insets(0, 0, 0, 0); card.add(Theme.field("Username", username), c);
        c.gridy = 2; c.insets = new Insets(18, 0, 0, 0); card.add(Theme.field("Password", password), c);
        c.gridy = 3; c.insets = new Insets(30, 0, 0, 0); card.add(login, c);
        outer.add(card);

        login.addActionListener(event -> {
            char[] value = password.getPassword();
            try {
                if (first) service.createAccount(username.getText(), value, "Admin");
                service.login(username.getText(), value);
                new DashboardUI(service).setVisible(true);
                dispose();
            } catch (Exception exception) {
                Theme.message(this, "Cannot sign in", exception.getMessage());
            } finally {
                Arrays.fill(value, '\0');
                password.setText("");
            }
        });
        getRootPane().setDefaultButton(login);
    }

    /** The StackUp logo. Its white lettering is recoloured dark so it reads on the white card. */
    private static final class LogoPanel extends JComponent {
        private static final long serialVersionUID = 1L;
        private final java.awt.image.BufferedImage logo;
        LogoPanel() {
            java.awt.image.BufferedImage image = null;
            try { image = javax.imageio.ImageIO.read(Theme.asset("logo.png")); }
            catch (Exception ignored) { }
            if (image != null) {
                for (int y = 0; y < image.getHeight(); y++)
                    for (int x = 0; x < image.getWidth(); x++) {
                        int argb = image.getRGB(x, y);
                        if ((argb >>> 24) > 0 && ((argb >> 16) & 255) > 185 && ((argb >> 8) & 255) > 185 && (argb & 255) > 185)
                            image.setRGB(x, y, (argb & 0xFF000000) | (Theme.INK.getRGB() & 0xFFFFFF));
                    }
            }
            logo = image;
            setPreferredSize(new Dimension(340, 130));
        }
        protected void paintComponent(Graphics graphics) {
            Graphics2D pen = (Graphics2D) graphics.create();
            pen.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            pen.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            pen.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            if (logo != null) {
                double scale = Math.min((getWidth() - 6.0) / logo.getWidth(), (getHeight() - 4.0) / logo.getHeight());
                int w = (int) (logo.getWidth() * scale), h = (int) (logo.getHeight() * scale);
                pen.drawImage(logo, (getWidth() - w) / 2, (getHeight() - h) / 2, w, h, null);
            } else {
                pen.setColor(Color.WHITE);
                pen.setFont(Theme.font(Font.BOLD, 30));
                pen.drawString("StackUp", 40, getHeight() / 2 + 10);
            }
            pen.dispose();
        }
    }
}
