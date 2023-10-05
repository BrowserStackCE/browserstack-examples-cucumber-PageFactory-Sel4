package Runner;

import com.browserstack.local.Local;
import com.qa.util.BrowserstackTestStatusListener;
import com.qa.util.CapabilityReader;
import io.cucumber.testng.CucumberOptions;
import io.cucumber.testng.FeatureWrapper;
import io.cucumber.testng.PickleWrapper;
import io.cucumber.testng.TestNGCucumberRunner;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.safari.SafariDriver;
import org.testng.annotations.*;

import java.io.FileReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@CucumberOptions(features = "src/test/resources/Features", glue = {"StepDefinitions"},
        plugin = {"pretty",
                "com.aventstack.extentreports.cucumber.adapter.ExtentCucumberAdapter:"},
        monochrome = true
       )
@Listeners(BrowserstackTestStatusListener.class)
public class CucumberTest {
       private TestNGCucumberRunner testNGCucumberRunner;
       public static ThreadLocal<WebDriver> tlDriver = new ThreadLocal<>();
       private Local l;
       public static String buildname;
       @Parameters(value = {"config"})
       @BeforeSuite
       public void localStart(String config_file) throws Exception {
              JSONParser parser = new JSONParser();
              if(!config_file.isEmpty()) {
                     JSONObject config = (JSONObject) parser.parse(new FileReader("src/test/resources/browserstack/conf/" + config_file));
                     JSONObject local = (JSONObject) config.get("capabilities");
                     boolean localKey = local.containsKey("local");
                     if (localKey && local.get("local").toString().contains("true")) {
                            l = new Local();
                            Map<String, String> options = new HashMap<String, String>();
                            if (System.getenv("BROWSERSTACK_ACCESS_KEY") == null)
                                   options.put("key", (String) config.get("key"));
                            else
                                   options.put("key", System.getenv("BROWSERSTACK_ACCESS_KEY"));
                            System.out.println("Starting Local");
                            l.start(options);
                     }
                     SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.YY hh.mm");
                     buildname = local.get("projectName") + "-" + sdf.format(new Date());
              }
       }
       @BeforeClass(alwaysRun = true)
       public void setUpClass() {
              testNGCucumberRunner = new TestNGCucumberRunner(this.getClass());
       }
       @Parameters(value = { "config", "environment" })
       @BeforeMethod()
       public void setUpHooks(String config_file, String environment) throws Exception {
              DesiredCapabilities capabilities = new DesiredCapabilities();
              String browser=System.getProperty("browser-type").toLowerCase();
              if (browser.equalsIgnoreCase("chrome")) {
                     WebDriverManager.chromedriver().setup();
                     tlDriver.set(new ChromeDriver());
              } else if (browser.equalsIgnoreCase("firefox")) {
                     WebDriverManager.firefoxdriver().setup();
                     tlDriver.set(new FirefoxDriver());
              } else if (browser.equalsIgnoreCase("safari")) {
                     WebDriverManager.safaridriver().setup();
                     tlDriver.set(new SafariDriver());
              } else if (browser.equalsIgnoreCase("remote")) {
                     JSONParser parser = new JSONParser();
                     JSONObject config;
                     JSONObject envs;
                     config = (JSONObject) parser.parse(new FileReader("src/test/resources/browserstack/conf/"+config_file));
                     envs = (JSONObject) config.get("environments");
                     Object env = envs.get(environment);
                     capabilities = CapabilityReader.getCapability((Map<String, String>) env, config);
                     String username = (String) config.get("user");
                     if (username == null || username.isEmpty()){
                            username = System.getenv("BROWSERSTACK_USERNAME");
                     }
                     String accessKey = (String) config.get("key");
                     if (accessKey == null || accessKey.isEmpty()) {
                            accessKey = System.getenv("BROWSERSTACK_ACCESS_KEY");
                     }
                     tlDriver.set( new RemoteWebDriver(
                             new URL("https://" + username + ":" + accessKey + "@" + config.get("server") + "/wd/hub"), capabilities));
              }else
                     throw new AssertionError("Invalid input for browser");
              if(!capabilities.toString().contains("realMobile"))
                     getDriver().manage().window().maximize();
       }

       public static synchronized WebDriver getDriver(){
              return tlDriver.get();
       }

       @Test(dataProvider = "scenarios")
       public void scenario(PickleWrapper pickleWrapper, FeatureWrapper featureWrapper) throws MalformedURLException {
              JavascriptExecutor jse = (JavascriptExecutor)getDriver();
              //if(System.getProperty("browser-type").equalsIgnoreCase("remote"))
              //jse.executeScript("browserstack_executor: {\"action\": \"setSessionName\", \"arguments\": {\"name\":\" "+       pickleWrapper.getPickle().getName()   +" \" }}");
              testNGCucumberRunner.runScenario(pickleWrapper.getPickle());
           }
       @DataProvider(parallel = true)
       public Object[][] scenarios(){
              return testNGCucumberRunner.provideScenarios();
       }

       @AfterClass(alwaysRun = true)
       public void tearDownClass() {
              //testNGCucumberRunner.finish(); //Conflict with the SDK   Runner.CucumberTest.tearDownClass
              // NoSuchMethod org.yaml.snakeyaml.constructor.Constructor.<init>
       }
       @AfterSuite
       public void shutLocal() throws Exception {
              System.out.println("Stopping Local");
              if(l != null) l.stop();
       }
}
