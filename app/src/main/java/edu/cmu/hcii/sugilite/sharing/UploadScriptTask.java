package edu.cmu.hcii.sugilite.sharing;

import com.google.api.client.util.Charsets;
import com.google.common.io.CharStreams;
import edu.cmu.hcii.sugilite.Const;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;
import okhttp3.*;
import org.apache.commons.lang3.SerializationUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
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
        OkHttpClient client = new OkHttpClient();

        MultipartBody.Builder requestBodyBuilder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("title", this.title)
                .addFormDataPart("script", "scriptfile", RequestBody.create(SerializationUtils.serialize(script)));
        if (author != null) requestBodyBuilder = requestBodyBuilder.addFormDataPart("author", author);

        RequestBody requestBody = requestBodyBuilder.build();

        Request request = new Request.Builder()
                .url(Const.SHARING_SERVER_BASE_URL + Const.UPLOAD_SCRIPT_TO_REPO_ENDPOINT)
                .post(requestBody).build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

            String id = response.body().string();
            return id;
        }

    }
}
