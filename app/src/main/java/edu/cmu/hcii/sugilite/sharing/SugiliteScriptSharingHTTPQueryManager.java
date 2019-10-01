package edu.cmu.hcii.sugilite.sharing;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.ArraySet;
import android.util.Log;
import android.widget.Toast;

import com.google.common.collect.Multimap;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.apache.commons.lang3.SerializationUtils;
import org.jetbrains.annotations.TestOnly;
import org.json.JSONArray;

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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.Nullable;

import edu.cmu.hcii.sugilite.R;
import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;
import edu.cmu.hcii.sugilite.pumice.PumiceDemonstrationUtil;
import edu.cmu.hcii.sugilite.sharing.model.HashedString;
import edu.cmu.hcii.sugilite.sharing.model.HashedUIStrings;
import edu.cmu.hcii.sugilite.sharing.model.StringInContext;
import edu.cmu.hcii.sugilite.sharing.model.StringInContextWithIndexAndPriority;
import edu.cmu.hcii.sugilite.sharing.model.SugiliteRepoListing;
import edu.cmu.hcii.sugilite.ui.dialog.SugiliteProgressDialog;
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

    private static final String DEFAULT_SERVER_URL = "http://35.207.16.161:4567/";
    private static final String DOWNLOAD_REPO_LIST_ENDPOINT = "repo/list";
    private static final String DOWNLOAD_SCRIPT_FROM_REPO_ENDPOINT_PREFIX = "repo/";
    private static final String UPLOAD_SCRIPT_TO_REPO_ENDPOINT = "repo/upload";
    private static final String UPLOAD_HASHED_UI_ENDPOINT = "privacy/upload_ui";
    private static final String FILTER_UI_STRING_ENDPOINT = "privacy/filter";
    private static final String SALTED_HASH_QUERY_ENDPOINT = "/privacy/hash";


    private static final int TIME_OUT_THRESHOLD = 3000;

    private ExecutorService executor;
    private SharedPreferences sharedPreferences;

    public static SugiliteScriptSharingHTTPQueryManager getInstance(Context context) {
        if (instance == null) {
            instance = new SugiliteScriptSharingHTTPQueryManager(context);
        }
        return instance;
    }

    private SugiliteScriptSharingHTTPQueryManager(Context context) {
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        this.executor = Executors.newSingleThreadExecutor();
    }

    private URI getBaseUri(){
        URI baseUri;
        String urlString = "null";
        if (sharedPreferences != null) {
            urlString = sharedPreferences.getString("script_sharing_server_address", "null");
        }
        try {
            if (urlString.equals("null")) {
                baseUri = new URI(DEFAULT_SERVER_URL);
                Log.e("SugiliteScriptSharingHTTPQueryManager", "Failed getting URL from preferencess");
            } else {
                baseUri = new URI(urlString);
            }
        } catch (URISyntaxException e) {
            throw new RuntimeException("malformed URL!");
        }
        return baseUri;
    }

    public List<SugiliteRepoListing> getRepoList() throws ExecutionException, InterruptedException {
        SugiliteProgressDialog progressDialog = new SugiliteProgressDialog(SugiliteData.getAppContext(), R.string.loading_remote_script_list_message);
        progressDialog.show();

        DownloadRepoListTask repoListTask = new DownloadRepoListTask();
        List<SugiliteRepoListing> repo = new ArrayList<>();
        try {
            repo = executor.submit(repoListTask).get();
        } catch (Exception e) {
            PumiceDemonstrationUtil.showSugiliteToast("Connection Failed", Toast.LENGTH_SHORT);

        } finally {
            progressDialog.dismiss();
        }
        return repo;
    }

    private class DownloadRepoListTask implements Callable<ArrayList<SugiliteRepoListing>> {
        @Override
        public ArrayList<SugiliteRepoListing> call() throws Exception {
            URL downloadUrl = getBaseUri().resolve(DOWNLOAD_REPO_LIST_ENDPOINT).toURL();

            HttpURLConnection urlConnection = (HttpURLConnection) downloadUrl.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
            urlConnection.setReadTimeout(TIME_OUT_THRESHOLD);
            urlConnection.setConnectTimeout(TIME_OUT_THRESHOLD);

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

    public SugiliteStartingBlock downloadScript(String id) throws ExecutionException, InterruptedException {
        SugiliteProgressDialog progressDialog = new SugiliteProgressDialog(SugiliteData.getAppContext(), R.string.downloading_script_message);
        progressDialog.show();

        DownloadScriptTask downloadScriptTask = new DownloadScriptTask(id);
        SugiliteStartingBlock script = null;
        try {
            script = executor.submit(downloadScriptTask).get();
        } catch (Exception e){
            PumiceDemonstrationUtil.showSugiliteToast("Connection Failed", Toast.LENGTH_SHORT);

        } finally {
            progressDialog.dismiss();
        }
        return script;
    }

    private class DownloadScriptTask implements Callable<SugiliteStartingBlock> {
        private String id;

        DownloadScriptTask(String id) {
            this.id = id;
        }

        void setId(String id) {
            this.id = id;
        }

        @Override
        public SugiliteStartingBlock call() throws Exception {
            URL downloadUrl = getBaseUri().resolve(DOWNLOAD_SCRIPT_FROM_REPO_ENDPOINT_PREFIX).resolve(id).toURL();

            HttpURLConnection urlConnection = (HttpURLConnection) downloadUrl.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
            urlConnection.setReadTimeout(TIME_OUT_THRESHOLD);
            urlConnection.setConnectTimeout(TIME_OUT_THRESHOLD);

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

    public String uploadScript(String title, String author, SugiliteStartingBlock script) throws ExecutionException, InterruptedException {
        SugiliteProgressDialog progressDialog = new SugiliteProgressDialog(SugiliteData.getAppContext(), R.string.uploading_script_message);
        progressDialog.show();

        UploadScriptTask uploadScriptTask = new UploadScriptTask(title, author, script);
        String scriptId = "";
        try {
            scriptId = executor.submit(uploadScriptTask).get();
        } catch (Exception e) {
            PumiceDemonstrationUtil.showSugiliteToast("Connection Failed", Toast.LENGTH_SHORT);

        } finally {
            progressDialog.dismiss();
        }
        return scriptId;
    }

    public String uploadScript(String title, SugiliteStartingBlock script) throws ExecutionException, InterruptedException {
        return uploadScript(title, null, script);
    }

    private class UploadScriptTask implements Callable<String> {
        UploadScriptTask(String title, @Nullable String author, SugiliteStartingBlock script) {
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
            URL uploadScriptURL = getBaseUri().resolve(UPLOAD_SCRIPT_TO_REPO_ENDPOINT).toURL();
            OkHttpClient client = new OkHttpClient();

            MultipartBody.Builder requestBodyBuilder = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("title", this.title)
                    .addFormDataPart("script", "scriptfile", RequestBody.create(SerializationUtils.serialize(script)));
            if (author != null)
                requestBodyBuilder = requestBodyBuilder.addFormDataPart("author", author);

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


    public void uploadHashedUI(HashedUIStrings hashedUIStrings) throws ExecutionException, InterruptedException {
        //no progress dialog because this happends in the background
        UploadHashedUITask uploadHashedUITask = new UploadHashedUITask(hashedUIStrings);
        executor.submit(uploadHashedUITask).get();
    }

    private class UploadHashedUITask implements Callable<Void> {
        private HashedUIStrings hashedUIStrings;

        UploadHashedUITask(HashedUIStrings hashedUIStrings) {
            this.hashedUIStrings = hashedUIStrings;
        }

        @Override
        public Void call() {

            try {
                URL uploadHashedUIUrl = getBaseUri().resolve(UPLOAD_HASHED_UI_ENDPOINT).toURL();
                HttpURLConnection urlConnection = (HttpURLConnection) uploadHashedUIUrl.openConnection();
                urlConnection.setRequestMethod("POST");
                urlConnection.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
                urlConnection.setRequestProperty("Content-Type", "application/json");
                urlConnection.setReadTimeout(TIME_OUT_THRESHOLD);
                urlConnection.setConnectTimeout(TIME_OUT_THRESHOLD);

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

    public GetFilteredStringsTaskResult getFilteredStrings(Set<StringInContextWithIndexAndPriority> queryStrings, Map<StringInContext, Integer> originalQueryStringsIndex, Multimap<HashedString, StringInContextWithIndexAndPriority> decodedStrings) throws Exception {
        SugiliteProgressDialog progressDialog = new SugiliteProgressDialog(SugiliteData.getAppContext(), R.string.filtering_personal_information_message);
        progressDialog.show();
        try {
            GetFilteredStringsTask getFilteredStringsTask = new GetFilteredStringsTask(queryStrings, originalQueryStringsIndex, decodedStrings);
            GetFilteredStringsTaskResult result = executor.submit(getFilteredStringsTask).get();
            return result;
        } catch (Exception e) {
            PumiceDemonstrationUtil.showSugiliteToast("Connection Failed", Toast.LENGTH_SHORT);

        } finally {
            progressDialog.dismiss();
        }

        return null;
    }

    public class GetFilteredStringsTaskResult {
        private Set<StringInContext> nonPrivateStringInContextSet;
        private Map<StringInContext, String> privateStringInContextSaltedHashMap;
        private Map<Integer, StringAlternativeGenerator.StringAlternative> indexStringAlternativeMap;

        public GetFilteredStringsTaskResult() {
            this.nonPrivateStringInContextSet = new HashSet<>();
            this.privateStringInContextSaltedHashMap = new HashMap<>();
            this.indexStringAlternativeMap = new HashMap<>();
        }

        public Map<StringInContext, String> getPrivateStringInContextSaltedHashMap() {
            return privateStringInContextSaltedHashMap;
        }

        public Set<StringInContext> getNonPrivateStringInContextSet() {
            return nonPrivateStringInContextSet;
        }

        public Map<Integer, StringAlternativeGenerator.StringAlternative> getIndexStringAlternativeMap() {
            return indexStringAlternativeMap;
        }
    }


    private class GetFilteredStringsTask implements Callable<GetFilteredStringsTaskResult> {
        private Set<StringInContextWithIndexAndPriority> queryStrings;
        private Map<StringInContext, Integer> originalQueryStringsIndex;
        private Multimap<HashedString, StringInContextWithIndexAndPriority> decodedStrings;

        GetFilteredStringsTask(Set<StringInContextWithIndexAndPriority> queryStrings, Map<StringInContext, Integer> originalQueryStringsIndex, Multimap<HashedString, StringInContextWithIndexAndPriority> decodedStrings) {
            this.queryStrings = queryStrings;
            this.originalQueryStringsIndex = originalQueryStringsIndex;
            this.decodedStrings = decodedStrings;
        }

        @Override
        public GetFilteredStringsTaskResult call() throws Exception {
            try {
                URL filterStringUrl = getBaseUri().resolve(FILTER_UI_STRING_ENDPOINT).toURL();
                HttpURLConnection urlConnection = (HttpURLConnection) filterStringUrl.openConnection();
                urlConnection.setRequestMethod("POST");
                urlConnection.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
                urlConnection.setRequestProperty("Content-Type", "application/json");
                urlConnection.setReadTimeout(TIME_OUT_THRESHOLD);
                urlConnection.setConnectTimeout(TIME_OUT_THRESHOLD);

                urlConnection.setDoOutput(true);
                urlConnection.setDoInput(true);
                urlConnection.setChunkedStreamingMode(0); // this might increase performance?
                BufferedOutputStream out = new BufferedOutputStream(urlConnection.getOutputStream());
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));

                writer.write("[");
                boolean first = true;
                for (StringInContextWithIndexAndPriority s : queryStrings) {
                    if (!first) writer.write(',');
                    writer.write(s.toHashedJson());
                    first = false;
                }
                writer.write("]");
                writer.flush();
                writer.close();
                out.close();
                urlConnection.connect();

                int responseCode = urlConnection.getResponseCode();

                if (responseCode != HttpURLConnection.HTTP_OK) {
                    throw new Exception("Failed string filtering request");
                }

                Gson g = new Gson();
                GetFilteredStringsTaskResult result = new GetFilteredStringsTaskResult();
                JsonObject serverResponseObject = g.fromJson(new InputStreamReader(urlConnection.getInputStream()), JsonObject.class);

                JsonArray nonPrivateArray = serverResponseObject.getAsJsonArray("nonPrivate");
                for (JsonElement entryElement : nonPrivateArray) {
                    JsonObject entryObject = entryElement.getAsJsonObject();

                    HashedString hashedString = HashedString.fromEncodedString(entryObject.get("text_hash").getAsString(), false);
                    for (StringInContextWithIndexAndPriority s : decodedStrings.get(hashedString)) {
                        if (entryObject.get("activity").getAsString().equals(s.getActivityName()) && entryObject.get("package").getAsString().equals(s.getPackageName())) {
                            //matched
                            if (result.getIndexStringAlternativeMap().containsKey(s.getIndex())) {
                                if (s.getPriority() <= result.getIndexStringAlternativeMap().get(s.getIndex()).priority) {
                                    result.getIndexStringAlternativeMap().put(s.getIndex(), new StringAlternativeGenerator.StringAlternative(s.getText(), s.getPriority(), s.getPriority() > 0 ? StringAlternativeGenerator.PATTERN_MATCH_TYPE : StringAlternativeGenerator.ORIGINAL_TYPE));
                                }
                            } else {
                                result.getIndexStringAlternativeMap().put(s.getIndex(), new StringAlternativeGenerator.StringAlternative(s.getText(), s.getPriority(), s.getPriority() > 0 ? StringAlternativeGenerator.PATTERN_MATCH_TYPE : StringAlternativeGenerator.ORIGINAL_TYPE));
                            }
                        }
                    }

                    //debug
                    StringInContextWithIndexAndPriority entry = new StringInContextWithIndexAndPriority(entryObject.get("activity").getAsString(), entryObject.get("package").getAsString(), entryObject.get("text_hash").getAsString());
                    result.getNonPrivateStringInContextSet().add(entry);
                }

                JsonArray privateArray = serverResponseObject.getAsJsonArray("private");
                for (JsonElement entrySaltedHashElement : privateArray) {
                    JsonObject entrySaltedHashObject = entrySaltedHashElement.getAsJsonObject();
                    JsonObject entryObject = entrySaltedHashObject.get("entry").getAsJsonObject();
                    String saltedHash = entrySaltedHashObject.get("salted_hash").getAsString();

                    HashedString hashedString = HashedString.fromEncodedString(entryObject.get("text_hash").getAsString(), false);
                    for (StringInContextWithIndexAndPriority s : decodedStrings.get(hashedString)) {
                        if (entryObject.get("activity").getAsString().equals(s.getActivityName()) && entryObject.get("package").getAsString().equals(s.getPackageName())) {
                            //matched
                            StringInContext stringInContext = new StringInContext(s.getActivityName(), s.getPackageName(), s.getText());
                            if (originalQueryStringsIndex.containsKey(stringInContext)) {
                                result.getIndexStringAlternativeMap().put(originalQueryStringsIndex.get(stringInContext), new StringAlternativeGenerator.StringAlternative(saltedHash, Integer.MAX_VALUE, StringAlternativeGenerator.HASH_TYPE));
                            }
                        }
                    }

                    //debug
                    StringInContextWithIndexAndPriority entry = new StringInContextWithIndexAndPriority(entryObject.get("activity").getAsString(), entryObject.get("package").getAsString(), entryObject.get("text_hash").getAsString());
                    result.getPrivateStringInContextSaltedHashMap().put(entry, saltedHash);
                }

              return result;
            } catch (Exception e) {
                Log.e("GetFilteredStringsTask", "Failed HTTP Request");
                throw new Exception("Failure when filtering the script");
            }
        }
    }

    public Map<String, HashedString> getServerSaltedHash(Set<String> strings) {
        try {
            GetServerSaltedHashTask getServerSaltedHashTask = new GetServerSaltedHashTask(strings);
            Map<String, HashedString> originalStringServerSaltedHashMap = getServerSaltedHashTask.call();
            return originalStringServerSaltedHashMap;
        } catch (Exception e) {
            PumiceDemonstrationUtil.showSugiliteToast("Connection Failed", Toast.LENGTH_SHORT);

        } finally {

        }

        return null;
    }

    private class GetServerSaltedHashTask implements Callable<Map<String, HashedString>> {
        private Set<String> strings;

        GetServerSaltedHashTask(Set<String> strings) {
            this.strings = strings;
        }

        @Override
        public Map<String, HashedString> call() throws Exception {
            try {
                URL filterStringUrl = getBaseUri().resolve(SALTED_HASH_QUERY_ENDPOINT).toURL();
                HttpURLConnection urlConnection = (HttpURLConnection) filterStringUrl.openConnection();
                urlConnection.setRequestMethod("POST");
                urlConnection.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
                urlConnection.setRequestProperty("Content-Type", "application/json");
                urlConnection.setReadTimeout(TIME_OUT_THRESHOLD);
                urlConnection.setConnectTimeout(TIME_OUT_THRESHOLD);

                urlConnection.setDoOutput(true);
                urlConnection.setDoInput(true);
                urlConnection.setChunkedStreamingMode(0); // this might increase performance?
                BufferedOutputStream out = new BufferedOutputStream(urlConnection.getOutputStream());
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));

                Gson g = new Gson();
                JsonObject jsonObject = new JsonObject();
                jsonObject.add("text", g.toJsonTree(strings));
                writer.write(g.toJson(jsonObject));
                writer.flush();
                writer.close();
                out.close();
                urlConnection.connect();

                int responseCode = urlConnection.getResponseCode();

                if (responseCode != HttpURLConnection.HTTP_OK) {
                    throw new Exception("Failed string filtering request");
                }

                Map<String, HashedString> result = new HashMap<>();
                JsonArray serverResponseArray = g.fromJson(new InputStreamReader(urlConnection.getInputStream()), JsonArray.class);
                for (JsonElement e : serverResponseArray) {
                    JsonObject o = e.getAsJsonObject();
                    result.put(o.get("text").getAsString(), HashedString.fromEncodedString(o.get("hash").getAsString(), true));
                }

                return result;
            } catch (Exception e) {
                Log.e("GetFilteredStringsTask", "Failed HTTP Request");
                throw new Exception("Failure when filtering the script");
            }
        }
    }




}


