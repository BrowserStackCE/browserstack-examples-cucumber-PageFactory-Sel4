package com.qa.util;

import org.apache.commons.codec.binary.Base64;
import org.json.JSONObject;
import org.testng.IAlterSuiteListener;
import org.testng.xml.XmlSuite;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class SuiteAlterer implements IAlterSuiteListener {
    @Override
    public void alter(List<XmlSuite> suites) {
        int count=5;
        if(System.getProperty("threadCount")==null && System.getProperty("browser-type").equalsIgnoreCase("remote")) {
            // This will fetch the parallel_sessions_max_allowed for the account and set the threadCount to it
            URL url = null;
            try {
                url = new URL("https://api.browserstack.com/automate/plan.json");
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            String auth = System.getenv("BROWSERSTACK_USERNAME") + ":" + System.getenv("BROWSERSTACK_ACCESS_KEY");
            byte[] encodedAuth = Base64.encodeBase64(auth.getBytes(StandardCharsets.UTF_8));
            String authHeaderValue = "Basic " + new String(encodedAuth);
            HttpURLConnection http = null;
            try {
                http = (HttpURLConnection) url.openConnection ();
            } catch (IOException e) {
                e.printStackTrace();
            }
            http.setRequestProperty("Authorization", authHeaderValue);
            BufferedReader in = null;
            try {
                in = new BufferedReader(new InputStreamReader(http.getInputStream()));
            } catch (IOException e) {
                e.printStackTrace();
            }
            String inputLine = "";
            StringBuffer response = new StringBuffer();
            while (true) {
                try {
                    if (!((inputLine = in.readLine()) != null)) break;
                } catch (IOException e) {
                    e.printStackTrace();
                }
                response.append(inputLine);
            }
            JSONObject myResponse = new JSONObject(response.toString());
            count = (int) myResponse.get("parallel_sessions_max_allowed");
        }else if(System.getProperty("threadCount")!=null)
            //Sets the thread count value via CLI
            count = Integer.parseInt(System.getProperty("threadCount"));
        XmlSuite suite = suites.get(0);
        suite.setDataProviderThreadCount(count);
    }
}
