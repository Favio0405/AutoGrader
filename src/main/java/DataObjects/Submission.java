package DataObjects;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Submission{
    public static final Submission SHUTDOWN = new Submission();
    public static final Submission NO_INCOMING = new Submission();
    private final String zipFile;
    private Path sourceDir;
    private Path classesDir;
    private final String firstName;
    private final String lastName;
    private final List<TestResult> results;

    public Submission(String firstName, String lastName, String zipFile) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.zipFile = zipFile;
        results = new ArrayList<>();
    }

    private Submission(){
     zipFile = "";
     firstName = "";
     lastName = "";
     results = new ArrayList<>();
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public double getScore() {
        double points = 0;
        for(TestResult t : results){
            if(t.passed()) points += t.points();
        }
        return points;
    }

    public void addResult(TestResult result) {
        results.add(result);
    }

    @Override
    public String toString(){
        StringBuilder str = new StringBuilder();
        str.append(firstName).append(' ').append(lastName).append(":\n");
        double achievedScore = 0;
        double maxScore = 0;
        for(TestResult t : results){
            str.append(t).append('\n');
            maxScore += t.points();
            achievedScore = t.passed() ? achievedScore + t.points() : achievedScore;
        }
        str.append("Score: ").append(achievedScore).append('/').append(maxScore);

        return str.toString();
    }

    public String getZipFile() {
        return zipFile;
    }

    public Path getSourceDir() {
        return sourceDir;
    }

    public Path getClassesDir() {
        return classesDir;
    }

    public void setSourceDir(Path sourceDir) {
        this.sourceDir = sourceDir;
    }

    public void setClassesDir(Path classesDir) {
        this.classesDir = classesDir;
    }
}