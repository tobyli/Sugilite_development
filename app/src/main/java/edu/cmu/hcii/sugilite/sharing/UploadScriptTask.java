package edu.cmu.hcii.sugilite.sharing;

import android.util.Base64;
import android.util.Log;
import com.google.api.client.util.Charsets;
import com.google.api.client.util.IOUtils;
import com.google.common.io.CharStreams;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import edu.cmu.hcii.sugilite.Const;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;
import org.apache.commons.lang3.SerializationUtils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Callable;

public class UploadScriptTask implements Callable<String> {

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
        URL filterStringUrl = new URL(Const.SHARING_SERVER_BASE_URL + Const.UPLOAD_SCRIPT_TO_REPO_ENDPOINT);
        HttpURLConnection urlConnection = (HttpURLConnection) filterStringUrl.openConnection();
        urlConnection.setRequestMethod("POST");
        urlConnection.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
        urlConnection.setRequestProperty("Content-Type", "application/json");
        urlConnection.setReadTimeout(1 * 3000);
        urlConnection.setConnectTimeout(1 * 3000);

        urlConnection.setDoOutput(true);
        urlConnection.setDoInput(true);
        urlConnection.setChunkedStreamingMode(0); // this might increase performance?
        BufferedOutputStream out = new BufferedOutputStream(urlConnection.getOutputStream());
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));

        Gson gson = new Gson();
        JsonObject obj = new JsonObject();
        obj.addProperty("title", this.title);
        if (author != null) obj.addProperty("author", this.author);
        obj.addProperty("script", android.util.Base64.encodeToString(SerializationUtils.serialize(script), Base64.DEFAULT));

        writer.write(gson.toJson(obj));

        writer.flush();
        writer.close();
        out.close();
        urlConnection.connect();

        int responseCode = urlConnection.getResponseCode();

        if (responseCode != HttpURLConnection.HTTP_OK) {
            // TODO scream or something
            Log.e("UploadScriptTask", "Uh oh");
            throw new Exception("aaa");
        }

        String id = CharStreams.toString(new InputStreamReader(urlConnection.getInputStream(), Charsets.UTF_8));
        return id;
    }
}
