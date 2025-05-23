//Gemini 2.5 Pro version, Google, 13 Apr. 2025. https://gemini.google.com/app

/*
 * - Helped me rewrite the file system to use JFileChooser
 * - Helped me figure out what libraries I need
 */

package com.codezide.ui.menubar;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set; 
import java.util.logging.Level;
import java.util.logging.Logger;


import com.codezide.ui.utils.PanelColors;
import com.codezide.ui.panels.CenterPanel;
import com.codezide.ui.panels.RightPanel;
import AppLogicLayer.JavaParser; 

import AppLogicLayer.CompileAndRunCode.CompilerCheck;

public class FileMenu extends JMenu {
    private static final Logger LOGGER = Logger.getLogger(FileMenu.class.getName());

    private Component parentComponent;
    private CenterPanel centerPanel;
    private RightPanel rightPanel;

    public FileMenu(Component parent, CenterPanel _centerPanel, RightPanel _rightPanel) {
        super("File");
        this.centerPanel = _centerPanel;
        this.rightPanel = _rightPanel;
        this.parentComponent = parent;
        CompilerCheck.setRightPanel(this.rightPanel);

        setOpaque(true);
        setBackground(PanelColors.HEADER_BG);
        setForeground(PanelColors.TEXT_COLOR);
        setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));

        // Use the createMenuItem helper method
        add(createMenuItem("New", this::handleNewAction));
        add(createMenuItem("Open", this::handleOpenAction));
        add(createMenuItem("Save", this::handleSaveAction));
        add(createMenuItem("Save As...", this::handleSaveAsAction));
        addSeparator();
        add(createMenuItem("Close Tab", this::handleCloseTabAction));
        addSeparator();
        add(createMenuItem("Exit", this::handleExitAction));
    }

    private JMenuItem createMenuItem(String text, ActionListener listener) {
        JMenuItem item = new JMenuItem(text);
        item.setOpaque(true); 
        item.setBackground(PanelColors.PANEL_BG); 
        item.setForeground(PanelColors.TEXT_COLOR);
        
        if (listener != null) {
            item.addActionListener(listener);
        }
        return item;
    }


    private void handleNewAction(ActionEvent e) {
        centerPanel.createNewFileTab();
    }

    private void handleOpenAction(ActionEvent e) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Open File");
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Java Files", "java"));

        int result = chooser.showOpenDialog(parentComponent);

        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = chooser.getSelectedFile();
            LOGGER.info("File selected for 'Open': " + selectedFile.getAbsolutePath());
            centerPanel.openFileInNewTab(selectedFile);

            String path = selectedFile.getAbsolutePath();

            JTextField fileRunLocation = rightPanel.GetFileRunLocation();
            JTextField fileWorkLocation = rightPanel.GetWorkSpaceLocation();
            fileRunLocation.setText(path);
            fileWorkLocation.setText(path.substring(0, path.lastIndexOf('\\')));
        } else {
            LOGGER.info("'Open' command cancelled by user.");
        }
    }

    private void handleSaveAction(ActionEvent e) {
        CenterPanel.EditorState activeState = centerPanel.getActiveEditorState();
        if (activeState == null) {
            LOGGER.warning("Save: No active editor tab.");
            return;
        }

        // Use getter method
        String originalFilePath = activeState.getOriginalFilePath();

        if (originalFilePath == null) {
            handleSaveAsAction(e); // Delegate to Save As for new files
        } else {
            // get textEditor
            saveContentToFile(activeState.getTextEditor().getTextPane().getText(), new File(originalFilePath), activeState);
        }
    }

    private void handleSaveAsAction(ActionEvent e) {
        CenterPanel.EditorState activeState = centerPanel.getActiveEditorState();
        if (activeState == null) {
            LOGGER.warning("Save As: No active editor tab.");
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save File As...");
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

        String suggestedName = "Untitled.java";
        String originalPath = activeState.getOriginalFilePath();
        Path tempPath = activeState.getTempFilePath();

        if (originalPath != null) {
            suggestedName = new File(originalPath).getName();
        } else if (tempPath != null) {
            suggestedName = tempPath.getFileName().toString();
        }
        chooser.setSelectedFile(new File(suggestedName));

        int result = chooser.showSaveDialog(parentComponent);

        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFileToSave = chooser.getSelectedFile();

            String filePath = selectedFileToSave.getAbsolutePath();
            if (!filePath.toLowerCase().endsWith(".java")) {
                selectedFileToSave = new File(filePath + ".java");
            }

            if (selectedFileToSave.exists()) {
                int overwriteResult = JOptionPane.showConfirmDialog(
                        parentComponent,
                        "File \"" + selectedFileToSave.getName() + "\" already exists.\nDo you want to replace it?",
                        "Confirm Save As",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE
                );
                if (overwriteResult != JOptionPane.YES_OPTION) {
                    LOGGER.info("'Save As' cancelled due to existing file.");
                    return;
                }
            }
            // get textEditor
            saveContentToFile(activeState.getTextEditor().getTextPane().getText(), selectedFileToSave, activeState);
        } else {
            LOGGER.info("'Save As' command cancelled by user.");
        }
    }

    // Helper method now uses getter methods for state access
    private void saveContentToFile(String contentToSave, File targetFile, CenterPanel.EditorState state) {
        LOGGER.info("Attempting to save to: " + targetFile.getAbsolutePath());
        Path tempFilePath = state.getTempFilePath();
        String editorId = state.getEditorId();      

        try {
            Files.writeString(targetFile.toPath(), contentToSave, StandardCharsets.UTF_8);
            LOGGER.info("Successfully saved content to target file.");

            if (tempFilePath != null) {
                try {
                    Files.writeString(tempFilePath, contentToSave, StandardCharsets.UTF_8);
                    LOGGER.info("Updated temporary file content: " + tempFilePath);
                } catch (IOException ioex) {
                    LOGGER.log(Level.WARNING, "Could not update temporary file after save: " + ioex.getMessage(), ioex);
                }
            }

            centerPanel.updateStateAfterSave(editorId, targetFile.getAbsolutePath());

             if (tempFilePath != null) {
                 JavaParser.getInstance().saveFile(tempFilePath.toString());
                 LOGGER.info("Sent didSave notification for temp URI: " + tempFilePath);
             }

            JOptionPane.showMessageDialog(parentComponent, "File Saved: \n" + targetFile.getAbsolutePath());

        } catch (IOException ioException) {
            LOGGER.log(Level.SEVERE, "Error saving file: " + targetFile.getAbsolutePath(), ioException);
            JOptionPane.showMessageDialog(parentComponent,
                    "Error Saving File:\n" + ioException.getMessage(),
                    "File Write Error",
                    JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
             LOGGER.log(Level.SEVERE, "Unexpected error during save to: " + targetFile.getAbsolutePath(), ex);
             JOptionPane.showMessageDialog(parentComponent,
                    "An unexpected error occurred during save.",
                    "Save Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }


    private void handleCloseTabAction(ActionEvent e) {
          CenterPanel.EditorState activeState = centerPanel.getActiveEditorState();
          if (activeState != null) {
              centerPanel.closeTab(activeState.getEditorId());
          } else {
             LOGGER.warning("Close Tab: No active editor to close.");
          }
     }

     

    private void handleExitAction(ActionEvent e) {
        LOGGER.info("Exit action triggered.");
        // TODO: Implement check for unsaved changes across all tabs

        int result = JOptionPane.showConfirmDialog(
                parentComponent,
                "Are you sure you want to exit?",
                "Confirm Exit",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (result == JOptionPane.YES_OPTION) {
            LOGGER.info("Proceeding with application exit.");
            try {
                LOGGER.fine("Closing all editor tabs...");
                Set<String> editorIds = centerPanel.getActiveEditorStateIds();
                LOGGER.fine("Found " + editorIds.size() + " tabs to close.");
                for (String editorId : editorIds) {
                    centerPanel.closeTab(editorId); 
                }
                LOGGER.fine("Finished closing tabs.");
            } catch (Exception ex) {
                 LOGGER.log(Level.SEVERE, "Error occurred during bulk tab closing on exit", ex);
            }

            try {
                LOGGER.fine("Shutting down JavaParser...");
                JavaParser.getInstance().shutdown();
                LOGGER.fine("JavaParser shutdown initiated.");
            } catch (Exception ex) {
                 LOGGER.log(Level.SEVERE, "Error occurred during JavaParser shutdown on exit", ex);
            }

            LOGGER.info("Exiting application (System.exit(0)).");
            System.exit(0);
        } else {
             LOGGER.info("Application exit cancelled by user.");
        }
    }
}