package com.pages;

import org.junit.Assert;
import org.openqa.selenium.*;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.How;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.*;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class bsHome {

    WebDriver driver;
    WebDriverWait wait;
    @FindBy(how = How.XPATH, using="//span[text()='Apple']")
    WebElement lblApple ;
    @FindBy(xpath="//select")
    WebElement ddPrice ;
    @FindBy(id="signin")
    WebElement lnksignIn;
    @FindBy(xpath="//div[text()='Checkout']")
    WebElement lnkCheckout;
    @FindBy(id = "offers")
    WebElement lnkOffers;
    @FindBy(id="logout")
    WebElement lnkLogout;
    @FindBy(xpath = "(//div[@class='shelf-item__thumb']/img)[1]")
    WebElement image;
    @FindBy(id="orders")
    WebElement lnkOrder;

    public bsHome(WebDriver driver){
        this.driver = driver;
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        PageFactory.initElements(driver,this);
    }
    public void searchProduct(){
        lblApple.click();
    }
    public void sortResult() throws InterruptedException {
        Select sort = new Select(ddPrice);
        sort.selectByValue("lowestprice");
        Thread.sleep(5000);

    }
    public Map<String, String> searchResult(){
        List<WebElement> iphones = driver.findElements(By.cssSelector("div.shelf-item"));
        Assert.assertEquals("9 products not shown as expected",9,iphones.size());
        String title,price;
        Map<String,String> iphoneDetails= new HashMap<String,String>();
        for (int i = 0;i<iphones.size()-1;i++){
            title = iphones.get(i).findElement(By.xpath(".//p[@class='shelf-item__title']")).getAttribute("innerText");
            price = iphones.get(i).findElement(By.xpath(".//div[@class='val']/b")).getText();
            iphoneDetails.put(title,price);
        }
        return iphoneDetails;
    }
    public void openOffers(){
        wait.until(ExpectedConditions.elementToBeClickable(lnkOffers));
        ((JavascriptExecutor)driver).executeScript("window.navigator.geolocation.getCurrentPosition=function(success){"+
                "var position = {\"coords\" : {\"latitude\": \"19.075984\",\"longitude\": \"72.877656\"}};"+
                "success(position);}");  //Sets location of Browser to Mumbai

        /*System.out.println(((JavascriptExecutor) driver).executeScript("var positionStr=\"\";"+
                "window.navigator.geolocation.getCurrentPosition(function(pos){positionStr=pos.coords.latitude+\":\"+pos.coords.longitude});"+
                "return positionStr;"));*/
        lnkOffers.click();
    }


    public void userSignIn(){
        wait.until(ExpectedConditions.elementToBeClickable(lnksignIn));
        lnksignIn.click();
    }

    public Boolean verifySignin() {
        try {
            FluentWait wait = new FluentWait(driver)
                    .pollingEvery(Duration.ofSeconds(2))
                    .ignoring(Exception.class)
                    .withTimeout(Duration.ofSeconds(10));
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("span.username")));
        }catch(Exception e){
            return false;
        }
        return true;
    }

    public void verifyandlogout(){
        try {
            if (lnkLogout.isDisplayed())
                lnkLogout.click();
        }catch (Exception e){

        }
    }

    public boolean verifyimageLoaded(){
        Object result = ((JavascriptExecutor) driver).executeScript(
                "return arguments[0].complete && "+
                        "typeof arguments[0].naturalWidth != \"undefined\" && "+
                        "arguments[0].naturalWidth > 0", image);

        boolean loaded = false;
        if (result instanceof Boolean) {
            loaded = (Boolean) result;
            return loaded;
        }
        return loaded;
    }

    public void AddtoCart(){
        WebElement cart = driver.findElement(By.xpath("(//div[text()='Add to cart'])[1]"));
        wait.until(ExpectedConditions.elementToBeClickable(cart));
        cart.click();
        wait.until(ExpectedConditions.elementToBeClickable(lnkCheckout));
        lnkCheckout.click();
    }
    public void NavigatetoOrder(){
        wait.until(ExpectedConditions.elementToBeClickable(lnkOrder));
        lnkOrder.click();
    }

}
