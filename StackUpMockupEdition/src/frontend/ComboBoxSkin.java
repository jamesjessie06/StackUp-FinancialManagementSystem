package frontend;

import java.awt.*;
import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicComboBoxUI;

/** Rounded white combo box without Metal's nested editor border. */
public final class ComboBoxSkin extends BasicComboBoxUI {
    public static ComponentUI createUI(JComponent component) { return new ComboBoxSkin(); }
    protected JButton createArrowButton() {
        JButton arrow=Theme.button("⌄",Color.WHITE,Theme.MUTED);
        arrow.setBorder(BorderFactory.createEmptyBorder(0,2,0,2));
        return arrow;
    }
    protected ListCellRenderer<Object> createRenderer() {
        return new DefaultListCellRenderer() {
            private static final long serialVersionUID=1L;
            public Component getListCellRendererComponent(JList<?> list,Object value,int index,boolean selected,boolean focus) {
                JLabel label=(JLabel)super.getListCellRendererComponent(list,value,index,selected,focus);
                label.setBorder(BorderFactory.createEmptyBorder(3,6,3,6));
                label.setBackground(selected?new Color(221,247,240):Color.WHITE);
                label.setForeground(Theme.INK);return label;
            }
        };
    }
}
