package edu.cmu.hcii.sugilite.verbal_instruction_demo.server_comm;

import android.content.SharedPreferences;

import com.google.gson.Gson;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

import edu.cmu.hcii.sugilite.pumice.communication.PumiceInstructionPacket;

/**
 * @author toby
 * @date 12/10/17
 * @time 1:12 AM
 */
public class SugiliteVerbalInstructionHTTPQueryManager {
    private final String DEFAULT_SERVER_URL =  "http://35.211.149.88:4567/semparse";//"http://codermoder.com:4567/semparse";
    private final String USER_AGENT = "Mozilla/5.0";
    private SugiliteVerbalInstructionHTTPQueryInterface parentInterface;
    private Gson gson;
    private SharedPreferences sharedPreferences;

    public SugiliteVerbalInstructionHTTPQueryManager(SugiliteVerbalInstructionHTTPQueryInterface parentInterface, SharedPreferences sharedPreferences){
        this.parentInterface = parentInterface;
        this.sharedPreferences = sharedPreferences;
        this.gson = new Gson();
    }

    public void sendPumiceInstructionPacketOnASeparateThread(PumiceInstructionPacket packet) throws Exception {
        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    String content = gson.toJson(packet);
                    sendRequest(content);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        thread.start();
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
        String url = sharedPreferences.getString("edit_text_server_address", "null");
        if(url.equals("null")){
            url = DEFAULT_SERVER_URL;
        }
        URL obj = new URL(url);
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

        System.out.println("Response Content:" + response);

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
