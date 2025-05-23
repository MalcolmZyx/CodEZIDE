//DeepSeek-V3 version, DeepSeek, 29 Mar. 2025. https://chat.deepseek.com/
//Gemini 2.5 Pro version, Google, 13 Apr. 2025. https://gemini.google.com/app

/*
 * Both models were used to inform the development of the javaParser
 *  - Informed me of how the LSP works
 *  - Helped work with the Gson commands
 *  - Helped run the LSP in a seperate thread from the main app
 *  - Helped me troubleshoot
 */

package AppLogicLayer;

import AppLogicLayer.LspModels;
import AppLogicLayer.LspModels.SemanticTokensCapabilities;
import AppLogicLayer.LspModels.SynchronizationCapabilities;
import AppLogicLayer.LspModels.WorkspaceEditCapabilities;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import java.io.StringReader; 

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Collections;
import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

import static AppLogicLayer.LspModels.*; 

public class JavaParser {

    // Singleton Instance
    private static JavaParser instance;
    // For thread-safe singleton initialization
    private static final Object lock = new Object(); 

    // JDTLS Process and Communication
    private Process jdtlsProcess;
    private InputStreamReader serverOutputReader;
    private OutputStreamWriter serverInputWriter;
    private BufferedReader serverOutputBufferedReader; 
    private LspModels.ServerCapabilities serverCapabilities;
    private List<String> semanticTokenTypesLegend = Collections.emptyList();

    private final Gson gson = new GsonBuilder().setLenient().create(); 
    //private final Gson prettyGson = new GsonBuilder().setPrettyPrinting().create();
    
    // LSP Request Management
    private final AtomicLong requestIdCounter = new AtomicLong(0);
    private final Map<String, CompletableFuture<JsonElement>> pendingRequests = new ConcurrentHashMap<>();


    // JDTLS Configuration Paths
    private static String JDTLS_LAUNCHER_PATH;
    private static String JDTLS_CONFIG_PATH;
    private static String WORKSPACE_PATH = "C:/Temp/jdtls_ide_workspace"; 
    private static String PROJECT_ROOT_PATH;


    // Relative path to JDTLS resources from the application's base directory
    private static final String JDTLS_DIRECTORY_NAME = "lib/jdt-language-server-1.9.0-202203031534";
    private static boolean pathsInitialized = false;

    // Internal state
    private Thread workerThread;
    private volatile boolean running = true; 
    private Thread readerThread;
    private Thread errorReaderThread;

