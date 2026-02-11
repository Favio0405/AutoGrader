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
import java.nio.charset.StandardCharsets;

public class ResultBundleWriter {
    private static final int HEADER = 0x52534C54;
    private static final int VERSION = 1;
    public static void writeBundle(TestResult[] results) throws IOException {
        DataOutputStream dataOut = new DataOutputStream(new BufferedOutputStream(System.out, 1 << 20));

        dataOut.writeInt(HEADER);
        dataOut.writeInt(VERSION);
        dataOut.writeInt(results.length);

        for(TestResult result : results) {
            dataOut.writeBoolean(result.passed());
            byte[] bytes = result.actualOutput().getBytes(StandardCharsets.UTF_8);
            dataOut.writeInt(bytes.length);
            dataOut.write(bytes);
            bytes = result.expectedOutput().getBytes(StandardCharsets.UTF_8);
            dataOut.writeInt(bytes.length);
            dataOut.write(bytes);
            dataOut.writeLong(result.runtimeMillis());
            bytes = result.error().getBytes(StandardCharsets.UTF_8);
            dataOut.writeInt(bytes.length);
            dataOut.write(bytes);
            dataOut.writeDouble(result.points());
        }
        dataOut.flush();
    }
}
