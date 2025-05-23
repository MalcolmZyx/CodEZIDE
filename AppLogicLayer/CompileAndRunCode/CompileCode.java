package AppLogicLayer.CompileAndRunCode;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import AppLogicLayer.CompileAndRunCode.CompileCode_Interface;
import AppLogicLayer.CompileAndRunCode.CompilerCheck.language;

public class CompileCode implements CompileCode_Interface {
    public static String getJavaCompilerPath(String userJavaPath) {
        if(userJavaPath.contains("bin/")){
            return userJavaPath + "javac";
        } else if (userJavaPath.contains("bin")){
            return userJavaPath + File.separator + "javac";
        } else {
            return userJavaPath + File.separator + "bin" + File.separator + "javac";
        }
    }

    @Override
    public int compileCode(JTextArea uiOutput, String userSourcePath, String compilerPath, String userFileName, language chosenLang) {
        if (uiOutput != null) {
            uiOutput.append("████████████████████████████████ Starting Compilation ███████████████████████████████████████████████████████████████████████████████████████████████████████████████████████████████████████████████████████████████████████████\n");
            uiOutput.append("Source Path: " + userSourcePath + "\n");
            uiOutput.append("File Name: " + userFileName + "\n");
        }

        ProcessBuilder compile;
        try {
            if(chosenLang == language.Java){
                // check if the file name already has a ".java" at the end or not, and add it if necessary but also keep a var without it
                boolean hasJavaExtension = userFileName.endsWith(".java");
                String fullJavaFileName = hasJavaExtension ? userFileName : userFileName + ".java";
            
                // getting the os of the machine
                String os = System.getProperty("os.name").toLowerCase();

                String javacPath = getJavaCompilerPath(compilerPath);
                
                if (uiOutput != null) {
                    uiOutput.append("Javac Path: " + javacPath + "\n");
                }
                
                if (os.contains("win")) {
                    // Windows command execution
                    compile = new ProcessBuilder("cmd.exe", "/c", javacPath, fullJavaFileName);
                } else {
                    // Linux/macOS command execution
                    compile = new ProcessBuilder("/bin/sh", "-c", javacPath + " " + fullJavaFileName);
                }
                
                // Set working directory for both process
                compile.directory(new File(userSourcePath));
                
                compile.redirectErrorStream(true);

            } else if(chosenLang == language.Cpp) {
                boolean hasCppExtension = userFileName.endsWith(".cpp");
                String cppFileWithoutExtension = hasCppExtension ? userFileName.replace(".cpp", "") : userFileName;
                String fullCppFileName = hasCppExtension ? userFileName : userFileName + ".cpp";
            
                String os = System.getProperty("os.name").toLowerCase();
            
                String fullCppFilePath = userSourcePath + fullCppFileName;
                String fullOutputFilePath = userSourcePath + cppFileWithoutExtension;
                String compileCommand = "g++ \"" + fullCppFilePath + "\" -o \"" + fullOutputFilePath + "\"";

                if (os.contains("win")) {
                    compile = new ProcessBuilder("cmd.exe", "/c", compileCommand);
                } else {
                    compile = new ProcessBuilder("/bin/sh", "-c", compileCommand);
                }
            } else {
                uiOutput.append("Sorry, the system does not support that language yet.\n");
                System.out.println("The system does not support that language yet.");
                return 1;
            }

            if (uiOutput != null) {
                uiOutput.append("Compiling: " + userFileName + "\n");
                uiOutput.append("Compile command: " + String.join(" ", compile.command()) + "\n");
            }
            
            // Compiling the Java file
            Process compileProcess = compile.start();
            
            BufferedReader compileReader = new BufferedReader(new InputStreamReader(compileProcess.getInputStream()));
            String line;
            while ((line = compileReader.readLine()) != null) {
                if (uiOutput != null) {
                    uiOutput.append(line + "\n");
                }
            }
 
            int compileExitCode = compileProcess.waitFor();
            if (uiOutput != null) {
                uiOutput.append("Compilation finished with exit code: " + compileExitCode + "\n");
            }
            return compileExitCode; 

        } catch (IOException | InterruptedException e) {
            if (uiOutput != null) {
                uiOutput.append("Error: " + e.getMessage() + "\n");
            }
        }
        return 1;
    }
}