    String javaExecutableForJdtls = "C:\\Program Files\\Java\\jdk-11\\bin\\java.exe";
    // Private constructor to prevent external instantiation
    private JavaParser() throws IllegalStateException {
        if (!pathsInitialized) {
            try {
                URL codeSourceUrl = JavaParser.class.getProtectionDomain().getCodeSource().getLocation();
                if (codeSourceUrl == null) {
                    throw new IllegalStateException("Cannot determine code source location. JDTLS paths cannot be initialized.");
                }

                File codeSourceFile = new File(codeSourceUrl.toURI());
                Path applicationBasePath;

                if (codeSourceFile.isFile() && codeSourceFile.getName().toLowerCase().endsWith(".jar")) {
                    // Running from a JAR file, get its parent directory
                    applicationBasePath = codeSourceFile.getParentFile().toPath();
                    System.out.println("[JavaParser] Running from JAR. Deduced application base path: " + applicationBasePath.toString());
                } else {
                    applicationBasePath = Paths.get(System.getProperty("user.dir"));
                    System.out.println("[JavaParser] Not running from JAR (or code source is a directory). Using current working directory as application base path: " + applicationBasePath.toString());
                    System.out.println("[JavaParser] Code source URL was: " + codeSourceUrl);
                }

                // Construct the absolute base path to the JDTLS directory
                Path jdtlsBaseDirectory = applicationBasePath.resolve(JDTLS_DIRECTORY_NAME);

                // Construct the full path to the JDTLS launcher JAR
                Path launcherPath = jdtlsBaseDirectory.resolve(Paths.get("plugins", "org.eclipse.equinox.launcher_1.6.400.v20210924-0641.jar"));
                JDTLS_LAUNCHER_PATH = launcherPath.toString();

                // Construct the full path to the JDTLS config directory
                Path configPathDir = jdtlsBaseDirectory.resolve("config_win");
                JDTLS_CONFIG_PATH = configPathDir.toString();

                // which is often the directory where the application was launched from.
                // This should remain independent of the JDTLS library location.
                Path initialDefaultProjectRoot = applicationBasePath.resolve("default_empty_project_root");
                File initialDefaultProjectRootDir = initialDefaultProjectRoot.toFile();
                if (!initialDefaultProjectRootDir.exists()) {
                    initialDefaultProjectRootDir.mkdirs();
                }
                PROJECT_ROOT_PATH = initialDefaultProjectRootDir.toPath().toAbsolutePath().toString().replace('\\', '/');
                System.out.println("[JavaParser] Using dedicated initial Project Root Path for JDTLS initialize: " + PROJECT_ROOT_PATH);

                Path jdtlsDataPath = applicationBasePath.resolve("jdtls_workspace");

                File jdtlsDataDir = jdtlsDataPath.toFile();
                if (!jdtlsDataDir.exists()) {
                    if (jdtlsDataDir.mkdirs()) {
                        System.out.println("[JavaParser] Created JDTLS data directory: " + jdtlsDataPath.toString());
                    } else {
                        System.err.println("[JavaParser CRITICAL] Failed to create JDTLS data directory: " + jdtlsDataPath.toString() + ". JDTLS might fail. Check permissions.");
                        // As a fallback, might revert to a temp directory or throw an error to stop
                        WORKSPACE_PATH = "C:/Temp/jdtls_ide_fallback_workspace";
                        System.err.println("[JavaParser] Falling back to JDTLS data directory: " + WORKSPACE_PATH);
                        if(!new File(WORKSPACE_PATH).mkdirs() && !new File(WORKSPACE_PATH).exists()) {
                            throw new IllegalStateException("Could not create any JDTLS workspace directory.");
                        }
                    }
                } else if (!jdtlsDataDir.isDirectory() || !jdtlsDataDir.canWrite()) {
                    System.err.println("[JavaParser CRITICAL] JDTLS data path exists but is not a writable directory: " + jdtlsDataPath.toString() + ". JDTLS may fail.");
                }

                WORKSPACE_PATH = jdtlsDataPath.toAbsolutePath().toString().replace('\\', '/');

                pathsInitialized = true;

                // debugging:
                System.out.println("Initialized JDTLS Launcher Path: " + JDTLS_LAUNCHER_PATH);
                System.out.println("Initialized JDTLS Config Path: " + JDTLS_CONFIG_PATH);
                System.out.println("Initialized default Project Root Path (user workspace): " + PROJECT_ROOT_PATH);
                System.out.println("JDTLS Data Workspace Path (now relative to app): " + WORKSPACE_PATH);

            } catch (URISyntaxException e) {
                System.err.println("[JavaParser CRITICAL] Error converting code source URL to URI: " + e.getMessage());
                e.printStackTrace();
                
                // the application might not be able to start JDTLS.
            } catch (IllegalStateException e) {
                 System.err.println("[JavaParser CRITICAL] Error initializing paths: " + e.getMessage());
                 e.printStackTrace();
            }
        }
    }

    // Public method to get the singleton instance
    public static JavaParser getInstance() throws IllegalStateException {
    if (instance == null) {
        synchronized (lock) {
            if (instance == null) {
                JavaParser tempInstance = new JavaParser();
                if (pathsInitialized) {
                    try {
                        tempInstance.start();
                        instance = tempInstance;
                    } catch (Exception e) {
                        System.err.println("[JavaParser CRITICAL] Failed to start JavaParser instance: " + e.getMessage());
                        
                        throw new IllegalStateException("Failed to initialize and start JavaParser service.", e);
                    }
                } else {
                    System.err.println("[JavaParser CRITICAL] JDTLS paths not initialized. JavaParser service cannot be created.");
                    throw new IllegalStateException("JDTLS paths not initialized for JavaParser service.");
                }
            }
        }
    }
    return instance;
}
    
    public List<String> getSemanticTokenTypesLegend() {
        // Return a copy or unmodifiable list for safety
        return Collections.unmodifiableList(semanticTokenTypesLegend);
    }

