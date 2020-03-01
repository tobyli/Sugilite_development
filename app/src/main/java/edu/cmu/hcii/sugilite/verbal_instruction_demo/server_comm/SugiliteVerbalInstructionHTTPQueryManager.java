package edu.cmu.hcii.sugilite.verbal_instruction_demo.server_comm;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import edu.cmu.hcii.sugilite.R;
import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.pumice.communication.PumiceInstructionPacket;
import edu.cmu.hcii.sugilite.pumice.communication.SkipPumiceJSONSerialization;
import edu.cmu.hcii.sugilite.sovite.communication.SoviteAppResolutionQueryPacket;
import edu.cmu.hcii.sugilite.ui.dialog.SugiliteProgressDialog;

/**
 * @author toby
 * @date 12/10/17
 * @time 1:12 AM
 */
public class SugiliteVerbalInstructionHTTPQueryManager {
    private static SugiliteVerbalInstructionHTTPQueryManager instance;

    private static final String DEFAULT_SEMANTIC_PARSING_SERVER_URL =  "http://35.211.149.88:4567/semparse";
    private static final String DEFAULT_BERT_EMBEDDING_SERVER_URL =  "http://35.207.16.161:5000/get_closest_embeddings";
    private static final String USER_AGENT = "Mozilla/5.0";
    private static final int TIME_OUT = 60000;


    private Gson gson;
    private SharedPreferences sharedPreferences;
    private URL semanticParsingServerUrl;
    private URL bertEmbeddingServerUrl;

    public static SugiliteVerbalInstructionHTTPQueryManager getInstance(Context context){
        if (instance == null) {
            instance = new SugiliteVerbalInstructionHTTPQueryManager(context);
        }
        return instance;
    }

    private SugiliteVerbalInstructionHTTPQueryManager(Context context){
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        this.gson = new GsonBuilder()
                .addSerializationExclusionStrategy(new ExclusionStrategy()
                {
                    @Override
                    public boolean shouldSkipField(FieldAttributes f)
                    {
                        return f.getAnnotation(SkipPumiceJSONSerialization.class) != null;
                    }

                    @Override
                    public boolean shouldSkipClass(Class<?> clazz)
                    {
                        return false;
                    }
                })
                .create();

        String semanticParsingServerUrlString = "null";
        String bertEmbeddingServerUrlString = "null";
            if (sharedPreferences != null) {
                semanticParsingServerUrlString = sharedPreferences.getString("semantic_parsing_server_address", "null");
                bertEmbeddingServerUrlString = sharedPreferences.getString("bert_embedding_server_address", "null");
            }

        try {
            if (semanticParsingServerUrlString.equals("null")) {
                semanticParsingServerUrl = new URL(DEFAULT_SEMANTIC_PARSING_SERVER_URL);
            } else {
                semanticParsingServerUrl = new URL(semanticParsingServerUrlString);
            }
        } catch (MalformedURLException e){
            throw new RuntimeException("malformed URL!");
        }

        try {
            if (bertEmbeddingServerUrlString.equals("null")) {
                bertEmbeddingServerUrl = new URL(DEFAULT_BERT_EMBEDDING_SERVER_URL);
            } else {
                bertEmbeddingServerUrl = new URL(bertEmbeddingServerUrlString);
            }
        } catch (MalformedURLException e){
            throw new RuntimeException("malformed URL!");
        }

    }

    public void sendPumiceInstructionPacketOnASeparateThread(PumiceInstructionPacket packet, SugiliteVerbalInstructionHTTPQueryInterface caller) throws Exception {
        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    String content = gson.toJson(packet);
                    sendRequest(content, semanticParsingServerUrl, caller, false);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        thread.start();
    }

    public void sendSoviteAppResolutionPacketOnASeparateThread(SoviteAppResolutionQueryPacket soviteAppResolutionQueryPacket, SugiliteVerbalInstructionHTTPQueryInterface caller) throws Exception {
        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    String content = gson.toJson(soviteAppResolutionQueryPacket);
                    sendRequest(content, bertEmbeddingServerUrl, caller, true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        thread.start();
    }

    public void sendQueryRequestOnASeparateThread(VerbalInstructionServerQuery query, URL url, SugiliteVerbalInstructionHTTPQueryInterface caller) throws Exception {

        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    sendQueryRequest(query, url, caller, true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        thread.start();
    }

    private void sendQueryRequest(VerbalInstructionServerQuery query, URL url, SugiliteVerbalInstructionHTTPQueryInterface caller, boolean toShowProgressDialog) throws Exception {
        String content = gson.toJson(query);
        sendRequest(content, url, caller, toShowProgressDialog);
    }

    private void sendResponseRequest(VerbalInstructionServerResponse response, URL url, SugiliteVerbalInstructionHTTPQueryInterface caller, boolean toShowProgressDialog) throws Exception {
        String content = gson.toJson(response);
        sendRequest(content, url, caller, toShowProgressDialog);
    }

    public void sendResponseRequestOnASeparateThread(VerbalInstructionServerResponse response, URL url, SugiliteVerbalInstructionHTTPQueryInterface caller) throws Exception {
        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    sendResponseRequest(response, url, caller, true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        thread.start();
    }

    public URL getBertEmbeddingServerUrl() {
        return bertEmbeddingServerUrl;
    }

    public URL getSemanticParsingServerUrl() {
        return semanticParsingServerUrl;
    }

    private void sendRequest(String content, URL url, SugiliteVerbalInstructionHTTPQueryInterface caller, boolean toShowProgressDialog) throws Exception {
        //progress dialog
        SugiliteProgressDialog progressDialog = null;
        if (toShowProgressDialog) {
            progressDialog = new SugiliteProgressDialog(SugiliteData.getAppContext(), R.string.processing_query_message);
            progressDialog.show();
        }

        try {
            HttpURLConnection con = (HttpURLConnection) url.openConnection();

            //add request header
            con.setRequestMethod("POST");
            con.setRequestProperty("User-Agent", USER_AGENT);
            con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
            con.setRequestProperty("Content-Type", "application/json");
            con.setReadTimeout(TIME_OUT);
            con.setConnectTimeout(TIME_OUT);

            // Send post request
            con.setDoOutput(true);
            con.setDoInput(true);
            OutputStream out = null;
            out = new BufferedOutputStream(con.getOutputStream());
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, "UTF-8"));
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
                    if(caller != null) {
                        caller.resultReceived(responseCode, response.toString(), content);
                    }
                }
            };
            if (caller != null) {
                SugiliteData.runOnUiThread(r);
            }
        } catch (Exception e){
            e.printStackTrace();
            throw e;
        } finally {
            if (progressDialog != null) {
                progressDialog.dismiss();
            }

        }
    }



}
