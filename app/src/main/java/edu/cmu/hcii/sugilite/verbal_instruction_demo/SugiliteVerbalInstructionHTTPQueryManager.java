package edu.cmu.hcii.sugilite.verbal_instruction_demo;

import com.google.gson.Gson;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

/**
 * @author toby
 * @date 12/10/17
 * @time 1:12 AM
 */
public class SugiliteVerbalInstructionHTTPQueryManager {
    private final String SERVER_URL =  "http://codermoder.com:4567/semparse";
    private final String USER_AGENT = "Mozilla/5.0";
    private SugiliteVerbalInstructionHTTPQueryInterface parentInterface;
    private Gson gson;

    public SugiliteVerbalInstructionHTTPQueryManager(SugiliteVerbalInstructionHTTPQueryInterface parentInterface){
        this.parentInterface = parentInterface;
        this.gson = new Gson();
    }

    public void sendQueryRequest(VerbalInstructionServerQuery query) throws Exception {
        String content = gson.toJson(query);
        sendRequest(content);
    }

    public void sendResponseRequest(VerbalInstructionServerResponse response) throws Exception {
        String content = gson.toJson(response);
        sendRequest(content);
    }



    private void sendRequest(String content) throws Exception {
        URL obj = new URL(SERVER_URL);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        //add request header
        con.setRequestMethod("POST");
        con.setRequestProperty("User-Agent", USER_AGENT);
        con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
        con.setRequestProperty("Content-Type", "application/json");
        con.setReadTimeout(1 * 60000);
        con.setConnectTimeout(1 * 60000);

        // Send post request
        con.setDoOutput(true);
        con.setDoInput(true);
        OutputStream out = null;
        out = new BufferedOutputStream(con.getOutputStream());
        BufferedWriter writer = new BufferedWriter (new OutputStreamWriter(out, "UTF-8"));
        writer.write(content);
        writer.flush();
        writer.close();
        out.close();
        con.connect();


        int responseCode = con.getResponseCode();
        System.out.println("Response Code:" + responseCode);

        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        System.out.println(response);

        //return result
        Runnable r = new Runnable() {
            @Override
            public void run() {
                parentInterface.resultReceived(responseCode, response.toString());
            }
        };
        parentInterface.runOnMainThread(r);
    }



}