    /**
     * Main worker thread loop for communication with JDTLS.
     */
    private void start() throws IOException, ExecutionException, InterruptedException, URISyntaxException {
        try {
            // Launch JDTLS
            ProcessBuilder pb = new ProcessBuilder(
                    javaExecutableForJdtls,
                    "-Declipse.application=org.eclipse.jdt.ls.core.id1",
                    "-Dosgi.bundles.defaultStartLevel=4",
                    "-Declipse.product=org.eclipse.jdt.ls.core.product",
                    "-Dlog.level=ALL",
                    "-noverify",
                    "-Xmx1G", 
                    "--add-modules=ALL-SYSTEM",
                    "--add-opens", "java.base/java.util=ALL-UNNAMED",
                    "--add-opens", "java.base/java.lang=ALL-UNNAMED",
                    "-jar", JDTLS_LAUNCHER_PATH,
                    "-configuration", JDTLS_CONFIG_PATH,
                    "-data", WORKSPACE_PATH
            );
            jdtlsProcess = pb.start();
            System.out.println("[JavaParser] JDTLS Process Started (PID: " + jdtlsProcess.pid() + ")");

            // Setup streams (stdin, stdout)
            // Separate reader for stderr
            serverInputWriter = new OutputStreamWriter(jdtlsProcess.getOutputStream(), StandardCharsets.UTF_8);
            serverOutputReader = new InputStreamReader(jdtlsProcess.getInputStream(), StandardCharsets.UTF_8);
            serverOutputBufferedReader = new BufferedReader(serverOutputReader);
            InputStreamReader serverErrorStreamReader = new InputStreamReader(jdtlsProcess.getErrorStream(), StandardCharsets.UTF_8);

            // Start Error Reader Thread
            errorReaderThread = new Thread(() -> readErrorStreamLoop(serverErrorStreamReader), "JDTLS-Error-Reader");
            errorReaderThread.setDaemon(true); 
            errorReaderThread.start();

            // Start Response Reader Thread
            readerThread = new Thread(this::readResponseLoop, "JDTLS-Reader-Thread");
            readerThread.setDaemon(true);
            readerThread.start();

            // Perform Initialization (on the current thread or a dedicated sender thread)
            System.out.println("[JavaParser] Performing initialization...");
            initializeServer(); 
            System.out.println("[JavaParser] Initialization sequence complete.");
        } catch (IOException | ExecutionException | InterruptedException | URISyntaxException e) { // Catch specific declared exceptions
            System.err.println("[JavaParser] CRITICAL STARTUP ERROR (from initializeServer or process start): " + e.getMessage());
            
            shutdown(); 
            throw e; 
        } catch (Exception e) { 
            System.err.println("[JavaParser] CRITICAL UNEXPECTED STARTUP ERROR: " + e.getMessage());
            e.printStackTrace();
            shutdown();

            throw new IOException("Unexpected JDTLS startup failure during start(): " + e.getMessage(), e);
        }
    }

    

    private void readResponseLoop() {
        System.out.println("[JavaParser] Reader thread started.");
        try {
            while (running && jdtlsProcess.isAlive()) {
                String content = readLspMessage();
                if (content == null) {
                    if (running) System.err.println("[JavaParser] Reader thread: readLspMessage returned null or stream closed.");
                    break; // Exit 
                }
                processLspMessage(content); // Completes futures
            }
        } catch (IOException e) {
             if (running) { 
                // Dont log errors during intentional shutdown
                 System.err.println("[JavaParser] Reader thread I/O error: " + e.getMessage());
                 e.printStackTrace();
             }
        } catch (Exception e) {
             if (running) {
                 System.err.println("[JavaParser] Reader thread unexpected error: " + e.getMessage());
                 e.printStackTrace();
             }
        } finally {
            // Signal other threads if this one dies unexpectedly
            System.out.println("[JavaParser] Reader thread finished.");
            running = false; 
        }
    }

    private void readErrorStreamLoop(InputStreamReader errorStreamReader) {
        System.out.println("[JavaParser] Error reader thread started.");
        try (BufferedReader errReader = new BufferedReader(errorStreamReader)) {
            String line;
            while ((line = errReader.readLine()) != null) {
                System.err.println("[JDTLS ERR] " + line);
            }
        } catch (IOException e) {
             if(running) System.err.println("[JavaParser] Error reading JDTLS stderr: " + e.getMessage());
        } finally {
            System.out.println("[JavaParser] Error reader thread finished.");
        }
    }


    /**
     * Sends an LSP request and returns a CompletableFuture for its response.
     */
    private CompletableFuture<JsonElement> sendRequest(String method, JsonElement params) throws IOException {
        String id = String.valueOf(requestIdCounter.incrementAndGet());
        LspModels.Request request = new LspModels.Request(id, method, params);
        CompletableFuture<JsonElement> future = new CompletableFuture<>();
        pendingRequests.put(id, future);
    
        String json = gson.toJson(request);
        writeLspMessage(json);
    
        System.out.println("[JavaParser] Sent Request [" + id + "]: " + method);
        return future;
    }
    
    

    /**
     * Sends an LSP notification.
    */
    private void sendNotification(String method, JsonElement params) throws IOException {
        LspModels.Notification notification = new LspModels.Notification(method, params);
         String json = gson.toJson(notification);
         writeLspMessage(json);

         System.out.println("[JavaParser] Sent Notification: " + method);
     }


