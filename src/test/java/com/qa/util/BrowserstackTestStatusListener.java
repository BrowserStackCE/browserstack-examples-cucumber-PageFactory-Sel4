package com.qa.util;

import Runner.CucumberTest;
import io.cucumber.java.Scenario;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.testng.ITestListener;
import org.testng.ITestResult;

public class BrowserstackTestStatusListener implements ITestListener {
    private void markTestStatus(String status, String reason, WebDriver driver) {
        try {
            JavascriptExecutor jse = (JavascriptExecutor) driver;
            jse.executeScript("browserstack_executor: {\"action\": \"setSessionStatus\", \"arguments\": {\"status\": \"" + status + "\", \"reason\": \"" + reason + "\"}}");
        } catch (Exception e) {
            System.out.print("Error executing javascript" + e);
        }
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        System.out.println("[DBG] onTestSuccess " + result.getName() + " thread=" + Thread.currentThread().getName());
        Object currentClass = result.getInstance();
        WebDriver driver = ((CucumberTest) currentClass).getDriver();
        //markTestStatus("passed", "", driver);
        driver.quit();
    }

    @Override
    public void onTestFailure(ITestResult result) {
        System.out.println("[DBG] onTestFailure " + result.getName() + " thread=" + Thread.currentThread().getName() + " error=" + result.getThrowable());
        Object currentClass = result.getInstance();
        WebDriver driver = ((CucumberTest) currentClass).getDriver();
        String message = result.getThrowable().getMessage();
        String reason = (message != null && message.length() > 254) ? message.substring(0, 254) : message;
        //markTestStatus("failed", reason.replaceAll("[^a-zA-Z0-9._-]", " "), driver);
        driver.quit();
    }

}
