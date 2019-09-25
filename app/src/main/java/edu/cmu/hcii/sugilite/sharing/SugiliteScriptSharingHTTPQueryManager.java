package edu.cmu.hcii.sugilite.sharing;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.apache.commons.lang3.SerializationUtils;
import org.jetbrains.annotations.TestOnly;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.Nullable;

import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;
import edu.cmu.hcii.sugilite.pumice.communication.SkipPumiceJSONSerialization;
import edu.cmu.hcii.sugilite.sharing.model.HashedUIStrings;
import edu.cmu.hcii.sugilite.sharing.model.SugiliteRepoListing;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * @author toby
 * @date 9/24/19
 * @time 1:32 PM
 */
public class SugiliteScriptSharingHTTPQueryManager {
    private static SugiliteScriptSharingHTTPQueryManager instance;

    private static final String DEFAULT_SERVER_URL =  "http://35.207.16.161:4567/";
    private static final String DOWNLOAD_REPO_LIST_ENDPOINT = "repo/list";
    private static final String DOWNLOAD_SCRIPT_FROM_REPO_ENDPOINT_PREFIX = "repo/";
    private static final String UPLOAD_SCRIPT_TO_REPO_ENDPOINT = "repo/upload";
    private static final String UPLOAD_HASHED_UI_ENDPOINT = "privacy/upload_ui";
    private static final String FILTER_UI_STRING_ENDPOINT = "privacy/debug_filter";


    private ExecutorService executor;



    private static final String USER_AGENT = "Mozilla/5.0";
    private static final int TIME_OUT = 3000;

    private Gson gson;
    private SharedPreferences sharedPreferences;
    private URI baseUri;

    public static SugiliteScriptSharingHTTPQueryManager getInstance(Context context){
        if (instance == null) {
            instance = new SugiliteScriptSharingHTTPQueryManager(context);
        }

        return instance;
    }



    private SugiliteScriptSharingHTTPQueryManager(Context context){
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        this.executor = Executors.newFixedThreadPool(1);
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

        String urlString = "null";
        if (sharedPreferences != null) {
            urlString = sharedPreferences.getString("script_sharing_server_address", "null");
        }

        try {
            if (urlString.equals("null")) {
                baseUri = new URI(DEFAULT_SERVER_URL);
            } else {
                baseUri = new URI(urlString);
            }
        } catch (URISyntaxException e){
            throw new RuntimeException("malformed URL!");
        }
    }




    public List<SugiliteRepoListing> getRepoList () throws ExecutionException, InterruptedException {
        DownloadRepoListTask repoListTask = new DownloadRepoListTask();
        List<SugiliteRepoListing> repo = executor.submit(repoListTask).get();
        return repo;
    }

    private class DownloadRepoListTask implements Callable<ArrayList<SugiliteRepoListing>> {
        @Override
        public ArrayList<SugiliteRepoListing> call() throws Exception {
            URL downloadUrl = baseUri.resolve(DOWNLOAD_REPO_LIST_ENDPOINT).toURL();

            HttpURLConnection urlConnection = (HttpURLConnection) downloadUrl.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
            urlConnection.setReadTimeout(TIME_OUT);
            urlConnection.setConnectTimeout(TIME_OUT);

            urlConnection.setDoInput(true);
            urlConnection.setChunkedStreamingMode(0); // this might increase performance?

            urlConnection.connect();

            int responseCode = urlConnection.getResponseCode();

            if (responseCode != HttpURLConnection.HTTP_OK) {
                // TODO scream or something
                Log.e("DownloadRepoListTask", "Uh oh");
                throw new Exception(String.format("Exception: Can't connect to %s; Error Code %d", downloadUrl.toString(), responseCode));
            }

            Gson gson = new Gson();

            JsonArray jsonArray = gson.fromJson(new InputStreamReader(urlConnection.getInputStream()), JsonArray.class);
            ArrayList<SugiliteRepoListing> result = new ArrayList<>();
            for (JsonElement e : jsonArray) {
                if (e instanceof JsonObject) {
                    JsonObject o = (JsonObject) e;
                    SugiliteRepoListing listing = new SugiliteRepoListing(o.get("script_id").getAsInt(), o.get("title").getAsString());
                    if (!o.get("author").isJsonNull()) {
                        listing.setAuthor(o.get("author").getAsString());
                    }
                    result.add(listing);
                }
            }

            return result;
        }
    }

    public SugiliteStartingBlock downloadScript (String id) throws ExecutionException, InterruptedException {
        DownloadScriptTask downloadScriptTask = new DownloadScriptTask(id);
        SugiliteStartingBlock script = executor.submit(downloadScriptTask).get();
        return script;
    }

    private class DownloadScriptTask implements Callable<SugiliteStartingBlock> {
        private String id;

