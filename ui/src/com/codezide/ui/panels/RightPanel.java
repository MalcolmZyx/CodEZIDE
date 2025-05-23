package com.codezide.ui.panels;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.awt.*;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.codezide.ui.components.AccentButton;
import com.codezide.ui.utils.PanelColors;

import AppLogicLayer.CompileAndRunCode.CompilerCheck;
import AppLogicLayer.CompileAndRunCode.CompilerCheck.language;
import AppLogicLayer.CompileAndRunCode.CaRCFacade;
import AppLogicLayer.CompileAndRunCode.RunCode_Interface;

import AppLogicLayer.JavaParser;

public class RightPanel extends JPanel {
    private JTextArea terminalArea; 
    private JTextField inputArea;
    private JButton inputButton;
    private String pathToCode;
    private String codeFileName;
    private language chosenLang;

    private String pathToWork;
    private String workFileName;

    private JTextField fileLoc;
    private JTextField workLoc;
    private JTextField compLoc;

    private static final int PANEL_WIDTH = 200;
    private static final int PADDING = PANEL_WIDTH / 10; // 10% padding

    public RightPanel(TerminalPanel theTPanel) {
        terminalArea = theTPanel.tArea;

        setLayout(new BorderLayout());
        setBackground(PanelColors.PANEL_BG);
        setBorder(new LineBorder(PanelColors.ACCENT_COLOR, 1, false));

        // Header
        JLabel header = new JLabel("Compiler Settings", SwingConstants.CENTER);
        header.setFont(new Font("Arial", Font.BOLD, 14));
        header.setForeground(Color.WHITE);
        header.setBackground(PanelColors.HEADER_BG);
        header.setOpaque(true);
        header.setBorder(new EmptyBorder(5, 5, 5, 5));
        add(header, BorderLayout.NORTH);

        // Container for buttons and fields
        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.setBackground(PanelColors.PANEL_BG);
        container.setBorder(new EmptyBorder(PADDING, PADDING, PADDING, PADDING));

        // Run button
        AccentButton runBtn = new AccentButton("Run ▶");
        runBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        runBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, runBtn.getPreferredSize().height));
        container.add(runBtn);
        container.add(Box.createVerticalStrut(PADDING));

        // adding action listener to the button
        runBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // Implement your run logic
                System.out.println("Run clicked!");
                
                doJob();
            }
        });

        // File Location field
        fileLoc = new JTextField("File Location");
        fileLoc.setHorizontalAlignment(JTextField.CENTER);
        fileLoc.setMaximumSize(new Dimension(Integer.MAX_VALUE, fileLoc.getPreferredSize().height));
        fileLoc.setBackground(PanelColors.TEXTBOX_BG);
        fileLoc.setForeground(PanelColors.TEXT_COLOR);
        fileLoc.setBorder(new LineBorder(PanelColors.ACCENT_COLOR, 1, false));
        container.add(fileLoc);
        container.add(Box.createVerticalStrut(PADDING));

        fileLoc.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updatePathToCode();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updatePathToCode();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                // Plain text components don't fire this event, so you can ignore it
            }

            private void updatePathToCode() {
                String pathEntered = fileLoc.getText();
                Path path = Paths.get(pathEntered);
                codeFileName = path.getFileName().toString();

                // Extract the directory path
                Path parentPath = path.getParent();

                if (parentPath == null) {
                    System.err.println("Error: Could not determine parent directory of the file.");
                } else {
                    pathToCode = parentPath.toString();
                    System.out.println("Working directory: " + pathToCode);
                    System.out.println("File to be run: " + codeFileName);
                }
            }
        });

        // Workspace Location field
        workLoc = new JTextField("Workspace Location");
        workLoc.setHorizontalAlignment(JTextField.CENTER);
        workLoc.setMaximumSize(new Dimension(Integer.MAX_VALUE, workLoc.getPreferredSize().height));
        workLoc.setBackground(PanelColors.TEXTBOX_BG);
        workLoc.setForeground(PanelColors.TEXT_COLOR);
        workLoc.setBorder(new LineBorder(PanelColors.ACCENT_COLOR, 1, false));
        container.add(workLoc);
        container.add(Box.createVerticalStrut(PADDING));

        
        workLoc.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updatePathToProjectRoot();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updatePathToProjectRoot();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                // Plain text components don't fire this event, so you can ignore it
            }

            private void updatePathToProjectRoot() {
                String pathEntered = workLoc.getText();
                Path path = Paths.get(pathEntered);
                workFileName = path.getFileName().toString();

                // Extract the directory path
                Path parentPath = path.getParent();

                if (parentPath == null) {
                    System.err.println("Error: Could not determine parent directory of the file.");
                } else {
                    pathToWork = parentPath.toString();
                    System.out.println("Working directory: " + pathToWork);
                    try{
                        JavaParser.getInstance().updateProjectRoot(pathToWork);
                    }
                    catch(IllegalStateException e){
                        System.err.println("[RightPanel.java] Error initializing Java Language Server: " + e.getMessage());
                    }
                    
                }
            }

        });

        // Compiler Location field
        compLoc = new JTextField("Compiler Location");
        compLoc.setHorizontalAlignment(JTextField.CENTER);
        compLoc.setMaximumSize(new Dimension(Integer.MAX_VALUE, compLoc.getPreferredSize().height));
        compLoc.setBackground(PanelColors.TEXTBOX_BG);
        compLoc.setForeground(PanelColors.TEXT_COLOR);
        compLoc.setBorder(new LineBorder(PanelColors.ACCENT_COLOR, 1, false));
        container.add(compLoc);
        container.add(Box.createVerticalStrut(PADDING));

        // Add Compiler button
        AccentButton addCompiler = new AccentButton("Add Compiler");
        addCompiler.setAlignmentX(Component.CENTER_ALIGNMENT);
        addCompiler.setMaximumSize(new Dimension(Integer.MAX_VALUE, addCompiler.getPreferredSize().height));
        container.add(addCompiler);

        add(container, BorderLayout.CENTER);
    }

    public JTextField GetFileRunLocation(){
        return fileLoc;
    }

    public JTextField GetCompilerLocation(){
        return compLoc;
    }

    public JTextField GetWorkSpaceLocation(){
        return workLoc;
    }

    private boolean isValidFile(String path, String fileName){
        // if empty invalid file
        if(path == "" || fileName == ""){
            terminalArea.append("Error: No path and/or file name were provided.\n");
            return false;
        }        

        File chosenFile = new File(path);
        if (!chosenFile.exists()) { // checking if the file exists at the given path location
            if (terminalArea != null) {
                terminalArea.append("Error: The file " + fileName + " does not exist at path: " + pathToCode + "\n");
            }
            return false;
        } else {
            if (terminalArea != null) {
                terminalArea.append("File found: " + chosenFile.getAbsolutePath() + "\n");
            }
            return true;
        }
    }

    private void doJob(){

        if(!isValidFile(pathToCode, codeFileName)){
            terminalArea.append("Error: invalid file.\n");
            return;
        }

        if(codeFileName.endsWith(".java")){
            chosenLang = language.Java;
        }
        else if (codeFileName.endsWith(".cpp")){
            chosenLang = language.Cpp;
        }else {
            chosenLang = language.Other;
        }

        if (CompilerCheck.isCompilerInPath(chosenLang, terminalArea)){ // checking if a compiler is installed
            String compilerPath = CompilerCheck.findbinPath(chosenLang, terminalArea);
            
            CaRCFacade cAr = new CaRCFacade();
            int exitCode = cAr.compileCode(terminalArea, pathToCode, compilerPath, codeFileName, chosenLang);
            if (exitCode != 0){
                terminalArea.append("Error: compilation error\n");
                return;
            }
            cAr.runCode(terminalArea, pathToCode, compilerPath, codeFileName, chosenLang);
        }
        else 
        {
            terminalArea.append("Error: no compiler found.\n");
            System.out.println("Error: no compiler found.");
        }
    }
}
