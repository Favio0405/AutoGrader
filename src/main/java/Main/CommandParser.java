package Main;


import CodeExecution.CodeExecutor;
import FileManipulation.Compiler;
import FileManipulation.FunctionTestBuilder;
import FileManipulation.ZipExtractor;
import TestObjects.FunctionTest;
import TestObjects.Submission;
import org.apache.commons.cli.*;

import java.io.File;

public class CommandParser {
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

        Submission result;
        if(cmd.hasOption("p")){
            result = GradeSingleProgram(cmd);
            System.out.println(result);
        }
        else if(cmd.hasOption("b")){
            System.out.println("Batch mode");
        }


    }

    public static Submission GradeSingleProgram(CommandLine cmd){
        String zipFile = cmd.getOptionValue("p");
        String testFile = cmd.getOptionValue("t");
        FunctionTestBuilder builder = new FunctionTestBuilder(testFile);
        FunctionTest[] tests = builder.buildFunctionTests();
        Submission submission = createSubmission(zipFile);
        ZipExtractor.processSubmission(submission);
        Compiler compiler = new Compiler();
        compiler.processSubmission(submission);
        CodeExecutor codeExecutor = new CodeExecutor(tests);
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

    //In Progress
    /*
    public static List<Submission> GradeBatch(CommandLine cmd){
        Path dirPath = Paths.get(cmd.getOptionValue("b"));

        try(Stream<Path> pathStream = Files.walk(dirPath)) {
            pathStream.filter(path -> path.toString().endsWith(".zip")).forEach();
        } catch (IOException e) {
            System.err.println("Could not iterate over directory");
            e.printStackTrace();
            System.exit(14);
        }
    }
    */
}

