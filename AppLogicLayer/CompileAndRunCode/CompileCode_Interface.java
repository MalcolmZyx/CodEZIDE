package AppLogicLayer.CompileAndRunCode;
import javax.swing.JTextArea;
import AppLogicLayer.CompileAndRunCode.CaRCFacade;
import AppLogicLayer.CompileAndRunCode.CompilerCheck.language;

public interface CompileCode_Interface {
    public int compileCode(JTextArea simpleOutput, String sourcePath, String compilerPath, String fileName, language langName);
}
