package StepDefinitions;

import com.pages.bsHome;
import com.pages.bsOffers;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import org.junit.Assert;

import java.util.Iterator;
import java.util.List;

public class OffersSteps {
    ApplicationHooks hooks;
    private bsHome bshome;
    private bsOffers bsoffer;
    public OffersSteps(ApplicationHooks hooks){
        this.hooks = hooks;
        bshome = new bsHome(hooks.driver);
        bsoffer = new bsOffers(hooks.driver);
    }
    Scenario sc;

    @Before
    public void getScenario(Scenario scenario){
        this.sc = scenario;
    }

    @And("User Clicks on Offers")
    public void user_clicks_on_offers() {
        bshome.openOffers();
    }
    @Then("User can see offers list for Mumbai location")
    public void user_can_see_offers_list_for_mumbai_location() {
       List<String> offerDetails = bsoffer.getOffers();
       if(offerDetails.isEmpty()){
           Assert.fail("No offers found for your location");
       }else{
           Iterator t = offerDetails.iterator();
           //System.out.println("Offers for your location:");
           sc.log("Offers for your location:");
           while(t.hasNext()){
               //System.out.println(t.next());
               sc.log((String) t.next());
           }
       }
    }

}
