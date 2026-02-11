package FileManipulation;

import DataObjects.Submission;

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
            throw new RuntimeException("No Java compiler available");
        }
    }
    private void compileInDirectory(Path sourceDir, Path outputDir) throws IOException{
        Files.createDirectories(outputDir);
        List<String> sourceFilePaths;
        try(Stream<Path> paths = Files.walk(sourceDir)) {
            sourceFilePaths = paths.map(Path::toString)
                    .filter(string -> string.endsWith(".java"))
                    .collect(Collectors.toList());
        }
        if(sourceFilePaths.isEmpty()){
            throw new IOException("No Java files found in " + sourceDir.toAbsolutePath());
        }
        List<String> args = sourceFilePaths;
        args.add(0, outputDir.toString());
        args.add(0, "-d");

        int result = compiler.run(null, null, System.err, args.toArray(new String[0]));
        if(result != 0){
            throw new RuntimeException("Compilation failed");
        }
    }

    public void processSubmission(Submission submission) throws IOException {
        Path sourceDir = submission.getSourceDir();
        String outputPath = sourceDir.toString();
        outputPath = outputPath.substring(0, outputPath.length() - 6) + "Classes";
        Path outputDir = Paths.get(outputPath);
        compileInDirectory(sourceDir, outputDir);

        submission.setClassesDir(outputDir);
    }
}
