package com.tappayments.automation.config;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.markuputils.CodeLanguage;
import com.aventstack.extentreports.markuputils.ExtentColor;
import com.aventstack.extentreports.markuputils.MarkupHelper;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import com.tappayments.automation.utils.CommonConstants;
import io.restassured.http.Header;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ExtentReportManager {

    public static ExtentReports extentReports;

    public static ExtentReports createInstance(String fileName, String reportName, String documentTitle){

        ExtentSparkReporter extentSparkReporter = new ExtentSparkReporter(fileName);
        extentSparkReporter.config().setReportName(reportName);
        extentSparkReporter.config().setDocumentTitle(documentTitle);
        extentSparkReporter.config().setTheme(Theme.STANDARD);
        extentSparkReporter.config().setEncoding(CommonConstants.UTF_8);

        extentReports = new ExtentReports();
        extentReports.attachReporter(extentSparkReporter);

        return extentReports;
    }

    public static String getExtentReportNameWithTimeStamp(){

        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyy_MM_dd_HH_mm_ss");
        LocalDateTime localDateTime = LocalDateTime.now();
        String formattedTime = dateTimeFormatter.format(localDateTime);
        return "QA_Automation_Extent_Report_" + formattedTime + ".html";
    }

    public static void logPassDetails(String log){

        ExtentReportSetup.extentTest.get().pass(MarkupHelper.createLabel(log, ExtentColor.GREEN));
    }

    public static void logFailureDetails(String log){

        ExtentReportSetup.extentTest.get().fail(MarkupHelper.createLabel(log, ExtentColor.RED));
    }

    public static void logExceptionDetails(String log){

        ExtentReportSetup.extentTest.get().fail(log);
    }

    public static void logInfoDetails(String log){

        ExtentReportSetup.extentTest.get().info(MarkupHelper.createLabel(log, ExtentColor.GREY));
    }

    public static void logJson(String json){

        ExtentReportSetup.extentTest.get().info(MarkupHelper.createCodeBlock(json, CodeLanguage.JSON));
    }

    public static void logHeaders(List<Header> headerList){

        String[][] headers = headerList.stream()
                .map(header -> new String[]{
                        header.getName(), header.getValue()
                })
                .toArray(String[][]::new);

        ExtentReportSetup.extentTest.get().info(MarkupHelper.createTable(headers));
    }

    public static void logWarningDetails(String log){

        ExtentReportSetup.extentTest.get().warning(MarkupHelper.createLabel(log, ExtentColor.YELLOW));
    }
}