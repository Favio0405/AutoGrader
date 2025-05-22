package TestObjects;

public class TestResult {
    private final String testName;
    private final boolean passed;
    private final String actualOutput;
    private final String expectedOutput;
    private final String errorMessage;
    private long runtimeMillis;
    private final FunctionTest testCase;

    public TestResult(String testName, boolean passed, String actualOutput,
                      String expectedOutput, String errorMessage, long runtimeMillis, FunctionTest testCase) {
        this.testName = testName;
        this.passed = passed;
        this.actualOutput = actualOutput;
        this.expectedOutput = expectedOutput;
        this.errorMessage = errorMessage;
        this.runtimeMillis = runtimeMillis;
        this.testCase = testCase;
    }
}
