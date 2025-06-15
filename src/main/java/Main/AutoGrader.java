package Main;


import CodeExecution.CodeExecutor;
import FileManipulation.Compiler;
import FileManipulation.FunctionTestBuilder;
import FileManipulation.ZipExtractor;
import DataObjects.FunctionTest;
import DataObjects.Submission;
import ThreadManagement.ThreadManager;
import org.apache.commons.cli.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

public class AutoGrader {
    public static FunctionTest[] TESTS;
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

        options.addOption(test);


        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println("Command line error");
            e.printStackTrace();
            System.exit(11);
        }

        if(cmd.hasOption("p")){
            Submission result = GradeSingleProgram(cmd);
            System.out.println(result);
        }
        else if(cmd.hasOption("b")){
            Submission[] results = GradeBatch(cmd);
            for(Submission submission : results){
                System.out.println(submission);
            }
        }
    }

    public static Submission GradeSingleProgram(CommandLine cmd){
        String zipFile = cmd.getOptionValue("p");
        String testFile = cmd.getOptionValue("t");
        FunctionTestBuilder builder = new FunctionTestBuilder(testFile);
        TESTS = builder.buildFunctionTests();
        Submission submission = createSubmission(zipFile);
        ZipExtractor.processSubmission(submission);
        Compiler compiler = new Compiler();
        compiler.processSubmission(submission);
        CodeExecutor codeExecutor = new CodeExecutor(TESTS);
        codeExecutor.processSubmission(submission);
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

    public static Submission[] GradeBatch(CommandLine cmd){
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
}

