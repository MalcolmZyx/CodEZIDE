package com.codezide.ui.components;

import javax.swing.JButton;
import javax.swing.border.EmptyBorder;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import com.codezide.ui.utils.PanelColors;

public class ControlButton extends JButton {
    public ControlButton(String text) {
        super(text);
        setForeground(PanelColors.TEXT_COLOR);
        setBackground(PanelColors.HEADER_BG);
        setBorder(new EmptyBorder(0, 5, 0, 5));
        setFocusPainted(false);
        setContentAreaFilled(false);
        setOpaque(true);
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                setBackground(PanelColors.BUTTON_HOVER);
            }
            @Override
            public void mouseExited(MouseEvent e) {
                setBackground(PanelColors.HEADER_BG);
            }
        });
    }
}
