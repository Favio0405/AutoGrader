package FileManipulation;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ZipExtractor implements Runnable{
    String zipPath;
    String outPath;

    public ZipExtractor(String zipPath, String outPath) {
        this.zipPath = zipPath;
        this.outPath = outPath;
    }

    public void unzip() throws IOException {
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

    private void extractFile(ZipInputStream zipIn, String filePath) throws IOException {
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

    @Override
    public void run() {
        try {
            unzip();
        } catch (IOException e) {
            System.err.println("Could not unzip file " + zipPath);
            System.exit(13);
        }
    }
}
