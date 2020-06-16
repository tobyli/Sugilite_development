package edu.cmu.hcii.sugilite.pumice.kb.default_query;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * @author toby
 * @date 6/15/20
 * @time 12:18 PM
 */
public class BuiltInValueHttpManager {

    private static BuiltInValueHttpManager instance;
    private final OkHttpClient httpClient = new OkHttpClient();

    public static BuiltInValueHttpManager getInstance(){
        if (instance == null) {
            instance = new BuiltInValueHttpManager();
        }
        return instance;
    }

    private BuiltInValueHttpManager(){

    }

    public String sendGet(String url){
        Request request = new Request.Builder()
                .url(url)
                .build();
        try  {
            Response response = httpClient.newCall(request).execute();
            // Get response body
            return response.body().string();
        } catch (IOException e) {

        }
        return null;
    }


}
