package Main;

import org.apache.commons.cli.*;

public class CommandParser {
    public static void main(String[] args){
        Options options = new Options();
        OptionGroup group = new OptionGroup();

        Option singleProgram =
                new Option("p", "program", true, "grade a single program");
        Option batch =
                new Option("b", "batch", false, "grade a batch of programs");

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
            System.out.println("Single program mode");
        }
        else if(cmd.hasOption("b")){
            System.out.println("Batch mode");
        }


    }
}
