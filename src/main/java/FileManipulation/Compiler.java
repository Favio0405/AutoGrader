package FileManipulation;

import TestObjects.Submission;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Compiler{
    private final JavaCompiler compiler;
    public Compiler(){
        this.compiler = ToolProvider.getSystemJavaCompiler();
        if(compiler == null){
            System.err.println("No Java compiler available");
            System.exit(3);
        }
    }
    private void compileInDirectory(Path sourceDir, Path outputDir){
        try {
            Files.createDirectories(outputDir);
            List<String> sourceFilePaths;
            try(Stream<Path> paths = Files.walk(sourceDir)) {
                sourceFilePaths = paths.map(Path::toString)
                        .filter(string -> string.endsWith(".java"))
                        .collect(Collectors.toList());
            }
            if(sourceFilePaths.isEmpty()){
                System.err.println("No Java files found in " + sourceDir.toAbsolutePath());
                System.exit(4);
            }
            List<String> args = sourceFilePaths;
            args.add(0, outputDir.toString());
            args.add(0, "-d");

            int result = compiler.run(null, null, System.err, args.toArray(new String[0]));
            if(result != 0){
                System.err.println("Compilation failed");
                System.exit(5);
            }
        } catch (IOException e) {
            System.err.println("Could not create file");
            e.printStackTrace();
            System.exit(6);
        }
    }

    public void processSubmission(Submission submission){
        Path sourceDir = submission.getSourceDir();
        String outputPath = sourceDir.toString();
        outputPath = outputPath.substring(0, outputPath.length() - 6) + "Classes";
        Path outputDir = Paths.get(outputPath);
        compileInDirectory(sourceDir, outputDir);

        submission.setClassesDir(outputDir);
    }
}
