package TestObjects;

public record TestResult(boolean passed, String actualOutput, String expectedOutput,
                         long runtimeMillis, FunctionTest testCase) {
}
