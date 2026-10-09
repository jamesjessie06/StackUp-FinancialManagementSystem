package frontend.style;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicComboBoxUI;

/** Rounded white drop-down matching the text inputs. */
public final class ComboBoxSkin extends BasicComboBoxUI {
    public static ComponentUI createUI(JComponent component) { return new ComboBoxSkin(); }

    protected void installDefaults() {
        super.installDefaults();
        comboBox.setOpaque(false);
        comboBox.setFont(Theme.font(Font.PLAIN, 14));
    }

    protected JButton createArrowButton() {
        JButton arrow = new JButton(Icons.of(Icons.Kind.CHEVRON, Theme.INK, 16));
        arrow.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 14));
        arrow.setContentAreaFilled(false);
        arrow.putClientProperty("ghost", Boolean.TRUE);
        arrow.setBackground(new Color(0, 0, 0, 0));
        arrow.setFocusPainted(false);
        arrow.setOpaque(false);
        return arrow;
    }

    public void paint(Graphics graphics, JComponent component) {
        Graphics2D pen = (Graphics2D) graphics.create();
        pen.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = component.getWidth(), h = component.getHeight();
        pen.setColor(component.isEnabled() ? Color.WHITE : Theme.SOFT);
        pen.fill(new RoundRectangle2D.Double(0, 0, w, h, 28, 28));
        pen.setColor(comboBox.hasFocus() ? Theme.BRAND : Theme.LINE);
        pen.draw(new RoundRectangle2D.Double(.5, .5, w - 1, h - 1, 28, 28));
        pen.dispose();
        super.paint(graphics, component);
    }

    public void paintCurrentValueBackground(Graphics graphics, Rectangle bounds, boolean hasFocus) { }

    protected ListCellRenderer<Object> createRenderer() {
        return new DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean focus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, selected, focus);
                label.setFont(Theme.font(Font.PLAIN, 14));
                label.setBorder(BorderFactory.createEmptyBorder(6, index < 0 ? 2 : 12, 6, 12));
                label.setOpaque(index >= 0);
                label.setBackground(selected ? Theme.MINT : Color.WHITE);
                label.setForeground(Theme.INK);
                return label;
            }
        };
    }
}
