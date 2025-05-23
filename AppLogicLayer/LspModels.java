// src/main/java/com/example/lsp/models/LspModels.java
package AppLogicLayer;

import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;

import java.util.Collections;

import java.util.List;
import java.util.Map;

// Basic LSP Message Structure for JSON requests
public class LspModels {

    public static class Message {
        @SerializedName("jsonrpc")
        public String jsonrpc = "2.0";
        public String id; 
        public String method; 
        public JsonElement params; 
        public JsonElement result;
        public ResponseError error; 
    }

    public static class Request extends Message {
        public Request(String id, String method, JsonElement params) {
            this.id = id;
            this.method = method;
            this.params = params;
        }
    }

    public static class Response extends Message {}

    public static class Notification extends Message {
        public Notification(String method, JsonElement params) {
            this.method = method;
            this.params = params;
        }
    }

    public static class ResponseError {
        public int code;
        public String message;
        public JsonElement data;
    }

    // --- Initialization LSP Structures ---
    public static class InitializeParams {
        public int processId;
        public ClientInfo clientInfo;
        public String rootUri; 
        public String rootPath; 
        public Capabilities capabilities;
        public Map<String, Object> initializationOptions;
        public List<WorkspaceFolder> workspaceFolders;
        public String locale;
    }

    public static class ClientInfo {
        public String name;
        public String version;
    }

    public static class Capabilities {
        public TextDocumentClientCapabilities textDocument;
        public WorkspaceClientCapabilities workspace;
    }

    public static class TextDocumentClientCapabilities {
        public SemanticTokensCapabilities semanticTokens;
        public SynchronizationCapabilities synchronization; 
    }

    public static class SemanticTokensCapabilities {
        public boolean dynamicRegistration;
        public ResultFormats[] resultFormats;
        public Provider capabilities;

        public static class Provider {
             public boolean full; 
             public boolean range;
        }
         public enum ResultFormats {
            @SerializedName("relative")
            RELATIVE
        }
    }

    public static class SynchronizationCapabilities {
        public boolean dynamicRegistration;
        public boolean willSave;
        public boolean willSaveWaitUntil;
        public boolean didSave;
    }


    public static class WorkspaceClientCapabilities {
        public WorkspaceEditCapabilities workspaceEdit;
        public Boolean workspaceFolders;
    }

     public static class WorkspaceEditCapabilities {
         public boolean documentChanges;
     }


    public static class InitializeResult {
        public ServerCapabilities capabilities;
        public ServerInfo serverInfo;
    }

    public static class ServerCapabilities {
        public SemanticTokensOptions semanticTokensProvider;
        public TextDocumentSyncOptions textDocumentSync;
    }

    public static class SemanticTokensFullOptions {
        public Boolean delta; 
    }

    public static class SemanticTokensLegend {
        public List<String> tokenTypes;

        public List<String> tokenModifiers;
    }

    public static class DocumentFilter {
        public String language;

        public String scheme;

        public String pattern;

    }

    public static class SemanticTokensOptions { 
        public SemanticTokensLegend legend;
        public Boolean range;
    
        public SemanticTokensFullOptions full;
    
        public List<DocumentFilter> documentSelector;
        
    }
    

     public static class TextDocumentSyncOptions {
        public Boolean openClose;
        public Integer change;
         public boolean willSave;
         public boolean willSaveWaitUntil;
         public SaveOptions save;

         public static class SaveOptions {
             public boolean includeText;
         }
     }


    public static class ServerInfo {
        public String name;
        public String version;
    }

    public static class WorkspaceFolder {
        public String uri;
        public String name;

        public WorkspaceFolder(String uri, String name) {
            this.uri = uri;
            this.name = name;
        }
    }


    // --- Text Document LSP Structures ---
    public static class TextDocumentIdentifier {
        public String uri;

        public TextDocumentIdentifier(String uri) {
            this.uri = uri;
        }
    }

     public static class VersionedTextDocumentIdentifier extends TextDocumentIdentifier {
         public int version;
         public VersionedTextDocumentIdentifier(String uri, int version) {
             super(uri);
             this.version = version;
         }
     }

