package com.pages;

import org.junit.Assert;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class bsConfirmation {
    WebDriver driver;
    WebDriverWait wait;
    public bsConfirmation(WebDriver driver){
        this.driver = driver;
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        PageFactory.initElements(driver,this);
    }

    @FindBy(xpath="//button[contains(text(),'Continue Shopping')]")
    WebElement btnContinue;
    @FindBy(xpath ="//div/strong")
    WebElement lblOrder;

    public String verifyPurchase(){
        wait.until(ExpectedConditions.elementToBeClickable(btnContinue));
        Assert.assertTrue("Order not placed!",driver.getPageSource().contains("Your Order has been successfully placed."));
        String orderid;
        orderid = lblOrder.getAttribute("innerText");
        return orderid;
    }
}
