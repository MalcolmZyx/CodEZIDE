package com.codezide.ui.components;

import javax.swing.JButton;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.Graphics;
import com.codezide.ui.utils.PanelColors;

public class AccentButton extends JButton {
    public AccentButton(String text) {
        super(text);
        // solid black text on green background
        setForeground(Color.BLACK);
        setBackground(PanelColors.ACCENT_COLOR);
        setBorder(new EmptyBorder(5, 15, 5, 15));
        setFocusPainted(false);
        setContentAreaFilled(false);
        setOpaque(true);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Color bg = getModel().isRollover()
            ? PanelColors.ACCENT_COLOR.darker()
            : PanelColors.ACCENT_COLOR;
        g.setColor(bg);
        g.fillRect(0, 0, getWidth(), getHeight());
        super.paintComponent(g);
    }
}
