package com.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class bsOffers {
    private WebDriver driver;
    private WebDriverWait wait;
    public bsOffers(WebDriver driver){
        this.driver = driver;
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        PageFactory.initElements(driver,this);
    }

    public List<String> getOffers(){
        List<WebElement> offers= null;
        List<String> offerDetails = new ArrayList<>();
        try {
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//div[contains(@class,'offers-listing')]")));
            offers = driver.findElements(By.xpath("//div[contains(@class,'offers-listing')]/div/div"));
        }catch(Exception e){
           return offerDetails;
        }
        for (WebElement offer : offers){
            offerDetails.add(offer.getText());
        }
        return offerDetails;
    }
}
