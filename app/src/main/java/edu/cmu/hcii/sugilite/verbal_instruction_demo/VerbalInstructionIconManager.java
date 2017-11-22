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
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;


import java.util.List;

import edu.cmu.hcii.sugilite.R;
import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.automation.ServiceStatusManager;


/**
 * @author toby
 * @date 11/22/17
 * @time 3:23 PM
 */
public class VerbalInstructionIconManager {
    private Context context;
    private WindowManager windowManager;
    private SugiliteData sugiliteData;
    private SharedPreferences sharedPreferences;
    private ServiceStatusManager serviceStatusManager;
    private LayoutInflater layoutInflater;
    private SugiliteVoiceRecognitionListener sugiliteVoiceRecognitionListener;
    public boolean isListening = false;


    private ImageView statusIcon;
    private WindowManager.LayoutParams iconParams;

    //whether the icon is currently shown
    private boolean showingIcon = false;
    private final int REQ_CODE_SPEECH_INPUT = 100;



    public VerbalInstructionIconManager(Context context, SugiliteData sugiliteData, SharedPreferences sharedPreferences){
        this.context = context;
        windowManager = (WindowManager) context.getSystemService(context.WINDOW_SERVICE);
        this.sugiliteData = sugiliteData;
        this.sharedPreferences = sharedPreferences;
        this.serviceStatusManager = ServiceStatusManager.getInstance(context);
        this.layoutInflater = (LayoutInflater) context.getSystemService( Context.LAYOUT_INFLATER_SERVICE);
        this.sugiliteVoiceRecognitionListener = new SugiliteVoiceRecognitionListener(context, this);
    }

    /**
     * callback for SugiliteVoiceRecogitionListener when listening has started 
     */
    public void listeningStarted(){
        isListening = true;
        statusIcon.setImageResource(R.mipmap.cat_talking);

    }

    /**
     * callback for SugiliteVoiceRecogitionListener when listening has ended
     */
    public void listeningEnded(){
        isListening = false;
        statusIcon.setImageResource(R.mipmap.cat_sleep);
    }

    /**
     * callback for SugiliteVoiceRecogitionListener to send the result back
     * @param matches
     */
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
        iconParams.x = displaymetrics.widthPixels;
        iconParams.y = 400;
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

        showingIcon = true;
    }

    /**
     * remove the status icon from the window manager
     */
    public void removeStatusIcon(){
        try{
            if(statusIcon != null) {
                windowManager.removeView(statusIcon);
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

                    // TODO: construct the UI snapshot
                    // pop up the on-click menu
                    if(isListening) {
                        sugiliteVoiceRecognitionListener.stopListening();
                    }

                    else {
                        sugiliteVoiceRecognitionListener.startListening();
                        statusIcon.setImageResource(R.mipmap.cat_regular);
                    }
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



}
