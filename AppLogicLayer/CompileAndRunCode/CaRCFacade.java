package AppLogicLayer.CompileAndRunCode;
import javax.swing.JTextArea;

import AppLogicLayer.CompileAndRunCode_Interface;
import AppLogicLayer.CompileAndRunCode.CompileCode_Interface;
import AppLogicLayer.CompileAndRunCode.RunCode_Interface;
import AppLogicLayer.CompileAndRunCode.CompilerCheck.language;

// acts as a "dumb" facade; purely used for delgation of tasks
public class CaRCFacade implements CompileAndRunCode_Interface {
    private final CompileCode_Interface compiler = new CompileCode();
    private final RunCode_Interface runner = new RunCode();

    @Override
    public int compileCode(JTextArea simpleOutput, String sourcePath, String compilerPath, String fileName, language langName){
        return compiler.compileCode(simpleOutput, sourcePath, compilerPath, fileName, langName);
    }

    @Override
    public void runCode(JTextArea simpleOutput, String sourcePath, String compilerPath, String fileName, language langName){
        runner.runCode(simpleOutput, sourcePath, compilerPath, fileName, langName);
    }
}
