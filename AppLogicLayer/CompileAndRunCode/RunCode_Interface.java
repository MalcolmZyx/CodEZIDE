package AppLogicLayer.CompileAndRunCode;
import javax.swing.JTextArea;
import AppLogicLayer.CompileAndRunCode.CompilerCheck.language;

public interface RunCode_Interface {
    public void runCode(JTextArea simpleOutput, String sourcePath, String compilerPath, String fileName, language langName);
}
