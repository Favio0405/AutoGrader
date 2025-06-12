package CodeExecution;

import TestObjects.FunctionTest;
import TestObjects.TestResult;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CodeExecutor implements Callable<TestResult[]> {
    private final JavaCompiler compiler;
    private final String sourceDirPath;
    private final String outputDirPath;
    private final FunctionTest[] functionTests;

    public CodeExecutor(String sourceDirPath, String outputDirPath, FunctionTest[] functionTests) {
        this.compiler = ToolProvider.getSystemJavaCompiler();
        if(compiler == null){
            System.err.println("No Java compiler available");
            System.exit(3);
        }
        this.sourceDirPath = sourceDirPath;
        this.outputDirPath = outputDirPath;
        this.functionTests = functionTests;
    }

    public void compileInDirectory(){
        Path sourceDir = Paths.get(sourceDirPath);
        Path outputDir = Paths.get(outputDirPath);

        try {
            Files.createDirectories(outputDir);
            List<String> sourceFilePaths;
            try(Stream<Path> paths = Files.walk(sourceDir)) {
                sourceFilePaths = paths.map(Path::toString)
                        .filter(string -> string.endsWith(".java"))
                        .collect(Collectors.toList());
            }
            if(sourceFilePaths.isEmpty()){
                System.err.println("No Java files found in " + sourceDir.toAbsolutePath());
                System.exit(4);
            }
            List<String> args = sourceFilePaths;
            args.add(0, outputDirPath);
            args.add(0, "-d");

            int result = compiler.run(null, null, System.err, args.toArray(new String[0]));
            if(result != 0){
                System.err.println("Compilation failed");
                System.exit(5);
            }
        } catch (IOException e) {
            System.err.println("Could not create file");
            e.printStackTrace();
            System.exit(6);
        }
    }

    public Map< String, Class<?>> loadClasses(){
        Map<String, Class<?>> classes = new HashMap<>();
        Path dir = Paths.get( outputDirPath);
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

        return new TestResult(passed, actualStr, expectedStr, 0, test);
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
    public TestResult[] call() throws Exception {
        compileInDirectory();
        Map<String, Class<?>> classMap = loadClasses();
        return runTests(classMap);
    }
}