    private String readLspMessage() throws IOException {
        int contentLength = -1;
        String line;
        System.out.println("[DEBUG] Reading headers...");

        // Read Headers using BufferedReader
        while ((line = serverOutputBufferedReader.readLine()) != null && !line.isEmpty()) {
            // System.out.println("[DEBUG] Header line: " + line);
            if (line.startsWith("Content-Length:")) {
                try {
                    contentLength = Integer.parseInt(line.substring("Content-Length:".length()).trim());
                    System.out.println("[DEBUG] Parsed Content-Length: " + contentLength);
                } catch (NumberFormatException e) {
                    System.err.println("[JavaParser] Malformed Content-Length header: " + line);
                    return null; // Indicate error
                }
            }
            // Ignore other headers
        }
        System.out.println("[DEBUG] Finished headers. Final Content-Length: " + contentLength);

        // Check if stream closed right after headers
        if (line == null && contentLength <= 0) {
            System.out.println("[JavaParser] Server stream closed after reading headers.");
            return null;
        }

        if (contentLength <= 0) {
            System.err.println("[JavaParser] Missing or invalid Content-Length header found.");
            return null;
        }

        // Read Body using the SAME BufferedReader
        char[] contentChars = new char[contentLength];
        int charsRead = 0;
        System.out.println("[DEBUG] Reading body (expecting " + contentLength + " chars)...");
        while (charsRead < contentLength) {
            // Use the BufferedReader's read method, NOT serverOutputReader.read()
            int read = serverOutputBufferedReader.read(contentChars, charsRead, contentLength - charsRead);
            if (read == -1) {
                System.err.println("[JavaParser] Unexpected end of stream while reading message body. Expected " + contentLength + ", got " + charsRead);
                return null; 
            }
            charsRead += read;
        }
        System.out.println("[DEBUG] Finished reading body. Total chars read: " + charsRead);

        String content = new String(contentChars);
        return content;
    }

    

