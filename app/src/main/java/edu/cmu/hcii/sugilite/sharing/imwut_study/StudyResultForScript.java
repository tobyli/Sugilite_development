package edu.cmu.hcii.sugilite.sharing.imwut_study;

import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.cmu.hcii.sugilite.Const;
import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.model.operation.SugiliteOperation;
import edu.cmu.hcii.sugilite.pumice.PumiceDemonstrationUtil;
import edu.cmu.hcii.sugilite.sharing.StringAlternativeGenerator;
import edu.cmu.hcii.sugilite.sharing.model.StringInContext;

/**
 * @author toby
 * @date 11/10/19
 * @time 2:26 PM
 */
public class StudyResultForScript {
    public String scriptName;
    public Set<StringInContext> informationEntriesInDataDescription;
    public Set<StringInContext> informationEntriesInUISnapshots;
    public Map<StringInContext, StringAlternativeGenerator.StringAlternative> filterdInformationEntriesInDataDescription;
    public Map<StringInContext, StringAlternativeGenerator.StringAlternative> filterdInformationEntriesInUISnapshots;
    public List<SugiliteOperation> operations;


    public StudyResultForScript() {
        this.informationEntriesInDataDescription = new LinkedHashSet<>();
        this.informationEntriesInUISnapshots = new LinkedHashSet<>();
        this.operations = new ArrayList<>();
    }


    public void saveToFile () throws IOException {
        File rootDataDir = new File("/sdcard/Download");
        File scriptDir = new File(rootDataDir.getPath() + "/imwut_study_data");
        if (!scriptDir.exists() || !scriptDir.isDirectory()) {
            scriptDir.mkdir();
        }
        Date time = Calendar.getInstance().getTime();
        String timeString = Const.dateFormat.format(time);
        String newPath = scriptDir.getPath() + "/" + scriptName + "_" + timeString + ".json";
        File newFileToWrite = new File(newPath);
        saveToFile(newFileToWrite);
    }

    public void saveToFile (File file) throws IOException {

        PrintWriter out = null;
        Gson gson = new Gson();
        try {
            out = new PrintWriter(file);
            out.println(gson.toJson(this));
        }
        catch (Exception e){
            e.printStackTrace();
            throw e;
            //TODO: error handling
        }
        finally {
            if (out != null) {
                out.close();
            }

            PumiceDemonstrationUtil.showSugiliteToast("Wrote to " + file.getAbsolutePath(), Toast.LENGTH_SHORT);
            Log.i(StudyResultForScript.class.getName(), "Wrote to " + file.getAbsolutePath());
        }
    }



}
