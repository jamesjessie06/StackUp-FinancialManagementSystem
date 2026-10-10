package frontend.style;

import java.awt.*;
import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicButtonUI;

/** Fully rounded buttons: solid, outlined or ghost, with a gentle hover and press state. */
public final class ButtonSkin extends BasicButtonUI {
    public static ComponentUI createUI(JComponent component) { return new ButtonSkin(); }

    protected void installDefaults(AbstractButton button) {
        super.installDefaults(button);
        button.setOpaque(false);
        button.setRolloverEnabled(true);
        if (button.getBorder() instanceof javax.swing.plaf.UIResource)
            button.setBorder(BorderFactory.createEmptyBorder(9, 20, 9, 20));
    }

    public void paint(Graphics graphics, JComponent component) {
        AbstractButton button = (AbstractButton) component;
        Graphics2D pen = (Graphics2D) graphics.create();
        pen.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        pen.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        boolean ghost = Boolean.TRUE.equals(button.getClientProperty("ghost"));
        boolean outline = Boolean.TRUE.equals(button.getClientProperty("outline"));
        ButtonModel model = button.getModel();
        Color base = button.getBackground();
        if (!button.isEnabled()) base = Theme.SOFT;
        else if (model.isPressed()) base = ghost ? Theme.MINT : shade(base, 0.82f);
        else if (model.isRollover()) base = ghost ? new Color(0, 0, 0, 14) : outline ? Theme.BACKGROUND : shade(base, 0.9f);
        int arc = button.getHeight();
        if (!(ghost && !model.isRollover() && !model.isPressed())) {
            pen.setColor(base);
            pen.fillRoundRect(0, 0, button.getWidth(), button.getHeight(), arc, arc);
        }
        if (outline && button.isEnabled()) {
            pen.setColor(Theme.LINE);
            pen.drawRoundRect(0, 0, button.getWidth() - 1, button.getHeight() - 1, arc, arc);
        }
        pen.dispose();
        super.paint(graphics, component);
    }

    private static Color shade(Color color, float factor) {
        return new Color(Math.round(color.getRed() * factor), Math.round(color.getGreen() * factor),
            Math.round(color.getBlue() * factor), color.getAlpha());
    }

    protected void paintText(Graphics graphics, JComponent component, Rectangle textRect, String text) {
        AbstractButton button = (AbstractButton) component;
        FontMetrics metrics = graphics.getFontMetrics();
        graphics.setColor(button.isEnabled() ? button.getForeground() : new Color(150, 158, 154));
        graphics.setFont(Theme.fontFor(text, button.getFont().getStyle(), button.getFont().getSize2D()));
        metrics = graphics.getFontMetrics();
        graphics.drawString(text, textRect.x, textRect.y + metrics.getAscent());
    }
}
