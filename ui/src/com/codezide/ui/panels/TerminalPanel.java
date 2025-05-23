package com.codezide.ui.panels;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import com.codezide.ui.components.AccentButton;
import com.codezide.ui.utils.PanelColors;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import AppLogicLayer.Input_Interface;
import AppLogicLayer.CompileAndRunCode.RunCode;

public class TerminalPanel extends JPanel {
    public JTextArea tArea;
    private Input_Interface inputter = new RunCode();

    public TerminalPanel() {
        setLayout(new BorderLayout());
        setBackground(PanelColors.PANEL_BG);
        setBorder(new LineBorder(PanelColors.ACCENT_COLOR, 1, false));

        tArea = new JTextArea();
        tArea.setEditable(false);
        tArea.setBackground(PanelColors.TEXTBOX_BG);
        tArea.setForeground(PanelColors.TEXT_COLOR);
        add(new JScrollPane(tArea), BorderLayout.CENTER);

        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.setBackground(PanelColors.PANEL_BG);
        inputPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JLabel inputLabel = new JLabel("Input: ");
        inputLabel.setForeground(PanelColors.TEXT_COLOR);
        inputPanel.add(inputLabel, BorderLayout.WEST);

        JTextField inputField = new JTextField();
        inputField.setBackground(PanelColors.TEXTBOX_BG);
        inputField.setForeground(PanelColors.TEXT_COLOR);
        inputField.setBorder(new LineBorder(PanelColors.ACCENT_COLOR, 1, false));
        inputPanel.add(inputField, BorderLayout.CENTER);

        AccentButton submitBtn = new AccentButton("Submit");
        inputPanel.add(submitBtn, BorderLayout.EAST);
        add(inputPanel, BorderLayout.SOUTH);

        // Input Button Action Listener
        submitBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // Only proceed if there's a running process and input text
                if (inputter.isProcessRunning() && !inputField.getText().isEmpty()) {
                    String input = inputField.getText(); 
                    if (inputter.sendInputToProcess(input)) {
                        tArea.append("Input: " + inputField.getText() + "\n");
                    }
                    inputField.setText(""); // Clear input area
                }
            }
        });
    }
}
