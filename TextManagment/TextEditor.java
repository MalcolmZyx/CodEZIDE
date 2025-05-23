// TextEditor.java (Relevant changes)
package TextManagement;

import java.awt.*;
import java.nio.file.Files; // Added
import java.nio.file.Path; // Added
import java.io.IOException; // Added
import AppLogicLayer.JavaParser; // Assuming accessible
import com.codezide.ui.panels.CenterPanel; // May not be needed directly if path comes via property
import com.codezide.ui.utils.PanelColors; // May not be needed directly if path comes via property


import java.awt.Color;
import java.awt.Font;

import javax.swing.JTextPane; // Added: JTextPane class
import javax.swing.SwingUtilities; // Added: SwingUtilities class
import javax.swing.Timer; // Added: Timer used in showTemporaryError
import javax.swing.event.DocumentEvent; // Added: DocumentEvent class
import javax.swing.event.DocumentListener; // Added: DocumentListener interface
import javax.swing.text.*; // Added: Imports for StyledDocument, Style, StyleContext, etc.

// IO/NIO Imports
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;

// Util Imports
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;  
import java.util.logging.Logger; 

// Project Specific Imports
import AppLogicLayer.JavaParser;
import AppLogicLayer.LspModels;

public class TextEditor {
    private static final Logger LOGGER = Logger.getLogger(TextEditor.class.getName());

    private JTextPane textPane;
    private ScheduledExecutorService debounceExecutor;
    private JavaParser parser;
    private ScheduledFuture<?> pendingHighlightTask;
    private TextEditorDocumentListener documentListener; // Store listener instance

    public TextEditor() {
        try {
            this.parser = JavaParser.getInstance();
            this.textPane = new JTextPane(); // javax.swing.JTextPane
            // java.util.concurrent.Executors
            this.debounceExecutor = Executors.newSingleThreadScheduledExecutor();
            this.documentListener = new TextEditorDocumentListener(); // Create listener instance

            setupDocumentListener();
            LOGGER.finest("TextEditor instance created.");
        } catch (IllegalStateException e) {
            System.err.println("[TextEditor.java] Error initializing Java Language Server: " + e.getMessage());
        }
    }

    // Inner class for the listener (implements javax.swing.event.DocumentListener)
    private class TextEditorDocumentListener implements DocumentListener {
        @Override public void insertUpdate(DocumentEvent e) { triggerHighlighting(); }
        @Override public void removeUpdate(DocumentEvent e) { triggerHighlighting(); }
        @Override public void changedUpdate(DocumentEvent e) { /* Attribute changes, ignore */ }
    }

    private void setupDocumentListener() {
        textPane.getDocument().addDocumentListener(this.documentListener);
    }

    private void triggerHighlighting() {
        // Retrieve necessary info stored as client properties
        Path tempFilePath = (Path) textPane.getClientProperty("tempFilePath");
        // java.util.concurrent.atomic.AtomicInteger
        AtomicInteger documentVersion = (AtomicInteger) textPane.getClientProperty("documentVersion");

        if (tempFilePath == null || documentVersion == null) {
            LOGGER.finer("Skipping highlighting trigger - tempFilePath or documentVersion not yet set.");
            return;
        }

        if (pendingHighlightTask != null && !pendingHighlightTask.isDone()) {
            pendingHighlightTask.cancel(false);
        }

        // java.util.concurrent.TimeUnit
        pendingHighlightTask = debounceExecutor.schedule(() -> {
            processTextAsyncWithTempFile(tempFilePath, documentVersion);
        }, 500, TimeUnit.MILLISECONDS);
    }

