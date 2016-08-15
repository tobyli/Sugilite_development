package edu.cmu.hcii.sugilite.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Environment;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import edu.cmu.hcii.sugilite.MainActivity;
import edu.cmu.hcii.sugilite.R;
import edu.cmu.hcii.sugilite.ScriptDetailActivity;
import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.automation.Automator;
import edu.cmu.hcii.sugilite.automation.ServiceStatusManager;
import edu.cmu.hcii.sugilite.communication.SugiliteBlockJSONProcessor;
import edu.cmu.hcii.sugilite.communication.SugiliteCommunicationController;
import edu.cmu.hcii.sugilite.dao.SugiliteScreenshotManager;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptDao;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;
import edu.cmu.hcii.sugilite.model.block.UIElementMatchingFilter;
import edu.cmu.hcii.sugilite.model.variable.VariableHelper;

/**
 * @author toby
 * @date 6/20/16
 * @time 4:03 PM
 */
public class StatusIconManager {
    private ImageView statusIcon;
    private Context context;
    private WindowManager windowManager;
    private SugiliteData sugiliteData;
    private SharedPreferences sharedPreferences;
    private SugiliteScriptDao sugiliteScriptDao;
    private ServiceStatusManager serviceStatusManager;
    private SugiliteScreenshotManager screenshotManager;
    private SugiliteBlockJSONProcessor jsonProcessor;
    private WindowManager.LayoutParams params;
    private VariableHelper variableHelper;
    private Random random;

    public StatusIconManager(Context context, SugiliteData sugiliteData, SharedPreferences sharedPreferences){
        this.context = context;
        windowManager = (WindowManager) context.getSystemService(context.WINDOW_SERVICE);
        this.sugiliteData = sugiliteData;
        this.sharedPreferences = sharedPreferences;
        this.sugiliteScriptDao = new SugiliteScriptDao(context);
        this.serviceStatusManager = new ServiceStatusManager(context);
        this.screenshotManager = new SugiliteScreenshotManager(sharedPreferences, context);
        variableHelper = new VariableHelper(sugiliteData.stringVariableMap);
        jsonProcessor = new SugiliteBlockJSONProcessor(context);
        random = new Random();

    }

    /**
     * add the status icon using the context specified in the class
     */
    public void addStatusIcon(){
        statusIcon = new ImageView(context);
        statusIcon.setImageResource(R.mipmap.ic_launcher);
        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        DisplayMetrics displaymetrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(displaymetrics);


        params.gravity = Gravity.TOP | Gravity.LEFT;
        params.x = displaymetrics.widthPixels;
        params.y = 200;
        addCrumpledPaperOnTouchListener(statusIcon, params, displaymetrics, windowManager);

        //NEEDED TO BE CONFIGURED AT APPS->SETTINGS-DRAW OVER OTHER APPS on API>=23
        int currentApiVersion = android.os.Build.VERSION.SDK_INT;
        if(currentApiVersion >= 23){
            checkDrawOverlayPermission();
            if(Settings.canDrawOverlays(context))
                windowManager.addView(statusIcon, params);
        }
        else {
            windowManager.addView(statusIcon, params);
        }


    }

