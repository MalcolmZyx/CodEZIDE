package com.codezide.ui.components;

import javax.swing.JButton;
import javax.swing.BorderFactory;
import java.awt.Component;
import com.codezide.ui.utils.PanelColors;

public class TabButton extends JButton {
    public TabButton(String text, boolean selected) {
        super(text);
        setForeground(PanelColors.TEXT_COLOR);
        setContentAreaFilled(false);
        setOpaque(selected);
        setBackground(selected ? PanelColors.SELECTED_TAB_BG : PanelColors.PANEL_BG);
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 1, 0, 1, PanelColors.ACCENT_COLOR),
            BorderFactory.createEmptyBorder(5, 15, 5, 15)
        ));
    }
}
