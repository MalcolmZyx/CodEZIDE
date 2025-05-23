// WindowDragger.java
package com.codezide.ui.utils;
import javax.swing.JComponent;
import javax.swing.JFrame;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class WindowDragger {
    public static void makeDraggable(JComponent comp, JFrame frame) {
        final Point[] offset = { new Point() };
        comp.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                offset[0] = e.getPoint();
            }
        });
        comp.addMouseMotionListener(new MouseAdapter() {
            public void mouseDragged(MouseEvent e) {
                Point p = e.getLocationOnScreen();
                frame.setLocation(p.x - offset[0].x, p.y - offset[0].y);
            }
        });
    }
}
