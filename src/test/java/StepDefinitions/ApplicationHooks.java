package StepDefinitions;

import Runner.CucumberTest;
import io.cucumber.java.After;
import io.cucumber.java.Scenario;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

public class ApplicationHooks extends CucumberTest {
    public WebDriver driver = CucumberTest.getDriver();
    @After(order=1)
    public void screenshot(Scenario sc){
        String screenshotName = sc.getName().replaceAll(" ","_");
        byte[] sourcePath = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
        sc.attach(sourcePath,"image/png",screenshotName);
    }

    private void markTestStatus(String status, String reason, WebDriver driver) {
        try {
            JavascriptExecutor jse = (JavascriptExecutor) driver;
            jse.executeScript("browserstack_executor: {\"action\": \"setSessionStatus\", \"arguments\": {\"status\": \"" + status + "\", \"reason\": \"" + reason + "\"}}");
        } catch (Exception e) {
            System.out.print("Error executing javascript" + e);
        }
    }
}
