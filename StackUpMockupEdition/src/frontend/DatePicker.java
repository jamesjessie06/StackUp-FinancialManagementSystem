package frontend;

import java.awt.*;
import java.time.*;
import javax.swing.*;

/** Read-only input backed by a Swing calendar. */
public final class DatePicker extends JTextField {
    private static final long serialVersionUID = 1L;
    private LocalDate selected;
    private YearMonth month = YearMonth.now();
    public DatePicker() {
        setEditable(false);
        setToolTipText("Choose a date");
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
        setText(date == null ? "" : date.toString());
        if (date != null) month = YearMonth.from(date);
    }
    private void open() {
        JPopupMenu popup = new JPopupMenu();
        JPanel calendar = Theme.card();
        popup.add(calendar);
        rebuild(calendar, popup);
        popup.show(this, 0, getHeight());
    }
    private void rebuild(JPanel calendar, JPopupMenu popup) {
        calendar.removeAll();
        calendar.setLayout(new BorderLayout(0, 8));
        JPanel header = Theme.clear(new BorderLayout());
        JButton previous = Theme.button("‹"), next = Theme.button("›");
        header.add(previous, BorderLayout.WEST);
        header.add(new JLabel(month.toString(), SwingConstants.CENTER));
        header.add(next, BorderLayout.EAST);
        previous.addActionListener(event -> { month=month.minusMonths(1); rebuild(calendar,popup); });
        next.addActionListener(event -> { month=month.plusMonths(1); rebuild(calendar,popup); });
        calendar.add(header, BorderLayout.NORTH);
        JPanel days = Theme.clear(new GridLayout(7,7,3,3));
        for (String name : new String[]{"Mo","Tu","We","Th","Fr","Sa","Su"})
            days.add(new JLabel(name, SwingConstants.CENTER));
        int offset=month.atDay(1).getDayOfWeek().getValue()-1;
        for (int cell=0; cell<42; cell++) {
            int number=cell-offset+1;
            if (number<1 || number>month.lengthOfMonth()) { days.add(new JLabel()); continue; }
            LocalDate date=month.atDay(number);
            JButton day=Theme.button(Integer.toString(number),
                date.equals(selected) ? Theme.TEAL : Color.WHITE,
                date.equals(selected) ? Color.WHITE : Theme.INK);
            day.setBorder(BorderFactory.createEmptyBorder(5,4,5,4));
            day.setPreferredSize(new Dimension(34,29));
            day.addActionListener(event -> { setSelectedDate(date); popup.setVisible(false); });
            days.add(day);
        }
        calendar.add(days);
        calendar.revalidate(); calendar.repaint(); popup.pack();
    }
}
