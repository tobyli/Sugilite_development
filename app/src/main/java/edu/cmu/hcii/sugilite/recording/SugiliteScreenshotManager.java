package edu.cmu.hcii.sugilite.recording;

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
    private SharedPreferences sharedPreferences;
    private Context appContext;
    public SugiliteScreenshotManager(SharedPreferences sharedPreferences, Context appContext){
        this.sharedPreferences = sharedPreferences;
        this.appContext = appContext;
    }
    public static final String DIRECTORY_PATH = "/sdcard/sugilite_screenshot";

    /**
     * Take a screen shot of the current screen
     *
     * @param waitForCompletion whether the manager should wait (and block all other UI events and IOs) for taking the screenshot
     * @param directoryPath
     * @param fileName
     * @return a File object for the screenshot taken
     * @throws RuntimeException
     * @throws IOException
     * @throws InterruptedException
     */
    public File take(boolean waitForCompletion, String directoryPath, String fileName) throws RuntimeException, IOException, InterruptedException {
        boolean rootEnabled = sharedPreferences.getBoolean("root_enabled", false);
        if(!rootEnabled){
            throw new RuntimeException("Root access denied!");
        }

        Process sh = Runtime.getRuntime().exec("su", null,null);
        OutputStream os = sh.getOutputStream();

        File screenshotDirectory = new File(directoryPath);
        if(!screenshotDirectory.exists())
            screenshotDirectory.mkdirs();
        os.write(("/system/bin/screencap -p " + directoryPath + "/" + fileName).getBytes("ASCII"));
        os.flush();
        os.close();
        File retVal = new File(directoryPath + "/" + fileName);
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

    public static String getScreenshotFileNameWithDate(){
        Date date = Calendar.getInstance().getTime();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("US/Eastern"));
        String fileName = "sugilite_screenshot_" + dateFormat.format(date) + ".png";
        return fileName;
    }

    public static String getDebugScreenshotFileNameWithDate(){
        Date date = Calendar.getInstance().getTime();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("US/Eastern"));
        String fileName = "sugilite_debug_screenshot_" + dateFormat.format(date) + ".png";
        return fileName;
    }







}
