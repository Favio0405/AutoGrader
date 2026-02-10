package CodeExecution;

import DataObjects.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

public class CodeExecutor{
    private final FunctionTest[] functionTests;
    private final ByteMapClassLoader classLoader;

    public CodeExecutor(FunctionTest[] functionTests, ByteMapClassLoader classLoader) {
        this.functionTests = functionTests;
        this.classLoader = classLoader;
    }

    private TestResult executeTest(FunctionTest test, Class<?> functionClass){
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
            return
                    new TestResult(false, "error", (expected == null) ? "null" : expected.toString(),
                            0, error, test, test.scoreVal());
        }
        boolean passed = Objects.deepEquals(result, expected);

        String actualStr = (result == null) ? "null" : result.toString();
        String expectedStr = (expected == null) ? "null" : expected.toString();
        double points = test.scoreVal();
        return new TestResult(passed, actualStr, expectedStr, 0, "none", test, points);
    }

    public TestResult[] runTests() throws ClassNotFoundException {
        TestResult[] results = new TestResult[functionTests.length];
        for (int i = 0; i < results.length; i++) {
            FunctionTest test = functionTests[i];
            results[i] = executeTest(test, classLoader.loadClass(test.className()));
        }
        return results;
    }
}
