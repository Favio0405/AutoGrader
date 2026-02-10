package IPC;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/*
    CLASS BUNDLE PROTOCOL FORMAT

    Header 4 bytes        = 0x434C5353 = "CLSS"
    Version    int (4 bytes)  = 1
    N      int            = number of classes

    N times repeated:
        name length             int (4 bytes)
        name                    byte[name length]
        num of data bytes       int (4 bytes)
        bytes                   byte[num of data bytes]
 */

public class ClassBundleReader {
    private static final int HEADER = 0x434C5353;
    private static final int TERMINATE = 0x54524D4E;
    private static final int VERSION = 1;
    public static Map<String, byte[]> readBundle(InputStream in) throws IOException {
        DataInputStream dataIn = new DataInputStream(new BufferedInputStream(in, 1 << 20));

        int header = dataIn.readInt();
        if (header == TERMINATE) return null;
        if (header != HEADER) throw new IOException("Wrong header: " + Integer.toHexString(header));

        int version = dataIn.readInt();
        if (version != VERSION) throw new IOException("Unsupported version: " + version);

        int numClasses = dataIn.readInt();
        if (numClasses < 0 || numClasses > 1000) throw new IOException("Suspicious amount of classes: " + numClasses);

        Map<String, byte[]> map = new HashMap<>(16, numClasses * 2);

        for(int i = 0; i < numClasses; i++){
            String name = readClassName(dataIn);
            int dataLen = dataIn.readInt();
            if (dataLen <= 0 || dataLen > 50 * 1024 * 1024)
                throw new IOException("Suspicious class size for class: " + name + " - " + dataLen + " bytes");
            map.put(name, dataIn.readNBytes(dataLen));
        }

        return map;
    }

    private static String readClassName(DataInputStream dataIn) throws IOException {
        int len = dataIn.readInt();
        if (len <= 0 || len > 1024) throw new IOException("Suspicious class name length: " + len);

        return new String(dataIn.readNBytes(len));
    }
}