    // Writes an LSP message (JSON string) to the server's input stream with LSP headers. 
    private void writeLspMessage(String json) throws IOException {
        byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);
        String header = "Content-Length: " + jsonBytes.length + "\r\n"
                + "Content-Type: application/vscode-jsonrpc; charset=utf-8\r\n\r\n";
        serverInputWriter.write(header);
        serverInputWriter.write(json);
        serverInputWriter.flush();
    }

    
    //Processes a received LSP message (JSON string).
    private void processLspMessage(String content) {
        System.out.println("--- START RAW CONTENT ---");
        System.out.println(content);
        System.out.println("--- END RAW CONTENT ---");

        StringReader stringReader = null;
        JsonReader jsonStreamReader = null;

        try {
            stringReader = new StringReader(content);
            jsonStreamReader = new JsonReader(stringReader);

            LspModels.Message message = gson.fromJson(jsonStreamReader, LspModels.Message.class);

            StringBuilder trailingChars = new StringBuilder();
            try {
                int c;
                while ((c = stringReader.read()) != -1) {
                    trailingChars.append((char) c);
                }
                if (trailingChars.length() > 0) {
                    System.err.println("[JavaParser] WARNING: Trailing data found after JSON message: '" + trailingChars.toString() + "'");
                }
            } catch (IOException e) {
                System.err.println("[JavaParser] IOException while reading trailing data from StringReader: " + e.getMessage());
            } finally {
                if (jsonStreamReader != null) {
                    try {
                        jsonStreamReader.close();
                    } catch (IOException e) {
                        System.err.println("[JavaParser] Minor error closing JsonReader: " + e.getMessage());
                    }
                }
            }
            
            // ----- Process the successfully parsed 'message' -----
            if (message == null) {
                System.err.println("[JavaParser] Failed to parse LSP message (resulted in null object). Original content: " + content);
                return;
            }

            if (message.id != null) { 
                CompletableFuture<JsonElement> future = pendingRequests.remove(message.id);
                if (future != null) {
                    if (message.error != null) {
                        System.err.println("[JavaParser] Received Error Response [" + message.id + "]: " + message.error.message);
                        future.completeExceptionally(new RuntimeException("LSP Error: " + message.error.message));
                    } else {
                        System.out.println("[JavaParser] Received Success Response [" + message.id + "]");
                        future.complete(message.result);
                    }
                } else {
                    System.out.println("[JavaParser] Received Response for unknown ID: " + message.id);
                }
            } else if (message.method != null) { 
                System.out.println("[JavaParser] Received Notification: " + message.method);
                if ("window/logMessage".equals(message.method) && message.params != null) {
                    try {
                        LspModels.LogMessageParams logParams = gson.fromJson(message.params, LspModels.LogMessageParams.class);
                        if (logParams != null) {
                            System.out.println("[JDTLS NOTIFICATION LOG] Type " + logParams.type + ": " + logParams.message);
                        } else {
                            System.err.println("[JavaParser] Parsed LogMessageParams as null for window/logMessage. Raw params: " + gson.toJson(message.params));
                        }
                    } catch (JsonSyntaxException e) {
                        System.err.println("[JavaParser] Failed to parse params for window/logMessage: " + e.getMessage() + ". Raw params: " + gson.toJson(message.params));
                    }
                } else if ("textDocument/publishDiagnostics".equals(message.method)) {
                    System.out.println("[JavaParser] Received textDocument/publishDiagnostics. Params: " + prettyGson.toJson(message.params));
                } else {
                    
                    System.out.println("[JavaParser] Unhandled notification method: " + message.method + (message.params != null ? " with params: " + prettyGson.toJson(message.params) : " without params."));
                }
            } else { 
                System.err.println("[JavaParser] Received message that is neither a response nor a known notification type: " + prettyGson.toJson(message));
            }

        } catch (JsonSyntaxException e) {
            System.err.println("[JavaParser] Error processing LSP message due to JSON syntax: " + e.getMessage());
            System.err.println("[JavaParser] Problematic content was: " + content);
            e.printStackTrace();
        } catch (Exception e) { 
            System.err.println("[JavaParser] Unexpected error processing LSP message: " + e.getMessage());
            System.err.println("[JavaParser] Content during error was: " + content);
            e.printStackTrace();
        }
    }


    
    //Performs the LSP initialize handshake. Blocks until the response is received.
    private void initializeServer() throws IOException, ExecutionException, InterruptedException, URISyntaxException {
        System.out.println("[JavaParser] Sending 'initialize' request...");

        String rootUri = pathToUri(PROJECT_ROOT_PATH);
        String workspaceUri = pathToUri(WORKSPACE_PATH); 

        LspModels.InitializeParams initParams = new LspModels.InitializeParams();
        initParams.processId = (int) ProcessHandle.current().pid();
        initParams.clientInfo = new LspModels.ClientInfo();
        initParams.clientInfo.name = "MyIDE";
        initParams.clientInfo.version = "1.0.0";
        initParams.rootUri = rootUri;
        initParams.rootPath = PROJECT_ROOT_PATH; 
        initParams.capabilities = new LspModels.Capabilities();
        initParams.capabilities.textDocument = new LspModels.TextDocumentClientCapabilities();
        initParams.capabilities.textDocument.semanticTokens = new LspModels.SemanticTokensCapabilities();
        initParams.capabilities.textDocument.semanticTokens.dynamicRegistration = true;
        initParams.capabilities.textDocument.semanticTokens.resultFormats = new SemanticTokensCapabilities.ResultFormats[]{SemanticTokensCapabilities.ResultFormats.RELATIVE};
        initParams.capabilities.textDocument.semanticTokens.capabilities = new SemanticTokensCapabilities.Provider();
        initParams.capabilities.textDocument.semanticTokens.capabilities.full = true; 
        initParams.capabilities.textDocument.semanticTokens.capabilities.range = false;

        initParams.capabilities.textDocument.synchronization = new SynchronizationCapabilities();
        initParams.capabilities.textDocument.synchronization.dynamicRegistration = true;
        initParams.capabilities.textDocument.synchronization.willSave = false;
        initParams.capabilities.textDocument.synchronization.willSaveWaitUntil = false;
        initParams.capabilities.textDocument.synchronization.didSave = true; 

        initParams.capabilities.workspace = new LspModels.WorkspaceClientCapabilities();
        initParams.capabilities.workspace.workspaceEdit = new WorkspaceEditCapabilities();
        initParams.capabilities.workspace.workspaceEdit.documentChanges = true;
        initParams.capabilities.workspace.workspaceFolders = true;


        initParams.initializationOptions = new HashMap<>(); 
        initParams.workspaceFolders = List.of(new LspModels.WorkspaceFolder(rootUri, "Project"));
        initParams.locale = "en-US";


        JsonElement params = gson.toJsonTree(initParams);
        CompletableFuture<JsonElement> future = sendRequest("initialize", params);

        try {
            // Add a timeout to prevent indefinite blocking
            JsonElement resultElement = future.get(15, TimeUnit.SECONDS);
            System.out.println("[JavaParser] Received 'initialize' response from server.");

            LspModels.InitializeResult initResult = gson.fromJson(resultElement, LspModels.InitializeResult.class);
            serverCapabilities = initResult.capabilities;

            // --- Store the semantic token legend (your existing logic) ---
            if (serverCapabilities != null &&
                serverCapabilities.semanticTokensProvider != null &&
                serverCapabilities.semanticTokensProvider.legend != null &&
                serverCapabilities.semanticTokensProvider.legend.tokenTypes != null) {
                this.semanticTokenTypesLegend = serverCapabilities.semanticTokensProvider.legend.tokenTypes;
                System.out.println("[JavaParser] Stored Semantic Token Types Legend: " + semanticTokenTypesLegend);
            } else {
                System.err.println("[JavaParser] WARNING: Server did not provide a semantic token types legend.");
                semanticTokenTypesLegend = Collections.emptyList();
            }
            // --- End storing legend ---

            System.out.println("[JavaParser] Full Server Capabilities: " + prettyGson.toJson(initResult.capabilities));

            sendNotification("initialized", JsonParser.parseString("{}"));
            System.out.println("[JavaParser] Sent 'initialized' notification to server.");

        } catch (TimeoutException e) {
            System.err.println("[JavaParser CRITICAL] Timeout waiting for 'initialize' response from JDTLS. Server may be unresponsive or handshake failed.");
            e.printStackTrace();
            throw new IOException("Timeout waiting for JDTLS initialize response", e);
        } catch (ExecutionException e) {
            System.err.println("[JavaParser CRITICAL] Failed to get 'initialize' result: " + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()));
            if (e.getCause() != null) e.getCause().printStackTrace(); else e.printStackTrace();
            throw new IOException("Failed to process JDTLS initialize response", e);
        } catch (InterruptedException e) {
            System.err.println("[JavaParser CRITICAL] 'initialize' request interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
            throw new IOException("JDTLS initialize request interrupted", e); 
        }
    }

    
    //Sends textDocument/didOpen notification.
    public void openFile(String filePath) {
        try {
            String fileUri = pathToUri(filePath);

            // Read content from the temp file JDTLS needs to see
            String fileContent = Files.readString(Paths.get(filePath));

            LspModels.TextDocumentItem textDocument = new LspModels.TextDocumentItem(fileUri, "java", 1, fileContent);
            LspModels.DidOpenTextDocumentParams params = new LspModels.DidOpenTextDocumentParams(textDocument);

            sendNotification("textDocument/didOpen", gson.toJsonTree(params));
            System.out.println("[JavaParser] Sent didOpen for URI: " + fileUri);

        } catch (IOException | URISyntaxException e) {
            System.err.println("[JavaParser] Error sending didOpen for " + filePath + ": " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) { 
            System.err.println("[JavaParser] Unexpected error during openFile for " + filePath + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void updateProjectRoot(String newProjectRootPath) {
        if (newProjectRootPath == null || newProjectRootPath.trim().isEmpty()) {
            System.err.println("[JavaParser] New project root path cannot be null or empty.");
            return;
        }
    
        System.out.println("[JavaParser] Attempting to update project root from '" + PROJECT_ROOT_PATH + "' to: '" + newProjectRootPath + "'");
    
        // Ensure JDTLS process is running and ideally initialized
        if (jdtlsProcess == null || !jdtlsProcess.isAlive()) {
            System.err.println("[JavaParser] JDTLS process is not running. Cannot update project root dynamically.");
            return;
        }
        if (serverCapabilities == null) {
            System.err.println("[JavaParser] JDTLS has not been initialized yet. Deferring project root update or updating variable for subsequent initialization.");
        }
    
        String oldProjectRootUriString = null;
        if (PROJECT_ROOT_PATH != null && !PROJECT_ROOT_PATH.trim().isEmpty()) {
            try {
                oldProjectRootUriString = pathToUri(PROJECT_ROOT_PATH);
            } catch (URISyntaxException e) {
                System.err.println("[JavaParser] Error converting current PROJECT_ROOT_PATH to URI: " + PROJECT_ROOT_PATH + " - " + e.getMessage());
            }
        }

        PROJECT_ROOT_PATH = newProjectRootPath;
    
        try {
            String newProjectRootUriString = newProjectRootPath;
    
            LspModels.WorkspaceFoldersChangeEvent event = new LspModels.WorkspaceFoldersChangeEvent();
            LspModels.WorkspaceFolder newWsFolder = new LspModels.WorkspaceFolder(newProjectRootUriString, "Project Root"); 
    
            event.added = List.of(newWsFolder);
    
            if (oldProjectRootUriString != null) {
                LspModels.WorkspaceFolder oldWsFolder = new LspModels.WorkspaceFolder(oldProjectRootUriString, "Project Root");
                event.removed = List.of(oldWsFolder);
            } else {
                event.removed = Collections.emptyList();
            }
    
            LspModels.DidChangeWorkspaceFoldersParams params = new LspModels.DidChangeWorkspaceFoldersParams(event);
            sendNotification("workspace/didChangeWorkspaceFolders", gson.toJsonTree(params));
    
            System.out.println("[JavaParser] Sent 'workspace/didChangeWorkspaceFolders' notification.");
            if (!event.added.isEmpty()) {
                System.out.println("  Added: " + event.added.get(0).uri);
            }
            if (!event.removed.isEmpty()) {
                System.out.println("  Removed: " + event.removed.get(0).uri);
            }
    
        } catch (IOException e) {
            System.err.println("[JavaParser] Error sending 'workspace/didChangeWorkspaceFolders' notification: " + e.getMessage());
            e.printStackTrace();
        }
    }

    
      //Sends textDocument/didChange notification.
      public void notifyDidChange(String filePath, String newContent, int version) {
        try {
            String fileUri = pathToUri(filePath);

            LspModels.VersionedTextDocumentIdentifier versionedIdentifier = new LspModels.VersionedTextDocumentIdentifier(fileUri, version);
            LspModels.TextDocumentContentChangeEvent changeEvent = new LspModels.TextDocumentContentChangeEvent();
            changeEvent.setText(newContent);
            List<LspModels.TextDocumentContentChangeEvent> changes = List.of(changeEvent);

            LspModels.DidChangeTextDocumentParams params = new LspModels.DidChangeTextDocumentParams();
            params.setTextDocument(versionedIdentifier);
            params.setContentChanges(changes);

            sendNotification("textDocument/didChange", gson.toJsonTree(params));

        } catch (IOException | URISyntaxException e) {
            System.err.println("[JavaParser] Error sending didChange for " + filePath + ": " + e.getMessage());
        } catch (Exception e) {
            System.err.println("[JavaParser] Unexpected error during notifyDidChange for " + filePath + ": " + e.getMessage());
            e.printStackTrace();
        }
    }


    //Sends textDocument/didSave notification.
    public void saveFile(String filePath) {
        try {
           String fileUri = pathToUri(filePath);
           // LspModels.DidSaveTextDocumentParams params = new LspModels.DidSaveTextDocumentParams(new LspModels.TextDocumentIdentifier(fileUri), content);
           LspModels.DidSaveTextDocumentParams params = new LspModels.DidSaveTextDocumentParams(new LspModels.TextDocumentIdentifier(fileUri));


           sendNotification("textDocument/didSave", gson.toJsonTree(params));
            System.out.println("[JavaParser] Sent didSave for URI: " + fileUri);

       } catch (IOException | URISyntaxException e) {
           System.err.println("[JavaParser] Error sending didSave for " + filePath + ": " + e.getMessage());
           e.printStackTrace();
       } catch (Exception e) {
           System.err.println("[JavaParser] Unexpected error during saveFile for " + filePath + ": " + e.getMessage());
           e.printStackTrace();
       }
    }

    //Sends textDocument/didClose notification.
    public void closeFile(String filePath) {
        try {
            String fileUri = pathToUri(filePath);
            LspModels.DidCloseTextDocumentParams params = new LspModels.DidCloseTextDocumentParams(new LspModels.TextDocumentIdentifier(fileUri));

            sendNotification("textDocument/didClose", gson.toJsonTree(params));
            System.out.println("[JavaParser] Sent didClose for URI: " + fileUri);

        } catch (IOException | URISyntaxException e) {
            System.err.println("[JavaParser] Error sending didClose for " + filePath + ": " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
           System.err.println("[JavaParser] Unexpected error during closeFile for " + filePath + ": " + e.getMessage());
           e.printStackTrace();
       }
    }


    //Requests semantic tokens for the previously opened file.
    public CompletableFuture<LspModels.SemanticTokens> requestSemanticTokens(String filePath) {

        CompletableFuture<LspModels.SemanticTokens> future = new CompletableFuture<>();
    
        boolean hasCaps = serverCapabilities != null;
        boolean hasProvider = hasCaps && serverCapabilities.semanticTokensProvider != null;
        boolean hasFullSupport = hasProvider && serverCapabilities.semanticTokensProvider.full != null; // Check if the 'full' object is non-null
    
        System.out.println("Server capabilities available: " + hasCaps);
        System.out.println("Semantic tokens provider available: " + hasProvider);
        System.out.println("Full semantic tokens support available: " + hasFullSupport);
    
    
        try {
            
            if (!hasFullSupport) {
                System.err.println("[JavaParser] Server capabilities indicate no full semantic tokens support.");
                future.complete(null);
                return future;
            }
    
            String fileUri = pathToUri(filePath);
            //System.out.println(fileUri);
            LspModels.TextDocumentIdentifier textDocument = new LspModels.TextDocumentIdentifier(fileUri);
            LspModels.SemanticTokensParams params = new LspModels.SemanticTokensParams(textDocument);
    
            CompletableFuture<JsonElement> jsonFuture = sendRequest(
                "textDocument/semanticTokens/full", 
                gson.toJsonTree(params)
            );
    
            // Chain the transformation
            jsonFuture.thenAccept(resultElement -> {
                if (resultElement == null || resultElement.isJsonNull()) {
                    System.err.println("[JavaParser] Received null result for semantic tokens.");
                    future.complete(null);
                    return;
                }
                
                try {
                    //System.out.println("[JavaParser] ResultElement: " + resultElement);
                    LspModels.SemanticTokens tokens = gson.fromJson(resultElement, LspModels.SemanticTokens.class);
                    //System.out.println("[JavaParser] Received semantic tokens for " + filePath + 
                    //    ". Token count: " + (tokens != null && tokens.data != null ? tokens.data.size() : 0));
    
                    if (tokens != null && tokens.data != null) {
                        //System.out.println("Raw Token Data (first 10 entries):");
                        for (int i = 0; i < Math.min(tokens.data.size(), 50); i+=5) {
                            if (i + 4 < tokens.data.size()) {
                                /*System.out.println("  [%d, %d, %d, %d, %d]%n"+
                                        tokens.data.get(i) +     // deltaLine
                                        tokens.data.get(i+1)+   // deltaStartChar
                                        tokens.data.get(i+2) +   // length
                                        tokens.data.get(i+3)+   // tokenType
                                        tokens.data.get(i+4)    // tokenModifiers
                                );*/
                            }
                        }
                        //if (tokens.data.size() > 50) System.out.println("...");
                    }
                    
                    future.complete(tokens);
                } catch (JsonSyntaxException e) {
                    System.err.println("[JavaParser] Failed to parse semantic tokens result: " + e.getMessage());
                    future.completeExceptionally(e);
                }
            }).exceptionally(e -> {
                System.err.println("[JavaParser] Error requesting semantic tokens: " + e.getMessage());
                future.completeExceptionally(e);
                return null;
            });
    
        } catch (IOException | URISyntaxException e) {
            System.err.println("[JavaParser] Error preparing semantic tokens request for " + filePath + ": " + e.getMessage());
            future.completeExceptionally(e);
        }
        
        return future;
    }



    // Reads the content of a file into a string.
    private String readFileContent(String filePath) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append('\n'); // Preserve line breaks
            }
        }
         // Remove the trailing newline if it was added from the last line
         if (content.length() > 0 && content.charAt(content.length() - 1) == '\n') {
             content.setLength(content.length() - 1);
         }
        return content.toString();
    }


    //Converts a file path to a file:
    private String pathToUri(String filePath) throws URISyntaxException {
         // Ensure path uses correct separators for the current OS
         File file = new File(filePath);
         return file.toURI().toString();
    }


    // Shuts down the JDTLS process and cleans up resources. Should be called when the application exits.
    public void shutdown() {
        System.out.println("[JavaParser] Shutting down JDTLS...");
        running = false; // Signal the worker thread to stop its loop

        try {
            // Send 'shutdown' request
            CompletableFuture<JsonElement> shutdownFuture = sendRequest("shutdown", JsonParser.parseString("{}"));

            // Wait for the shutdown response (max 5 seconds)
            try {
                 shutdownFuture.get(5, TimeUnit.SECONDS);
                 System.out.println("[JavaParser] Received 'shutdown' response.");
            } catch (TimeoutException e) {
                 System.err.println("[JavaParser] Timeout waiting for 'shutdown' response.");
            } catch (Exception e) {
                 System.err.println("[JavaParser] Error during 'shutdown' request: " + e.getMessage());
            }


            // Send 'exit' notification
            sendNotification("exit", JsonParser.parseString("{}"));

        } catch (IOException e) {
            System.err.println("[JavaParser] Error sending shutdown/exit messages: " + e.getMessage());
        } finally {
             // Ensure process is destroyed even if messages failed
             shutdownJdtls();

            // Interrupt the worker thread if it's still alive (e.g., blocked reading)
            if (workerThread != null && workerThread.isAlive()) {
                workerThread.interrupt();
                 try {
                     workerThread.join(2000); // Wait a bit for it to finish
                 } catch (InterruptedException e) {
                    System.err.println("[JavaParser] Shutdown interrupted.");
                 }
            }
        }
    }


    // Destroys the JDTLS process and closes streams.
    private void shutdownJdtls() {
        if (jdtlsProcess != null) {
            System.out.println("[JavaParser] Destroying JDTLS process (PID: " + jdtlsProcess.pid() + ")");
            try {
                // Give it a moment to exit gracefully after 'exit' notification
                if (!jdtlsProcess.waitFor(2, TimeUnit.SECONDS)) {
                    System.out.println("[JavaParser] JDTLS did not exit gracefully, forcing destroy.");
                    jdtlsProcess.destroyForcibly();
                }
            } catch (InterruptedException e) {
                System.err.println("[JavaParser] Error waiting for or destroying JDTLS process: " + e.getMessage());
                jdtlsProcess.destroyForcibly();
            }
            jdtlsProcess = null;
        }

        // Close streams
        try { if (serverInputWriter != null) serverInputWriter.close(); } catch (IOException e) { e.printStackTrace(); }
        try { if (serverOutputReader != null) serverOutputReader.close(); } catch (IOException e) { e.printStackTrace(); }
        try { if (serverOutputBufferedReader != null) serverOutputBufferedReader.close(); } catch (IOException e) { e.printStackTrace(); }

        serverInputWriter = null;
        serverOutputReader = null;
        serverOutputBufferedReader = null;

        System.out.println("[JavaParser] JDTLS shutdown complete.");
    }
}


