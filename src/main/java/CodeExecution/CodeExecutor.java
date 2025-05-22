package CodeExecution;

import TestObjects.FunctionTest;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CodeExecutor {
    private final JavaCompiler compiler;
    private final String sourceDirPath;
    private final String outputDirPath;
    private final FunctionTest[] functionTests;

    public CodeExecutor(String sourceDirPath, String outputDirPath, FunctionTest[] functionTests) {
        this.compiler = ToolProvider.getSystemJavaCompiler();
        if(compiler == null){
            System.err.println("No Java compiler available");
            System.exit(3);
        }
        this.sourceDirPath = sourceDirPath;
        this.outputDirPath = outputDirPath;
        this.functionTests = functionTests;
    }

    public void compileInDirectory(){
        Path sourceDir = Paths.get(sourceDirPath);
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
            System.exit(6);
        }
    }

    public void loadClasses(URL[] urls){
        try(URLClassLoader loader = new URLClassLoader(urls)){

        } catch (IOException e) {
            System.err.println("Could not instantiate class loader");
            System.exit(7);
        }

    }

}

