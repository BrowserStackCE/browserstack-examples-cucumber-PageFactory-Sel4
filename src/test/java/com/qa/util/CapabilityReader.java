package com.qa.util;

import Runner.CucumberTest;
import com.browserstack.local.Local;
import org.json.simple.JSONObject;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class CapabilityReader {
    protected static HashMap<String, String> browserstackOptions = new HashMap<>();

    public static synchronized DesiredCapabilities getCapability(Map<String, String> envCapabilities, JSONObject config) throws Exception {
        DesiredCapabilities capabilities = new DesiredCapabilities();
        Iterator it = envCapabilities.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            if (pair.getKey().toString().toLowerCase().contains("envoptions")) {
                browserstackOptions = (HashMap<String, String>) pair.getValue();
            } else {
                String key = pair.getKey().toString();
                // Selenium 4 rejects legacy non-W3C keys; map them to their W3C equivalents
                String originalKey = key;
                if (key.equals("browser")) key = "browserName";
                else if (key.equals("browser_version")) key = "browserVersion";
                System.out.println("[DBG] CapabilityReader env key '" + originalKey + "' -> '" + key + "' = " + pair.getValue());
                capabilities.setCapability(key, pair.getValue().toString());
            }
        }
        Map<String, String> commonCapabilities = (Map<String, String>) config.get("capabilities");
        it = commonCapabilities.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            String capKey = pair.getKey().toString();
            // The BrowserStack SDK (browserstack.yml) owns build/project/session naming;
            // setting them here would split sessions into a separate "Selenium" build.
            if (capKey.equals("buildName") || capKey.equals("projectName") || capKey.equals("sessionName")) {
                System.out.println("[DBG] CapabilityReader skipping '" + capKey + "' (owned by SDK / browserstack.yml)");
                continue;
            }
            browserstackOptions.put(capKey, pair.getValue().toString());
        }
        // Explicit opt-in override only; otherwise the SDK decides the build name
        if (System.getenv("BROWSERSTACK_BUILD_NAME") != null) {
            browserstackOptions.put("buildName", System.getenv("BROWSERSTACK_BUILD_NAME"));
        }

        System.out.println("[DBG] CapabilityReader bstack:options keys=" + browserstackOptions.keySet());
        capabilities.setCapability("bstack:options", browserstackOptions);

        return capabilities;
    }
}
