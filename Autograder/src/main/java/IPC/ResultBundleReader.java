package IPC;

/*
                RESULT BUNDLE PROTOCOL FORMAT

    HEADER     4 bytes         =   0x52534C54  =   "RSLT"
    VERSION    int (4 bytes)   =   1
    N          int (4 bytes)   =   Number of test results

    N times repeated:
        passed  boolean             (1 byte)
        actual output length        int (4 bytes)
        actual output               byte[actual output length]
        expected output length      int (4 bytes)
        expected output             byte[expected output length]
        runtime milliseconds        long (8 bytes)
        error length                int (4 bytes)
        error                       byte[error length]
        score                       double (8 b ytes)
 */


import DataObjects.FunctionTest;
import DataObjects.TestResult;
import Main.AutoGrader;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class ResultBundleReader {
    private final static FunctionTest[] TESTS = AutoGrader.TESTS;
    private static final int HEADER = 0x52534C54;
    private static final int VERSION = 1;

    public static TestResult[] readBundle(InputStream in) throws IOException {
        DataInputStream dataIn = new DataInputStream(new BufferedInputStream(in, 1 << 20));

        int header = dataIn.readInt();
        if (header != HEADER) throw new IOException("Wrong header: "
                + header + "\n"
                + new String(dataIn.readAllBytes(), StandardCharsets.UTF_8));

        int version = dataIn.readInt();
        if (version != VERSION) throw new IOException("Unsupported version: " + version);

        int numTests = dataIn.readInt();
        if(numTests <= 0 || numTests > 1024) throw new IOException("Suspicious number of tests: " + numTests);

        TestResult[] results = new TestResult[numTests];

        for(int i = 0; i < numTests; i++){
            boolean passed = dataIn.readBoolean();
            int actualOutputLen = dataIn.readInt();
            String actualOutput = new String(dataIn.readNBytes(actualOutputLen), StandardCharsets.UTF_8);
            int expectedOutputLen = dataIn.readInt();
            String expectedOutput = new String(dataIn.readNBytes(expectedOutputLen), StandardCharsets.UTF_8);
            long runtimeMillis = dataIn.readLong();
            int errorLength = dataIn.readInt();
            String error = new String(dataIn.readNBytes(errorLength), StandardCharsets.UTF_8);
            double score = dataIn.readDouble();
            results[i] =
                    new TestResult(passed, actualOutput, expectedOutput, runtimeMillis, error, TESTS[i], score);
        }

        return results;
    }
}
