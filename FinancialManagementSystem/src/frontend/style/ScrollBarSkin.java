package frontend.style;

import java.awt.*;
import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicScrollBarUI;

/** Slim, buttonless scroll bars that stay out of the way of the cards. */
public final class ScrollBarSkin extends BasicScrollBarUI {
    public static ComponentUI createUI(JComponent component) { return new ScrollBarSkin(); }

    protected JButton createDecreaseButton(int orientation) { return empty(); }
    protected JButton createIncreaseButton(int orientation) { return empty(); }
    private JButton empty() {
        JButton button = new JButton();
        button.setPreferredSize(new Dimension(0, 0));
        return button;
    }
    public Dimension getPreferredSize(JComponent component) { return new Dimension(10, 10); }

    protected void paintTrack(Graphics graphics, JComponent component, Rectangle bounds) { }

    protected void paintThumb(Graphics graphics, JComponent component, Rectangle bounds) {
        if (bounds.isEmpty() || !scrollbar.isEnabled()) return;
        Graphics2D pen = (Graphics2D) graphics.create();
        pen.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        pen.setColor(isThumbRollover() || isDragging ? new Color(150, 160, 155) : new Color(188, 196, 192));
        boolean vertical = scrollbar.getOrientation() == Adjustable.VERTICAL;
        int x = vertical ? bounds.x + 2 : bounds.x, y = vertical ? bounds.y : bounds.y + 2;
        int w = vertical ? bounds.width - 4 : bounds.width, h = vertical ? bounds.height : bounds.height - 4;
        pen.fillRoundRect(x, y, w, h, 6, 6);
        pen.dispose();
    }
}
