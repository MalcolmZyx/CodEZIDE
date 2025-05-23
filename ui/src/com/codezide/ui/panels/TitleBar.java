package com.codezide.ui.panels;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import com.codezide.ui.utils.PanelColors;
import com.codezide.ui.components.ControlButton;
import com.codezide.ui.menubar.MenuBarFactory;

public class TitleBar extends JPanel {
    public TitleBar(CenterPanel centerPanel, RightPanel rightPanel) {
        setLayout(new BorderLayout());
        setBackground(PanelColors.HEADER_BG);
        setBorder(BorderFactory.createMatteBorder(2,2,0,2,PanelColors.ACCENT_COLOR));

        JMenuBar menu = MenuBarFactory.create(this, centerPanel, rightPanel);
        add(menu, BorderLayout.WEST);

        JLabel title = new JLabel("CodEZ", SwingConstants.CENTER);
        title.setForeground(PanelColors.TEXT_COLOR);
        add(title, BorderLayout.CENTER);

        JPanel ctrls = new JPanel(new FlowLayout(FlowLayout.RIGHT,5,0));

        Window window = SwingUtilities.getWindowAncestor(this);
        Frame frame = (Frame) window;

        ctrls.setOpaque(false);
        ControlButton min = new ControlButton("—");
        min.addActionListener(e -> {
            Frame currentFrame = (Frame) SwingUtilities.getAncestorOfClass(Frame.class, this);
            if (currentFrame != null) {
                currentFrame.setState(Frame.ICONIFIED);
            } else {
                 System.err.println("Minimize Error: Could not find parent Frame.");
            }
        });

        ControlButton max = new ControlButton("□");
        max.addActionListener(e -> {
            // Get the Frame ancestor *when the button is clicked*
           Frame currentFrame = (Frame) SwingUtilities.getAncestorOfClass(Frame.class, this);
            if (currentFrame != null) {
                if (currentFrame.getExtendedState() == Frame.MAXIMIZED_BOTH) {
                    currentFrame.setExtendedState(Frame.NORMAL);
                    max.setText("□");
                } else {
                    currentFrame.setExtendedState(Frame.MAXIMIZED_BOTH);
                    max.setText("❐"); 
                }
           } else {
                System.err.println("Maximize Error: Could not find parent Frame.");
           }
       });

        ControlButton close = new ControlButton("×");
        close.addActionListener(e -> System.exit(0));

        close.setBackground(new Color(200,50,50));
        ctrls.add(min); ctrls.add(max); ctrls.add(close);
        add(ctrls, BorderLayout.EAST);
    }
}
