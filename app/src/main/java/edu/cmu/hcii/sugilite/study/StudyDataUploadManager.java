package edu.cmu.hcii.sugilite.study;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.database.Cursor;
import android.os.Build;
import android.provider.ContactsContract;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.communication.SugiliteBlockJSONProcessor;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;

import static edu.cmu.hcii.sugilite.R.id.textView;

/**
 * Created by toby on 4/27/17.
 */

public class StudyDataUploadManager {
    private Context context;
    private String clientIdentifier;
    private String uploadURL;
    private BluetoothAdapter mBluetoothAdapter;
    private SugiliteData sugiliteData;

    public StudyDataUploadManager(Context context, SugiliteData sugiliteData){
        this.context = context;
        String deviceModel = Build.MODEL;
        this.sugiliteData = sugiliteData;
        this.clientIdentifier = deviceModel + getLocalBluetoothName() + "_" + getOwnerName();
        uploadURL = StudyConst.STUDY_UPLOAD_URL;
    }

    public void uploadScript(String filePath, long timeStamp) throws IOException, FileNotFoundException {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US);
        Date time = new Date(timeStamp);
        String timeStampString = dateFormat.format(time);
        (new StudyDataScriptFileUploadTask(context)).execute(filePath, uploadURL, clientIdentifier, timeStampString);
    }

    public void uploadScriptJSON(SugiliteStartingBlock script) throws IOException{
        SugiliteBlockJSONProcessor jsonProcessor = new SugiliteBlockJSONProcessor(context);
        String directoryPath = context.getFilesDir().getPath().toString();
        String filePath = directoryPath + "/" + script.getScriptName() + ".json";
        PrintWriter out = new PrintWriter(new FileOutputStream(filePath, false));
        out.print(jsonProcessor.scriptToJson(script));
        out.flush();
        out.close();
        uploadScript(filePath, script.getCreatedTime());
    }
    public String getLocalBluetoothName(){
        if(mBluetoothAdapter == null){
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        }
        String name = mBluetoothAdapter.getName();
        if(name == null){
            System.out.println("Name is null!");
            name = mBluetoothAdapter.getAddress();
        }
        return name;
    }

    public String getOwnerName(){
        try {
            Cursor c = context.getContentResolver().query(ContactsContract.Profile.CONTENT_URI, null, null, null, null);
            c.moveToFirst();
            String name = c.getString(c.getColumnIndex("display_name"));
            c.close();
            if (name != null)
                return name;
            else
                return "NULL";
        }
        catch (Exception e){
            e.printStackTrace();
            return "NULL";
        }
    }




}