    // Modified to accept and use the document version
    private void processTextAsyncWithTempFile(Path tempFilePath, AtomicInteger documentVersion) {
        if (!textPane.isDisplayable()) {
             LOGGER.info("Text pane no longer displayable, aborting text processing for: " + tempFilePath);
             return;
        }

        String currentText = textPane.getText();

        LOGGER.fine("Processing text for temp file: " + tempFilePath);

        try {
            Files.writeString(tempFilePath, currentText);
            LOGGER.finer("Successfully wrote buffer content to temp file: " + tempFilePath);

            int newVersion = documentVersion.incrementAndGet();
            parser.notifyDidChange(tempFilePath.toString(), currentText, newVersion);

            LOGGER.finer("Requesting semantic tokens for version " + newVersion);
            // java.util.concurrent.CompletableFuture
            CompletableFuture<LspModels.SemanticTokens> tokensFuture = parser.requestSemanticTokens(tempFilePath.toString());

            tokensFuture.thenAcceptAsync(tokens -> {
                // javax.swing.SwingUtilities
                SwingUtilities.invokeLater(() -> {
                    if (textPane.isDisplayable() && currentText.equals(textPane.getText())) {
                        if (tokens != null && tokens.data != null) {
                            LOGGER.fine("Applying " + (tokens.data.size() / 5) + " semantic tokens for " + tempFilePath.getFileName());
                            applySemanticHighlights(currentText, tokens.data);
                        } else {
                            LOGGER.warning("No semantic tokens returned or data is null for: " + tempFilePath.getFileName());
                             clearHighlights(textPane.getStyledDocument());
                        }
                    } else {
                        LOGGER.info("Skipping highlight application; text changed during processing or pane closed for " + tempFilePath.getFileName());
                    }
                });
            }).exceptionally(ex -> {
                SwingUtilities.invokeLater(() -> {
                     // java.util.concurrent.CancellationException
                     if (!(ex instanceof CancellationException)) {
                         // java.util.logging.Level
                         LOGGER.log(Level.SEVERE, "Error requesting/processing semantic tokens for " + tempFilePath.getFileName(), ex);
                     } else {
                         LOGGER.fine("Semantic token request cancelled for " + tempFilePath.getFileName());
                     }
                });
                return null;
            });

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error writing to temporary file: " + tempFilePath, e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error during text processing for " + tempFilePath, e);
        }
    }

    private void applySemanticHighlights(String textForPositioning, List<Integer> tokenData) {
        // javax.swing.text.StyledDocument
        StyledDocument doc = textPane.getStyledDocument();
        Element rootElement = doc.getDefaultRootElement(); 
        clearHighlights(doc);

        if (tokenData == null || tokenData.isEmpty()) {
            LOGGER.finer("No token data to apply.");
            return;
        }

        StyleContext styleContext = StyleContext.getDefaultStyleContext();
        int currentLine = 0;
        int currentChar = 0;

        for (int i = 0; i < tokenData.size(); i += 5) {
            int deltaLine = tokenData.get(i);
            int deltaChar = tokenData.get(i + 1);
            int length = tokenData.get(i + 2);
            int tokenTypeIndex = tokenData.get(i + 3);
            // int tokenModifiers = tokenData.get(i + 4);

            currentLine += deltaLine;
            if (deltaLine > 0) {
                currentChar = deltaChar; // Reset character offset on new line
            } else {
                currentChar += deltaChar; // Add to previous char offset on same line
            }

            // Calculate position using Document model
            int position = -1;
            if (currentLine < 0 || currentLine >= rootElement.getElementCount()) {
                LOGGER.warning(String.format("Skipping token due to invalid line number: %d (max: %d)",
                                            currentLine, rootElement.getElementCount() -1));
                continue; // Skip token
            }

            Element lineElement = rootElement.getElement(currentLine);
            if (lineElement == null) {
                LOGGER.warning("Could not get Element for line: " + currentLine);
                continue; // Skip token
            }

            int lineStartOffset = lineElement.getStartOffset();
            int lineEndOffset = lineElement.getEndOffset();
            int lineLength = lineEndOffset - lineStartOffset;

            // Adjust for potential trailing newline character in length calculation for comparison
            int effectiveLineLength = lineLength;
            if (lineLength > 0) {
                try {
                    if (doc.getText(lineEndOffset - 1, 1).equals("\n")) {
                        effectiveLineLength = lineLength - 1; 
                    }
                } catch (BadLocationException e) {
                    LOGGER.log(Level.WARNING,
                        String.format("BadLocationException checking for newline at end offset %d for line %d. Using full line length %d.",
                                    lineEndOffset, currentLine, lineLength), e);
                }
            }
            if (currentChar < 0 || currentChar > effectiveLineLength) {
                LOGGER.warning(String.format("Character offset %d is out of bounds for line %d (length: %d, effective length: %d). Clamping.",
                                            currentChar, currentLine, lineLength, effectiveLineLength));
                currentChar = Math.max(0, Math.min(currentChar, effectiveLineLength));
            }

            position = lineStartOffset + currentChar;


            // String tokenText = getTextFromEditor(textPane, currentLine, currentChar, length); // Still useful for debugging
            // System.out.printf("Token: Line=%d, Start=%d, Len=%d, Type=%d, Mod=%d, Text='%s', Pos=%d%n",
            //         currentLine, currentChar, length, tokenTypeIndex, tokenModifiers, tokenText, position);


            if (position == -1) { 
                LOGGER.warning(String.format("Skipping token due to failed position calculation: line=%d, char=%d", currentLine, currentChar));
                continue;
            }

            int docLength = doc.getLength();
            if (position > docLength) {
                LOGGER.warning(String.format("Calculated position %d exceeds document length %d. Skipping token. Line: %d, Char: %d", position, docLength, currentLine, currentChar));
                continue;
            }

            // Adjust length if it exceeds document bounds *from the calculated position*
            int effectiveLength = Math.min(length, docLength - position);
            if (effectiveLength <= 0) {
                // LOGGER.finer("Skipping token with zero or negative effective length."); // Can be noisy
                continue;
            }


            Color color = getColorForToken(tokenTypeIndex);
            MutableAttributeSet attributes = new SimpleAttributeSet();
            StyleConstants.setForeground(attributes, color);

            try {
                doc.setCharacterAttributes(position, effectiveLength, attributes, false);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, String.format("Error applying attributes at pos %d, len %d (orig len %d) for line %d, char %d",
                                                        position, effectiveLength, length, currentLine, currentChar), e);
            }
        }
    }

    public static String getTextFromEditor(JTextComponent textComponent, int absoluteLine, int absoluteStartChar, int length) {
        Document doc = textComponent.getDocument();
        if (doc == null || length <= 0) {
            return ""; // Nothing to extract
        }

        Element root = doc.getDefaultRootElement();
        if (absoluteLine < 0 || absoluteLine >= root.getElementCount()) {
            LOGGER.warning("Attempted to get text from invalid line: " + absoluteLine + " (Total lines: " + root.getElementCount() + ")");
            return ""; // Line number out of bounds
        }

        Element lineElement = root.getElement(absoluteLine);
        if (lineElement == null) {
            LOGGER.warning("Could not get element for line: " + absoluteLine);
            return "";
        }

        int lineStartOffset = lineElement.getStartOffset();

        // Calculate the absolute start offset in the document
        int startOffset = lineStartOffset + absoluteStartChar;

        // Basic bounds checking for the start offset
        if (startOffset < lineStartOffset || startOffset > doc.getLength()) {
             LOGGER.warning("Calculated start offset (" + startOffset
                           + ") is outside valid range for line " + absoluteLine
                           + " (Line start: " + lineStartOffset + ", Doc length: " + doc.getLength() + ")");
            return "";
        }
        // Ensure calculated end offset doesn't exceed document length
        int effectiveLength = Math.min(length, doc.getLength() - startOffset);
         if (effectiveLength <= 0) {
              LOGGER.finer(() -> "Calculated effective length is zero or negative for token at line " + absoluteLine + ", char " + absoluteStartChar);
              return "";
         }

        try {
            // Extract the text
            return doc.getText(startOffset, effectiveLength);
        } catch (BadLocationException e) {
            LOGGER.log(Level.SEVERE, "BadLocationException getting text: offset=" + startOffset
                       + ", length=" + effectiveLength + ", docLength=" + doc.getLength()
                       + ", requestedAbsLine=" + absoluteLine + ", reqAbsChar=" + absoluteStartChar + ", reqLen=" + length, e);
            return "";
        } catch (Exception e) {
             LOGGER.log(Level.SEVERE, "Unexpected error getting text from editor", e);
             return "";
        }
    }

    private void clearHighlights(StyledDocument doc) {
        // javax.swing.text.Style
        Style defaultStyle = StyleContext.getDefaultStyleContext().getStyle(StyleContext.DEFAULT_STYLE);
        
        StyleConstants.setForeground(defaultStyle, PanelColors.TEXT_COLOR);
        StyleConstants.setBackground(defaultStyle, PanelColors.CODE_BG);
        doc.setCharacterAttributes(0, doc.getLength(), defaultStyle, true);
    }


    private Color getColorForToken(int tokenTypeIndex) {
        List<String> tokenTypesLegend = JavaParser.getInstance().getSemanticTokenTypesLegend();
        if (tokenTypesLegend == null || tokenTypeIndex < 0 || tokenTypeIndex >= tokenTypesLegend.size()) {
            LOGGER.warning("Invalid tokenTypeIndex: " + tokenTypeIndex + " or legend not available.");
            return DEFAULT_TEXT;
        }
        String tokenType = tokenTypesLegend.get(tokenTypeIndex);
        switch (tokenType) {
            case "namespace": return KEYWORD_PURPLE;
            case "type":
            case "class":     return CLASSNAME_BLUE;
            case "enum":      return CLASSNAME_BLUE;
            case "interface": return CLASSNAME_BLUE;
            case "struct":    return CLASSNAME_BLUE;
            case "typeParameter": return KEYWORD_TEAL;
            case "parameter": return DEFAULT_YELLOWISH;
            case "variable": return DEFAULT_YELLOWISH;
            case "property":  return DEFAULT_YELLOWISH;
            case "enumMember": return KEYWORD_PURPLE;
            case "function":
            case "method":    return METHOD_CALL_YELLOW;
            case "macro":     return KEYWORD_PURPLE;
            case "keyword":   return KEYWORD_TEAL;
            case "modifier":  return KEYWORD_TEAL;
            case "comment":   return COMMENT_GREY;
            case "string":    return STRING_ORANGE;
            case "number":    return LITERAL_GREEN;
            case "regexp":    return STRING_ORANGE;
            case "operator":  return DEFAULT_TEXT;
            default:
                LOGGER.finer("Using default color for unmapped token type: " + tokenType);
                return DEFAULT_TEXT;
        }
    }

    public JTextPane getTextPane(){
        return textPane;
    }

    public void dispose() {
        LOGGER.info("Disposing TextEditor resources for pane: " + textPane.hashCode());
        if (textPane != null && this.documentListener != null) {
             textPane.getDocument().removeDocumentListener(this.documentListener);
        }
        if (pendingHighlightTask != null) {
            pendingHighlightTask.cancel(true);
        }
        debounceExecutor.shutdownNow();
        try {
            // java.util.concurrent.TimeUnit
            if (!debounceExecutor.awaitTermination(500, TimeUnit.MILLISECONDS)) {
                LOGGER.warning("Debounce executor did not terminate gracefully.");
            }
        } catch (InterruptedException e) {
            LOGGER.warning("Interrupted while waiting for executor shutdown.");
            Thread.currentThread().interrupt();
        }
         textPane.putClientProperty("tempFilePath", null);
         textPane.putClientProperty("originalFilePath", null);
         textPane.putClientProperty("editorId", null);
         textPane.putClientProperty("documentVersion", null);
        LOGGER.fine("TextEditor disposed for pane: " + textPane.hashCode());
    }

     // Color constants (using java.awt.Color)
     private static final Color METHOD_CALL_YELLOW = new Color(220, 220, 150);
     private static final Color CLASSNAME_BLUE = new Color(130, 190, 255);
     private static final Color LITERAL_GREEN = new Color(180, 230, 160);
     private static final Color STRING_ORANGE = new Color(255, 180, 130);
     private static final Color DEFAULT_TEXT = new Color(220, 220, 220);
     private static final Color COMMENT_GREY = new Color(140, 140, 140);
     private static final Color KEYWORD_PURPLE = new Color(190, 140, 255);
     private static final Color KEYWORD_TEAL = new Color(100, 210, 190);
     private static final Color DEFAULT_YELLOWISH = new Color(230, 230, 200);
}