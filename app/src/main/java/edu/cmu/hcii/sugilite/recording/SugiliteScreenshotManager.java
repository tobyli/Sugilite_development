package edu.cmu.hcii.sugilite.recording;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.pumice.PumiceDemonstrationUtil;

/**
 * @author toby
 * @date 6/17/16
 * @time 6:33 PM
 */

//Singleton
public class SugiliteScreenshotManager {

    private static SugiliteScreenshotManager instance = null;

    private SharedPreferences sharedPreferences;
    private Context appContext;
    private SugiliteData sugiliteData;

    private MediaProjection mMediaProjection = null;
    private VirtualDisplay mVirtualDisplay = null;


    public static int mResultCode = 0;
    public static Intent mResultData = null;
    public static MediaProjectionManager mMediaProjectionManager = null;

    private WindowManager mWindowManager = null;
    private int windowWidth = 0;
    private int windowHeight = 0;
    private ImageReader mImageReader = null;
    private DisplayMetrics metrics = null;
    private int mScreenDensity = 0;

    private static final String TAG = "SugiliteScreenshotManager";


    public static final int REQUEST_MEDIA_PROJECTION = 1;
    public static final String DIRECTORY_PATH = Environment.getExternalStorageDirectory().getPath()+"/sugilite_screenshot/";


    public static SugiliteScreenshotManager getInstance(SharedPreferences sharedPreferences, SugiliteData sugiliteData){
        if (instance == null) {
            instance = new SugiliteScreenshotManager(sharedPreferences, sugiliteData.getApplicationContext(), sugiliteData);
        }
        return instance;
    }

    private SugiliteScreenshotManager(SharedPreferences sharedPreferences, Context appContext, SugiliteData sugiliteData){
        this.sharedPreferences = sharedPreferences;
        this.appContext = appContext;
        this.sugiliteData = sugiliteData;

        //create the virtual environment
        mMediaProjectionManager = (MediaProjectionManager)sugiliteData.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        mWindowManager = (WindowManager)sugiliteData.getSystemService(Context.WINDOW_SERVICE);
        windowWidth = mWindowManager.getDefaultDisplay().getWidth();
        windowHeight = mWindowManager.getDefaultDisplay().getHeight();
        metrics = new DisplayMetrics();
        mWindowManager.getDefaultDisplay().getMetrics(metrics);
        mScreenDensity = metrics.densityDpi;
        mImageReader = ImageReader.newInstance(windowWidth, windowHeight, 0x1, 2); //ImageFormat.RGB_565
    }

    public String getFileNameFromDate() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd_hh_mm_ss");
        String strDate = dateFormat.format(new java.util.Date());
        return "Screenshot_" + strDate + ".png";
    }

    private Boolean screenshotAvailable = true;
    private Handler handler2 = new Handler();
    public File takeScreenshot(String directoryPath, String fileName) {
        String imagePath = directoryPath + fileName;
        File imageFile = new File(imagePath);
        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (screenshotAvailable) {
                    startVirtualDisplay();
                    if (mResultData != null && mResultCode != 0 && mMediaProjectionManager != null) {
                        handler2.postDelayed(new Runnable() {
                            public void run() {
                                //capture the screen
                                startCapture(imageFile);
                                screenshotAvailable = true;
                                handler2.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        releaseVirtualDisplay();

                                    }
                                }, 200);
                            }
                        }, 100);
                    } else {
                        PumiceDemonstrationUtil.showSugiliteToast("Media Projection Manager is not running!", Toast.LENGTH_SHORT);
                        screenshotAvailable = false;
                    }
                }
            }
        }).start();
        if (screenshotAvailable) {
            return imageFile;
        } else {
            return null;
        }
    }


    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void startVirtualDisplay(){
        if (mMediaProjection != null) {
            setUpVirtualDisplay();
        } else {
            setUpMediaProjection();
            setUpVirtualDisplay();
        }
    }

    private void releaseVirtualDisplay(){
        if (mVirtualDisplay == null) {
            return;
        }
        mVirtualDisplay.release();
        mVirtualDisplay = null;
    }



    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void setUpMediaProjection(){
        mResultData = sugiliteData.getScreenshotIntent();
        mResultCode = sugiliteData.getScreenshotResult();
        if (mResultData != null && mResultCode != 9) {
            mMediaProjectionManager = sugiliteData.getScreenshotMediaProjectionManager();
            if (mMediaProjectionManager != null) {
                mMediaProjection = mMediaProjectionManager.getMediaProjection(mResultCode, mResultData);
            }
        } else {
            Log.e(TAG, "null mResultData or mResultCode!");
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void setUpVirtualDisplay(){
        if (mMediaProjection != null) {
            mVirtualDisplay = mMediaProjection.createVirtualDisplay("screen-mirror",
                    windowWidth, windowHeight, mScreenDensity, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    mImageReader.getSurface(), null, null);
        } else {
            Log.e(TAG, "null mMediaProjection!");
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void startCapture(File imageFile){
        // PumiceDemonstrationUtil.showSugiliteToast("Capturing the screenshot", Toast.LENGTH_SHORT);
        Image image = mImageReader.acquireLatestImage();
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (image != null) {
                    int width = image.getWidth();
                    int height = image.getHeight();
                    final Image.Plane[] planes = image.getPlanes();
                    final ByteBuffer buffer = planes[0].getBuffer();
                    int pixelStride = planes[0].getPixelStride();
                    int rowStride = planes[0].getRowStride();
                    int rowPadding = rowStride - pixelStride * width;
                    Bitmap bitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888);
                    bitmap.copyPixelsFromBuffer(buffer);
                    bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height);
                    image.close();

                    if (bitmap != null) {
                        try {
                            if (!imageFile.exists()) {
                                imageFile.createNewFile();
                            }
                            FileOutputStream out = new FileOutputStream(imageFile);
                            if (out != null) {
                                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                                out.flush();
                                out.close();
                                Intent media = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                                Uri contentUri = Uri.fromFile(imageFile);
                                media.setData(contentUri);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    Log.e(TAG, "null image!");
                }
            }
        }).start();

    }




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
    public File takeScreenshotUsingShellCommand(boolean waitForCompletion, String directoryPath, String fileName) throws RuntimeException, IOException, InterruptedException {
        boolean rootEnabled = sharedPreferences.getBoolean("root_enabled", false);
        if(!rootEnabled){
            throw new RuntimeException("Root access denied!");
        }

        Process sh = Runtime.getRuntime().exec("su", null,null);
        OutputStream os = sh.getOutputStream();

        File screenshotDirectory = new File(directoryPath);
        if(!screenshotDirectory.exists()) {
            screenshotDirectory.mkdirs();
        }
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
