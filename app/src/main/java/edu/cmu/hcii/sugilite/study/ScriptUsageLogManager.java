package edu.cmu.hcii.sugilite.study;

import android.content.Context;

import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Created by toby on 4/27/17.
 */

public class ScriptUsageLogManager {
    Context context;
    DateFormat dateFormat;
    public final static int CREATE_SCRIPT = 1, EXECUTE_SCRIPT = 2, EDIT_SCRIPT = 3, REMOVE_SCRIPT = 4, CLEAR_ALL_SCRIPTS = 5;
    public ScriptUsageLogManager(Context context){
        this.context = context;
        try {
        }
        catch (Exception e){
            e.printStackTrace();
        }
        dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US);
    }

    public void addLog(int type, String scriptName){
        try {
            String directoryPath = context.getFilesDir().getPath().toString();
            CSVWriter csvWriter = new CSVWriter(new FileWriter(directoryPath + "/" + StudyConst.SCRIPT_USAGE_LOG_FILE_NAME, true));
            String[] row = new String[3];
            switch (type){
                case CREATE_SCRIPT:
                    row[0] = "CREATE_SCRIPT";
                    break;
                case EXECUTE_SCRIPT:
                    row[0] = "EXECUTE_SCRIPT";
                    break;
                case EDIT_SCRIPT:
                    row[0] = "EDIT_SCRIPT";
                    break;
                case REMOVE_SCRIPT:
                    row[0] = "REMOVE_SCRIPT";
                    break;
                case CLEAR_ALL_SCRIPTS:
                    row[0] = "CLEAR_ALL_SCRIPTS";
                    break;
                default:
                    row[0] = "UNKNOWN";
            }
            row[1] = scriptName;
            Date date = Calendar.getInstance().getTime();
            row[2] = dateFormat.format(date);
            csvWriter.writeNext(row);
            csvWriter.flush();
            csvWriter.close();
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    public void clearLog() {
        String directoryPath = context.getFilesDir().getPath().toString();
        File file = new File(directoryPath + "/" + StudyConst.SCRIPT_USAGE_LOG_FILE_NAME);
        if (file.exists())
            file.delete();
    }


}
