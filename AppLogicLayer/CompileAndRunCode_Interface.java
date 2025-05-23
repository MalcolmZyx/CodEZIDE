package AppLogicLayer;
import javax.swing.JTextArea;
import AppLogicLayer.CompileAndRunCode.CompilerCheck.language;

public interface CompileAndRunCode_Interface {
    public int compileCode(JTextArea simpleOutput, String sourcePath, String compilerPath, String fileName, language langName);
    public void runCode(JTextArea simpleOutput, String sourcePath, String compilerPath, String fileName, language langName);
}