    /**
     * remove the status icon from the window manager
     */
    public void removeStatusIcon(){
        try{
            if(statusIcon != null)
                windowManager.removeView(statusIcon);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * refresh the status icon to reflect the status of Sugilite
     */
    public void refreshStatusIcon(AccessibilityNodeInfo rootNode, UIElementMatchingFilter filter){
        Rect rect = new Rect();
        boolean matched = false;
        if(rootNode != null) {
            List<AccessibilityNodeInfo> allNode = Automator.preOrderTraverse(rootNode);
            List<AccessibilityNodeInfo> filteredNode = new ArrayList<>();
            for (AccessibilityNodeInfo node : allNode) {
                if (filter.filter(node, variableHelper))
                    filteredNode.add(node);
            }
            if (filteredNode.size() > 0) {
                AccessibilityNodeInfo targetNode = filteredNode.get(0);
                targetNode.getBoundsInScreen(rect);
                matched = true;
            }
        }
        int offset = random.nextInt(5);

        try{
            if(statusIcon != null){
                boolean recordingInProcess = sharedPreferences.getBoolean("recording_in_process", false);
                boolean trackingInProcess = sharedPreferences.getBoolean("tracking_in_process", false);
                boolean broadcastingInProcess = sharedPreferences.getBoolean("broadcasting_enabled", false);
                if(recordingInProcess)
                    statusIcon.setImageResource(R.mipmap.duck_icon_recording);
                else if(sugiliteData.getInstructionQueueSize() > 0) {
                    statusIcon.setImageResource(R.mipmap.duck_icon_playing);
                    if(matched) {
                        params.x = (rect.centerX() > 150 ? rect.centerX()  - 150 : 0);
                        params.y = (rect.centerY() > 150 ? rect.centerY()  - 150 : 0);
                    }
                    /*
                    params.x = params.x + offset;
                    params.y = params.y + offset;
                    */
                    windowManager.updateViewLayout(statusIcon, params);
                }
                else if(trackingInProcess || (broadcastingInProcess && sugiliteData.registeredBroadcastingListener.size() > 0)){
                    statusIcon.setImageResource(R.mipmap.duck_icon_spying);
                }
                else
                    statusIcon.setImageResource(R.mipmap.ic_launcher);

            }

        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    /** code to post/handler request for permission */
    public final static int REQUEST_CODE = -1010101;

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
                    // gesture is clicking -> pop up the on-click menu
                    AlertDialog.Builder textDialogBuilder = new AlertDialog.Builder(context);
                    final boolean recordingInProcess = sharedPreferences.getBoolean("recording_in_process", false);
                    final SugiliteStartingBlock startingBlock = (SugiliteStartingBlock) sugiliteData.getScriptHead();
                    String scriptName = (startingBlock == null ? "" : startingBlock.getScriptName());
                    final String scriptDefinedName = scriptName.replace(".SugiliteScript", "");
                    //set pop up title
                    if(recordingInProcess){
                        textDialogBuilder.setTitle("RECORDING: " + scriptDefinedName);
                    }
                    else if (sugiliteData.getScriptHead() != null){
                        textDialogBuilder.setTitle("NOT RECORDING\nLAST RECORDED: " + scriptDefinedName);
                    }

                    else {
                        textDialogBuilder.setTitle("NOT RECORDING");
                    }

                    boolean recordingInProgress = sharedPreferences.getBoolean("recording_in_process", false);

                    List<String> operationList = new ArrayList<>();
                    operationList.add("View Script List");
                    if(sugiliteData.getInstructionQueueSize() > 0)
                        operationList.add("Clear Instruction Queue");
                    if(startingBlock == null){
                        operationList.add("New Recording");
                    }
                    else{
                        if(recordingInProcess){
                            operationList.add("View Current Recording");
                            operationList.add("End Recording");
                        }
                        else{
                            operationList.add("View Last Recording");
                            operationList.add("Resume Last Recording");
                            operationList.add("New Recording");
                        }
                    }
                    operationList.add("Quit Sugilite");
                    String[] operations = new String[operationList.size()];
                    operations = operationList.toArray(operations);
                    final String[] operationClone = operations.clone();
                    textDialogBuilder.setItems(operationClone, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            switch (operationClone[which]) {
                                case "View Script List":
                                    Intent scriptListIntent = new Intent(context, MainActivity.class);
                                    scriptListIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                    context.startActivity(scriptListIntent);
                                    Toast.makeText(context, "view script list", Toast.LENGTH_SHORT).show();
                                    break;
                                //bring the user to the script list activity
                                case "View Last Recording":
                                case "View Current Recording":
                                    Intent intent = new Intent(context, ScriptDetailActivity.class);
                                    intent.putExtra("scriptName", startingBlock.getScriptName());
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    context.startActivity(intent);
                                    Toast.makeText(context, "view current script", Toast.LENGTH_SHORT).show();
                                    break;
                                case "End Recording":
                                    //end recording
                                    SharedPreferences.Editor prefEditor = sharedPreferences.edit();
                                    prefEditor.putBoolean("recording_in_process", false);
                                    prefEditor.commit();
                                    if (sugiliteData.initiatedExternally == true && sugiliteData.getScriptHead() != null) {
                                        sugiliteData.communicationController.sendRecordingFinishedSignal(sugiliteData.getScriptHead().getScriptName());
                                        sugiliteData.sendCallbackMsg("FINISHED_RECORDING", jsonProcessor.scriptToJson(sugiliteData.getScriptHead()), sugiliteData.callbackString);
                                    }
                                    Toast.makeText(context, "end recording", Toast.LENGTH_SHORT).show();
                                    break;
                                case "New Recording":
                                    //create a new script
                                    NewScriptDialog newScriptDialog = new NewScriptDialog(v.getContext(), sugiliteScriptDao, serviceStatusManager, sharedPreferences, sugiliteData, true, null, null);
                                    newScriptDialog.show();
                                    break;
                                case "Resume Last Recording":
                                    //resume the recording of an existing script
                                    SharedPreferences.Editor prefEditor2 = sharedPreferences.edit();
                                    prefEditor2.putBoolean("recording_in_process", true);
                                    prefEditor2.commit();
                                    Toast.makeText(context, "resume recording", Toast.LENGTH_SHORT).show();
                                    break;
                                case "Quit Sugilite":
                                    Toast.makeText(context, "quit sugilite", Toast.LENGTH_SHORT).show();
                                    try {
                                        screenshotManager.take(false);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                    break;
                                case "Clear Instruction Queue":
                                    sugiliteData.clearInstructionQueue();
                                    break;
                                default:
                                    //do nothing
                            }
                        }
                    });
                    Dialog dialog = textDialogBuilder.create();
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

    public void moveIcon (int x ,int y){
        if(statusIcon == null)
            return;
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.LEFT;
        params.x = x;
        params.y = y;
        windowManager.updateViewLayout(statusIcon, params);
        statusIcon.invalidate();
    }







}
