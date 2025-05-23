//DeepSeek-V3 version, DeepSeek, 29 Mar. 2025. https://chat.deepseek.com/

/*
 * - Helped me understand Java's processBuilder and Process Classes
 * - Helped me run the compiler and runner in a seperate thread
 * - Helped me make the terminal input interact with the runtime  
 */

package AppLogicLayer.CompileAndRunCode;
import AppLogicLayer.Input_Interface;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import AppLogicLayer.CompileAndRunCode.RunCode_Interface;
import AppLogicLayer.CompileAndRunCode.CompilerCheck.language;

public class RunCode implements RunCode_Interface, Input_Interface {
    
    private static Process currentProcess;
    private static boolean isProcessAlive = false;
    private static BufferedWriter processWriter = null;

    public static String getJavaRuntimePath(String userJavaPath) {
        if(userJavaPath.contains("bin/")){
            return userJavaPath + "java";
        } else if (userJavaPath.contains("bin")){
            return userJavaPath + File.separator + "java";
        } else {
            return userJavaPath + File.separator + "bin" + File.separator + "java";
        }
    }

    @Override
    public void runCode(JTextArea uiOutput, String userSourcePath, String compilerPath, String userFileName, language chosenLang) {
        //if (uiOutput != null) {
        //   uiOutput.append("Starting execution...\n");
        //}

        ProcessBuilder run;
        try {
            if(chosenLang == language.Java){     
                String os = System.getProperty("os.name").toLowerCase();
                
                boolean hasJavaExtension = userFileName.endsWith(".java");
                String javaFileWithoutExtension = hasJavaExtension ? userFileName.replace(".java", "") : userFileName;

                String javaExecPath = getJavaRuntimePath(compilerPath); 
                
                //if (uiOutput != null) {
                //    uiOutput.append("Java Path: " + javaExecPath + "\n");
                //}
                
                if (os.contains("win")) {
                    // Windows command execution
                    run = new ProcessBuilder("cmd.exe", "/c", javaExecPath, javaFileWithoutExtension);
                } else {
                    // Linux/macOS command execution
                    run = new ProcessBuilder("/bin/sh", "-c", javaExecPath + " " + javaFileWithoutExtension);
                }
                
                // Set working directory for both process
                run.directory(new File(userSourcePath));
                
                run.redirectErrorStream(true);

            } else if(chosenLang == language.Cpp) {
                String os = System.getProperty("os.name").toLowerCase();
            
                boolean hasCppExtension = userFileName.endsWith(".cpp");
                String cppFileWithoutExtension = hasCppExtension ? userFileName.replace(".cpp", "") : userFileName;
                String fullCppFileName = hasCppExtension ? userFileName : userFileName + ".cpp";

                String fullCppFilePath = userSourcePath + fullCppFileName;
                String fullOutputFilePath = userSourcePath + cppFileWithoutExtension;

                if (os.contains("win")) {
                    run = new ProcessBuilder("cmd.exe", "/c", "\"" + fullOutputFilePath + ".exe\"");
                } else {
                    run = new ProcessBuilder("/bin/sh", "-c", "\"./" + cppFileWithoutExtension + "\"");
                }
            } else {
                uiOutput.append("Sorry, the system does not support that language yet.\n");
                System.out.println("The system does not support that language yet.");
                return;
            }

            if (uiOutput != null) {
                uiOutput.append("████████████████████████████████ Running: " +  userFileName + " █████████████████████████████████████████████████████████████████████████████████████████████████████████████████████████████████████████████████████████████████████████████████████████████████████████████████████████████\n");
            } 
                
            // Running the compiled Java class
            Process runProcess = run.start();
            currentProcess = runProcess;
            processWriter = new BufferedWriter(new OutputStreamWriter(runProcess.getOutputStream()));
            isProcessAlive = true;

            // Read output in a background thread
            new Thread(() -> {
                try (BufferedReader runOutput = new BufferedReader(new InputStreamReader(runProcess.getInputStream()))) {
                    String outLine;
                    while ((outLine = runOutput.readLine()) != null) {
                        String finalLine = outLine;
                        SwingUtilities.invokeLater(() -> {
                            if (uiOutput != null) {
                            uiOutput.append(finalLine + "\n");
                            }
                        });
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    isProcessAlive = false;
                    if (processWriter != null) {
                        try {
                            processWriter.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        processWriter = null;
                    }
                    currentProcess = null;
                }
            }).start();

            new Thread(() -> {
                try {
                    int runExitCode = runProcess.waitFor();
                    SwingUtilities.invokeLater(() -> {
                        if (uiOutput != null) {
                            uiOutput.append("Program exited with code: " + runExitCode + "\n");
                        }
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    isProcessAlive = false;
                    if (processWriter != null) {
                    try {
                        processWriter.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        processWriter = null;
                    }
                    currentProcess = null;
                }
            }).start();
        } catch (IOException e) {
            e.printStackTrace();
            if (uiOutput != null) {
                uiOutput.append("Error: " + e.getMessage() + "\n");
            }
        }
    }

    @Override
    public boolean sendInputToProcess(String input) {
        if (isProcessRunning() && processWriter != null) {
            try {
                processWriter.write(input);
                processWriter.newLine(); // Add a newline
                processWriter.flush();
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
        return false;
    }

    public boolean isProcessRunning() {
        return isProcessAlive && currentProcess != null && currentProcess.isAlive();
    }
}
