package com.tappayments.automation.config;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ExtentReportSetup implements ITestListener {

    private static ExtentReports extentReports;
    public static ThreadLocal<ExtentTest> extentTest = new ThreadLocal<>();
    private static Map<String, ExtentTest> categoryNodes = new HashMap<>();
    private static Map<String, ExtentTest> subCategoryNodes = new HashMap<>();

    public void onStart(ITestContext context){

        String fileName = ExtentReportManager.getExtentReportNameWithTimeStamp();
        String fullReportPath = System.getProperty("user.dir") + "/extent.reports/" + fileName;

        extentReports = ExtentReportManager.createInstance(fullReportPath, "QA Automation API Report", "QA Automation API Report");
    }

    public void onFinish(ITestContext context){

        if(extentReports != null)
            extentReports.flush();
    }

    public void onTestStart(ITestResult result) {

        String[] groups = result.getMethod().getGroups();

        if (groups.length > 0) {

            String mainCategory = "";
            String subCategory = "";
            String sectionTag = "";
            String author = "";

            for (String group : groups) {
                if (group.startsWith("MC:")) {
                    mainCategory = group.substring(3); // Remove "MC:" prefix
                } else if (group.startsWith("SC:")) {
                    subCategory = group.substring(3); // Remove "SC:" prefix
                } else if (group.startsWith("SECTION:")) {
                    sectionTag = group.substring(8); // Remove "TAG:" prefix
                } else if (group.startsWith("AUTHOR:")) {
                    author = group.substring(7); // Remove "AUTHOR:" prefix
                }
            }

            // Level 3: Specific Test Case Node
            String testCaseName = result.getMethod().getDescription();

            // Create or retrieve the main category node
            ExtentTest mainCategoryNode = categoryNodes.computeIfAbsent(mainCategory, k -> extentReports.createTest(k));

            // Create or retrieve the subcategory node under the main category
            String finalSubCategory = subCategory;
            ExtentTest subCategoryNode = subCategoryNodes.computeIfAbsent(mainCategory + subCategory, k -> mainCategoryNode.createNode(finalSubCategory));

            // Create a node for the specific test case
            ExtentTest testNode = subCategoryNode.createNode(testCaseName);
            subCategoryNode.assignCategory(mainCategory);
            subCategoryNode.assignAuthor(author);

            testNode.assignCategory(sectionTag);
            extentTest.set(testNode);
        }
    }

    public void onTestFailure(ITestResult result){

        ExtentReportManager.logFailureDetails(result.getThrowable().getMessage());

        String stackTrace = Arrays.toString(result.getThrowable().getStackTrace());
        stackTrace = stackTrace.replaceAll(",", "<br>");

        String formattedTrace = """
                    <details>
                        <summary>Click here to see detail exception logs</summary>
                        """ + stackTrace + """
                    </details>
                """;

        ExtentReportManager.logExceptionDetails(formattedTrace);
    }
}
