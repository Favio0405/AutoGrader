package Main;

import CodeExecution.CodeExecutor;
import FileReading.FunctionTestBuilder;
import TestObjects.FunctionTest;
import TestObjects.TestResult;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class AutoGrader {
    public static void main(String[] args){
        FunctionTestBuilder functionTestBuilder = new FunctionTestBuilder("test.json");
        FunctionTest[] tests = functionTestBuilder.buildFunctionTests();

        CodeExecutor executor = new CodeExecutor("Test Program", "target\\test-output", tests);
        TestResult[] results = null;

        try(ExecutorService executorService = Executors.newSingleThreadExecutor()) {

            Future<TestResult[]> futureResults = executorService.submit(executor);
            results = futureResults.get();
        }
        catch (InterruptedException | ExecutionException e){
            System.err.println("Submission could not be executed");
        }

        if(results == null){
            System.err.println("Tests could not be executed");
        }
        else {
            for (TestResult t : results) {
                System.out.println(t);
            }
        }
    }
}
