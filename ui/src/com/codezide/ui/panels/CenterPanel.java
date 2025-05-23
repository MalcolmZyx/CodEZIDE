package com.codezide.ui.panels;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Set;

import com.codezide.ui.components.TabButton;
import com.codezide.ui.utils.PanelColors;
import TextManagement.TextEditor;
import AppLogicLayer.JavaParser;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import java.util.logging.Level;
import java.util.logging.Logger;

public class CenterPanel extends JPanel {
    private static final Logger LOGGER = Logger.getLogger(CenterPanel.class.getName());

    private CardLayout cardLayout;
    private JPanel cardContainer;
    private JPanel tabPanel;
    // Use ConcurrentHashMap for thread safety
    private final Map<String, EditorState> editorStates = new ConcurrentHashMap<>();
    private volatile String activeEditorId = "Welcome";

    public static class EditorState {
        final TextEditor textEditor;
        final String originalFilePath;
        final Path tempFilePath;
        final String editorId;
        final TabButton tabButton;
        final AtomicInteger documentVersion = new AtomicInteger(0);

        EditorState(TextEditor editor, String originalPath, Path tempPath, String id, TabButton button) {
            this.textEditor = editor;
            this.originalFilePath = originalPath;
            this.tempFilePath = tempPath;
            this.editorId = id;
            this.tabButton = button;

            // Associate info with the text pane
            editor.getTextPane().putClientProperty("tempFilePath", tempPath);
            editor.getTextPane().putClientProperty("originalFilePath", originalPath);
            editor.getTextPane().putClientProperty("editorId", id);
            editor.getTextPane().putClientProperty("documentVersion", this.documentVersion);
        }

        public TextEditor getTextEditor() {
            return textEditor;
        }

        public String getOriginalFilePath() {
            return originalFilePath;
        }

        public Path getTempFilePath() {
            return tempFilePath;
        }

        public String getEditorId() {
            return editorId;
        }

        public TabButton getTabButton() { 
            return tabButton;
        }


        public int incrementAndGetVersion() {
            return this.documentVersion.incrementAndGet();
        }

        public int getVersion() {
            return this.documentVersion.get();
        }
    }

    public CenterPanel() {
        setLayout(new BorderLayout());
        setBackground(PanelColors.PANEL_BG);
        setBorder(new LineBorder(PanelColors.ACCENT_COLOR, 1, false));

        cardLayout = new CardLayout();
        cardContainer = new JPanel(cardLayout);
        cardContainer.setBackground(PanelColors.PANEL_BG);

        tabPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        tabPanel.setBackground(PanelColors.PANEL_BG);

        // Initial "Welcome" pane setup...
        JTextPane welcomePane = new JTextPane();
        welcomePane.setEditable(false);
        welcomePane.setFocusable(false);
        welcomePane.setEnabled(false);
        welcomePane.setBackground(PanelColors.CODE_BG);
        welcomePane.setForeground(PanelColors.REDUCED_TEXT);
        welcomePane.setText("Please Open or Create a file to begin.");
        cardContainer.add(new JScrollPane(welcomePane), "Welcome");
        activeEditorId = "Welcome";

        add(tabPanel, BorderLayout.NORTH);
        add(cardContainer, BorderLayout.CENTER);
        LOGGER.info("CenterPanel initialized.");
    }

    // Method called by FileMenu when opening a file
    public void openFileInNewTab(File file) {
        Path tempFilePath = null;
        Path tempDir = null;
        try {
            String originalPath = file.getAbsolutePath();
            LOGGER.info("Opening file: " + originalPath);
            String content = Files.readString(file.toPath());

            tempDir = Files.createTempDirectory("codezide_unsaved_" + file.getName() + "_");
            tempFilePath = tempDir.resolve(file.getName());
            Files.writeString(tempFilePath, content);
            LOGGER.info("Created temporary file: " + tempFilePath);

            TextEditor newEditor = createNewTextEditor();
            newEditor.getTextPane().setText(content);
            newEditor.getTextPane().setCaretPosition(0);

            String editorId = "editor_" + UUID.randomUUID().toString();
            TabButton tabButton = createTabButton(file.getName(), editorId);

            EditorState state = new EditorState(newEditor, originalPath, tempFilePath, editorId, tabButton);
            state.documentVersion.set(1); // Initial version
            editorStates.put(editorId, state);
            LOGGER.finer("Stored editor state for ID: " + editorId + ", Version: 1");

            JScrollPane scrollPane = new JScrollPane(newEditor.getTextPane());
            cardContainer.add(scrollPane, editorId);
            tabPanel.add(tabButton);

            switchTab(editorId); 

            JavaParser.getInstance().openFile(tempFilePath.toString());

            revalidate();
            repaint();

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error opening file in new tab: " + file.getAbsolutePath(), e);
            JOptionPane.showMessageDialog(this, "Could not open file: " + e.getMessage(), "File Open Error", JOptionPane.ERROR_MESSAGE);
             // Cleanup
             if (tempFilePath != null) { try { Files.deleteIfExists(tempFilePath); } catch (IOException ex) { LOGGER.log(Level.WARNING, "Failed cleanup: " + tempFilePath, ex); } }
             if (tempDir != null) { try { Files.deleteIfExists(tempDir); } catch (IOException ex) { LOGGER.log(Level.WARNING, "Failed cleanup: " + tempDir, ex); } }
        } catch (Exception e) {
             LOGGER.log(Level.SEVERE, "Unexpected error opening file: " + file.getAbsolutePath(), e);
             JOptionPane.showMessageDialog(this, "An unexpected error occurred opening the file.", "Error", JOptionPane.ERROR_MESSAGE);
              // Cleanup
             if (tempFilePath != null) { try { Files.deleteIfExists(tempFilePath); } catch (IOException ex) { LOGGER.log(Level.WARNING, "Failed cleanup: " + tempFilePath, ex); } }
             if (tempDir != null) { try { Files.deleteIfExists(tempDir); } catch (IOException ex) { LOGGER.log(Level.WARNING, "Failed cleanup: " + tempDir, ex); } }
        }
    }

