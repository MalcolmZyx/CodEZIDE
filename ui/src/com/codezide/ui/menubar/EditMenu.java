package com.codezide.ui.menubar;

import javax.swing.*;
import java.awt.*;
import com.codezide.ui.utils.PanelColors;

public class EditMenu extends JMenu {
    public EditMenu() {
        super("Edit");
        setOpaque(true);
        setBackground(PanelColors.HEADER_BG);
        setForeground(PanelColors.TEXT_COLOR);
        setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));

        add(createMenuItem("Undo"));
        add(createMenuItem("Redo"));
    }

    private JMenuItem createMenuItem(String text) {
        JMenuItem item = new JMenuItem(text);
        item.setOpaque(true);
        item.setBackground(PanelColors.PANEL_BG);
        item.setForeground(PanelColors.TEXT_COLOR);
        return item;
    }
}
