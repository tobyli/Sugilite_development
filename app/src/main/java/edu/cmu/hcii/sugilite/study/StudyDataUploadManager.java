package edu.cmu.hcii.sugilite.study;

import android.content.Context;
import android.os.Build;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import edu.cmu.hcii.sugilite.communication.SugiliteBlockJSONProcessor;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;

/**
 * Created by toby on 4/27/17.
 */

public class StudyDataUploadManager {
    private Context context;
    private String clientIdentifier;
    private String uploadURL;

    public StudyDataUploadManager(Context context){
        this.context = context;
        String deviceModel = Build.MODEL;
        this.clientIdentifier = "xxx";
        uploadURL = StudyConst.STUDY_UPLOAD_URL;
    }

    public void uploadScript(String filePath) throws IOException, FileNotFoundException {
        (new StudyDataScriptFileUploadTask(context)).execute(filePath, uploadURL, clientIdentifier);
    }

    public void uploadScriptJSON(SugiliteStartingBlock script) throws IOException{
        SugiliteBlockJSONProcessor jsonProcessor = new SugiliteBlockJSONProcessor(context);
        PrintWriter out = new PrintWriter(new FileOutputStream(script.getScriptName() + ".json", false));
        out.print(jsonProcessor.scriptToJson(script));
    }

}
