import CodeExecution.ByteMapClassLoader;
import CodeExecution.CodeExecutor;
import DataObjects.FunctionTest;
import DataObjects.TestResult;
import IPC.ClassBundleReader;
import IPC.ResultBundleWriter;


import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Map;

public class Main {
    public static void main(String[] args){
        FunctionTest[] arr;
        try (ObjectInputStream objInput = new ObjectInputStream(new FileInputStream("/tests/tests.bin"))){
            arr = (FunctionTest[]) objInput.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        while (true){
            try {
                Map<String, byte[]> classMap = ClassBundleReader.readBundle(System.in);
                ByteMapClassLoader classLoader =
                        new ByteMapClassLoader(ByteMapClassLoader.class.getClassLoader(), classMap);
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
