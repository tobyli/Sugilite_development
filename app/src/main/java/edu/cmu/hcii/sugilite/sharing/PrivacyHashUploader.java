package edu.cmu.hcii.sugilite.sharing;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import com.google.gson.Gson;
import edu.cmu.hcii.sugilite.Const;
import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.sharing.debug.HasPlaintext;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;

public class PrivacyHashUploader {

    // TODO make this server address permanent

    private SugiliteScriptSharingHTTPQueryManager sugiliteScriptSharingHTTPQueryManager;

    public PrivacyHashUploader(Context context) {
        this.sugiliteScriptSharingHTTPQueryManager = SugiliteScriptSharingHTTPQueryManager.getInstance(context);
        /*
        try {
            uploadHashedUIUrl = new URL(Const.SHARING_SERVER_BASE_URL + Const.UPLOAD_HASHED_UI_ENDPOINT);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }*/
    }

    private static String jsonProperty(String key, String value) {
        return "\"" + key + "\": \"" + value + "\"";
    }

    private static String jsonProperty(String key, int value) {
        return "\"" + key + "\": " + value;
    }

    private static String hashedTextToJson(HashedSplitString hashedText) {
        StringBuilder sb = new StringBuilder("{\n");

        sb.append(jsonProperty("text_hash", hashedText.preferred.toString()));
        sb.append(",\n");

        if (hashedText instanceof HasPlaintext) {
            sb.append(jsonProperty("debug_text", ((HasPlaintext)hashedText).getPlaintext()));
            sb.append(",\n");
        }

        sb.append("\"derived_hashes\": [\n");

        for (int i = 0; i < hashedText.alternatives.size(); i++) {
            HashedSubString subString = hashedText.alternatives.get(i);

            if (i != 0) sb.append(",\n");
            sb.append("{\n");

            sb.append(jsonProperty("text_hash", subString.toString()));
            sb.append(",\n");

            if (subString instanceof HasPlaintext) {
                sb.append(jsonProperty("debug_text", ((HasPlaintext)subString).getPlaintext()));
                sb.append(",\n");
            }

            sb.append(jsonProperty("tokens_removed", subString.priority));

            sb.append("}");
        }

        sb.append("\n]}");
        return sb.toString();
    }

    public static String hashedUIToJson(HashedUIStrings ui) {
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

    public void uploadHashedUI(HashedUIStrings ui) throws ExecutionException, InterruptedException {
        sugiliteScriptSharingHTTPQueryManager.uploadHashedUI(ui);
    }

}
