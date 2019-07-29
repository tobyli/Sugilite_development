package edu.cmu.hcii.sugilite.sharing;

import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import edu.cmu.hcii.sugilite.Const;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.Callable;

public class DownloadRepoListTask implements Callable<ArrayList<DownloadRepoListTask.SugiliteRepoListing>> {

    public static class SugiliteRepoListing {
        public int id;
        public String title;
        public String author;
    }

    @Override
    public ArrayList<SugiliteRepoListing> call() throws Exception {

        URL downloadUrl = new URL(Const.SHARING_SERVER_BASE_URL + Const.DOWNLOAD_REPO_LIST_ENDPOINT);

        HttpURLConnection urlConnection = (HttpURLConnection) downloadUrl.openConnection();
        urlConnection.setRequestMethod("GET");
        urlConnection.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
        urlConnection.setReadTimeout(1 * 3000);
        urlConnection.setConnectTimeout(1 * 3000);

        urlConnection.setDoInput(true);
        urlConnection.setChunkedStreamingMode(0); // this might increase performance?

        urlConnection.connect();

        int responseCode = urlConnection.getResponseCode();

        if (responseCode != HttpURLConnection.HTTP_OK) {
            // TODO scream or something
            Log.e("DownloadRepoListTask", "Uh oh");
            throw new Exception("aaa");
        }

        Gson gson = new Gson();

        JsonArray jsonArray = gson.fromJson(new InputStreamReader(urlConnection.getInputStream()), JsonArray.class);
        ArrayList<SugiliteRepoListing> result = new ArrayList<SugiliteRepoListing>();
        for (JsonElement e : jsonArray) {
            if (e instanceof JsonObject) {
                JsonObject o = (JsonObject) e;
                SugiliteRepoListing listing = new SugiliteRepoListing();
                listing.id = o.get("script_id").getAsInt();
                listing.title = o.get("title").getAsString();
                if (!o.get("author").isJsonNull()) listing.author = o.get("author").getAsString();
                result.add(listing);
            }
        }

        return result;
    }
}
