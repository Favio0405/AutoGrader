package FileManipulation;

import DataObjects.Submission;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class  ZipExtractor{

    private static void unzip(String zipPath, String outPath) throws IOException {
        Path outDir = Paths.get(outPath);
        Files.createDirectories(outDir);

        Path outRoot = outDir.toRealPath();

        try (ZipInputStream zipIn = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipPath)))) {
            ZipEntry entry;
            while ((entry = zipIn.getNextEntry()) != null) {
                String rawName = entry.getName();

                if (rawName.isBlank()) {
                    zipIn.closeEntry();
                    throw new IOException("Blocked zip entry with blank name");
                }

                Path entryPath = Paths.get(rawName);
                if (entryPath.isAbsolute()
                        || rawName.startsWith("\\")
                        || rawName.startsWith("/")
                        || rawName.matches("^[a-zA-Z]:.*")) {
                    zipIn.closeEntry();
                    throw new IOException("Blocked zip entry with absolute path: " + rawName);
                }

                Path target = outRoot.resolve(rawName).normalize();
                if (!target.startsWith(outRoot)) {
                    zipIn.closeEntry();
                    throw new IOException("Blocked Zip Slip entry: " + rawName);
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(target);
                } else {
                    Path parent = target.getParent();
                    if (parent != null) Files.createDirectories(parent);

                    try (BufferedOutputStream bos =
                                 new BufferedOutputStream(Files.newOutputStream(target,
                                         StandardOpenOption.CREATE,
                                         StandardOpenOption.TRUNCATE_EXISTING))) {
                        byte[] buffer = new byte[4096];
                        int read;
                        while ((read = zipIn.read(buffer)) != -1) {
                            bos.write(buffer, 0, read);
                        }
                    }
                }

                zipIn.closeEntry();
            }
        }
    }

    public static void processSubmission(Submission submission){
        String zipPath = submission.getZipFile();
        String outPath = zipPath.substring(0, zipPath.length() - 4) + "Source";
        try {
            unzip(zipPath, outPath);
        } catch (IOException e) {
            System.err.println("Could not unzip file");
            e.printStackTrace();
            System.exit(16);
        }
        submission.setSourceDir(Paths.get(outPath));
    }
}
