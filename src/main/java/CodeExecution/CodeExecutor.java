package CodeExecution;

import DataObjects.FunctionTest;
import DataObjects.Submission;
import DataObjects.TestResult;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

public class CodeExecutor{
    private final FunctionTest[] functionTests;

    public CodeExecutor(FunctionTest[] functionTests) {
        this.functionTests = functionTests;
    }

    private Map< String, Class<?>> loadClasses(Path dir){
        Map<String, Class<?>> classes = new HashMap<>();
        try(URLClassLoader loader = new URLClassLoader(new URL[]{dir.toUri().toURL()})){
            try(Stream<Path> pathStream = Files.walk(dir)){
                pathStream.filter(path -> path.toString().endsWith(".class"))
                        .forEach(path -> {
                            String className = dir.relativize(path)
                                    .toString()
                                    .replace(FileSystems.getDefault().getSeparator(), ".")
                                    .replaceAll("\\.class$", "");
                            try {
                                classes.put(className, loader.loadClass(className));
                            } catch (ClassNotFoundException e) {
                                System.err.println("Class " + className + " not found");
                                e.printStackTrace();
                                System.exit(7);
                            }
                        });
            }
        } catch (IOException e) {
            System.err.println("Could not instantiate class loader");
            e.printStackTrace();
            System.exit(8);
        }

        return classes;
    }

    private void executeTest(FunctionTest test, Class<?> functionClass, Submission submission){
        String className = test.className();
        String methodName = test.methodName();
        Class<?>[] paramTypes = test.paramTypes();
        Object[] args = test.args();
        Object expected = test.expected();
        Object result = null;
        try{
            Method method = functionClass.getDeclaredMethod(methodName, paramTypes);
            Object instance = null;
            if(!Modifier.isStatic(method.getModifiers())){
                instance = functionClass.getDeclaredConstructor().newInstance();
            }
            result = method.invoke(instance, args);
        }
        catch (NoSuchMethodException | IllegalAccessException e){
            System.err.println("Method " + methodName + " could not be invoked");
            e.printStackTrace();
            System.exit(9);
        }
        catch (InstantiationException e) {
            System.err.println("Could not instantiate class " + className);
            e.printStackTrace();
            System.exit(10);
        }
        catch (InvocationTargetException e){
            Throwable realException = e.getCause();
            String error = realException.getClass().getName();
            submission.addResult
                    (new TestResult(false, "error", (expected == null) ? "null" : expected.toString(),
                            0, error, test, test.scoreVal()));
            return;
        }
        boolean passed = Objects.deepEquals(result, expected);

        String actualStr = (result == null) ? "null" : result.toString();
        String expectedStr = (expected == null) ? "null" : expected.toString();
        double points = test.scoreVal();
        submission.addResult(new TestResult(passed, actualStr, expectedStr, 0, "none", test, points));
    }

    private void runTests(Map<String, Class<?>> classMap, Submission submission){
        for (FunctionTest test : functionTests) {
            executeTest(test, classMap.get(test.className()), submission);
        }
    }

    public void processSubmission(Submission submission){
        Map<String, Class<?>> classMap = loadClasses(submission.getClassesDir());
        runTests(classMap, submission);
    }
}

