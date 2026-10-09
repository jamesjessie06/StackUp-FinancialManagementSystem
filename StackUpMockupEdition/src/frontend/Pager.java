package frontend;

import frontend.style.*;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

/** "Showing 1-7 of 128" with numbered page buttons. */
public final class Pager extends Theme.SmoothPanel {
    private static final long serialVersionUID = 1L;
    private final int size;
    private final Runnable onChange;
    private int page, total;

    public Pager(int size, Runnable onChange) {
        super(new BorderLayout());
        this.size = size;
        this.onChange = onChange;
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
    }

    public int from() { return page * size; }
    public int to() { return Math.min(total, from() + size); }
    public void reset() { page = 0; }

    public void update(int total) {
        this.total = total;
        int pages = Math.max(1, (total + size - 1) / size);
        page = Math.min(page, pages - 1);
        removeAll();
        setVisible(pages > 1);
        JPanel buttons = Theme.clear(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        buttons.add(square("‹", page > 0, false, page - 1));
        for (int number : numbers(pages)) {
            if (number < 0) buttons.add(square("…", false, false, 0));
            else buttons.add(square(Integer.toString(number + 1), true, number == page, number));
        }
        buttons.add(square("›", page < pages - 1, false, page + 1));
        add(buttons, BorderLayout.EAST);
        revalidate();
        repaint();
    }

    /** Page indexes to show; -1 stands for an ellipsis. */
    private List<Integer> numbers(int pages) {
        List<Integer> list = new ArrayList<>();
        if (pages <= 6) {
            for (int i = 0; i < pages; i++) list.add(i);
            return list;
        }
        list.add(0);
        if (page > 2) list.add(-1);
        for (int i = Math.max(1, page - 1); i <= Math.min(pages - 2, page + 1); i++) list.add(i);
        if (page < pages - 3) list.add(-1);
        list.add(pages - 1);
        return list;
    }

    private JButton square(String text, boolean enabled, boolean active, int target) {
        JButton button = active ? Theme.dark(text) : Theme.outline(text);
        button.setBorder(BorderFactory.createEmptyBorder());
        button.setPreferredSize(new Dimension(32, 32));
        button.setEnabled(enabled);
        button.addActionListener(event -> { page = target; onChange.run(); });
        return button;
    }
}
