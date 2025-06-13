package CodeExecution;

import TestObjects.FunctionTest;
import TestObjects.Submission;
import TestObjects.TestResult;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

public class CodeExecutor implements Callable<Submission> {
    private final String dirPath;
    private final FunctionTest[] functionTests;

    public CodeExecutor(String dirPath, FunctionTest[] functionTests) {
        this.dirPath = dirPath;
        this.functionTests = functionTests;
    }

    public Map< String, Class<?>> loadClasses(){
        Map<String, Class<?>> classes = new HashMap<>();
        Path dir = Paths.get(dirPath);
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

    public TestResult executeTest(FunctionTest test, Class<?> functionClass){
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
        catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e){
            System.err.println("Method " + methodName + " could not be invoked");
            e.printStackTrace();
            System.exit(9);
        }catch (InstantiationException e) {
            System.err.println("Could not instantiate class " + className);
            e.printStackTrace();
            System.exit(10);
        }
        boolean passed = Objects.deepEquals(result, expected);

        String actualStr = (result == null) ? "null" : result.toString();
        String expectedStr = (expected == null) ? "null" : expected.toString();
        double points = test.scoreVal();
        return new TestResult(passed, actualStr, expectedStr, 0, test, points);
    }

    public TestResult[] runTests(Map<String, Class<?>> classMap){
        TestResult[] results = new TestResult[functionTests.length];
        for(int i = 0; i < results.length; i++){
            FunctionTest test = functionTests[i];
            results[i] = executeTest(test, classMap.get(test.className()));
        }

        return results;
    }

    @Override
    public Submission call() throws Exception {
        Map<String, Class<?>> classMap = loadClasses();
        return null;
    }
}

