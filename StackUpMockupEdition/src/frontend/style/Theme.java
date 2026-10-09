package frontend.style;

import java.awt.*;
import java.awt.geom.*;
import java.math.BigDecimal;
import java.util.Locale;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/** Design system: green and near-black on soft grey, pill controls and large rounded cards. */
public final class Theme {
    public static final Color BRAND = new Color(11, 128, 82);
    public static final Color BRAND_LIGHT = new Color(23, 148, 98);
    public static final Color BRAND_MID = new Color(66, 196, 143);
    public static final Color BRAND_PALE = new Color(168, 222, 193);
    public static final Color MINT = new Color(225, 245, 235);
    public static final Color INK = new Color(17, 22, 20);
    public static final Color BACKGROUND = new Color(243, 245, 244);
    public static final Color FRAME = new Color(229, 233, 232);
    public static final Color LINE = new Color(225, 230, 228);
    public static final Color SOFT = new Color(238, 241, 240);
    public static final Color MUTED = new Color(100, 111, 106);
    public static final Color SELECTED = new Color(236, 246, 240);
    public static final Color DARK_CARD_CIRCLE = new Color(255, 255, 255, 14);
    public static final Color BRAND_CARD_CIRCLE = new Color(255, 255, 255, 22);
    /** Retained names so older call sites read naturally. */
    public static final Color TEAL = BRAND, BLACK = INK, GREEN = BRAND;
    public static final Color ERROR = new Color(190, 52, 52);

    private Theme() {}

