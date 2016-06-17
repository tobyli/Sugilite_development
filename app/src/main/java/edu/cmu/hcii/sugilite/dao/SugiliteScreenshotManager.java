package edu.cmu.hcii.sugilite.dao;

import android.graphics.Bitmap;
import android.os.Environment;
import android.view.View;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
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
    public SugiliteScreenshotManager(){
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("US/Eastern"));
    }

    /**
     *
     * @param rootView pass in the rootView of activity
     * @return
     */
    public File take(View rootView){
        Date now = new Date();
        String dateString = dateFormat.format(now);
        try {
            // image naming and path  to include sd card  appending name you choose for file
            String mPath = Environment.getExternalStorageDirectory().toString() + "/" + now + ".jpg";

            // create bitmap screen capture
            //View v1 = getWindow().getDecorView().getRootView();
            rootView.setDrawingCacheEnabled(true);
            Bitmap bitmap = Bitmap.createBitmap(rootView.getDrawingCache());
            rootView.setDrawingCacheEnabled(false);

            File imageFile = new File(mPath);

            FileOutputStream outputStream = new FileOutputStream(imageFile);
            int quality = 100;
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);
            outputStream.flush();
            outputStream.close();
            return imageFile;
            //openScreenshot(imageFile);
        } catch (Exception e) {
            // Several error may come out with file handling or OOM
            e.printStackTrace();
        }

        return null;
    }




}
