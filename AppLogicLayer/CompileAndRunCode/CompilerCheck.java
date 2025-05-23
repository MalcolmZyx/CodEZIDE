package AppLogicLayer.CompileAndRunCode;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import com.codezide.ui.panels.RightPanel;

public class CompilerCheck {
    public static enum language {
        Java, Cpp, Python, Other;
    }

    private String userJavaPath; 
    private static RightPanel rightPanel;

    public static void setRightPanel(RightPanel _rightPanel){
        rightPanel = _rightPanel;
    }

    public static String getJavaCompilerPath(String userJavaPath) {
        if(userJavaPath.contains("bin/")){
            String path = userJavaPath + "javac";
            return path;
        } else if (userJavaPath.contains("bin")){
            String path = userJavaPath + File.separator + "javac";
            return path;
        } else {
            String path = userJavaPath + File.separator + "bin" + File.separator + "javac";
            return path;
        }
    }

    public static String getJavaRuntimePath(String userJavaPath) {
        if(userJavaPath.contains("bin/")){
            return userJavaPath + "java";
        } else if (userJavaPath.contains("bin")){
            return userJavaPath + File.separator + "java";
        } else {
            return userJavaPath + File.separator + "bin" + File.separator + "java";
        }
    }


    public static boolean isCompilerInPath(language chosenLang, JTextArea uiOutput) {
        String compilerUsed = ""; 
        String versionType = "-version";
        if(chosenLang == language.Java)
        {
            compilerUsed = "javac";
        } 
        else if (chosenLang == language.Cpp)
        {
            compilerUsed = "g++";
            versionType = "-" + versionType;
        } else {
            uiOutput.append("That language is not supported yet and/or not downloaded on your device.\n");
            return false; 
        }

        // create a ProcessBuilder to run the "javac -version"/"g++ --version" command
        ProcessBuilder processBuilder = new ProcessBuilder(compilerUsed, versionType);
        processBuilder.redirectErrorStream(true);

        try {
            Process process = processBuilder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = reader.readLine();

            int exitCode = process.waitFor();

            // if the exit code is 0 and the output contains "javac"/"g++", the compiler is in PATH
            if (exitCode == 0 && line != null && line.contains(compilerUsed)) {
                return true;
            }
        } catch (IOException | InterruptedException e) {
            uiOutput.append("Sorry, something went wrong...\n");
            System.err.println("Error checking for " + compilerUsed + ": " + e.getMessage());
        }

        // if we reach here, compiler is not in PATH
        return false;
    } 

    public static String findbinPath(language chosenLang, JTextArea uiOutput) {
        String langName;   
        ProcessBuilder pb;     
        if(chosenLang == language.Java)
        {
            langName = "Java";
            pb = new ProcessBuilder("java", "-XshowSettings:properties", "-version");
        } 
        else if (chosenLang == language.Cpp)
        {
            langName = "g++";
            pb = new ProcessBuilder("g++", "-v");
        } else {
            uiOutput.append("The system does not support that language yet.\n");
            System.out.println("The system does not support that language yet.");
            return "";
        }

        try {
            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            String line;
            String binPath = null;

            // look for the line that contains "bin" in both streams
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.contains("bin")) {
                    binPath = extractPath(line); //line.split("=")[1].trim();  
                    if (binPath != null) break;
                }
            }
            while ((line = errorReader.readLine()) != null) {
                line = line.trim();
                if (line.contains("bin")) {
                    binPath = extractPath(line); //line.split("=")[1].trim();
                    if (binPath != null) break;
                }
            }
            if(line == null && binPath == null){
                uiOutput.append("Error: couldn't find or parse the bin path for " + langName + ".\n");
                System.out.println("Error: couldn't find or parse the bin path for " + langName + ".");
                return ""; 
            }

            int exitCode = process.waitFor();

            // Debugging output
            System.out.println("Command: " + String.join(" ", pb.command()));
            System.out.println("Exit Code: " + exitCode);
            uiOutput.append("Your " + langName + " bin path was found at: " + binPath + "\n");
            System.out.println(langName + " Home Path: " + binPath);
            JTextField compLoc = rightPanel.GetCompilerLocation();
            compLoc.setText(binPath);

            // check if we found a valid path
            if (exitCode == 0 && binPath != null && !binPath.isEmpty()) {
                return binPath;
            } else {
                uiOutput.append("Failed to find your " + langName + " bin path.\n");
                System.err.println("Failed to find " + langName + " bin path. Exit code: " + exitCode);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }
    
    // Extract the first valid (binary) path from a string
    private static String extractPath(String line) {
        line = line.replace("\\ ", " "); // for when there are spaces in a folder/file name like "Program\ files"
        //System.out.println(line);
        int start = line.indexOf(":")-1; // if not found it will be -2 (-1 - 1)
        if(start == -2){ // for Linux/MacOS - when ":" is not found
            start = line.indexOf("/"); 
        }
        int binIndex = line.toLowerCase().indexOf("bin", start);
        if (start > -1 && binIndex != -1) {
            line = line.replace("\\", "/");
            String binPath = line.substring(start, binIndex + 3); // Extract up to "/bin/"
            if (new File(binPath).isDirectory()) { 
                return binPath;
            } else {
                System.out.println("No valid bin path was found.");
            }
        }
        return null;
    }    
}
