package edu.cmu.hcii.sugilite.verbal_instruction_demo;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import edu.cmu.hcii.sugilite.Const;
import edu.cmu.hcii.sugilite.R;
import edu.cmu.hcii.sugilite.SugiliteAccessibilityService;
import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.ontology.SerializableUISnapshot;
import edu.cmu.hcii.sugilite.ontology.UISnapshot;
import edu.cmu.hcii.sugilite.recording.newrecording.fullscreen_overlay.FullScreenRecordingOverlayManager;
import edu.cmu.hcii.sugilite.ui.StatusIconManager;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.speech.SugiliteVoiceInterface;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.speech.SugiliteVoiceRecognitionListener;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.study.SugiliteStudyHandler;


/**
 * @author toby
 * @date 11/22/17
 * @time 3:23 PM
 */
public class VerbalInstructionIconManager implements SugiliteVoiceInterface {
    private Context context;
    private WindowManager windowManager;
    private LayoutInflater layoutInflater;
    private SugiliteData sugiliteData;
    private SharedPreferences sharedPreferences;
    private SugiliteVoiceRecognitionListener sugiliteVoiceRecognitionListener;
    private SugiliteStudyHandler sugiliteStudyHandler;
    private FullScreenRecordingOverlayManager recordingOverlayManager;
    private SugiliteAccessibilityService sugiliteAccessibilityService;
    private StatusIconManager duckIconManager;
    private TextToSpeech tts;
    public boolean isListening = false;
    private Dialog dialog;

    //rotation degree for the cat
    private int rotation = 0;

    //previous x, y coordinates before the icon is removed
    Integer prev_x = null;
    Integer prev_y = null;

    private ImageView statusIcon;
    private WindowManager.LayoutParams iconParams;
    private Timer timer;

    //whether the icon is currently shown
    private boolean showingIcon = false;
    private final int REQ_CODE_SPEECH_INPUT = 100;


    //for saving the latest ui snapshot
    private UISnapshot latestUISnapshot = null;

    public VerbalInstructionIconManager(Context context, SugiliteStudyHandler sugiliteStudyHandler, SugiliteData sugiliteData, SharedPreferences sharedPreferences, FullScreenRecordingOverlayManager recordingOverlayManager, SugiliteAccessibilityService sugiliteAccessibilityService, TextToSpeech tts){
        this.context = context;
        this.sugiliteData = sugiliteData;
        this.sharedPreferences = sharedPreferences;
        this.sugiliteStudyHandler = sugiliteStudyHandler;
        this.sugiliteAccessibilityService = sugiliteAccessibilityService;
        this.duckIconManager = sugiliteAccessibilityService.getDuckIconManager();
        this.tts = tts;
        sugiliteStudyHandler.setIconManager(this);
        windowManager = (WindowManager) context.getSystemService(context.WINDOW_SERVICE);
        this.layoutInflater = (LayoutInflater) context.getSystemService( Context.LAYOUT_INFLATER_SERVICE);
        this.sugiliteVoiceRecognitionListener = new SugiliteVoiceRecognitionListener(context, this, tts);
        this.recordingOverlayManager = recordingOverlayManager;

    }

    /**
     * Callback for SugiliteVoiceRecogitionListener when listening has started
     */
    @Override
    public void listeningStarted(){
        isListening = true;
        statusIcon.setImageResource(R.mipmap.cat_talking);

    }

    /**
     * Callback for SugiliteVoiceRecogitionListener when listening has ended
     */
    @Override
    public void listeningEnded(){
        isListening = false;
        statusIcon.setImageResource(R.mipmap.cat_sleep);
    }

