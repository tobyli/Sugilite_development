package edu.cmu.hcii.sugilite.sharing;

import android.util.Log;
import edu.cmu.hcii.sugilite.Const;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;
import org.apache.commons.lang3.SerializationUtils;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Callable;

public class DownloadScriptTask implements Callable<SugiliteStartingBlock> {

    private String id;

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public SugiliteStartingBlock call() throws Exception {

        URL downloadUrl = new URL(Const.SHARING_SERVER_BASE_URL + Const.DOWNLOAD_SCRIPT_FROM_REPO_ENDPOINT_PREFIX + id);

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
            Log.e("DownloadScriptTask", "Uh oh");
            throw new Exception("aaa");
        }

        SugiliteStartingBlock downloadedScript = SerializationUtils.deserialize(urlConnection.getInputStream());

        return downloadedScript;
    }
}
