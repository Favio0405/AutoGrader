package FileManipulation;

import TestObjects.Submission;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Compiler implements Callable<Submission> {
    private final JavaCompiler compiler;
    private final String zipPath;
    private final String outputDirPath;
    public Compiler(String zipPath, String outputDirPath){
        this.compiler = ToolProvider.getSystemJavaCompiler();
        if(compiler == null){
            System.err.println("No Java compiler available");
            System.exit(3);
        }
        this.zipPath = zipPath;
        this.outputDirPath = outputDirPath;
    }
    public void compileInDirectory(){
        Path sourceDir = Paths.get(zipPath);
        Path outputDir = Paths.get(outputDirPath);

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
            args.add(0, outputDirPath);
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

    @Override
    public Submission call() throws Exception {
        String name = zipPath.substring(zipPath.lastIndexOf(File.separator + 1), zipPath.indexOf('.'));
        String firstName = null;
        String lastName = null;
        for(int i = 1; i < name.length(); i++){
            if(Character.isUpperCase(name.charAt(i))){
                firstName = name.substring(0, i);
                lastName = name.substring(i);
            }
        }

        compileInDirectory();

        return new Submission(firstName, lastName);
    }
}
