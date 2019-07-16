package edu.cmu.hcii.sugilite.sharing;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import edu.cmu.hcii.sugilite.ontology.SerializableUISnapshot;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class PrivacyHashUploader {

    // TODO make this server address permanent
    private final String DEFAULT_SERVER_URL = "http://128.237.120.186:8080/";
    private final String UPLOAD_HASHED_UI_ENDPOINT = "privacy/upload_ui";

    private URL uploadHashedUIUrl;

    public PrivacyHashUploader() {
        try {
            uploadHashedUIUrl = new URL(DEFAULT_SERVER_URL + UPLOAD_HASHED_UI_ENDPOINT);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    private static String jsonProperty(String key, String value) {
        return "\"" + key + "\": \"" + value + "\"";
    }

    private static String jsonProperty(String key, int value) {
        return "\"" + key + "\": " + value;
    }

    public static String hashedTextToJson(HashedSplitString hashedText) {
        StringBuilder sb = new StringBuilder("{\n");

        sb.append(jsonProperty("text_hash", hashedText.preferred.toString()));
        sb.append(",\n");

        sb.append("\"derived_hashes\": [\n");

        for (int i = 0; i < hashedText.alternatives.size(); i++) {
            HashedSubString subString = hashedText.alternatives.get(i);

            if (i != 0) sb.append(",\n");
            sb.append("{\n");

            sb.append(jsonProperty("text_hash", subString.toString()));
            sb.append(",\n");
            sb.append(jsonProperty("tokens_removed", subString.priority));

            sb.append("}");
        }

        sb.append("\n]}");
        return sb.toString();
    }

    public static String hashedUIToJson(HashedUI ui) {
        StringBuilder sb = new StringBuilder("{\n");

        sb.append(jsonProperty("package", ui.packageName));
        sb.append(",\n");

        sb.append(jsonProperty("activity", ui.activityName));
        sb.append(",\n");

        sb.append(jsonProperty("package_user_hash", ui.packageUserHash.toString()));
        sb.append(",\n");

        sb.append("\"text_hashes\":\n[\n");

        for (int i = 0; i < ui.hashedTexts.size(); i++) {
            if (i != 0) sb.append(",\n");
            sb.append(hashedTextToJson(ui.hashedTexts.get(i)));
        }

        sb.append("\n]\n}");
        return sb.toString();
    }

    public void uploadHashedUI(HashedUI ui) {
        new UploadHashedUITask().execute(ui);
    }

    private class UploadHashedUITask extends AsyncTask<HashedUI, Void, Integer> {
        @Override
        protected Integer doInBackground(HashedUI... hashedUIS) {
            if (hashedUIS.length != 1) return 0;
            HashedUI ui = hashedUIS[0];
            try {
                HttpURLConnection urlConnection = (HttpURLConnection) uploadHashedUIUrl.openConnection();
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
                writer.write(hashedUIToJson(ui));
                writer.flush();
                writer.close();
                out.close();
                urlConnection.connect();

                int responseCode = urlConnection.getResponseCode();
                Log.v("PrivacyHashUploader", "uploaded hashed UI with response code " + responseCode);
                return responseCode;
            } catch (SocketTimeoutException e) {
                Log.i("PrivacyHashUploader", "upload hashed UI timed out");
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
            }
            return 400;

        }
    }
}