    /**
     * Callback for SugiliteVoiceRecogitionListener to send the result back
     * @param matches
     */
    @Override
    public void resultAvailable(List<String> matches){
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Voice Recognized!");
        String text = "";
        for(String match : matches){
            text += (match + "\n");
        }
        builder.setMessage(text);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //do nothing
            }
        });
        Dialog dialog = builder.create();
        dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_box);
        dialog.show();
    }

    // initiate the floating icon
    /**
     * add the status icon using the context specified in the class
     */
    public void addStatusIcon(){
        statusIcon = new ImageView(context);
        statusIcon.setImageResource(R.mipmap.cat_sleep);
        iconParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        DisplayMetrics displaymetrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(displaymetrics);


        iconParams.gravity = Gravity.TOP | Gravity.LEFT;
        iconParams.x = prev_x == null ? displaymetrics.widthPixels : prev_x;
        iconParams.y = prev_y == null ? 400 : prev_y;
        addCrumpledPaperOnTouchListener(statusIcon, iconParams, displaymetrics, windowManager);

        //NEEDED TO BE CONFIGURED AT APPS->SETTINGS-DRAW OVER OTHER APPS on API>=23
        int currentApiVersion = android.os.Build.VERSION.SDK_INT;
        if(currentApiVersion >= 23){
            checkDrawOverlayPermission();
            if(Settings.canDrawOverlays(context))
                windowManager.addView(statusIcon, iconParams);
        }
        else {
            windowManager.addView(statusIcon, iconParams);

        }

        //add timer service
        timer = new Timer();
        timer.schedule(new TimerTask()
        {
            @Override
            public void run()
            {
                sugiliteAccessibilityService.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        sugiliteAccessibilityService.updateUISnapshotInVerbalInstructionManager();
                        sugiliteAccessibilityService.checkIfAutomationCanBePerformed();
                    }
                });
            }
        }, 0, 1000);
        showingIcon = true;
    }

    public void rotateStatusIcon(){
        rotation = (rotation + 20) % 360;

        //rotate the duck
        statusIcon.setRotation(rotation);
    }

    /**
     * remove the status icon from the window manager
     */
    public void removeStatusIcon(){
        try{
            if(statusIcon != null) {
                windowManager.removeView(statusIcon);
            }
            if(timer != null) {
                timer.cancel();
            }
            showingIcon = false;
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    public boolean isShowingIcon() {
        return showingIcon;
    }

    public synchronized UISnapshot getLatestUISnapshot(){
        return latestUISnapshot;
    }


    public synchronized void setLatestUISnapshot(UISnapshot snapshot){
        this.latestUISnapshot = snapshot;
        //also update the uisnapshot for the testing full screen overlay
        recordingOverlayManager.setUiSnapshot(snapshot);
    }

    public void checkDrawOverlayPermission() {
        /** check if we already  have permission to draw over other apps */
        int currentApiVersion = android.os.Build.VERSION.SDK_INT;
        if(currentApiVersion >= 23) {
            if (!Settings.canDrawOverlays(context)) {
                /** if not construct intent to request permission */
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + context.getPackageName()));
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                /** request permission via start activity for result */
                context.startActivity(intent);

            }
        }
    }

    // handle touch event
    /**
     * make the chathead draggable. ref. http://blog.dision.co/2016/02/01/implement-floating-widget-like-facebook-chatheads/
     * @param view
     * @param mPaperParams
     * @param displayMetrics
     * @param windowManager
     */
    private void addCrumpledPaperOnTouchListener(final View view, final WindowManager.LayoutParams mPaperParams, DisplayMetrics displayMetrics, final WindowManager windowManager) {
        final int windowWidth = displayMetrics.widthPixels;
        view.setOnTouchListener(new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;
            GestureDetector gestureDetector = new GestureDetector(context, new SingleTapUp());



            @Override
            public boolean onTouch(final View v, MotionEvent event) {
                if (gestureDetector.onTouchEvent(event)) {
                    // gesture is clicking

                    //initialize the popup dialog
                    AlertDialog.Builder textDialogBuilder = new AlertDialog.Builder(context);
                    textDialogBuilder.setTitle("Verbal Instruction");
                    List<String> operationList = new ArrayList<>();

                    //fill in the options
                    operationList.add("Send a verbal instruction");
                    operationList.add("Test ASR");
                    operationList.add("Dump the latest UI snapshot");
                    operationList.add("Record a Sugilite study packet");
                    operationList.add("Switch recording overlay");


                    String[] operations = new String[operationList.size()];
                    operations = operationList.toArray(operations);
                    final String[] operationClone = operations.clone();
                    textDialogBuilder.setItems(operationClone, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    switch (operationClone[which]) {
                                        case "Send a verbal instruction":
                                            //send a verbal instruction
                                            if(getLatestUISnapshot() != null) {
                                                SerializableUISnapshot serializedUISnapshot = new SerializableUISnapshot(getLatestUISnapshot());
                                                VerbalInstructionTestDialog verbalInstructionDialog = new VerbalInstructionTestDialog(serializedUISnapshot, context, layoutInflater, sugiliteData, sharedPreferences, tts);
                                                if(dialog != null){
                                                    dialog.dismiss();
                                                }
                                                verbalInstructionDialog.show();
                                            }
                                            else{
                                                Toast.makeText(context, "UI snapshot is NULL!", Toast.LENGTH_SHORT).show();
                                            }
                                            break;

                                        case "Test ASR":
                                            if(dialog != null){
                                                dialog.dismiss();
                                            }
                                            testASR();
                                            break;

                                        case "Dump the latest UI snapshot":
                                            //dump the latest UI snapshot
                                            if(dialog != null){
                                                dialog.dismiss();
                                            }
                                            if(getLatestUISnapshot() != null) {
                                                Gson gson = new GsonBuilder().excludeFieldsWithModifiers(Modifier.FINAL, Modifier.TRANSIENT, Modifier.STATIC)
                                                        .serializeNulls()
                                                        .create();
                                                int snapshot_size = getLatestUISnapshot().getNodeSugiliteEntityMap().size();
                                                SerializableUISnapshot serializedUISnapshot2 = new SerializableUISnapshot(getLatestUISnapshot());
                                                dumpUISnapshot(serializedUISnapshot2);
                                                Toast.makeText(context, "dumped a UI snapshot with " + snapshot_size + " nodes", Toast.LENGTH_SHORT).show();
                                            }
                                            break;
                                        case "Record a Sugilite study packet":
                                            sugiliteStudyHandler.setToRecordNextOperation(true);
                                            break;
                                        case "Switch recording overlay":
                                            if(recordingOverlayManager.isShowingOverlay()){
                                                recordingOverlayManager.removeOverlays();
                                            }
                                            else{
                                                recordingOverlayManager.enableOverlay();
                                                //remove and re-add the status icon so that it can show on top of the overlay
                                                removeStatusIcon();
                                                addStatusIcon();
                                                if(duckIconManager != null){
                                                    duckIconManager.removeStatusIcon();
                                                    duckIconManager.addStatusIcon();
                                                }

                                            }
                                    }
                                }
                            });


                    dialog = textDialogBuilder.create();
                    dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
                    dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_box);
                    dialog.show();
                    return true;

                }

                //gesture is not clicking - handle the drag & move
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = mPaperParams.x;
                        initialY = mPaperParams.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;
                    case MotionEvent.ACTION_UP:
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        // move paper ImageView
                        mPaperParams.x = initialX - (int) (initialTouchX - event.getRawX());
                        mPaperParams.y = initialY + (int) (event.getRawY() - initialTouchY);
                        prev_x = mPaperParams.x;
                        prev_y = mPaperParams.y;
                        windowManager.updateViewLayout(view, mPaperParams);
                        return true;
                }
                return false;
            }

            class SingleTapUp extends GestureDetector.SimpleOnGestureListener {

                @Override
                public boolean onSingleTapUp(MotionEvent event) {
                    return true;
                }
            }

        });
    }

    private void testASR(){
        if(isListening) {
            sugiliteVoiceRecognitionListener.stopListening();
        }

        else {
            sugiliteVoiceRecognitionListener.startListening();
            statusIcon.setImageResource(R.mipmap.cat_regular);
        }
        return;
    }

    public static void dumpUISnapshot(SerializableUISnapshot serializedUISnapshot){
        Gson gson = new Gson();
        String uiSnapshot_gson = gson.toJson(serializedUISnapshot);
        String serverQueryTripes = new Gson().toJson(serializedUISnapshot.triplesToString());

        PrintWriter out1 = null;
        PrintWriter out2 = null;
        try {
            File f = new File("/sdcard/Download/ui_snapshots");
            if (!f.exists() || !f.isDirectory()) {
                f.mkdirs();
                System.out.println("dir created");
            }
            System.out.println(f.getAbsolutePath());


            Date time = Calendar.getInstance().getTime();
            String timeString = Const.dateFormat.format(time);

            File snapshot = new File(f.getPath() + "/snapshot_" + timeString + ".json");
            File serverQuery = new File(f.getPath() + "/triple_" + timeString + ".json");

            if (!snapshot.exists()) {
                snapshot.getParentFile().mkdirs();
                snapshot.createNewFile();
                System.out.println("file created");
            }

            if (!serverQuery.exists()) {
                serverQuery.getParentFile().mkdirs();
                serverQuery.createNewFile();
                System.out.println("file created");
            }

            out1 = new PrintWriter(new FileOutputStream(snapshot), true);
            out1.println(uiSnapshot_gson);
            out2 = new PrintWriter(new FileOutputStream(serverQuery), true);
            out2.println(serverQueryTripes);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (out1 != null) out1.close();
            if (out2 != null) out2.close();
        }

    }

    public void startStudyRecording(){
        statusIcon.setImageResource(R.mipmap.cat_standing);
    }

    public void endStudyRecording(){
        statusIcon.setImageResource(R.mipmap.cat_sleep);
    }



}