    public static void install() {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        for (String name : new String[]{"Inter-Regular.ttf", "Inter-Bold.ttf"}) {
            try { GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(
                Font.createFont(Font.TRUETYPE_FONT, asset(name))); }
            catch (Exception ignored) { }
        }
        try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); }
        catch (Exception ignored) { }
        Font font = font(Font.PLAIN, 13);
        for (String key : new String[]{"Label.font", "Button.font", "TextField.font", "PasswordField.font",
                "Table.font", "TableHeader.font", "ComboBox.font", "OptionPane.messageFont",
                "OptionPane.buttonFont", "ToolTip.font", "MenuItem.font", "PopupMenu.font"})
            UIManager.put(key, font);
        UIManager.put("ButtonUI", "frontend.style.ButtonSkin");
        UIManager.put("ComboBoxUI", "frontend.style.ComboBoxSkin");
        UIManager.put("ScrollBarUI", "frontend.style.ScrollBarSkin");
        UIManager.put("ComboBox.border", new InputBorder());
        UIManager.put("Panel.background", BACKGROUND);
        UIManager.put("Label.foreground", INK);
        UIManager.put("Button.background", SOFT);
        UIManager.put("Button.foreground", INK);
        UIManager.put("ComboBox.background", Color.WHITE);
        UIManager.put("ComboBox.foreground", INK);
        UIManager.put("Table.background", Color.WHITE);
        UIManager.put("Table.foreground", INK);
        UIManager.put("OptionPane.background", Color.WHITE);
        UIManager.put("OptionPane.messageForeground", INK);
        UIManager.put("OptionPane.border", new EmptyBorder(18, 20, 14, 20));
        UIManager.put("OptionPane.buttonAreaBorder", new EmptyBorder(8, 0, 0, 0));
        UIManager.put("PopupMenu.background", Color.WHITE);
        UIManager.put("PopupMenu.border", BorderFactory.createLineBorder(LINE));
        UIManager.put("MenuItem.background", Color.WHITE);
        UIManager.put("MenuItem.foreground", INK);
        UIManager.put("MenuItem.selectionBackground", MINT);
        UIManager.put("MenuItem.selectionForeground", INK);
        UIManager.put("ToolTip.background", INK);
        UIManager.put("ToolTip.foreground", Color.WHITE);
        UIManager.put("ToolTip.border", new EmptyBorder(4, 8, 4, 8));
    }

    /**
     * Finds a file in the assets folder wherever the app was started from: the working
     * directory, a StackUpMockupEdition folder inside it, or any folder above the compiled classes.
     */
    public static java.io.File asset(String name) {
        java.util.List<java.io.File> roots = new java.util.ArrayList<>();
        roots.add(new java.io.File("."));
        roots.add(new java.io.File("StackUpMockupEdition"));
        try {
            java.io.File here = new java.io.File(Theme.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            for (java.io.File dir = here; dir != null; dir = dir.getParentFile()) roots.add(dir);
        } catch (Exception ignored) { }
        for (java.io.File root : roots) {
            java.io.File file = new java.io.File(new java.io.File(root, "assets"), name);
            if (file.isFile()) return file;
        }
        return new java.io.File("assets", name);
    }

    public static Font font(int style, float size) {
        return new Font("Inter", style, 12).deriveFont(size);
    }

    /** Inter covers the peso sign, so every string uses the same family. */
    public static Font fontFor(String text, int style, float size) {
        return font(style, size);
    }

    public static String money(BigDecimal value) {
        return "₱" + String.format(Locale.US, "%,.2f", value);
    }

    public static String whole(BigDecimal value) {
        return "₱" + String.format(Locale.US, "%,.0f", value);
    }

    public static String compact(double value) {
        double abs = Math.abs(value);
        String sign = value < 0 ? "-" : "";
        if (abs >= 1_000_000) return sign + "₱" + trim(abs / 1_000_000) + "M";
        if (abs >= 1_000) return sign + "₱" + trim(abs / 1_000) + "k";
        return sign + "₱" + trim(abs);
    }

    private static String trim(double value) {
        String text = String.format(Locale.US, "%.1f", value);
        return text.endsWith(".0") ? text.substring(0, text.length() - 2) : text;
    }

    public static JLabel label(String text, int size, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(fontFor(text, Font.BOLD, size));
        label.setForeground(color);
        return label;
    }

    public static JLabel muted(String text, int size) {
        JLabel label = new JLabel(text);
        label.setFont(fontFor(text, Font.PLAIN, size));
        label.setForeground(MUTED);
        return label;
    }

    public static JLabel plain(String text, int size, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(fontFor(text, Font.PLAIN, size));
        label.setForeground(color);
        return label;
    }

    /** "Page already open" pop-up: one dark button, no choice to make. */
    public static void alreadyOpen(Component owner, String page) {
        popup(owner, null, Icons.Kind.WINDOW_CHECK, "Page already open",
            "You're already on <b>" + page + "</b>. Pick another section from the sidebar to switch pages.",
            "OK, got it", null);
    }

    /** Themed replacement for JOptionPane.showMessageDialog: title, plain-text message, one OK button. */
    public static void message(Component owner, String title, String message) {
        String safe = String.valueOf(message).replace("&", "&amp;").replace("<", "&lt;").replace("\n", "<br>");
        popup(owner, null, Icons.Kind.INFO, title, safe, "OK", null);
    }

    /** Themed pop-up that holds a small form. Returns true when the main button was pressed. */
    public static boolean form(Component owner, Icons.Kind icon, String title, String message, JComponent body, String yes, String no) {
        return popup(owner, body, icon, title, message, yes, no);
    }

    /** Themed yes/no question. Returns true when the main button was pressed. */
    public static boolean confirm(Component owner, Icons.Kind icon, String title, String message, String yes, String no) {
        return popup(owner, null, icon, title, message, yes, no);
    }

    /**
     * Rounded white pop-up used for every message and question: an icon badge, a close button,
     * a title, a short message and one or two pill buttons. A hidden JOptionPane holds the
     * answer so the window closes as soon as a button sets its value.
     */
    private static boolean popup(Component owner, JComponent extra, Icons.Kind icon, String title, String message, String yes, String no) {
        Window parent = owner == null ? null : SwingUtilities.getWindowAncestor(owner);
        JDialog dialog = new JDialog(parent, title, Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setUndecorated(true);
        dialog.setBackground(new Color(0, 0, 0, 0));

        JOptionPane result = new JOptionPane("", JOptionPane.PLAIN_MESSAGE, JOptionPane.DEFAULT_OPTION, null, new Object[0]);
        result.setVisible(false);
        result.addPropertyChangeListener(JOptionPane.VALUE_PROPERTY, event -> dialog.dispose());

        RoundedPanel card = new RoundedPanel(Color.WHITE);
        card.radius = 30;
        card.outlined = true;
        card.setLayout(new BorderLayout(0, 14));
        card.setBorder(new EmptyBorder(24, 24, 24, 24));

        JPanel top = clear(new BorderLayout());
        top.add(badge(icon, MINT, BRAND, 56), BorderLayout.WEST);
        JButton close = new JButton(Icons.of(Icons.Kind.CLOSE, INK, 16));
        close.setBackground(SOFT);
        close.setPreferredSize(new Dimension(40, 40));
        close.setBorder(BorderFactory.createEmptyBorder());
        close.setFocusPainted(false);
        close.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        close.addActionListener(event -> result.setValue(JOptionPane.CLOSED_OPTION));
        JPanel closeHolder = clear(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        closeHolder.add(close);
        top.add(closeHolder, BorderLayout.EAST);

        JPanel words = clear(new BorderLayout(0, 8));
        words.add(label(title, 22, INK), BorderLayout.NORTH);
        words.add(plain("<html><body style='width:300px'>" + message + "</html>", 14, MUTED));

        JButton primary = dark(yes);
        primary.setPreferredSize(new Dimension(100, 50));
        primary.addActionListener(event -> result.setValue(JOptionPane.OK_OPTION));
        JPanel buttons;
        if (no == null) buttons = equal(0, primary);
        else {
            JButton secondary = outline(no);
            secondary.setPreferredSize(new Dimension(100, 50));
            secondary.addActionListener(event -> result.setValue(JOptionPane.NO_OPTION));
            buttons = equal(10, secondary, primary);
        }

        card.add(top, BorderLayout.NORTH);
        if (extra == null) card.add(words);
        else {
            JPanel middle = clear(new BorderLayout(0, 14));
            middle.add(words, BorderLayout.NORTH);
            middle.add(extra);
            card.add(middle);
        }
        card.add(buttons, BorderLayout.SOUTH);
        card.add(result, BorderLayout.WEST);

        dialog.setContentPane(card);
        dialog.getRootPane().registerKeyboardAction(event -> result.setValue(JOptionPane.CLOSED_OPTION),
            KeyStroke.getKeyStroke("ESCAPE"), JComponent.WHEN_IN_FOCUSED_WINDOW);
        dialog.getRootPane().setDefaultButton(primary);
        if (extra == null) dialog.setPreferredSize(new Dimension(420, 270));
        dialog.pack();
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
        return Integer.valueOf(JOptionPane.OK_OPTION).equals(result.getValue());
    }

    public static JPanel clear(LayoutManager layout) {
        JPanel panel = new SmoothPanel(layout);
        panel.setOpaque(false);
        return panel;
    }

    /** White card with generous padding. */
    public static JPanel card() { return card(Color.WHITE); }

    public static JPanel card(Color fill) {
        RoundedPanel panel = new RoundedPanel(fill);
        panel.setBorder(new EmptyBorder(16, 18, 16, 18));
        return panel;
    }

    /** Solid coloured card with the large, softly lighter circle used in the reference. */
    public static RoundedPanel hero(Color fill) {
        RoundedPanel panel = new RoundedPanel(fill);
        panel.decor = fill.equals(INK) ? DARK_CARD_CIRCLE : BRAND_CARD_CIRCLE;
        panel.setBorder(new EmptyBorder(16, 20, 16, 20));
        return panel;
    }

    /** Lays components in a row; each gets the matching share of the width. */
    public static JPanel columns(int gap, double[] weights, Component... items) {
        JPanel row = clear(new GridBagLayout());
        for (int i = 0; i < items.length; i++) {
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = i; c.gridy = 0; c.weightx = weights[i]; c.weighty = 1;
            c.fill = GridBagConstraints.BOTH;
            c.insets = new Insets(0, i == 0 ? 0 : gap / 2, 0, i == items.length - 1 ? 0 : gap - gap / 2);
            if (i > 0) c.insets.left = gap - gap / 2;
            row.add(items[i], c);
        }
        return row;
    }

    public static JPanel equal(int gap, Component... items) {
        double[] weights = new double[items.length];
        java.util.Arrays.fill(weights, 1);
        return columns(gap, weights, items);
    }

    public static JButton button(String text) { return solid(text, BRAND, Color.WHITE); }

    public static JButton button(String text, Color fill, Color ink) {
        return fill.equals(Color.WHITE) ? outline(text) : solid(text, fill, ink);
    }

    public static JButton dark(String text) { return solid(text, INK, Color.WHITE); }

    public static JButton solid(String text, Color fill, Color ink) {
        JButton button = new JButton(text);
        button.setBackground(fill);
        button.setForeground(ink);
        styleButton(button);
        return button;
    }

    public static JButton outline(String text) {
        JButton button = new JButton(text);
        button.setBackground(Color.WHITE);
        button.setForeground(INK);
        button.putClientProperty("outline", Boolean.TRUE);
        styleButton(button);
        return button;
    }

    public static JButton link(String text) {
        JButton button = new JButton(text);
        button.setBackground(new Color(0, 0, 0, 0));
        button.setForeground(BRAND);
        button.putClientProperty("ghost", Boolean.TRUE);
        styleButton(button);
        button.setBorder(new EmptyBorder(4, 6, 4, 6));
        return button;
    }

    private static void styleButton(JButton button) {
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setOpaque(false);
        button.setBorder(new EmptyBorder(9, 20, 9, 20));
        button.setFont(font(Font.BOLD, 13));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setIconTextGap(8);
    }


    public static JComponent field(String title, JComponent input) {
        JPanel panel = clear(new BorderLayout(0, 5));
        JLabel caption = new JLabel(title);
        caption.setFont(fontFor(title, Font.PLAIN, 13));
        caption.setForeground(INK);
        panel.add(caption, BorderLayout.NORTH);
        input.setPreferredSize(new Dimension(190, 38));
        panel.add(input);
        return panel;
    }

    public static RoundedField input(String placeholder) {
        RoundedField field = new RoundedField();
        field.setPlaceholder(placeholder);
        return field;
    }

    public static RoundedField search(String placeholder) {
        RoundedField field = new RoundedField();
        field.setPlaceholder(placeholder);
        field.setLeadingIcon(Icons.of(Icons.Kind.SEARCH, MUTED, 17));
        return field;
    }

    /** A grey pill holding mutually exclusive choices; the selected one is filled. */
    public static JPanel segmented(String[] labels, int selected, Color accent, java.util.function.IntConsumer onSelect) {
        RoundedPanel group = new RoundedPanel(SOFT);
        group.radius = 999;
        group.outlined = false;
        group.setLayout(new GridLayout(1, labels.length, 2, 0));   // equal-width, centred labels
        group.setBorder(new EmptyBorder(4, 4, 4, 4));
        JButton[] buttons = new JButton[labels.length];
        for (int i = 0; i < labels.length; i++) {
            int index = i;
            JButton button = new JButton(labels[i]);
            buttons[i] = button;
            styleButton(button);
            button.setBorder(new EmptyBorder(6, 16, 6, 16));
            button.setFont(font(Font.PLAIN, 13));
            button.addActionListener(event -> {
                for (int j = 0; j < buttons.length; j++) paintSegment(buttons[j], j == index, accent);
                onSelect.accept(index);
            });
            group.add(button);
        }
        for (int j = 0; j < buttons.length; j++) paintSegment(buttons[j], j == selected, accent);
        return group;
    }

    private static void paintSegment(JButton button, boolean active, Color accent) {
        button.setBackground(active ? accent : new Color(0, 0, 0, 0));
        button.setForeground(active ? Color.WHITE : MUTED);
        button.setFont(font(active ? Font.BOLD : Font.PLAIN, 13));
        button.putClientProperty("ghost", active ? null : Boolean.TRUE);
        button.repaint();
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

    public static class RoundedPanel extends SmoothPanel {
        private static final long serialVersionUID = 1L;
        private Color fill;
        public int radius = 30;
        public boolean outlined;
        public boolean clip;
        public Color decor;
        public RoundedPanel(Color fill) {
            super(new FlowLayout());
            this.fill = fill;
            setOpaque(false);
        }
        public void setFill(Color fill) { this.fill = fill; repaint(); }
        private Shape shape() {
            int arc = Math.min(radius * 2, Math.min(getWidth(), getHeight()));
            return new RoundRectangle2D.Double(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
        }
        protected void paintComponent(Graphics graphics) {
            Graphics2D pen = (Graphics2D) graphics.create();
            pen.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Shape shape = shape();
            pen.setColor(fill);
            pen.fill(shape);
            if (decor != null) {
                pen.clip(shape);
                int size = (int) (Math.min(getWidth(), getHeight() * 2.2) * 0.62);
                pen.setColor(decor);
                pen.fillOval(getWidth() - size * 3 / 4, getHeight() - size * 3 / 5, size, size);
            }
            pen.dispose();
            if (outlined) {
                Graphics2D line = (Graphics2D) graphics.create();
                line.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                line.setColor(LINE);
                line.draw(shape);
                line.dispose();
            }
        }
        protected void paintChildren(Graphics graphics) {
            if (!clip) { super.paintChildren(graphics); return; }
            Graphics2D pen = (Graphics2D) graphics.create();
            pen.clip(shape());
            super.paintChildren(pen);
            pen.dispose();
        }
    }

    public static class InputBorder extends javax.swing.border.AbstractBorder {
        private static final long serialVersionUID = 1L;
        public Insets getBorderInsets(Component component) { return new Insets(8, 16, 8, 16); }
        public Insets getBorderInsets(Component component, Insets insets) {
            insets.set(8, 16, 8, 16); return insets;
        }
        public void paintBorder(Component component, Graphics graphics, int x, int y, int width, int height) {
            Graphics2D pen = (Graphics2D) graphics.create();
            pen.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            pen.setColor(LINE);
            pen.draw(new RoundRectangle2D.Double(x + .5, y + .5, width - 1, height - 1, 26, 26));
            pen.dispose();
        }
    }

    /** Text input with a rounded outline, optional leading icon and placeholder. */
    public static class RoundedField extends JTextField {
        private static final long serialVersionUID = 1L;
        private String placeholder = "";
        private Icon leading;
        public RoundedField() {
            setOpaque(false);
            setFont(font(Font.PLAIN, 14));
            setForeground(INK);
            setCaretColor(INK);
            setSelectionColor(BRAND_PALE);
            setBorder(new EmptyBorder(8, 16, 8, 16));
            setPreferredSize(new Dimension(190, 38));
        }
        public void setPlaceholder(String text) { placeholder = text; repaint(); }
        public void setLeadingIcon(Icon icon) {
            leading = icon;
            setBorder(new EmptyBorder(8, 16 + icon.getIconWidth() + 10, 8, 16));
        }
        protected void paintComponent(Graphics graphics) {
            Graphics2D pen = (Graphics2D) graphics.create();
            pen.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            pen.setColor(isEnabled() ? Color.WHITE : SOFT);
            pen.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 28, 28));
            pen.setColor(isFocusOwner() ? BRAND : LINE);
            pen.draw(new RoundRectangle2D.Double(.5, .5, getWidth() - 1, getHeight() - 1, 28, 28));
            if (leading != null) leading.paintIcon(this, pen, 16, (getHeight() - leading.getIconHeight()) / 2);
            pen.dispose();
            super.paintComponent(graphics);
            if (getText().isEmpty() && !placeholder.isEmpty()) {
                Graphics2D hint = (Graphics2D) graphics.create();
                hint.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                hint.setColor(new Color(130, 140, 135));
                hint.setFont(getFont());
                Insets in = getInsets();
                hint.drawString(placeholder, in.left, (getHeight() + hint.getFontMetrics().getAscent() - hint.getFontMetrics().getDescent()) / 2);
                hint.dispose();
            }
        }
        { addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent event) { repaint(); }
            public void focusLost(java.awt.event.FocusEvent event) { repaint(); }
        }); }
    }

    public static class RoundedPassword extends JPasswordField {
        private static final long serialVersionUID = 1L;
        private String placeholder = "";
        public RoundedPassword() {
            setOpaque(false);
            setFont(font(Font.PLAIN, 14));
            setForeground(INK);
            setCaretColor(INK);
            setBorder(new EmptyBorder(8, 16, 8, 16));
            setPreferredSize(new Dimension(190, 38));
            addFocusListener(new java.awt.event.FocusAdapter() {
                public void focusGained(java.awt.event.FocusEvent event) { repaint(); }
                public void focusLost(java.awt.event.FocusEvent event) { repaint(); }
            });
        }
        public void setPlaceholder(String text) { placeholder = text; repaint(); }
        protected void paintComponent(Graphics graphics) {
            Graphics2D pen = (Graphics2D) graphics.create();
            pen.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            pen.setColor(Color.WHITE);
            pen.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 28, 28));
            pen.setColor(isFocusOwner() ? BRAND : LINE);
            pen.draw(new RoundRectangle2D.Double(.5, .5, getWidth() - 1, getHeight() - 1, 28, 28));
            pen.dispose();
            super.paintComponent(graphics);
            if (getPassword().length == 0 && !placeholder.isEmpty()) {
                Graphics2D hint = (Graphics2D) graphics.create();
                hint.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                hint.setColor(new Color(130, 140, 135));
                hint.setFont(getFont());
                Insets in = getInsets();
                hint.drawString(placeholder, in.left, (getHeight() + hint.getFontMetrics().getAscent() - hint.getFontMetrics().getDescent()) / 2);
                hint.dispose();
            }
        }
    }

    /** Pill-shaped status badge with a leading dot, coloured by meaning. */
    public static class Chip extends JComponent {
        private static final long serialVersionUID = 1L;
        private String text;
        private Color background, foreground, dot;
        private boolean showDot = true;
        public Chip(String text) { setText(text); setOpaque(false); }
        public Chip(String text, Color background, Color foreground, Color dot) {
            this.text = text; this.background = background; this.foreground = foreground; this.dot = dot;
            setOpaque(false);
        }
        public void setShowDot(boolean value) { showDot = value; }
        public void setText(String value) {
            text = value;
            switch (value) {
                case "Revenue": case "Created": case "Active": case "Scheduled": case "Paid": case "Upcoming":
                    background = MINT; foreground = BRAND; dot = BRAND; break;
                case "Admin": case "Edited": case "Deleted":
                    background = INK; foreground = Color.WHITE; dot = Color.WHITE; break;
                default:
                    background = SOFT; foreground = new Color(60, 68, 64); dot = new Color(60, 68, 64);
            }
            if (value.startsWith("Due")) { background = INK; foreground = Color.WHITE; dot = Color.WHITE; }
            revalidate(); repaint();
        }
        public Dimension getPreferredSize() {
            FontMetrics metrics = getFontMetrics(font(Font.BOLD, 12));
            return new Dimension(metrics.stringWidth(text) + (showDot ? 38 : 26), 26);
        }
        protected void paintComponent(Graphics graphics) {
            Graphics2D pen = (Graphics2D) graphics.create();
            pen.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            pen.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            pen.setColor(background);
            pen.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
            int x = 13;
            if (showDot) { pen.setColor(dot); pen.fillOval(x, getHeight() / 2 - 3, 6, 6); x += 12; }
            pen.setColor(foreground);
            pen.setFont(font(Font.BOLD, 12));
            FontMetrics metrics = pen.getFontMetrics();
            pen.drawString(text, x, (getHeight() + metrics.getAscent() - metrics.getDescent()) / 2);
            pen.dispose();
        }
    }

    /** Circle with the user's initials: dark for admins, mint for everyone else. */
    public static class Avatar extends JComponent {
        private static final long serialVersionUID = 1L;
        private final String initials;
        private final boolean dark;
        private final int size;
        public Avatar(String name, boolean dark, int size) {
            String clean = name == null ? "" : name.trim();
            initials = clean.length() >= 2 ? clean.substring(0, 2).toUpperCase(Locale.ROOT) : clean.toUpperCase(Locale.ROOT);
            this.dark = dark; this.size = size; setOpaque(false);
        }
        public Dimension getPreferredSize() { return new Dimension(size, size); }
        protected void paintComponent(Graphics graphics) {
            Graphics2D pen = (Graphics2D) graphics.create();
            pen.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            pen.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            pen.setColor(dark ? INK : MINT);
            pen.fillOval(0, 0, getWidth(), getHeight());
            pen.setColor(dark ? Color.WHITE : BRAND);
            Font initialsFont = font(Font.BOLD, size * 0.36f);
            pen.setFont(initialsFont);
            // Centre the visible letter shapes, not the font's line box, so the initials sit in the middle of the circle.
            java.awt.geom.Rectangle2D shape = initialsFont
                .createGlyphVector(pen.getFontRenderContext(), initials).getVisualBounds();
            pen.drawString(initials,
                (float) ((getWidth() - shape.getWidth()) / 2 - shape.getX()),
                (float) ((getHeight() - shape.getHeight()) / 2 - shape.getY()));
            pen.dispose();
        }
    }

    /** Filled circle holding a line icon, used as the corner badge on stat cards. */
    public static JComponent badge(Icons.Kind kind, Color circle, Color stroke, int size) {
        return new JComponent() {
            private static final long serialVersionUID = 1L;
            { setOpaque(false); }
            public Dimension getPreferredSize() { return new Dimension(size, size); }
            protected void paintComponent(Graphics graphics) {
                Graphics2D pen = (Graphics2D) graphics.create();
                pen.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                pen.setColor(circle);
                pen.fillOval(0, 0, size - 1, size - 1);
                Icon icon = Icons.of(kind, stroke, size * 5 / 12);
                icon.paintIcon(this, pen, (size - icon.getIconWidth()) / 2, (size - icon.getIconHeight()) / 2);
                pen.dispose();
            }
        };
    }
}
