package com.pages;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class bsCheckout {

    WebDriver driver;
    WebDriverWait wait;
    public bsCheckout(WebDriver driver){
        this.driver = driver;
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        PageFactory.initElements(driver,this);
    }

    @FindBy(id = "firstNameInput")
    WebElement txtName;
    @FindBy(id="lastNameInput")
    WebElement txtLast;
    @FindBy(id = "addressLine1Input")
    WebElement txtAddress;
    @FindBy(id = "provinceInput")
    WebElement txtState;
    @FindBy(id="postCodeInput")
    WebElement txtPostal;
    @FindBy(xpath = "//button[text()='Submit']")
    WebElement btnSubmit;

    public void fillAddress(String first, String last, String address, String state, Integer pin){
        wait.until(ExpectedConditions.elementToBeClickable(txtName));
        txtName.sendKeys(first);
        txtLast.sendKeys(last);
        txtAddress.sendKeys(address);
        txtState.sendKeys(state);
        txtPostal.sendKeys(Integer.toString(pin));
        btnSubmit.submit();

    }
}
