package DataObjects;

public record TestResult(boolean passed, String actualOutput, String expectedOutput,
                         long runtimeMillis, FunctionTest testCase, double points) {

    @Override
    public String toString(){
        return testCase.toString() + '\n' +
                String.format("expectedOutput=%s, actualOutput=%s, passed=%b, runtime=%d, points=%.2f",
                        expectedOutput, actualOutput, passed, runtimeMillis, points);
    }
}
