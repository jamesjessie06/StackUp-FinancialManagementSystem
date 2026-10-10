package frontend;

import frontend.style.*;
import java.awt.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import javax.swing.*;

public final class DatePicker extends Theme.RoundedField {
    private static final long serialVersionUID = 1L;
    private LocalDate selected;
    private YearMonth month = YearMonth.now();

    public DatePicker() {
        setEditable(false);
        setPlaceholder("Select date");
        setToolTipText("Choose a date");
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent event) { open(); }
        });
        getInputMap().put(KeyStroke.getKeyStroke("SPACE"), "choose");
        getActionMap().put("choose", new AbstractAction() {
            private static final long serialVersionUID = 1L;
            public void actionPerformed(java.awt.event.ActionEvent event) { open(); }
        });
    }

    public LocalDate getSelectedDate() {
        if (selected == null) throw new IllegalArgumentException("Choose a date.");
        return selected;
    }

    public void setSelectedDate(LocalDate date) {
        selected = date;
        setText(date == null ? "" : date.format(PageSupport.DAY));
        if (date != null) month = YearMonth.from(date);
    }

    private void open() {
        JPopupMenu popup = new JPopupMenu();
        popup.setBorder(BorderFactory.createLineBorder(Theme.LINE));
        JPanel calendar = new JPanel();
        calendar.setBackground(Color.WHITE);
        calendar.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        popup.add(calendar);
        rebuild(calendar, popup);
        popup.show(this, 0, getHeight() + 4);
    }

    private void rebuild(JPanel calendar, JPopupMenu popup) {
        calendar.removeAll();
        calendar.setLayout(new BorderLayout(0, 10));
        JPanel header = Theme.clear(new BorderLayout());
        JButton previous = Theme.outline("‹"), next = Theme.outline("›");
        for (JButton arrow : new JButton[]{previous, next}) {
            arrow.setBorder(BorderFactory.createEmptyBorder());
            arrow.setPreferredSize(new Dimension(32, 32));
        }
        header.add(previous, BorderLayout.WEST);
        JLabel title = Theme.label(month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)), 14, Theme.INK);
        title.setHorizontalAlignment(SwingConstants.CENTER);
        header.add(title);
        header.add(next, BorderLayout.EAST);
        previous.addActionListener(event -> { month = month.minusMonths(1); rebuild(calendar, popup); });
        next.addActionListener(event -> { month = month.plusMonths(1); rebuild(calendar, popup); });
        calendar.add(header, BorderLayout.NORTH);
        JPanel days = Theme.clear(new GridLayout(7, 7, 2, 2));
        for (String name : new String[]{"Mo", "Tu", "We", "Th", "Fr", "Sa", "Su"}) {
            JLabel label = Theme.muted(name, 12);
            label.setHorizontalAlignment(SwingConstants.CENTER);
            days.add(label);
        }
        int offset = month.atDay(1).getDayOfWeek().getValue() - 1;
        for (int cell = 0; cell < 42; cell++) {
            int number = cell - offset + 1;
            if (number < 1 || number > month.lengthOfMonth()) { days.add(new JLabel()); continue; }
            LocalDate date = month.atDay(number);
            boolean chosen = date.equals(selected);
            JButton day = chosen ? Theme.solid(Integer.toString(number), Theme.BRAND, Color.WHITE)
                : Theme.link(Integer.toString(number));
            if (!chosen) day.setForeground(date.equals(LocalDate.now()) ? Theme.BRAND : Theme.INK);
            day.setBorder(BorderFactory.createEmptyBorder());
            day.setPreferredSize(new Dimension(36, 32));
            day.addActionListener(event -> { setSelectedDate(date); popup.setVisible(false); });
            days.add(day);
        }
        calendar.add(days);
        calendar.revalidate();
        calendar.repaint();
        popup.pack();
    }
}
