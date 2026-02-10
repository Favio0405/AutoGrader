package Main;


import Containerization.ContainerCLI;
import DataObjects.TestResult;
import FileManipulation.Compiler;
import FileManipulation.FunctionTestBuilder;
import FileManipulation.ZipExtractor;
import DataObjects.FunctionTest;
import DataObjects.Submission;
import IPC.ClassBundleWriter;
import IPC.ResultBundleReader;
import ReportGeneration.ReportGenerator;
import ThreadManagement.ThreadManager;
import org.apache.commons.cli.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;
public class AutoGrader {
    public static FunctionTest[] TESTS;
    public static String TESTDIR;
    public static void main(String[] args){
        Options options = new Options();
        OptionGroup group = new OptionGroup();

        Option singleProgram =
                new Option("p", "program", true, "grade a single program");
        Option batch =
                new Option("b", "batch", true, "grade a batch of programs");

        group.addOption(singleProgram);
        group.addOption(batch);
        group.setRequired(true);

        options.addOptionGroup(group);

        Option test =
                new Option("t", "test", true, "Add test file");
        test.setRequired(true);
        Option verbose =
                new Option("v", "verbose", false, "Output program info to command line");
        verbose.setRequired(false);

        options.addOption(test);
        options.addOption(verbose);

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println("Command line error");
            e.printStackTrace();
            System.exit(11);
        }

        TESTDIR = Path.of(cmd.getOptionValue("t")).toAbsolutePath().getParent().toString();

        if(cmd.hasOption("p")){
            Submission result;
            try {
                result = gradeSingleProgram(cmd);
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
            System.out.println(result);
        }
        else if(cmd.hasOption("b")){
            Submission[] submissions = gradeBatch(cmd);
            if(cmd.hasOption("v")){
                for(Submission submission : submissions){
                    System.out.println(submission);
                }
            }
            generateReport(submissions, cmd.getOptionValue("b") + File.separator + "report.xlsx");
        }
    }

    public static Submission gradeSingleProgram(CommandLine cmd) throws IOException, InterruptedException {
        String zipFile = cmd.getOptionValue("p");
        FunctionTestBuilder builder = new FunctionTestBuilder(TESTDIR);
        TESTS = builder.buildFunctionTests();
        Submission submission = createSubmission(zipFile);
        ZipExtractor.processSubmission(submission);
        Compiler compiler = new Compiler();
        compiler.processSubmission(submission);
        String container = "autograder-container" + java.util.UUID.randomUUID();
        ContainerCLI.createAndStart(container, TESTDIR);
        Process p = new ProcessBuilder(
                "docker", "exec", "-i", container,
                "java", "-jar", "/runner/runner-fat.jar"
        ).start();
        OutputStream outStream = p.getOutputStream();
        InputStream inStream = p.getInputStream();
        ClassBundleWriter.writeBundle(outStream , submission.getClassesDir());
        TestResult[] results = ResultBundleReader.readBundle(inStream);
        submission.setResults(results);
        ClassBundleWriter.sendTerminationSignal(outStream);
        if(ResultBundleReader.readBundle(inStream) != null) {
            ContainerCLI.remove(container);
            throw new RuntimeException("Could not terminate container runner");
        }
        return submission;
    }

    public static Submission createSubmission(String zipFile){
        String fileName = zipFile.substring(zipFile.lastIndexOf(File.separator) + 1, zipFile.lastIndexOf('.'));
        String firstName = "";
        String lastName = "";
        for(int i = 1; i < fileName.length(); i++){
            if(Character.isUpperCase(fileName.charAt(i))){
                firstName = fileName.substring(0, i);
                lastName = fileName.substring(i);
            }
        }
        return new Submission(firstName, lastName, zipFile);
    }

    public static Submission[] gradeBatch(CommandLine cmd){
        String testFile = cmd.getOptionValue("t");
        FunctionTestBuilder builder = new FunctionTestBuilder(testFile);
        TESTS = builder.buildFunctionTests();

        Path dirPath = Paths.get(cmd.getOptionValue("b"));
        ThreadManager threadManager = ThreadManager.getInstance();

        try(Stream<Path> pathStream = Files.walk(dirPath)) {
            pathStream.filter(path -> path.toString().endsWith(".zip"))
                    .map(file -> createSubmission(file.toString()))
                    .forEach(threadManager::addSubmission);
        } catch (IOException e) {
            System.err.println("Could not iterate over directory");
            e.printStackTrace();
            System.exit(14);
        }
        threadManager.addSubmission(Submission.NO_INCOMING);
        Submission[] submissions = null;
        try {
            submissions = threadManager.getFuture().get();
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("Could not retrieve grading results");
            e.printStackTrace();
            System.exit(17);
        }
        threadManager.shutdown();
        return submissions;
    }

    public static void generateReport(Submission[] submissions, String filePath){
        ReportGenerator reportGenerator = new ReportGenerator(submissions, TESTS);
        reportGenerator.generateReport(filePath);
    }
}

