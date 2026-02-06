package ReportGeneration;

import DataObjects.FunctionTest;
import DataObjects.Submission;
import DataObjects.TestResult;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;

public class ReportGenerator {

    private final Submission[] submissions;
    private final FunctionTest[] tests;

    public ReportGenerator(Submission[] submissions, FunctionTest[] tests) {
        this.submissions = submissions;
        this.tests = tests;
    }

    public void generateReport(String filePath){
        try(Workbook workbook = new XSSFWorkbook();
            FileOutputStream outStream = new FileOutputStream(filePath)){
            generateSheet(workbook);

            workbook.write(outStream);
        } catch (IOException e) {
            System.err.println("Could not generate .xlsx file");
            e.printStackTrace();
            System.exit(18);
        }
    }

    private void generateSheet(Workbook workbook){
        Sheet sheet = workbook.createSheet("Grading Report");
        Row headers = sheet.createRow(0);
        headers.createCell(0).setCellValue("Name");
        int i;
        for(i = 0; i < tests.length; i++){
            headers.createCell(i + 1).setCellValue(tests[i].testName());
        }
        headers.createCell(i + 1).setCellValue("Score");

        i = 1;

        for(Submission submission : submissions){
            Row row = sheet.createRow(i);
            row.createCell(0).setCellValue(submission.getLastName() + ", " + submission.getFirstName());
            int j = 1;
            for(TestResult result : submission.getResults()){
                String passed = result.passed() ? "passed" : "failed";
                row.createCell(j).setCellValue(passed);
                j++;
            }
            row.createCell(j).setCellValue(submission.getScore());
            i++;
        }
    }
}
