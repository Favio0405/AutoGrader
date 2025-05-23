package TestObjects;

import java.util.Arrays;
import java.util.stream.Collectors;

public record FunctionTest(String className, String methodName, Class<?>[] paramTypes, Object[] args, Object expected) {

    @Override
    public String toString() {
        String params = Arrays.stream(paramTypes)
                .map(Class::getSimpleName)
                .collect(Collectors.joining(",", "[", "]"));
        String arguments = Arrays.stream(args)
                .map(FunctionTest::argToString)
                .collect(Collectors.joining(",", "[", "]"));
        String expectedStr = expected != null && expected.getClass().isArray()
                ? arrayToString(expected)
                : String.valueOf(expected);
        return String.format(
                "FunctionTest{className='%s', methodName='%s', paramTypes=%s, args=%s, expected=%s}",
                className, methodName, params, arguments, expectedStr
        );
    }

    private static String argToString(Object arg) {
        if (arg == null) return "null";
        if (arg.getClass().isArray()) return arrayToString(arg);
        return String.valueOf(arg);
    }

    private static String arrayToString(Object array) {
        if (array instanceof Object[])
            return Arrays.deepToString((Object[]) array);
        if (array instanceof int[])
            return Arrays.toString((int[]) array);
        if (array instanceof long[])
            return Arrays.toString((long[]) array);
        if (array instanceof double[])
            return Arrays.toString((double[]) array);
        if (array instanceof boolean[])
            return Arrays.toString((boolean[]) array);
        return array.toString();
    }
}
