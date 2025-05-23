// MenuBarFactory.java
package com.codezide.ui.menubar;

import javax.swing.*;
import javax.swing.plaf.basic.BasicMenuBarUI;
import java.awt.*;
import com.codezide.ui.utils.PanelColors;
import com.codezide.ui.panels.CenterPanel;
import com.codezide.ui.panels.RightPanel;

public class MenuBarFactory {

    public static JMenuBar create(Component parent, CenterPanel centerPanel, RightPanel rightPanel) {
        JMenuBar bar = new JMenuBar() {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(PanelColors.HEADER_BG); 
                g.fillRect(0, 0, getWidth(), getHeight());
            }

            @Override
            public void updateUI() {
                 setUI(new BasicMenuBarUI() {
                     @Override
                     public void paint(Graphics g, JComponent c) {
                         g.setColor(PanelColors.HEADER_BG); 
                         g.fillRect(0, 0, c.getWidth(), c.getHeight());
                     }
                 });
            }
        };
        bar.setOpaque(true); 
        bar.setBackground(PanelColors.HEADER_BG); 
        bar.setForeground(PanelColors.TEXT_COLOR);
        bar.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

        bar.add(new FileMenu(parent, centerPanel, rightPanel));

        bar.add(new EditMenu()); 

        return bar;
    }
}
