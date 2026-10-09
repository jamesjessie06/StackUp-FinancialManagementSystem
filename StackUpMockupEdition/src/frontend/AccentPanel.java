package frontend;

import java.awt.*;

/** Decorative shapes give the interface depth without adding financial data. */
public final class AccentPanel extends Theme.SmoothPanel {
    private static final long serialVersionUID = 1L;
    private final boolean dark;

    public AccentPanel(LayoutManager layout, boolean dark) {
        super(layout);
        this.dark = dark;
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D pen = (Graphics2D) graphics.create();
        pen.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Color start = dark ? new Color(0, 93, 88) : new Color(240, 249, 246);
        Color end = dark ? new Color(0, 163, 142) : new Color(249, 251, 253);
        pen.setPaint(new GradientPaint(0, 0, start, getWidth(), getHeight(), end));
        pen.fillRoundRect(0, 0, getWidth(), getHeight(), 28, 28);
        pen.setColor(dark ? new Color(255, 255, 255, 18) : new Color(0, 151, 139, 12));
        int size = Math.max(160, getHeight() * 2);
        pen.fillOval(getWidth() - size / 2, -size / 2, size, size);
        pen.setStroke(new BasicStroke(22));
        pen.drawOval(getWidth() - size / 2 - 80, -size / 2 + 60, size, size);
        pen.dispose();
    }
}
