package frontend;

import backend.*;
import frontend.style.*;
import java.awt.*;
import java.awt.geom.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.*;

public final class ChartPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    public enum Mode { MONTHLY, TOTALS, REVENUE_CATEGORIES, EXPENSE_CATEGORIES }
    private static final Color[] SLICES = {Theme.INK, Theme.BRAND, Theme.BRAND_MID, Theme.BRAND_PALE, new Color(200, 205, 203)};
    private static final String[] MONTHS = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};

    private final List<FinancialRecord> records;
    private final Mode mode;
    private final int year;
    private int hover = -1;
    public static final Color REVENUE_LINE = Theme.BRAND, EXPENSE_LINE = new Color(232, 84, 84);

    public ChartPanel(List<FinancialRecord> records, Mode mode, int year) {
        this.records = List.copyOf(records);
        this.mode = mode;
        this.year = year;
        setOpaque(false);
        setPreferredSize(new Dimension(300, 240));
        if (mode == Mode.MONTHLY) hoverTracking();
    }

    private void hoverTracking() {
        addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseMoved(java.awt.event.MouseEvent event) {
                double slot = (getWidth() - 24 - 52) / 12.0;
                int month = (int) Math.floor((event.getX() - 52) / slot);
                int next = month < 0 || month >= shownMonths() || records.isEmpty() ? -1 : month;
                if (next != hover) { hover = next; repaint(); }
            }
        });
        addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseExited(java.awt.event.MouseEvent event) { hover = -1; repaint(); }
        });
    }

    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D pen = (Graphics2D) graphics.create();
        pen.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        pen.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        pen.setFont(Theme.font(Font.PLAIN, 12));
        if (mode == Mode.REVENUE_CATEGORIES || mode == Mode.EXPENSE_CATEGORIES) donut(pen);
        else if (mode == Mode.MONTHLY) monthly(pen);
        else totals(pen);
        pen.dispose();
    }

    

    private static double ceiling(double value) {
        if (value <= 0) return 100;
        double magnitude = Math.pow(10, Math.floor(Math.log10(value)));
        for (double step : new double[]{1, 1.5, 2, 2.5, 3, 4, 5, 6, 8, 10}) {
            if (value <= step * magnitude) return step * magnitude;
        }
        return 10 * magnitude;
    }

    private void text(Graphics2D pen, String text, double x, double y, Color color, boolean center) {
        pen.setColor(color);
        pen.setFont(Theme.fontFor(text, pen.getFont().getStyle(), pen.getFont().getSize2D()));
        double width = pen.getFontMetrics().stringWidth(text);
        pen.drawString(text, (float) (center ? x - width / 2 : x), (float) y);
    }

    private void legend(Graphics2D pen, int right, int y, String[] names, Paint[] paints) {
        pen.setFont(Theme.font(Font.PLAIN, 13));
        int x = right;
        for (int i = names.length - 1; i >= 0; i--) {
            int width = pen.getFontMetrics().stringWidth(names[i]);
            x -= width;
            text(pen, names[i], x, y + 4, Theme.INK, false);
            x -= 18;
            pen.setPaint(paints[i]);
            pen.fill(new RoundRectangle2D.Double(x, y - 5, 12, 12, 5, 5));
            x -= 14;
        }
    }

    private void bar(Graphics2D pen, Paint paint, double x, double top, double width, double bottom) {
        double height = bottom - top;
        if (height < 1) return;
        double arc = Math.min(Math.min(width, 22), height * 2);
        Shape shape = new RoundRectangle2D.Double(x, top, width, height + arc / 2, arc, arc);
        pen.setPaint(paint);
        Shape previous = pen.getClip();
        pen.clipRect((int) Math.floor(x) - 1, (int) Math.floor(top) - 1, (int) Math.ceil(width) + 2, (int) Math.ceil(height) + 2);
        pen.fill(shape);
        pen.setClip(previous);
    }

    private void grid(Graphics2D pen, int left, int right, int top, int bottom, double max, boolean lines) {
        pen.setFont(Theme.font(Font.PLAIN, 12));
        for (int i = 0; i <= 4; i++) {
            double y = bottom - (bottom - top) * i / 4.0;
            if (lines && i > 0) { pen.setColor(new Color(0, 0, 0, 14)); pen.draw(new Line2D.Double(left, y, right, y)); }
            double value = max * i / 4;
            String label = value == 0 ? "0" : value >= 1000 ? Theme.compact(value).substring(1) : String.format(java.util.Locale.US, "%.0f", value);
            text(pen, label, left - 12 - pen.getFontMetrics().stringWidth(label), y + 4, Theme.MUTED, false);
        }
    }

    private void emptyNote(Graphics2D pen, int cx, int cy) {
        pen.setColor(Theme.MINT);
        pen.fill(new RoundRectangle2D.Double(cx - 125, cy - 28, 250, 56, 28, 28));
        pen.setFont(Theme.font(Font.BOLD, 13));
        text(pen, "Your chart starts with your data", cx, cy - 2, Theme.BRAND, true);
        pen.setFont(Theme.font(Font.PLAIN, 12));
        text(pen, "No transactions recorded yet", cx, cy + 14, Theme.MUTED, true);
    }

    
    private void monthly(Graphics2D pen) {
        BigDecimal[][] data = ChartData.monthly(records, year);
        int shown = shownMonths();
        double highest = 0;
        for (int m = 0; m < shown; m++) highest = Math.max(highest, Math.max(data[0][m].doubleValue(), data[1][m].doubleValue()));
        double max = ceiling(highest);
        int left = 52, right = getWidth() - 24, top = 24, bottom = getHeight() - 34;
        grid(pen, left, right, top, bottom, max, true);
        double slot = (right - left) / 12.0;
        for (int m = 0; m < 12; m++) text(pen, MONTHS[m], left + slot * (m + .5), bottom + 22, Theme.MUTED, true);

        double[] x = new double[shown], revenue = new double[shown], expense = new double[shown];
        for (int m = 0; m < shown; m++) {
            x[m] = left + slot * (m + .5);
            revenue[m] = bottom - data[0][m].doubleValue() / max * (bottom - top);
            expense[m] = bottom - data[1][m].doubleValue() / max * (bottom - top);
        }
        if (hover >= 0 && hover < shown) {
            pen.setColor(new Color(0, 0, 0, 30));
            pen.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{4f, 4f}, 0f));
            pen.draw(new Line2D.Double(x[hover], top, x[hover], bottom));
        }
        curve(pen, x, expense, EXPENSE_LINE, top, bottom);
        curve(pen, x, revenue, REVENUE_LINE, top, bottom);
        if (hover >= 0 && hover < shown) {
            dot(pen, x[hover], revenue[hover], REVENUE_LINE);
            dot(pen, x[hover], expense[hover], EXPENSE_LINE);
            tooltip(pen, x[hover], Math.min(revenue[hover], expense[hover]), left, right,
                MONTHS[hover] + " " + year, data[0][hover], data[1][hover]);
        }
        if (records.isEmpty()) emptyNote(pen, (left + right) / 2, (top + bottom) / 2);
    }

    private int shownMonths() {
        return year == java.time.LocalDate.now().getYear() ? java.time.LocalDate.now().getMonthValue() : 12;
    }

    private void curve(Graphics2D pen, double[] x, double[] y, Color color, int top, int bottom) {
        if (x.length == 0) return;
        Path2D path = new Path2D.Double();
        path.moveTo(x[0], y[0]);
        for (int i = 0; i < x.length - 1; i++) {
            int before = Math.max(0, i - 1), after = Math.min(x.length - 1, i + 2);
            double c1x = x[i] + (x[i + 1] - x[before]) / 6, c1y = y[i] + (y[i + 1] - y[before]) / 6;
            double c2x = x[i + 1] - (x[after] - x[i]) / 6, c2y = y[i + 1] - (y[after] - y[i]) / 6;
            c1y = Math.max(top, Math.min(bottom, c1y));
            c2y = Math.max(top, Math.min(bottom, c2y));
            path.curveTo(c1x, c1y, c2x, c2y, x[i + 1], y[i + 1]);
        }
        pen.setColor(color);
        pen.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        pen.draw(path);
    }

    private void dot(Graphics2D pen, double x, double y, Color color) {
        pen.setStroke(new BasicStroke(3f));
        pen.setColor(Color.WHITE);
        pen.fill(new Ellipse2D.Double(x - 6, y - 6, 12, 12));
        pen.setColor(color);
        pen.draw(new Ellipse2D.Double(x - 6, y - 6, 12, 12));
    }

    private void tooltip(Graphics2D pen, double x, double y, int left, int right, String month, BigDecimal revenue, BigDecimal expense) {
        pen.setStroke(new BasicStroke(1f));
        pen.setFont(Theme.font(Font.BOLD, 12));
        String[] lines = {month, "Revenue  " + Theme.whole(revenue), "Expenses  " + Theme.whole(expense)};
        int width = 0;
        for (String line : lines) width = Math.max(width, pen.getFontMetrics().stringWidth(line));
        width += 28;
        int height = 74;
        double bx = x + 16 + width > right ? x - 16 - width : x + 16;
        double by = Math.max(6, y - height - 12);
        pen.setColor(Color.WHITE);
        pen.fill(new RoundRectangle2D.Double(bx, by, width, height, 16, 16));
        pen.setColor(Theme.LINE);
        pen.draw(new RoundRectangle2D.Double(bx, by, width, height, 16, 16));
        text(pen, lines[0], bx + 14, by + 22, Theme.INK, false);
        pen.setFont(Theme.font(Font.PLAIN, 12));
        text(pen, lines[1], bx + 14, by + 42, REVENUE_LINE, false);
        text(pen, lines[2], bx + 14, by + 60, EXPENSE_LINE, false);
    }

    private void totals(Graphics2D pen) {
        double revenue = ChartData.total(records, "Revenue").doubleValue();
        double expense = ChartData.total(records, "Expense").doubleValue();
        double[] values = {revenue, expense, revenue - expense};
        String[] names = {"Revenue", "Expenses", "Profit"};
        Paint[] paints = {Theme.BRAND, Theme.INK, Theme.BRAND};
        double max = ceiling(Math.max(revenue, expense)), min = values[2] < 0 ? -ceiling(-values[2]) : 0;
        int left = 52, right = getWidth() - 16, top = 34, bottom = getHeight() - 34;
        double zero = bottom - (0 - min) / (max - min) * (bottom - top);
        pen.setFont(Theme.font(Font.PLAIN, 12));
        for (int i = 0; i <= 4; i++) {
            double value = min + (max - min) * i / 4;
            double y = bottom - (bottom - top) * i / 4.0;
            String label = value == 0 ? "0" : Theme.compact(value).replace("₱", "");
            if (i > 0) { pen.setColor(new Color(0, 0, 0, 14)); pen.draw(new Line2D.Double(left, y, right, y)); }
            text(pen, label, left - 12 - pen.getFontMetrics().stringWidth(label), y + 4, Theme.MUTED, false);
        }
        pen.setColor(new Color(0, 0, 0, 40));
        pen.draw(new Line2D.Double(left, zero, right, zero));
        double slot = (right - left) / 3.0, width = Math.min(120, slot * .5);
        for (int i = 0; i < 3; i++) {
            double x = left + slot * i + (slot - width) / 2;
            double y = bottom - (values[i] - min) / (max - min) * (bottom - top);
            if (values[i] >= 0) bar(pen, paints[i], x, y, width, zero);
            else {
                pen.setPaint(paints[i]);
                pen.fill(new RoundRectangle2D.Double(x, zero - 8, width, y - zero + 8, 16, 16));
            }
            pen.setFont(Theme.font(Font.BOLD, 13));
            text(pen, Theme.compact(values[i]), x + width / 2, (values[i] >= 0 ? y : zero) - 10, Theme.INK, true);
            pen.setFont(Theme.font(Font.PLAIN, 13));
            text(pen, names[i], x + width / 2, bottom + 22, Theme.MUTED, true);
        }
        if (records.isEmpty()) emptyNote(pen, (left + right) / 2, (top + bottom) / 2);
    }

    private void donut(Graphics2D pen) {
        String type = mode == Mode.REVENUE_CATEGORIES ? "Revenue" : "Expense";
        Map<String, BigDecimal> data = ChartData.categories(records, type);
        BigDecimal total = ChartData.total(records, type);
        List<Map.Entry<String, BigDecimal>> entries = new ArrayList<>(data.entrySet());
        entries.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        if (entries.size() > SLICES.length) {
            BigDecimal rest = BigDecimal.ZERO;
            for (int i = SLICES.length - 1; i < entries.size(); i++) rest = rest.add(entries.get(i).getValue());
            entries = new ArrayList<>(entries.subList(0, SLICES.length - 1));
            entries.add(Map.entry("Other", rest));
        }
        int diameter = Math.max(90, Math.min(getHeight() - 8, 150));
        int x = 4, y = (getHeight() - diameter) / 2;
        double hole = diameter * .62;
        if (entries.isEmpty()) {
            Area ring = new Area(new Ellipse2D.Double(x, y, diameter, diameter));
            ring.subtract(new Area(new Ellipse2D.Double(x + (diameter - hole) / 2, y + (diameter - hole) / 2, hole, hole)));
            pen.setColor(Theme.SOFT);
            pen.fill(ring);
        } else {
            double angle = 90;
            for (int i = 0; i < entries.size(); i++) {
                double share = entries.get(i).getValue().doubleValue() / total.doubleValue();
                double sweep = -360 * share;
                Area slice = new Area(new Arc2D.Double(x, y, diameter, diameter, angle, sweep, Arc2D.PIE));
                slice.subtract(new Area(new Ellipse2D.Double(x + (diameter - hole) / 2, y + (diameter - hole) / 2, hole, hole)));
                pen.setColor(SLICES[i]);
                pen.fill(slice);
                angle += sweep;
            }
        }
        double cx = x + diameter / 2.0, cy = y + diameter / 2.0;
        pen.setFont(Theme.font(Font.PLAIN, 12));
        text(pen, type.equals("Revenue") ? "Revenue" : "Expenses", cx, cy - 4, Theme.MUTED, true);
        pen.setFont(Theme.font(Font.BOLD, 17));
        text(pen, Theme.compact(total.doubleValue()), cx, cy + 17, Theme.INK, true);
        int legendLeft = x + diameter + 30, legendRight = getWidth() - 2;
        int rowHeight = 32, startY = Math.max(16, (getHeight() - rowHeight * Math.max(1, entries.size())) / 2 + 18);
        if (entries.isEmpty()) {
            pen.setFont(Theme.font(Font.PLAIN, 13));
            text(pen, "No records yet", legendLeft, getHeight() / 2.0 + 4, Theme.MUTED, false);
            return;
        }
        for (int i = 0; i < entries.size(); i++) {
            int rowY = startY + i * rowHeight;
            pen.setColor(SLICES[i]);
            pen.fill(new RoundRectangle2D.Double(legendLeft, rowY - 9, 13, 13, 5, 5));
            pen.setFont(Theme.font(Font.PLAIN, 14));
            String name = entries.get(i).getKey();
            while (name.length() > 3 && pen.getFontMetrics().stringWidth(name) > legendRight - legendLeft - 150) name = name.substring(0, name.length() - 2) + "…";
            text(pen, name, legendLeft + 24, rowY + 2, Theme.INK, false);
            double share = entries.get(i).getValue().doubleValue() / total.doubleValue() * 100;
            String percent = String.format(java.util.Locale.US, "%.0f%%", share);
            pen.setFont(Theme.font(Font.PLAIN, 13));
            int percentWidth = pen.getFontMetrics().stringWidth(percent);
            text(pen, percent, legendRight - percentWidth, rowY + 2, Theme.MUTED, false);
            pen.setFont(Theme.fontFor("₱", Font.BOLD, 14));
            String amount = Theme.whole(entries.get(i).getValue());
            text(pen, amount, legendRight - percentWidth - 8 - pen.getFontMetrics().stringWidth(amount), rowY + 2, Theme.INK, false);
            if (i < entries.size() - 1) {
                pen.setColor(Theme.LINE);
                pen.draw(new Line2D.Double(legendLeft, rowY + 17, legendRight, rowY + 17));
            }
        }
    }
}