        DownloadScriptTask (String id) {
            this.id = id;
        }
        void setId(String id) {
            this.id = id;
        }

        @Override
        public SugiliteStartingBlock call() throws Exception {
            URL downloadUrl = baseUri.resolve(DOWNLOAD_SCRIPT_FROM_REPO_ENDPOINT_PREFIX).resolve(id).toURL();

            HttpURLConnection urlConnection = (HttpURLConnection) downloadUrl.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
            urlConnection.setReadTimeout(TIME_OUT);
            urlConnection.setConnectTimeout(TIME_OUT);

            urlConnection.setDoInput(true);
            urlConnection.setChunkedStreamingMode(0); // this might increase performance?

            urlConnection.connect();

            int responseCode = urlConnection.getResponseCode();

            if (responseCode != HttpURLConnection.HTTP_OK) {
                // TODO scream or something
                Log.e("DownloadScriptTask", "Uh oh");
                throw new Exception("aaa");
            }
            SugiliteStartingBlock downloadedScript = SerializationUtils.deserialize(urlConnection.getInputStream());
            return downloadedScript;
        }
    }

    public String uploadScript (String title, String author, SugiliteStartingBlock script) throws ExecutionException, InterruptedException {
        UploadScriptTask uploadScriptTask = new UploadScriptTask(title, author, script);
        String scriptId = executor.submit(uploadScriptTask).get();
        return scriptId;
    }

    public String uploadScript (String title, SugiliteStartingBlock script) throws ExecutionException, InterruptedException {
        return uploadScript(title, null, script);
    }

    private class UploadScriptTask implements Callable<String> {
        UploadScriptTask (String title, @Nullable String author, SugiliteStartingBlock script) {
            this.title = title;
            this.author = author;
            this.script = script;
        }

        private String title;
        private String author;
        private SugiliteStartingBlock script;

        public SugiliteStartingBlock getScript() {
            return script;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public void setAuthor(String author) {
            this.author = author;
        }

        public void setScript(SugiliteStartingBlock script) {
            this.script = script;
        }

        @Override
        public String call() throws Exception {
            URL uploadScriptURL = baseUri.resolve(UPLOAD_SCRIPT_TO_REPO_ENDPOINT).toURL();
            OkHttpClient client = new OkHttpClient();

            MultipartBody.Builder requestBodyBuilder = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("title", this.title)
                    .addFormDataPart("script", "scriptfile", RequestBody.create(SerializationUtils.serialize(script)));
            if (author != null) requestBodyBuilder = requestBodyBuilder.addFormDataPart("author", author);

            RequestBody requestBody = requestBodyBuilder.build();

            Request request = new Request.Builder()
                    .url(uploadScriptURL)
                    .post(requestBody).build();
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

                String id = response.body().string();
                return id;
            }

        }
    }


    public void uploadHashedUI (HashedUIStrings hashedUIStrings) throws ExecutionException, InterruptedException {
        UploadHashedUITask uploadHashedUITask = new UploadHashedUITask(hashedUIStrings);
        executor.submit(uploadHashedUITask).get();
    }

    private class UploadHashedUITask implements Callable<Void> {
        private HashedUIStrings hashedUIStrings;
        public UploadHashedUITask(HashedUIStrings hashedUIStrings) {
            this.hashedUIStrings = hashedUIStrings;
        }

        @Override
        public Void call() {

            try {
                URL uploadHashedUIUrl = baseUri.resolve(UPLOAD_HASHED_UI_ENDPOINT).toURL();
                HttpURLConnection urlConnection = (HttpURLConnection) uploadHashedUIUrl.openConnection();
                urlConnection.setRequestMethod("POST");
                urlConnection.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
                urlConnection.setRequestProperty("Content-Type", "application/json");
                urlConnection.setReadTimeout(TIME_OUT);
                urlConnection.setConnectTimeout(TIME_OUT);

                urlConnection.setDoOutput(true);
                urlConnection.setDoInput(true);
                urlConnection.setChunkedStreamingMode(0); // this might increase performance?
                BufferedOutputStream out = new BufferedOutputStream(urlConnection.getOutputStream());
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
                writer.write(hashedUIStrings.toJson());
                writer.flush();
                writer.close();
                out.close();
                urlConnection.connect();

                int responseCode = urlConnection.getResponseCode();
                Log.v("PrivacyHashUploader", "uploaded hashed UI with response code " + responseCode);
                return null;
            } catch (IOException e) {
                Log.i("PrivacyHashUploader", "upload hashed ui failed");
                e.printStackTrace();
            } finally {

            }
            return null;

        }
    }

    @TestOnly
    public URL getFilterURL(){
        try {
            URL filterStringUrl = baseUri.resolve(FILTER_UI_STRING_ENDPOINT).toURL();
            return filterStringUrl;
        } catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

}
