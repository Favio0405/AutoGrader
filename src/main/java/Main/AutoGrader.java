package Main;

import CodeExecution.CodeExecutor;
import FileReading.FunctionTestBuilder;
import TestObjects.FunctionTest;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class AutoGrader {
    public static void main(String[] args){
        FunctionTestBuilder functionTestBuilder = new FunctionTestBuilder("test.json");
        FunctionTest[] tests = functionTestBuilder.buildFunctionTests();
        for(FunctionTest test : tests){
            System.out.println(test);
        }

        CodeExecutor executor = new CodeExecutor("Test Program", "target\\test-output", tests);
        executor.compileInDirectory();

        List<Class<?>> classes = executor.loadClasses(Paths.get("target\\test-output"));

        for(Class<?> c : classes){
            System.out.println(c.getName());
        }
    }
}
