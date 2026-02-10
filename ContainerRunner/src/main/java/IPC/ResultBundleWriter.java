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
        score                       double (8 bytes)
 */


import DataObjects.TestResult;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ResultBundleWriter {
    private static final int HEADER = 0x52534C54;
    private static final int TERMINATE = 0x54524D4E;
    private static final int VERSION = 1;
    public static void writeBundle(TestResult[] results) throws IOException {
        DataOutputStream dataOut = new DataOutputStream(new BufferedOutputStream(System.out, 1 << 20));

        dataOut.write(HEADER);
        dataOut.writeInt(VERSION);
        dataOut.writeInt(results.length);

        for(TestResult result : results) {
            dataOut.writeBoolean(result.passed());
            dataOut.writeInt(result.actualOutput().length());
            dataOut.writeChars(result.actualOutput());
            dataOut.writeInt(result.expectedOutput().length());
            dataOut.writeChars(result.expectedOutput());
            dataOut.writeLong(result.runtimeMillis());
            dataOut.writeInt(result.error().length());
            dataOut.writeChars(result.error());
            dataOut.writeDouble(result.points());
        }
        dataOut.flush();
    }

    public static void sendTerminationSignal() throws IOException {
        DataOutputStream dataOut = new DataOutputStream(new BufferedOutputStream(System.out, 1 << 20));
        dataOut.write(TERMINATE);
        dataOut.flush();
    }
}
