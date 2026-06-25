package com.pages;

import org.openqa.selenium.*;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class bsSignin {
    WebDriver driver;
    WebDriverWait wait;
    public bsSignin(WebDriver driver){
        this.driver = driver;
        PageFactory.initElements(driver,this);
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    @FindBy(xpath = "//div[contains(text(),'Username')]/..")
    WebElement ddUsername;
    @FindBy(xpath = "//div[contains(text(),'Password')]/..")
    WebElement ddpassword;
    @FindBy(id="login-btn")
    WebElement btnLogin;

    public void userCredentials(String user,String password) {
        wait.until(ExpectedConditions.elementToBeClickable(btnLogin));
        JavascriptExecutor js = (JavascriptExecutor) driver;
        ddUsername.click();
        WebElement selectuser = driver.findElement(By.xpath("//*[text()='"+user+"']"));
        js.executeScript("arguments[0].click();",selectuser);
        ddpassword.click();
        WebElement selectpaserd = driver.findElement(By.xpath("//*[text()='"+password+"']"));
        js.executeScript("arguments[0].click();",selectpaserd);
        btnLogin.submit();
    }

    public boolean verifyLockedUsersign() throws InterruptedException {
        Thread.sleep(2000);
        boolean msg =driver.getPageSource().contains("Your account has been locked");
        return msg;
    }


}
