package FileManipulation;

import TestObjects.Submission;

import java.io.*;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ZipExtractor{

    private static void unzip(String zipPath, String outPath) throws IOException {
        File outDir = new File(outPath);
        if (!outDir.exists()) {
            outDir.mkdirs();
        }

        try (ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipPath))) {
            ZipEntry entry = zipIn.getNextEntry();
            while (entry != null) {
                String filePath = outDir + File.separator + entry.getName();
                if (!entry.isDirectory()) {
                    extractFile(zipIn, filePath);
                } else {
                    new File(filePath).mkdirs();
                }
                zipIn.closeEntry();
                entry = zipIn.getNextEntry();
            }
        }
    }

    private static void extractFile(ZipInputStream zipIn, String filePath) throws IOException {
        File outFile = new File(filePath);
        File parent = outFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(outFile))) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = zipIn.read(buffer)) != -1) {
                bos.write(buffer, 0, read);
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
