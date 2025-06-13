package TestObjects;

public class Submission{
    private final String firstName;
    private final String lastName;
    private double score;

    public Submission(String firstName, String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;
        score = 0;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }
}