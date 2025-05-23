//ChatGPT-4 version, OpenAI, 20 Apr. 2025. chat.openai.com/chat 

/*
 * - Helped me learn inheritance in java
 * - Helped me convert the designs into code
 * - Helped me figure out what libraries I need
 */

package com.codezide.ui;

import javax.swing.*;
import java.awt.*;
import com.codezide.ui.utils.PanelColors;
import com.codezide.ui.utils.SplitPaneCustomizer;
import com.codezide.ui.utils.WindowDragger;
import com.codezide.ui.panels.*;

public class MainFrame extends JFrame {
    public TerminalPanel tPanel = new TerminalPanel(); 

    public MainFrame() {
        setTitle("CodEZ");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(700, 500);
        setLocationRelativeTo(null);
        setUndecorated(true);

        // making the primary panels to use later
        //LeftPanel left = new LeftPanel();
        CenterPanel center = new CenterPanel();
        RightPanel right = new RightPanel(tPanel);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(PanelColors.PANEL_BG);
        // top title bar
        TitleBar titleBar = new TitleBar(center, right);
        root.add(titleBar, BorderLayout.NORTH);
        WindowDragger.makeDraggable(titleBar, this);

        // center splits

        JSplitPane horizontal = SplitPaneCustomizer.horizontal(
            center, right
        );
        JSplitPane vertical = SplitPaneCustomizer.vertical(
            horizontal, tPanel
        );
        root.add(vertical, BorderLayout.CENTER);

        setContentPane(root);
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            UIManager.put("SplitPane.background", PanelColors.PANEL_BG);
            UIManager.put("SplitPane.dividerSize", 5);
            UIManager.put("MenuBar.background", PanelColors.HEADER_BG);
            UIManager.put("Menu.background", PanelColors.HEADER_BG);
            UIManager.put("Menu.foreground", PanelColors.TEXT_COLOR);
            UIManager.put("MenuItem.background", PanelColors.PANEL_BG);
            UIManager.put("MenuItem.foreground", PanelColors.TEXT_COLOR);
            UIManager.put("MenuItem.selectionBackground", PanelColors.BUTTON_HOVER);
            UIManager.put("ScrollBar.thumb", new Color(100,100,100));
        } catch (Exception e) {
            e.printStackTrace();
        }
        SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
    }
}