    public static class TextDocumentItem extends TextDocumentIdentifier {
        public String languageId;
        public int version;
        public String text;

        public TextDocumentItem(String uri, String languageId, int version, String text) {
            super(uri);
            this.languageId = languageId;
            this.version = version;
            this.text = text;
        }
    }

    public static class DidOpenTextDocumentParams {
        public TextDocumentItem textDocument;

        public DidOpenTextDocumentParams(TextDocumentItem textDocument) {
            this.textDocument = textDocument;
        }
    }


    // --- Semantic Tokens LSP Structures ---
    public static class SemanticTokensParams {
        public TextDocumentIdentifier textDocument;

        public SemanticTokensParams(TextDocumentIdentifier textDocument) {
            this.textDocument = textDocument;
        }
    }

    // Result structure for textDocument/semanticTokens/full
    public static class SemanticTokens {
        public String resultId;
        public List<Integer> data;
    }

     // Example Error Message
     public static class ShowMessageParams {
         public int type; 
         public String message;
     }

    public enum MessageType {
        ERROR(1), WARNING(2), INFO(3), LOG(4);
        private final int value;
        MessageType(int value) { this.value = value; }
        public int getValue() { return value; }
    }

     // For $/logTrace or window/logMessage
    public static class LogMessageParams extends ShowMessageParams {}
    
    public static class DidCloseTextDocumentParams {

        private TextDocumentIdentifier textDocument;
    
        public DidCloseTextDocumentParams() {
        }
    
        public DidCloseTextDocumentParams(TextDocumentIdentifier textDocument) {
            this.textDocument = textDocument;
        }
    
        public TextDocumentIdentifier getTextDocument() {
            return textDocument;
        }
    
        public void setTextDocument(TextDocumentIdentifier textDocument) {
            this.textDocument = textDocument;
        }
    }

    public static class DidSaveTextDocumentParams {
        private TextDocumentIdentifier textDocument;
    
        private String text; 
        public DidSaveTextDocumentParams() {
        }
    
        public DidSaveTextDocumentParams(TextDocumentIdentifier textDocument) {
            this.textDocument = textDocument;
            this.text = null;
        }
    
        public DidSaveTextDocumentParams(TextDocumentIdentifier textDocument, String text) {
            this.textDocument = textDocument;
            this.text = text;
        }
    
        public TextDocumentIdentifier getTextDocument() {
            return textDocument;
        }
    
        public void setTextDocument(TextDocumentIdentifier textDocument) {
            this.textDocument = textDocument;
        }
    
        public String getText() {
            return text; 
        }
    
        public void setText(String text) {
            this.text = text;
        }
    }

    public static class DidChangeTextDocumentParams {
        private VersionedTextDocumentIdentifier textDocument;
        private List<TextDocumentContentChangeEvent> contentChanges;
    
        public DidChangeTextDocumentParams() {}
    
        public void setTextDocument(VersionedTextDocumentIdentifier textDocument) { this.textDocument = textDocument; }
        public VersionedTextDocumentIdentifier getTextDocument() { return textDocument; }
    
        public void setContentChanges(List<TextDocumentContentChangeEvent> contentChanges) { this.contentChanges = contentChanges; }
        public List<TextDocumentContentChangeEvent> getContentChanges() { return contentChanges; }
    }

    public static class TextDocumentContentChangeEvent {
        
        private String text;
    
        
        public TextDocumentContentChangeEvent() {}
    
        public void setText(String text) { this.text = text; }
        public String getText() { return text; }
    }

    
    public static class Range {
    
        public Position start;
        
        public Position end;

    }

    public static class Position {
        public int line;
        public int character;
    }


    public static class WorkspaceFoldersChangeEvent {
        public List<WorkspaceFolder> added;
        public List<WorkspaceFolder> removed;
    
        // Default constructor for Gson
        public WorkspaceFoldersChangeEvent() {
            this.added = Collections.emptyList();
            this.removed = Collections.emptyList();
        }
    }
    
    public static class DidChangeWorkspaceFoldersParams {
        public WorkspaceFoldersChangeEvent event;
    
        public DidChangeWorkspaceFoldersParams(WorkspaceFoldersChangeEvent event) {
            this.event = event;
        }
    }
}