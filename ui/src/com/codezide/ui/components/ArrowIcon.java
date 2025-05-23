package com.codezide.ui.components;

import javax.swing.Icon;
import java.awt.*;
import com.codezide.ui.utils.PanelColors;

public class ArrowIcon implements Icon {
    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(PanelColors.TEXT_COLOR);
        g2.fillPolygon(new int[]{x, x + 10, x + 5}, new int[]{y, y, y + 6}, 3);
        g2.dispose();
    }

    @Override public int getIconWidth() { return 10; }
    @Override public int getIconHeight() { return 6; }
}
