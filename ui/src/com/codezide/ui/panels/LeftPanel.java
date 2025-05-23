package com.codezide.ui.panels;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import com.codezide.ui.utils.PanelColors;

public class LeftPanel extends JPanel {
    public LeftPanel() {
        setLayout(new BorderLayout());
        setMinimumSize(new Dimension(150, 0));
        setPreferredSize(new Dimension(200, 0));
        setBackground(PanelColors.PANEL_BG);
        setBorder(new LineBorder(PanelColors.ACCENT_COLOR, 1, false));

        JLabel header = new JLabel("Project Files", SwingConstants.CENTER);
        header.setFont(new Font("Arial", Font.BOLD, 14));
        header.setForeground(Color.WHITE);
        header.setBackground(PanelColors.HEADER_BG);
        header.setOpaque(true);
        header.setBorder(new EmptyBorder(5, 5, 5, 5));
        add(header, BorderLayout.NORTH);

        JList<String> fileList = new JList<>(new String[]{"Java File 1", "Java File 2", "Java File 3"});
        fileList.setBackground(PanelColors.PANEL_BG);
        fileList.setForeground(PanelColors.TEXT_COLOR);
        fileList.setSelectionBackground(PanelColors.LIST_SELECTION);
        fileList.setSelectionForeground(Color.WHITE);
        add(new JScrollPane(fileList), BorderLayout.CENTER);
    }
}