    // Method called by FileMenu for a new file
    public void createNewFileTab() {
        Path tempFilePath = null;
        Path tempDir = null;
        try {
            LOGGER.info("Creating new file tab.");
            tempDir = Files.createTempDirectory("codezide_new_");
            tempFilePath = tempDir.resolve("Untitled.java");
            Files.writeString(tempFilePath, "");
            LOGGER.info("Created temporary file for new tab: " + tempFilePath);

            TextEditor newEditor = createNewTextEditor();
            newEditor.getTextPane().setText("");

            String editorId = "editor_" + UUID.randomUUID().toString();
            TabButton tabButton = createTabButton("Untitled*", editorId);

            EditorState state = new EditorState(newEditor, null, tempFilePath, editorId, tabButton);
            state.documentVersion.set(1);
            editorStates.put(editorId, state);
            LOGGER.finer("Stored editor state for new file ID: " + editorId + ", Version: 1");

            JScrollPane scrollPane = new JScrollPane(newEditor.getTextPane());
            cardContainer.add(scrollPane, editorId);
            tabPanel.add(tabButton);

            switchTab(editorId);

            JavaParser.getInstance().openFile(tempFilePath.toString());

            revalidate();
            repaint();

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error creating new file tab", e);
            JOptionPane.showMessageDialog(this, "Could not create new file: " + e.getMessage(), "New File Error", JOptionPane.ERROR_MESSAGE);
             // Cleanup
             if (tempFilePath != null) { try { Files.deleteIfExists(tempFilePath); } catch (IOException ex) { LOGGER.log(Level.WARNING, "Failed cleanup: " + tempFilePath, ex); } }
             if (tempDir != null) { try { Files.deleteIfExists(tempDir); } catch (IOException ex) { LOGGER.log(Level.WARNING, "Failed cleanup: " + tempDir, ex); } }
        } catch (Exception e) {
             LOGGER.log(Level.SEVERE, "Unexpected error creating new file tab", e);
             JOptionPane.showMessageDialog(this, "An unexpected error occurred creating the new file.", "Error", JOptionPane.ERROR_MESSAGE);
              // Cleanup
             if (tempFilePath != null) { try { Files.deleteIfExists(tempFilePath); } catch (IOException ex) { LOGGER.log(Level.WARNING, "Failed cleanup: " + tempFilePath, ex); } }
             if (tempDir != null) { try { Files.deleteIfExists(tempDir); } catch (IOException ex) { LOGGER.log(Level.WARNING, "Failed cleanup: " + tempDir, ex); } }
        }
    }

    // Method to get all current editor IDs
    public Set<String> getActiveEditorStateIds() {
        return Set.copyOf(editorStates.keySet());
    }

    // Helper to create a configured TextEditor
    private TextEditor createNewTextEditor() {
        TextEditor editor = new TextEditor();
        JTextPane pane = editor.getTextPane();
        pane.setFont(new Font("Monospaced", Font.PLAIN, 14));
        pane.setBackground(PanelColors.CODE_BG);
        pane.setForeground(PanelColors.TEXT_COLOR);
        pane.setCaretColor(PanelColors.TEXT_COLOR);
        return editor;
    }

    // Helper to create and configure a tab button
    private TabButton createTabButton(String title, String editorId) {
        TabButton tabButton = new TabButton(title, false);
        tabButton.addActionListener(e -> switchTab(editorId)); 
        return tabButton;
    }

    

