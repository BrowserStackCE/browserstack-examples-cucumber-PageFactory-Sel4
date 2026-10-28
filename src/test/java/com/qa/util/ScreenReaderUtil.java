package com.qa.util;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.FluentWait;

import java.time.Duration;

/**
 * Thin wrapper around the BrowserStack screen reader (SRA) executor commands.
 */
public class ScreenReaderUtil {

    private static final long SETTLE_MS = 2000;

    private final WebDriver driver;
    private final JavascriptExecutor jse;

    public ScreenReaderUtil(WebDriver driver) {
        this.driver = driver;
        this.jse = (JavascriptExecutor) driver;
    }

    public void enable() {
        jse.executeScript("browserstack_executor: {\"action\": \"screenReader\", \"arguments\": {\"enable\": \"true\"}}");
        settle();
    }

    /** command examples: next_focusable_item, previous_focusable_item, activate */
    public void performAction(String command) {
        jse.executeScript("browserstack_executor: {\"action\": \"screenReaderAction\", \"arguments\": {\"command\": \"" + command + "\"}}");
        settle();
    }

    /**
     * Polls until the screen reader reports non-empty text. Exceptions thrown while the
     * screen reader is still initialising are ignored and the call is retried.
     */
    public String getSpokenDescription() {
        FluentWait<WebDriver> wait = new FluentWait<>(driver)
                .withTimeout(Duration.ofSeconds(10))
                .pollingEvery(Duration.ofMillis(500))
                .ignoring(Exception.class);
        try {
            return wait.until(d -> {
                Object out = ((JavascriptExecutor) d).executeScript(
                        "browserstack_executor: {\"action\": \"screenReaderSpokenDescription\"}");
                if (out != null && !out.toString().trim().isEmpty()) {
                    return out.toString();
                }
                return null; // keep polling
            });
        } catch (TimeoutException e) {
            throw new AssertionError("Screen reader produced no spoken output within 10 seconds", e);
        }
    }

    public void disable() {
        jse.executeScript("browserstack_executor: {\"action\": \"screenReader\", \"arguments\": {\"enable\": \"false\"}}");
    }

    private void settle() {
        try {
            Thread.sleep(SETTLE_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
