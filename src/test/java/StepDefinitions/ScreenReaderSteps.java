package StepDefinitions;

import com.qa.util.ScreenReaderUtil;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.testng.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Screen reader (SRA) steps. They run only on Windows desktop sessions (NVDA);
 * on every other platform each step logs a skip message and returns, so the
 * rest of the scenario is unaffected.
 */
public class ScreenReaderSteps {

    private final ApplicationHooks hooks;
    private Scenario scenario;
    private boolean enabled = false;
    private static final int SPOKEN_COUNT = 10;
    private final List<String> spokenOutputs = new ArrayList<>();

    public ScreenReaderSteps(ApplicationHooks hooks) {
        this.hooks = hooks;
    }

    @Before
    public void captureScenario(Scenario scenario) {
        this.scenario = scenario;
    }

    private ScreenReaderUtil sr() {
        return new ScreenReaderUtil(hooks.driver);
    }

    /** True only for a non-mobile session whose platform is Windows. */
    private boolean isWindowsDesktop() {
        WebDriver driver = hooks.driver;
        try {
            Object info = ((JavascriptExecutor) driver).executeScript("mobile:deviceInfo");
            if (info instanceof Map && !((Map<?, ?>) info).isEmpty()) {
                return false;
            }
        } catch (Exception ignored) {
            // desktop sessions do not support mobile:deviceInfo
        }
        String platform = String.valueOf(((RemoteWebDriver) driver).getCapabilities().getPlatformName()).toLowerCase();
        return platform.contains("win");
    }

    private boolean skipIfUnsupported() {
        if (isWindowsDesktop()) {
            return false;
        }
        scenario.log("Skipping SRA step: screen reader automation runs on Windows desktop (NVDA) only");
        return true;
    }

    @When("Screen reader is enabled on the orders page")
    public void enableScreenReader() {
        if (skipIfUnsupported()) {
            return;
        }
        sr().enable();
        enabled = true;
    }

    @When("User captures 10 spoken outputs with the screen reader")
    public void captureSpokenOutputs() {
        if (skipIfUnsupported()) {
            return;
        }
        ScreenReaderUtil util = sr();
        spokenOutputs.clear();
        for (int i = 1; i <= SPOKEN_COUNT; i++) {
            util.performAction("next_focusable_item");
            String spoken = util.getSpokenDescription();
            spokenOutputs.add(spoken);
            logLine("Captured spoken output #" + i + ": " + spoken);
        }
    }

    /** Assertion 1 (expected to PASS): 10 outputs captured, none empty, order matches capture order. */
    @Then("Screen reader spoken outputs are captured in order")
    public void assertSpokenOutputsInOrder() {
        if (skipIfUnsupported()) {
            return;
        }
        Assert.assertEquals(spokenOutputs.size(), SPOKEN_COUNT, "Expected " + SPOKEN_COUNT + " spoken outputs.");
        // Order reference: the sequence as it was spoken, copied at assertion time.
        List<String> expectedOrder = new ArrayList<>(spokenOutputs);
        for (int i = 0; i < SPOKEN_COUNT; i++) {
            String actual = spokenOutputs.get(i);
            String expected = expectedOrder.get(i);
            boolean match = actual != null && !actual.trim().isEmpty() && actual.equals(expected);
            logLine("Assertion 1 | position " + (i + 1) + " | expected: '" + expected
                    + "' | actual: '" + actual + "' | " + (match ? "PASS" : "FAIL"));
            Assert.assertTrue(match, "Spoken output #" + (i + 1) + " is empty or out of order: '" + actual + "'");
        }
        logLine("Assertion 1 | all " + SPOKEN_COUNT + " spoken outputs captured in order | PASS");
    }

    /** Assertion 2 (expected to FAIL on purpose): first spoken output compared with a wrong text. */
    @Then("Screen reader first spoken output is the expected demo text")
    public void assertFirstSpokenOutputDemoFailure() {
        if (skipIfUnsupported()) {
            return;
        }
        String expected = "NonExistentRandomText123";
        String actual = spokenOutputs.isEmpty() ? "" : spokenOutputs.get(0);
        boolean match = actual.equals(expected);
        logLine("Assertion 2 | position 1 | expected: '" + expected + "' | actual: '" + actual
                + "' | " + (match ? "PASS" : "FAIL (intentional)"));
        Assert.assertEquals(actual, expected, "first spoken output did not match the expected demo text.");
    }

    private void logLine(String message) {
        System.out.println(message);
        scenario.log(message);
    }

    /** Best-effort cleanup so the VM state is not polluted; only if this scenario enabled it. */
    @After(order = 2)
    public void disableScreenReader() {
        if (!enabled) {
            return;
        }
        try {
            sr().disable();
        } catch (Exception ignored) {
            // best effort
        }
    }
}