    // changes tabs and updates editor state
    private void switchTab(String editorIdToShow) {
        if (editorIdToShow == null || editorIdToShow.equals(activeEditorId)) {
             return; // No change needed
        }
        LOGGER.finer("Switching tab to: " + editorIdToShow);

        cardLayout.show(cardContainer, editorIdToShow); 
        activeEditorId = editorIdToShow; 

        // Update tab button visuals
        editorStates.forEach((id, state) -> {
            boolean isActive = id.equals(editorIdToShow);
            state.tabButton.setSelected(isActive);
            state.tabButton.setBackground(isActive ? PanelColors.SELECTED_TAB_BG : PanelColors.PANEL_BG);
        });

         // Request focus for the text pane in the newly selected tab
         EditorState newState = editorStates.get(editorIdToShow);
         if (newState != null && newState.textEditor != null) {
             newState.textEditor.getTextPane().requestFocusInWindow();
         } else if ("Welcome".equals(editorIdToShow)) {
             cardContainer.requestFocusInWindow();
         }

        revalidate();
        repaint();
    }


    
    // only lets the most recently editor work
    public EditorState getActiveEditorState() {
        if (activeEditorId == null || activeEditorId.equals("Welcome")) {
            return null;
        }
        return editorStates.get(activeEditorId);
    }

    public void closeTab(String editorId) {
        LOGGER.info("Attempting to close tab: " + editorId);
        EditorState state = editorStates.remove(editorId);

        if (state != null) {
            LOGGER.fine("Closing editor: Original=" + state.originalFilePath + ", Temp=" + state.tempFilePath);
            try {
                JavaParser.getInstance().closeFile(state.tempFilePath.toString());
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to send didClose notification to JDTLS for " + state.tempFilePath, e);
            }

            SwingUtilities.invokeLater(() -> {
                Component[] components = cardContainer.getComponents();
                for (Component comp : components) {
                    if (comp instanceof JScrollPane) {
                         JScrollPane scrollPane = (JScrollPane) comp;
                         if (scrollPane.getViewport().getView() == state.textEditor.getTextPane()) {
                             cardContainer.remove(scrollPane);
                             LOGGER.finer("Removed JScrollPane for editor " + editorId + " from card container.");
                             break;
                         }
                    }
                }
                tabPanel.remove(state.tabButton);
                LOGGER.finer("Removed tab button for editor " + editorId + ".");

                String nextTabId = "Welcome";
                if (!editorStates.isEmpty()) {
                    nextTabId = editorStates.keySet().iterator().next();
                }

                 if (editorId.equals(activeEditorId)) {
                     switchTab(nextTabId); 
                 }

                revalidate();
                repaint();
            });

             Path tempPath = state.tempFilePath;
             Path tempDir = tempPath.getParent();
             try {
                 boolean deletedFile = Files.deleteIfExists(tempPath);
                 LOGGER.log(deletedFile ? Level.INFO : Level.WARNING, "Deletion status for temp file " + tempPath + ": " + deletedFile);
                 if (tempDir != null && tempDir.toString().contains("codezide_")) {
                     boolean deletedDir = Files.deleteIfExists(tempDir);
                     LOGGER.log(deletedDir ? Level.INFO : Level.WARNING, "Deletion status for temp dir " + tempDir + ": " + deletedDir);
                 } else if (tempDir == null) {
                     LOGGER.warning("Could not determine parent directory for temp file: " + tempPath);
                 }
             } catch (IOException e) {
                 LOGGER.log(Level.SEVERE, "Error deleting temporary resources for " + tempPath, e);
             }

            state.textEditor.dispose();
            LOGGER.fine("Disposed resources for editor " + editorId);

        } else {
            LOGGER.warning("Attempted to close non-existent editor ID: " + editorId);
        }
    }

    public void updateStateAfterSave(String editorId, String newOriginalFilePath) {
        EditorState state = editorStates.get(editorId);
        if (state != null) {
            EditorState newState = new EditorState(
                    state.textEditor,
                    newOriginalFilePath,
                    state.tempFilePath,
                    state.editorId,
                    state.tabButton
            );
            newState.documentVersion.set(state.getVersion());
            editorStates.put(editorId, newState);

            File file = new File(newOriginalFilePath);
            state.tabButton.setText(file.getName());

            state.textEditor.getTextPane().putClientProperty("originalFilePath", newOriginalFilePath);
            state.textEditor.getTextPane().putClientProperty("documentVersion", newState.documentVersion);

            LOGGER.info("Editor state updated after save. EditorID: " + editorId + ", New Original Path: " + newOriginalFilePath + ", Version: " + newState.getVersion());
        } else {
             LOGGER.warning("Attempted to update state for non-existent editor ID: " + editorId);
        }
    }



    // --- Methods needed by FileMenu ---

    // Gets the JTextPane of the currently active tab (if any)
    public JTextPane getActiveTextPane() {
         EditorState activeState = getActiveEditorState();
         return (activeState != null) ? activeState.textEditor.getTextPane() : null;
    }

    public String getActiveOriginalFilePath() {
        EditorState activeState = getActiveEditorState();
        return (activeState != null) ? activeState.originalFilePath : null;
    }

     public Path getActiveTempFilePath() {
        EditorState activeState = getActiveEditorState();
        return (activeState != null) ? activeState.tempFilePath : null;
    }
}