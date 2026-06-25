package com.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class bsOrders {

    public bsOrders(WebDriver driver){
        this.driver = driver;
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        PageFactory.initElements(driver,this);
    }
    WebDriver driver;
    WebDriverWait wait;

    @FindBy(xpath = "//div[@id='__next']//div[contains(@class,'top-medium')]")
    WebElement paneOrders;

    public List<String> VerifyOrders(){
        wait.until(ExpectedConditions.visibilityOf(paneOrders));
        List<WebElement> orderTitle = driver.findElements(By.xpath("//strong[text()='Title:']/.."));
        List<WebElement> orderPrice = driver.findElements(By.xpath("//span[contains(@class,'price')]"));
        List<String> Allorders = new ArrayList<>();
        for(int i=0;i<orderPrice.size();i++){
            Allorders.add(orderTitle.get(i).getText().substring(7)+' '+orderPrice.get(i).getText());
        }
        return Allorders;
    }
}