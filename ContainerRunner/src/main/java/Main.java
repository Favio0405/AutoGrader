import CodeExecution.ByteMapClassLoader;
import CodeExecution.CodeExecutor;
import DataObjects.FunctionTest;
import DataObjects.TestResult;
import IPC.ClassBundleReader;
import IPC.ResultBundleWriter;


import java.io.IOException;
import java.util.Map;

public class Main {
    public static void main(String[] args){
        while (true){
            try {
                Map<String, byte[]> classMap = ClassBundleReader.readBundle(System.in);
                if (classMap == null){
                    ResultBundleWriter.sendTerminationSignal();
                    return;
                }
                ByteMapClassLoader classLoader =
                        new ByteMapClassLoader(ByteMapClassLoader.class.getClassLoader(), classMap);
                FunctionTest[] arr = new FunctionTest[0];
                CodeExecutor executor = new CodeExecutor(arr, classLoader);
                TestResult[] results = executor.runTests();
                ResultBundleWriter.writeBundle(results);
            } catch (IOException e) {
                throw new RuntimeException("Couldn't read bundle: " + e);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Class not found: " + e);
            }

        }
    }
}
