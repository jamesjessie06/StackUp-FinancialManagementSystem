package frontend.style;

import java.awt.*;
import java.awt.geom.*;
import javax.swing.Icon;

/** Thin line icons drawn on a 24-unit grid, so they stay crisp at any size and colour. */
public final class Icons {
    public enum Kind {
        HOME, PLUS_CIRCLE, CLOCK, DOC, BARS, PIE, PULSE, USERS, HELP, LOGOUT, SEARCH, BELL, INFO,
        CHEVRON, DOWNLOAD, CALENDAR, TREND, WALLET, CHECK, UP_RIGHT, DOWN_RIGHT, PLUS, MINUS, WIFI, WINDOW_CHECK, CLOSE
    }

    private Icons() {}

    public static Icon of(Kind kind, Color color, int size) {
        return new Icon() {
            public int getIconWidth() { return size; }
            public int getIconHeight() { return size; }
            public void paintIcon(Component component, Graphics graphics, int x, int y) {
                Graphics2D pen = (Graphics2D) graphics.create();
                pen.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                pen.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
                pen.translate(x, y);
                pen.scale(size / 24.0, size / 24.0);
                pen.setColor(color);
                pen.setStroke(new BasicStroke(1.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                draw(pen, kind);
                pen.dispose();
            }
        };
    }

    private static void line(Graphics2D pen, double x1, double y1, double x2, double y2) {
        pen.draw(new Line2D.Double(x1, y1, x2, y2));
    }

    private static void poly(Graphics2D pen, double... points) {
        Path2D path = new Path2D.Double();
        path.moveTo(points[0], points[1]);
        for (int i = 2; i < points.length; i += 2) path.lineTo(points[i], points[i + 1]);
        pen.draw(path);
    }

    private static void circle(Graphics2D pen, double cx, double cy, double r) {
        pen.draw(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
    }

    private static void draw(Graphics2D pen, Kind kind) {
        switch (kind) {
            case HOME:
                poly(pen, 3.5, 11, 12, 3.8, 20.5, 11);
                poly(pen, 6, 9.5, 6, 20, 18, 20, 18, 9.5);
                break;
            case PLUS_CIRCLE:
                circle(pen, 12, 12, 9); line(pen, 12, 7.5, 12, 16.5); line(pen, 7.5, 12, 16.5, 12);
                break;
            case CLOCK:
                circle(pen, 12, 12, 9); poly(pen, 12, 6.8, 12, 12, 15.6, 14);
                break;
            case DOC:
                pen.draw(new RoundRectangle2D.Double(5, 3, 14, 18, 4, 4));
                line(pen, 8.6, 9, 15.4, 9); line(pen, 8.6, 13, 15.4, 13); line(pen, 8.6, 17, 12.4, 17);
                break;
            case BARS:
                line(pen, 6.5, 20, 6.5, 14); line(pen, 12, 20, 12, 5); line(pen, 17.5, 20, 17.5, 10);
                break;
            case PIE:
                pen.draw(new Arc2D.Double(3, 5, 16, 16, 90, 270, Arc2D.PIE));
                pen.draw(new Arc2D.Double(12, 2, 9.5, 9.5, 0, 90, Arc2D.PIE));
                break;
            case PULSE:
                poly(pen, 2.5, 12, 7, 12, 10, 4, 14, 20, 17, 12, 21.5, 12);
                break;
            case USERS:
                circle(pen, 9, 8, 3.6);
                pen.draw(new Arc2D.Double(2.8, 14, 12.4, 12, 0, 180, Arc2D.OPEN));
                pen.draw(new Arc2D.Double(13.2, 4.6, 6.4, 6.4, -80, 200, Arc2D.OPEN));
                pen.draw(new Arc2D.Double(15, 13.2, 6.6, 9, 0, 150, Arc2D.OPEN));
                break;
            case HELP:
                circle(pen, 12, 12, 9);
                pen.draw(new Arc2D.Double(9, 7.3, 6, 5.4, 160, -230, Arc2D.OPEN));
                poly(pen, 12, 12.4, 12, 14.2);
                pen.fill(new Ellipse2D.Double(11.1, 16, 1.8, 1.8));
                break;
            case LOGOUT:
                poly(pen, 10, 7, 5, 12, 10, 17); line(pen, 5, 12, 15, 12);
                poly(pen, 14, 4.5, 20, 4.5, 20, 19.5, 14, 19.5);
                break;
            case SEARCH:
                circle(pen, 10.8, 10.8, 6.6); line(pen, 15.8, 15.8, 21, 21);
                break;
            case BELL:
                pen.draw(new Arc2D.Double(6, 3.5, 12, 12, 0, 180, Arc2D.OPEN));
                line(pen, 6, 9.5, 6, 16); line(pen, 18, 9.5, 18, 16); line(pen, 3.8, 16.5, 20.2, 16.5);
                pen.draw(new Arc2D.Double(9.4, 17.4, 5.2, 4.4, 180, 180, Arc2D.OPEN));
                break;
            case INFO:
                circle(pen, 12, 12, 9); line(pen, 12, 11, 12, 17);
                pen.fill(new Ellipse2D.Double(11, 6.4, 2, 2));
                break;
            case CHEVRON:
                poly(pen, 6, 9.2, 12, 15, 18, 9.2);
                break;
            case DOWNLOAD:
                line(pen, 12, 4, 12, 15); poly(pen, 7, 10.4, 12, 15.4, 17, 10.4); line(pen, 5, 20, 19, 20);
                break;
            case CALENDAR:
                pen.draw(new RoundRectangle2D.Double(3.8, 5, 16.4, 15.4, 4, 4));
                line(pen, 3.8, 10, 20.2, 10); line(pen, 8, 3, 8, 7); line(pen, 16, 3, 16, 7);
                break;
            case TREND:
                poly(pen, 4, 15.5, 8.5, 11, 12.5, 14.5, 20, 7.5);
                break;
            case WALLET:
                pen.draw(new RoundRectangle2D.Double(3, 5.5, 18, 13, 4, 4));
                line(pen, 3, 10, 21, 10); line(pen, 16, 14.5, 18, 14.5);
                break;
            case CHECK:
                poly(pen, 5, 12.6, 10, 17.2, 19, 7);
                break;
            case UP_RIGHT:
                line(pen, 7, 17, 17, 7); poly(pen, 8, 7, 17, 7, 17, 16);
                break;
            case DOWN_RIGHT:
                line(pen, 7, 7, 17, 17); poly(pen, 17, 8, 17, 17, 8, 17);
                break;
            case PLUS:
                line(pen, 12, 5, 12, 19); line(pen, 5, 12, 19, 12);
                break;
            case MINUS:
                line(pen, 5, 12, 19, 12);
                break;
            case WINDOW_CHECK:
                pen.draw(new RoundRectangle2D.Double(3, 4, 18, 16, 4, 4));
                line(pen, 3, 9, 21, 9);
                poly(pen, 8.5, 14.5, 11, 17, 15.5, 12.2);
                break;
            case CLOSE:
                line(pen, 6, 6, 18, 18); line(pen, 18, 6, 6, 18);
                break;
            case WIFI:
                pen.draw(new Arc2D.Double(5, 7, 14, 14, 45, 90, Arc2D.OPEN));
                pen.draw(new Arc2D.Double(8.4, 11.4, 7.2, 7.2, 45, 90, Arc2D.OPEN));
                pen.fill(new Ellipse2D.Double(10.9, 16.6, 2.4, 2.4));
                pen.draw(new Arc2D.Double(1.8, 3.2, 20.4, 20.4, 45, 90, Arc2D.OPEN));
                break;
            default:
                break;
        }
    }
}
