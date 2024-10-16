package StepDefinitions;

import com.pages.bsHome;
import com.pages.bsOrders;
import com.pages.bsSignin;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.util.Iterator;
import java.util.List;

public class UsersSteps {

    public UsersSteps(ApplicationHooks hooks){
        this.hooks = hooks;
        bshome  = new bsHome(hooks.driver);
        bsorder = new bsOrders(hooks.driver);
        bssign = new bsSignin(hooks.driver);
    }

    ApplicationHooks hooks;
    private final bsHome bshome;
    private final bsOrders bsorder;
    private final bsSignin bssign ;
    Scenario sc;

    @Before
    public void beforeScenario(Scenario scenario){
        this.sc = scenario;
    }

    @Then("Product images are not loaded for the {string}")
    public void product_images_are_not_loaded_for_the_user(String user) {
        boolean imageload = bshome.verifyimageLoaded();
        if (imageload){
            System.out.println("Product images loaded for user");
            sc.log("Product images loaded for user");
        }else
            throw new AssertionError("Product images not loaded for "+ user);
    }
    @When("User clicks on Orders")
    public void user_clicks_on_orders() {
        bshome.NavigatetoOrder();
    }
    @Then("Existing orders are shown for the {string}")
    public void existing_orders_are_shown_for_the_user(String user) {
        List<String> orders = bsorder.VerifyOrders();
        if (orders.isEmpty()){
            throw new AssertionError("No orders found for the "+user);
        }else{
            //System.out.println("Details of Orders for "+user+":");
            sc.log("Details of Orders for "+user+":");
            Iterator t = orders.iterator();
            while(t.hasNext()){
                //System.out.println(t.next());
                sc.log(t.next().toString());
            }
        }
    }
    @Then("{string} gets Your account has been locked.")
    public void gets_your_account_has_been_locked(String user) throws InterruptedException {
        boolean verify = bssign.verifyLockedUsersign();
        if(verify){
            //System.out.println(user+" was not able to login");
            sc.log(user+" was not able to login");
        }else
            throw new AssertionError("Locked user "+user+" logged in!");

    }

}
