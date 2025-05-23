// SplitPaneCustomizer.java
package com.codezide.ui.utils;

import javax.swing.JComponent;
import javax.swing.JSplitPane;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import java.awt.Graphics;

public class SplitPaneCustomizer {
    public static JSplitPane horizontal(JComponent left, JComponent right) {
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        style(split);
        return split;
    }

    public static JSplitPane vertical(JComponent top, JComponent bottom) {
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, top, bottom);
        style(split);
        return split;
    }

    private static void style(JSplitPane split) {
        split.setResizeWeight(0.5);
        split.setDividerSize(5);
        split.setBorder(null);
        split.setUI(new BasicSplitPaneUI() {
            @Override
            public BasicSplitPaneDivider createDefaultDivider() {
                return new BasicSplitPaneDivider(this) {
                    @Override
                    public void paint(Graphics g) {
                        g.setColor(PanelColors.ACCENT_COLOR);
                        g.fillRect(0, 0, getWidth(), getHeight());
                    }
                };
            }
        });
    }
}
