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

    public List<TestResult> getResults() {
        return results;
    }

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

    public String getScore() {
        double achievedScore = 0;
        double maxScore = 0;
        for(TestResult t : results){
            maxScore += t.points();
            achievedScore = t.passed() ? achievedScore + t.points() : achievedScore;
        }
        return achievedScore + "/" + maxScore;
    }

    public void addResult(TestResult result) {
        results.add(result);
    }

    @Override
    public String toString(){
        StringBuilder str = new StringBuilder();
        str.append(firstName).append(' ').append(lastName).append(":\n");
        for(TestResult t : results){
            str.append(t).append('\n');
        }
        str.append("Score: ").append(getScore());

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