package IPC;

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

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ClassBundleWriter {
    private static final int HEADER = 0x434C5353;
    private static final int VERSION = 1;
    private static final int TERMINATE = 0x54524D4E;

    public static void writeBundle(OutputStream out, Path classesDir) throws IOException{
        List<Path> classFiles;
        try (Stream<Path> stream = Files.walk(classesDir)) {
            classFiles = stream.filter(p -> p.toString().endsWith(".class"))
                    .collect(Collectors.toList());
        }

        DataOutputStream dataOut = new DataOutputStream(new BufferedOutputStream(out, 1 << 20));

        dataOut.writeInt(HEADER);
        dataOut.writeInt(VERSION);
        dataOut.writeInt(classFiles.size());

        for(Path p : classFiles){
            String className = getClassName(classesDir, p);
            byte[] bytes = Files.readAllBytes(p);
            dataOut.writeInt(className.length());
            dataOut.writeChars(className);
            dataOut.writeInt(bytes.length);
            dataOut.write(bytes);
        }
        dataOut.flush();
    }
    public static void sendTerminationSignal(OutputStream out) throws IOException {
        DataOutputStream dataOut = new DataOutputStream(new BufferedOutputStream(out, 1 << 20));
        dataOut.write(TERMINATE);
        dataOut.flush();
    }
    private static String getClassName(Path root, Path classFile){
        return root.relativize(classFile)
                .toString()
                .replace(FileSystems.getDefault().getSeparator(), ".")
                .replaceAll("\\.class$", "");
    }
}
