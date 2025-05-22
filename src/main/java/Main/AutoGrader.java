package Main;

import CodeExecution.CodeExecutor;
import FileReading.FunctionTestBuilder;
import TestObjects.FunctionTest;

public class AutoGrader {
    public static void main(String[] args){
        FunctionTestBuilder functionTestBuilder = new FunctionTestBuilder("test.json");
        FunctionTest[] tests = functionTestBuilder.buildFunctionTests();
        for(FunctionTest test : tests){
            System.out.println(test);
        }

        CodeExecutor executor = new CodeExecutor("Test Program", "target\\test-output", tests);
        executor.compileInDirectory();

    }
}
