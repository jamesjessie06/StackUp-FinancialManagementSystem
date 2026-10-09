package frontend;

import java.awt.*;
import java.math.BigDecimal;
import java.util.Locale;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/** The new mockup's design system; independent of the previous frontend. */
public final class Theme {
    public static final Color TEAL = new Color(0, 151, 139);
    public static final Color BLACK = new Color(23, 29, 29);
    public static final Color BACKGROUND = new Color(246, 246, 246);
    public static final Color INK = new Color(16, 30, 46);
    public static final Color MUTED = new Color(112, 123, 139);
    public static final Color LINE = new Color(227, 232, 237);
    public static final Color GREEN = new Color(0, 166, 92);
    public static final Color RED = new Color(244, 53, 81);
    public static final Color BLUE = new Color(31, 103, 234);

    private Theme() {}

    public static void install() {
        for (String name : new String[]{"NimbusSans-Regular.otf", "NimbusSans-Bold.otf", "DejaVuSans.ttf", "DejaVuSans-Bold.ttf"}) {
            try { GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(
                Font.createFont(Font.TRUETYPE_FONT, new java.io.File("assets", name))); }
            catch (Exception ignored) { }
        }
        try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); }
        catch (Exception ignored) { }
        Font font = new Font("Nimbus Sans", Font.PLAIN, 12);
        for (String key : new String[]{"Label.font", "Button.font", "TextField.font",
                "PasswordField.font", "Table.font", "TableHeader.font", "ComboBox.font"})
            UIManager.put(key, font);
        UIManager.put("ComboBoxUI", "frontend.ComboBoxSkin");
        UIManager.put("ComboBox.border", new InputBorder());
        UIManager.put("TextField.border", new InputBorder());
        UIManager.put("PasswordField.border", new InputBorder());
        UIManager.put("Panel.background", BACKGROUND);
        UIManager.put("Label.foreground", INK);
        UIManager.put("TextField.background", Color.WHITE);
        UIManager.put("PasswordField.background", Color.WHITE);
        UIManager.put("ComboBox.background", Color.WHITE);
        UIManager.put("Table.background", Color.WHITE);
        UIManager.put("Table.foreground", INK);
        UIManager.put("OptionPane.background", Color.WHITE);
    }

    public static String money(BigDecimal value) {
        return "₱" + String.format(Locale.US, "%,.2f", value);
    }

    public static JLabel label(String text, int size, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(new Font(text.contains("₱") ? "DejaVu Sans" : "Nimbus Sans", Font.BOLD, size));
        label.setForeground(color);
        return label;
    }

    public static Icon icon(int kind, Color color, boolean badge) {
        return new Icon() {
            public int getIconWidth() { return badge ? 38 : 18; }
            public int getIconHeight() { return badge ? 38 : 18; }
            public void paintIcon(Component component, Graphics graphics, int x, int y) {
                Graphics2D pen = (Graphics2D) graphics.create();
                pen.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (badge) {
                    pen.setColor(new Color(color.getRed(),color.getGreen(),color.getBlue(),25));
                    pen.fillOval(x,y,38,38); x+=10; y+=10;
                }
                pen.setColor(color); pen.setStroke(new BasicStroke(1.3f));
                if (kind == 0) {
                    pen.drawLine(x+2,y+8,x+9,y+2); pen.drawLine(x+9,y+2,x+16,y+8);
                    pen.drawRect(x+4,y+8,10,8);
                } else if (kind == 2) {
                    pen.drawRect(x+3,y+10,3,6); pen.drawRect(x+8,y+6,3,10); pen.drawRect(x+13,y+2,3,14);
                } else if (kind == 3) {
                    pen.drawOval(x+2,y+2,14,14); pen.drawLine(x+9,y+5,x+9,y+13);pen.drawLine(x+5,y+9,x+13,y+9);
                } else if (kind == 4) {
                    pen.drawOval(x+2,y+2,14,14); pen.drawLine(x+9,y+4,x+9,y+9);pen.drawLine(x+9,y+9,x+13,y+10);
                } else {
                    pen.drawRoundRect(x+3,y+2,12,14,2,2);
                    pen.drawLine(x+6,y+6,x+12,y+6);pen.drawLine(x+6,y+10,x+12,y+10);
                }
                pen.dispose();
            }
        };
    }

    public static JPanel clear(LayoutManager layout) {
        JPanel panel = new SmoothPanel(layout);
        panel.setOpaque(false);
        return panel;
    }

    public static JPanel card() {
        JPanel panel = new RoundedPanel(Color.WHITE);
        panel.setBorder(new EmptyBorder(18, 18, 18, 18));
        return panel;
    }

    public static JButton button(String text) { return button(text, TEAL, Color.WHITE); }

    public static JButton button(String text, Color fill, Color ink) {
        JButton button = new JButton(text) {
            private static final long serialVersionUID = 1L;
            protected void paintComponent(Graphics graphics) {
                Graphics2D pen = (Graphics2D) graphics.create();
                pen.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color color = isEnabled() ? getBackground() : LINE;
                if (getModel().isPressed() || getModel().isRollover()) color = color.darker();
                pen.setColor(color);
                pen.fillRoundRect(0, 0, getWidth(), getHeight(), 13, 13);
                pen.dispose();
                super.paintComponent(graphics);
            }
        };
        button.setBackground(fill);
        button.setForeground(ink);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setBorder(new EmptyBorder(9, 12, 9, 12));
        button.setFont(new Font("Nimbus Sans", Font.BOLD, 11));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    public static JComponent field(String title, JComponent input) {
        JPanel panel = clear(new BorderLayout(0, 6));
        panel.add(new JLabel(title), BorderLayout.NORTH);
        input.setPreferredSize(new Dimension(190, 34));
        input.setBorder(new InputBorder());
        panel.add(input);
        return panel;
    }

    public static class SmoothPanel extends JPanel {
        private static final long serialVersionUID = 1L;
        public SmoothPanel(LayoutManager layout) { super(layout); }
        protected void paintChildren(Graphics graphics) {
            Graphics2D pen = (Graphics2D) graphics.create();
            pen.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            pen.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            super.paintChildren(pen);
            pen.dispose();
        }
    }

    public static class InputBorder extends javax.swing.border.AbstractBorder {
        private static final long serialVersionUID = 1L;
        public Insets getBorderInsets(Component component) { return new Insets(6,8,6,8); }
        public Insets getBorderInsets(Component component, Insets insets) {
            insets.set(6,8,6,8); return insets;
        }
        public void paintBorder(Component component, Graphics graphics, int x, int y, int width, int height) {
            Graphics2D pen = (Graphics2D) graphics.create();
            pen.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            pen.setColor(LINE); pen.drawRoundRect(x,y,width-1,height-1,9,9); pen.dispose();
        }
    }

    public static class RoundedPanel extends SmoothPanel {
        private static final long serialVersionUID = 1L;
        private final Color fill;
        public RoundedPanel(Color fill) { super(new FlowLayout()); this.fill = fill; setOpaque(false); }
        protected void paintComponent(Graphics graphics) {
            Graphics2D pen = (Graphics2D) graphics.create();
            pen.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            pen.setColor(fill);
            pen.fillRoundRect(0, 0, getWidth(), getHeight(), 22, 22);
            pen.setColor(fill.equals(BLACK) ? BLACK : LINE);
            pen.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 22, 22);
            pen.dispose();
        }
    }
}
