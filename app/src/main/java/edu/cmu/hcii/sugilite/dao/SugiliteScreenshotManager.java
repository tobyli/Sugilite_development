package edu.cmu.hcii.sugilite.dao;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Environment;
import android.view.View;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * @author toby
 * @date 6/17/16
 * @time 6:33 PM
 */
public class SugiliteScreenshotManager {
    private SimpleDateFormat dateFormat;
    private SharedPreferences sharedPreferences;
    private Context appContext;
    public SugiliteScreenshotManager(SharedPreferences sharedPreferences, Context appContext){
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("US/Eastern"));
        this.sharedPreferences = sharedPreferences;
        this.appContext = appContext;
    }

    /**
     * Take a screenshot of the current screen
     * @return the screenshot
     */
    public File take(boolean waitForCompletion) throws RuntimeException, IOException, InterruptedException {
        boolean rootEnabled = sharedPreferences.getBoolean("root_enabled", false);
        if(!rootEnabled){
            throw new RuntimeException("Root access denied!");
        }

        Process sh = Runtime.getRuntime().exec("su", null,null);
        OutputStream os = sh.getOutputStream();
        Date date = Calendar.getInstance().getTime();
        String fileName = "sugilite_screenshot_" + dateFormat.format(date) + ".png";
        File screenshotDirectory = new File("/sdcard/sugilite_screenshot");
        if(!screenshotDirectory.exists())
            screenshotDirectory.mkdirs();
        os.write(("/system/bin/screencap -p " + "/sdcard/sugilite_screenshot/" + fileName).getBytes("ASCII"));
        os.flush();
        os.close();
        File retVal = new File("/sdcard/sugilite_screenshot/" + fileName);
        if(waitForCompletion){
            sh.waitFor();
            if(retVal.exists())
                return retVal;
            else
                throw new RuntimeException("Error in getting screenshot");
        }
        else
            return retVal;
    }




}
