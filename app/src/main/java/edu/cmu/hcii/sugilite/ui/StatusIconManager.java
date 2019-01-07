package edu.cmu.hcii.sugilite.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.net.Uri;
import android.provider.Settings;
import android.renderscript.Script;
import android.text.Html;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Queue;
import java.util.Random;

import edu.cmu.hcii.sugilite.Const;
import edu.cmu.hcii.sugilite.R;
import edu.cmu.hcii.sugilite.SugiliteAccessibilityService;
import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.automation.AutomatorUtil;
import edu.cmu.hcii.sugilite.automation.ServiceStatusManager;
import edu.cmu.hcii.sugilite.communication.SugiliteBlockJSONProcessor;
import edu.cmu.hcii.sugilite.model.operation.SugiliteSpecialOperation;
import edu.cmu.hcii.sugilite.recording.SugiliteScreenshotManager;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptDao;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptFileDao;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptSQLDao;
import edu.cmu.hcii.sugilite.model.block.SugiliteBlock;
import edu.cmu.hcii.sugilite.model.block.special_operation.SugiliteDelaySpecialOperationBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteOperationBlock;
import edu.cmu.hcii.sugilite.model.block.special_operation.SugiliteSpecialOperationBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;
import edu.cmu.hcii.sugilite.model.block.special_operation.SugiliteSubscriptSpecialOperationBlock;
import edu.cmu.hcii.sugilite.model.block.util.UIElementMatchingFilter;
import edu.cmu.hcii.sugilite.model.operation.SugiliteOperation;
import edu.cmu.hcii.sugilite.model.variable.VariableHelper;
import edu.cmu.hcii.sugilite.recording.ReadableDescriptionGenerator;
import edu.cmu.hcii.sugilite.ui.dialog.NewScriptDialog;
import edu.cmu.hcii.sugilite.ui.dialog.SelectElementWithTextDialog;
import edu.cmu.hcii.sugilite.ui.main.SugiliteMainActivity;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.VerbalInstructionIconManager;

import static edu.cmu.hcii.sugilite.Const.SQL_SCRIPT_DAO;

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
    private ReadableDescriptionGenerator descriptionGenerator;
    private WindowManager.LayoutParams iconParams, textViewParams;
    private VariableHelper variableHelper;
    private LayoutInflater layoutInflater;
    private Random random;
    private AccessibilityManager accessibilityManager;
    private CurrentStateView statusView;
    private Queue<SugiliteBlock> storedQueue;
    private AlertDialog progressDialog;
    private boolean showingIcon = false;
    private VerbalInstructionIconManager verbalInstructionIconManager = null;
    private Dialog duckDialog = null;

    //rotation degree for the cat
    int rotation = 0;

    //previous x, y coordinates before the icon is removed
    Integer prev_x = null;
    Integer prev_y = null;

    public StatusIconManager(Context context, SugiliteData sugiliteData, SharedPreferences sharedPreferences, AccessibilityManager accessibilityManager){
        this.context = context;
        windowManager = (WindowManager) context.getSystemService(context.WINDOW_SERVICE);
        this.sugiliteData = sugiliteData;
        this.sharedPreferences = sharedPreferences;
        if(Const.DAO_TO_USE == SQL_SCRIPT_DAO)
            sugiliteScriptDao = new SugiliteScriptSQLDao(context);
        else
            sugiliteScriptDao = new SugiliteScriptFileDao(context, sugiliteData);
        this.serviceStatusManager = ServiceStatusManager.getInstance(context);
        this.screenshotManager = new SugiliteScreenshotManager(sharedPreferences, context);
        this.layoutInflater = (LayoutInflater) context.getSystemService( Context.LAYOUT_INFLATER_SERVICE );
        this.accessibilityManager = accessibilityManager;
        variableHelper = new VariableHelper(sugiliteData.stringVariableMap);
        jsonProcessor = new SugiliteBlockJSONProcessor(context);
        descriptionGenerator = new ReadableDescriptionGenerator(context);
        random = new Random();

    }

    /**
     * add the status icon using the context specified in the class
     */
    public void addStatusIcon(){
        statusIcon = new ImageView(context);
        statusIcon.setImageResource(R.mipmap.ic_launcher);
        statusView = getViewForCurrentState("");
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
        iconParams.y = prev_y == null ? 200 : prev_y;
        addCrumpledPaperOnTouchListener(statusIcon, iconParams, displaymetrics, windowManager);


        textViewParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        textViewParams.gravity = Gravity.BOTTOM | Gravity.CENTER;

        //NEEDED TO BE CONFIGURED AT APPS->SETTINGS-DRAW OVER OTHER APPS on API>=23
        int currentApiVersion = android.os.Build.VERSION.SDK_INT;
        if(currentApiVersion >= 23){
            checkDrawOverlayPermission();
            if(Settings.canDrawOverlays(context))
                windowManager.addView(statusIcon, iconParams);

                //=== temporarily set the status view to invisible  ===
                windowManager.addView(statusView, textViewParams);
                statusView.setVisibility(View.INVISIBLE);
        }
        else {
            windowManager.addView(statusIcon, iconParams);


            //=== temporarily set the status view to invisible ===
            windowManager.addView(statusView, textViewParams);
            statusView.setVisibility(View.INVISIBLE);
        }

        //AUTOMATICALLY ADD CAT ICON WHEN ADDING THE DUCK ICON
        if(!verbalInstructionIconManager.isShowingIcon()) {
            verbalInstructionIconManager.addStatusIcon();
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
            if(statusView != null) {
                windowManager.removeView(statusView);
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

    /**
     * refresh the status icon to reflect the status of Sugilite
     * this is called during the execution
     */
    public void refreshStatusIcon(AccessibilityNodeInfo rootNode, UIElementMatchingFilter filter, boolean inProcess){
        Rect rect = new Rect();
        boolean matched = false;
        if(rootNode != null) {
            List<AccessibilityNodeInfo> allNode = AutomatorUtil.preOrderTraverse(rootNode);
            List<AccessibilityNodeInfo> filteredNode = new ArrayList<>();
            for (AccessibilityNodeInfo node : allNode) {
                if (filter != null && filter.filter(node, variableHelper))
                    filteredNode.add(node);
            }
            if (filteredNode.size() > 0) {
                AccessibilityNodeInfo targetNode = filteredNode.get(0);
                targetNode.getBoundsInScreen(rect);
                matched = true;
            }
        }


        try{
            SugiliteBlock nextBlock = null;
            if(statusIcon != null){
                boolean recordingInProcess = sharedPreferences.getBoolean("recording_in_process", false);
                boolean trackingInProcess = sharedPreferences.getBoolean("tracking_in_process", false);
                boolean broadcastingInProcess = sharedPreferences.getBoolean("broadcasting_enabled", false);
                if(recordingInProcess)
                    statusIcon.setImageResource(R.mipmap.duck_icon_recording);
                else if(sugiliteData.getInstructionQueueSize() > 0) {
                    nextBlock = sugiliteData.peekInstructionQueue();
                    statusIcon.setImageResource(R.mipmap.duck_icon_playing);
                    if(matched) {
                        iconParams.x = (rect.centerX() > 150 ? rect.centerX()  - 150 : 0);
                        iconParams.y = (rect.centerY() > 150 ? rect.centerY()  - 150 : 0);
                    }
                    windowManager.updateViewLayout(statusIcon, iconParams);
                    rotation = (rotation + 20) % 360;

                    //rotate the duck
                    statusIcon.setRotation(rotation);

                    /**
                     *
                     *
                     *  send out empty accessibility event for triggering the automator
                     *
                     *
                     */

                    //only send this out when the previous successful operation (check the error handler) was more than X seconds ago
                    long sinceLastWindowChange = -1;
                    if(sugiliteData.errorHandler != null) {
                        Calendar calendar = Calendar.getInstance();
                        long currentTime = calendar.getTimeInMillis();
                        sinceLastWindowChange = currentTime - sugiliteData.errorHandler.getLastWindowChange();
                    }
                    if(sinceLastWindowChange < 0 || sinceLastWindowChange > Const.THRESHOLD_FOR_START_SENDING_ACCESSIBILITY_EVENT) {
                        System.out.println("INFO: SENDING GENERATED ACCESSIBILITY EVENT: sinceLastWindowChange = " + sinceLastWindowChange);
                        AccessibilityEvent event = AccessibilityEvent.obtain();
                        event.setEventType(AccessibilityEvent.TYPE_ANNOUNCEMENT);
                        event.getText().add("NULL");
                        accessibilityManager.sendAccessibilityEvent(event);
                    }

                }
                else if(trackingInProcess || (broadcastingInProcess && sugiliteData.registeredBroadcastingListener.size() > 0)){
                    statusIcon.setImageResource(R.mipmap.duck_icon_spying);
                    rotation = 0;
                    statusIcon.setRotation(rotation);

                }
                else if(sugiliteData.getCurrentSystemState() == SugiliteData.PAUSED_FOR_BREAKPOINT_STATE){
                    statusIcon.setImageResource(R.mipmap.debug_transparent_icon);
                    rotation = 0;
                    statusIcon.setRotation(rotation);
                }
                else {
                    statusIcon.setImageResource(R.mipmap.ic_launcher);
                    rotation = 0;
                    statusIcon.setRotation(rotation);
                }

            }
            //refresh the status view based on the current state
            if(nextBlock != null && sugiliteData.getCurrentSystemState() == SugiliteData.REGULAR_DEBUG_STATE){
                statusView.setCurrentStateView(sugiliteData.getCurrentSystemState(), nextBlock.getDescription());
            }
            else if(sugiliteData.getCurrentSystemState() == SugiliteData.PAUSED_FOR_DUCK_MENU_IN_DEBUG_MODE || sugiliteData.getCurrentSystemState() == SugiliteData.PAUSED_FOR_DUCK_MENU_IN_REGULAR_EXECUTION_STATE){
                if(storedQueue != null && storedQueue.size() > 0) {
                    SugiliteBlock block = storedQueue.peek();
                    if(block != null) {
                        statusView.setCurrentStateView(sugiliteData.getCurrentSystemState(), storedQueue.peek().getDescription());
                        System.out.print("show paused_for_duck_menu status view for " + storedQueue.peek().getDescription());
                    }
                }
            }
            else if(sugiliteData.getCurrentSystemState() == SugiliteData.PAUSED_FOR_BREAKPOINT_STATE){
                if(sugiliteData.storedInstructionQueueForPause != null && sugiliteData.storedInstructionQueueForPause.size() > 0) {
                    SugiliteBlock block = sugiliteData.storedInstructionQueueForPause.peek();
                    if(block != null) {
                        statusView.setCurrentStateView(sugiliteData.getCurrentSystemState(), block.getDescription());
                        System.out.print("show paused_for_breakpoint_state status view for " + block.getDescription());
                    }
                }
            }
            else {
                statusView.setCurrentStateView(sugiliteData.getCurrentSystemState(), "");
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
                v.performClick();
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

                        if(sugiliteData.getCurrentSystemState() == SugiliteData.PAUSED_FOR_BREAKPOINT_STATE){
                            textDialogBuilder.setTitle("PAUSED FOR A BREAKPOINT");
                        }
                    }
                    else if (sugiliteData.getScriptHead() != null){
                        boolean hasLast = false;
                        try{
                            List<String> allNames = sugiliteScriptDao.getAllNames();
                            hasLast = allNames.contains(scriptName);
                        }
                        catch(Exception e){
                            e.printStackTrace();
                        }
                        if(hasLast) {
                            textDialogBuilder.setTitle("NOT RECORDING\nLAST RECORDED: " + scriptDefinedName);
                        }
                        else{
                            textDialogBuilder.setTitle("NOT RECORDING\n");
                        }
                    }

                    else {
                        textDialogBuilder.setTitle("NOT RECORDING");
                    }

                    boolean recordingInProgress = sharedPreferences.getBoolean("recording_in_process", false);
                    final boolean runningInProgress = sugiliteData.getInstructionQueueSize() > 0;



                    //pause the execution when the duck is clicked
                    storedQueue = runningInProgress ? sugiliteData.getCopyOfInstructionQueue() : null;
                    final int previousState = sugiliteData.getCurrentSystemState();
                    if(runningInProgress) {
                        sugiliteData.clearInstructionQueue();
                        if(sugiliteData.getCurrentSystemState() == SugiliteData.PAUSED_FOR_BREAKPOINT_STATE
                                || sugiliteData.getCurrentSystemState() == SugiliteData.PAUSED_FOR_ERROR_HANDLING_STATE
                                || sugiliteData.getCurrentSystemState() == SugiliteData.PAUSED_FOR_CRUCIAL_STEP_STATE){
                            //TODO: change the icon based on the current status
                        }
                        else {
                            if(previousState == SugiliteData.REGULAR_DEBUG_STATE)
                                sugiliteData.setCurrentSystemState(SugiliteData.PAUSED_FOR_DUCK_MENU_IN_DEBUG_MODE);
                            else
                                sugiliteData.setCurrentSystemState(SugiliteData.PAUSED_FOR_DUCK_MENU_IN_REGULAR_EXECUTION_STATE);
                        }
                    }

                    //TODO: show different menu items for different state

                    List<String> operationList = new ArrayList<>();
                    if(sugiliteData.getCurrentSystemState() == SugiliteData.PAUSED_FOR_BREAKPOINT_STATE){
                        operationList.add("Resume Next Step");
                        operationList.add("Quit Debugging");
                    }
                    if(sugiliteData.getCurrentSystemState() == SugiliteData.PAUSED_FOR_DUCK_MENU_IN_REGULAR_EXECUTION_STATE
                            || sugiliteData.getCurrentSystemState() == SugiliteData.PAUSED_FOR_DUCK_MENU_IN_DEBUG_MODE) {
                        operationList.add("Resume Running");
                        operationList.add("Clear Instruction Queue");
                    }
                    operationList.add("View Script List");




                    if(startingBlock == null){
                        operationList.add("New Recording");
                    }
                    else{
                        if(recordingInProcess){
                            operationList.add("View Current Recording");
                            operationList.add("Add GO_HOME Operation Block");
                            operationList.add("Add Running a Subscript");
                            if(Const.KEEP_ALL_TEXT_LABEL_LIST)
                                operationList.add("Get a Text Element on the Screen");
                            operationList.add("Add a Delay");
                            operationList.add("End Recording");
                        }
                        else{
                            operationList.add("View Last Recording");
                            operationList.add("Resume Last Recording");
                            operationList.add("New Recording");
                        }
                    }

                    if(verbalInstructionIconManager != null) {
                        if(verbalInstructionIconManager.isShowingIcon()) {
                            operationList.add("Turn off verbal instruction");
                        }
                        else{
                            operationList.add("Turn on verbal instruction");
                        }
                    }
                    operationList.add("Hide Duck Icon");
                    operationList.add("Quit Sugilite");
                    String[] operations = new String[operationList.size()];
                    operations = operationList.toArray(operations);
                    final String[] operationClone = operations.clone();
                    final SharedPreferences.Editor prefEditor = sharedPreferences.edit();
                    textDialogBuilder.setItems(operationClone, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            switch (operationClone[which]) {
                                case "View Script List":
                                    Intent scriptListIntent = new Intent(context, SugiliteMainActivity.class);
                                    scriptListIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                    context.startActivity(scriptListIntent);
                                    Toast.makeText(context, "view script list", Toast.LENGTH_SHORT).show();
                                    if(runningInProgress)
                                        sugiliteData.setCurrentSystemState(SugiliteData.DEFAULT_STATE);
                                    break;
                                //bring the user to the script list activity
                                case "View Last Recording":
                                case "View Current Recording":
                                    Intent intent = new Intent(context, ScriptDetailActivity.class);
                                    if(startingBlock != null && startingBlock.getScriptName() != null) {
                                        intent.putExtra("scriptName", startingBlock.getScriptName());
                                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                        context.startActivity(intent);
                                    }
                                    Toast.makeText(context, "view current script", Toast.LENGTH_SHORT).show();
                                    if(runningInProgress)
                                        sugiliteData.setCurrentSystemState(SugiliteData.DEFAULT_STATE);
                                    break;
                                case "End Recording":
                                    //end recording
                                    prefEditor.putBoolean("recording_in_process", false);
                                    prefEditor.apply();

                                    progressDialog = new AlertDialog.Builder(context).setMessage(Const.SAVING_MESSAGE).create();
                                    if(progressDialog.getWindow() != null) {
                                        progressDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
                                    }
                                    progressDialog.setCanceledOnTouchOutside(false);
                                    progressDialog.show();
                                    new Thread(new Runnable() {
                                        @Override
                                        public void run()
                                        {
                                            try {
                                                sugiliteScriptDao.commitSave();
                                            }
                                            catch (Exception e){
                                                e.printStackTrace();
                                            }
                                            Runnable dismissDialog = new Runnable() {
                                                @Override
                                                public void run() {
                                                    progressDialog.dismiss();
                                                }
                                            };
                                            if(context instanceof SugiliteAccessibilityService) {
                                                ((SugiliteAccessibilityService) context).runOnUiThread(dismissDialog);
                                            }
                                            else if(context instanceof Activity){
                                                ((Activity)context).runOnUiThread(dismissDialog);
                                            }
                                        }
                                    }).start();


                                    if (sugiliteData.initiatedExternally && sugiliteData.getScriptHead() != null) {
                                        sugiliteData.communicationController.sendRecordingFinishedSignal(sugiliteData.getScriptHead().getScriptName());
                                        sugiliteData.sendCallbackMsg(Const.FINISHED_RECORDING, jsonProcessor.scriptToJson(sugiliteData.getScriptHead()), sugiliteData.callbackString);
                                    }
                                    sugiliteData.setCurrentSystemState(SugiliteData.DEFAULT_STATE);
                                    Toast.makeText(context, "end recording", Toast.LENGTH_SHORT).show();
                                    break;
                                case "New Recording":
                                    //create a new script
                                    NewScriptDialog newScriptDialog = new NewScriptDialog(v.getContext(), layoutInflater, sugiliteScriptDao, serviceStatusManager, sharedPreferences, sugiliteData, true, null, null);
                                    newScriptDialog.show();
                                    break;
                                case "Resume Last Recording":
                                    //resume the recording of an existing script
                                    sugiliteData.initiatedExternally = false;
                                    SharedPreferences.Editor prefEditor2 = sharedPreferences.edit();
                                    prefEditor2.putBoolean("recording_in_process", true);
                                    prefEditor2.apply();
                                    Toast.makeText(context, "resume recording", Toast.LENGTH_SHORT).show();
                                    sugiliteData.setCurrentSystemState(SugiliteData.RECORDING_STATE);
                                    break;
                                case "Hide Duck Icon":
                                    //step: remove the duck and the status view
                                    removeStatusIcon();
                                    break;
                                case "Quit Sugilite":
                                    Toast.makeText(context, "quit sugilite", Toast.LENGTH_SHORT).show();

                                    //step 1: end recording if one is in progress
                                    if(recordingInProgress){
                                        //end recording
                                        prefEditor.putBoolean("recording_in_process", false);
                                        prefEditor.apply();
                                        progressDialog = new AlertDialog.Builder(context).setMessage(Const.SAVING_MESSAGE).create();
                                        if(progressDialog.getWindow() != null) {
                                            progressDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
                                        }
                                        progressDialog.setCanceledOnTouchOutside(false);
                                        progressDialog.show();
                                        new Thread(new Runnable() {
                                            @Override
                                            public void run()
                                            {
                                                try {
                                                    sugiliteScriptDao.commitSave();
                                                }
                                                catch (Exception e){
                                                    e.printStackTrace();
                                                }
                                                Runnable dismissDialog = new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        progressDialog.dismiss();
                                                    }
                                                };
                                                if(context instanceof SugiliteAccessibilityService) {
                                                    ((SugiliteAccessibilityService) context).runOnUiThread(dismissDialog);
                                                }
                                                else if(context instanceof Activity){
                                                    ((Activity)context).runOnUiThread(dismissDialog);
                                                }
                                            }
                                        }).start();
                                    }

                                    //step 2: clear instruction queue if there is one
                                    sugiliteData.clearInstructionQueue();
                                    sugiliteData.setCurrentSystemState(SugiliteData.DEFAULT_STATE);
                                    if(storedQueue != null)
                                        storedQueue.clear();

                                    //step 3: remove the duck and the status view
                                    removeStatusIcon();

                                    //step 4: kill Sugilite app
                                    Intent first_activity_intent = new Intent(context, SugiliteMainActivity.class);
                                    first_activity_intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                    first_activity_intent.putExtra("EXIT", true);
                                    context.startActivity(first_activity_intent);

                                    break;
                                case "Clear Instruction Queue":
                                    sugiliteData.clearInstructionQueue();
                                    sugiliteData.setCurrentSystemState(SugiliteData.DEFAULT_STATE);
                                    if(storedQueue != null)
                                        storedQueue.clear();
                                    break;
                                case "Resume Running":
                                    dialog.dismiss();
                                    break;
                                case "Add GO_HOME Operation Block":
                                    //insert a GO_HOME opertion block AND go home
                                    SugiliteOperationBlock operationBlock = new SugiliteOperationBlock();
                                    SugiliteOperation operation = new SugiliteSpecialOperation(SugiliteOperation.SPECIAL_GO_HOME);
                                    operationBlock.setOperation(operation);
                                    operationBlock.setDescription(descriptionGenerator.generateReadableDescription(operationBlock));
                                    try {
                                        SugiliteBlock currentBlock = sugiliteData.getCurrentScriptBlock();
                                        if(currentBlock == null || sugiliteData.getScriptHead() == null)
                                            throw new Exception("NULL CURRENT BLOCK OR NULL SCRIPT");
                                        operationBlock.setPreviousBlock(currentBlock);
                                        if (currentBlock instanceof SugiliteOperationBlock)
                                            ((SugiliteOperationBlock) currentBlock).setNextBlock(operationBlock);
                                        else if (currentBlock instanceof SugiliteStartingBlock)
                                            ((SugiliteStartingBlock) currentBlock).setNextBlock(operationBlock);
                                        else if (currentBlock instanceof SugiliteSpecialOperationBlock)
                                            ((SugiliteSpecialOperationBlock) currentBlock).setNextBlock(operationBlock);
                                        else
                                            throw new Exception("UNSUPPORTED BLOCK TYPE");
                                        //TODO: deal with blocks other than operation block and starting block
                                        sugiliteData.setCurrentScriptBlock(operationBlock);
                                        sugiliteScriptDao.save(sugiliteData.getScriptHead());
                                        //go to home
                                        Intent startMain = new Intent(Intent.ACTION_MAIN);
                                        startMain.addCategory(Intent.CATEGORY_HOME);
                                        startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                        context.startActivity(startMain);
                                    }
                                    catch (Exception e){
                                        e.printStackTrace();
                                    }
                                    break;
                                case "Add Running a Subscript":
                                    final SugiliteSubscriptSpecialOperationBlock subscriptBlock = new SugiliteSubscriptSpecialOperationBlock();
                                    subscriptBlock.setDescription(descriptionGenerator.generateReadableDescription(subscriptBlock));
                                    List<String> subscriptNames = new ArrayList<String>();
                                    try {
                                        subscriptNames = sugiliteScriptDao.getAllNames();
                                    }
                                    catch (Exception e){
                                        e.printStackTrace();
                                    }
                                    AlertDialog.Builder chooseSubscriptDialogBuilder = new AlertDialog.Builder(context);
                                    String[] subscripts = new String[subscriptNames.size()];
                                    subscripts = subscriptNames.toArray(subscripts);
                                    final String[] subscriptClone = subscripts.clone();

                                    chooseSubscriptDialogBuilder.setItems(subscriptClone, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.dismiss();
                                            String chosenScriptName = subscriptClone[which];
                                            //add a subscript operation block with the script name "chosenScriptName"
                                            subscriptBlock.setSubscriptName(chosenScriptName);
                                            SugiliteStartingBlock script = null;
                                            try {
                                                script = sugiliteScriptDao.read(chosenScriptName);
                                            }
                                            catch (Exception e){
                                                e.printStackTrace();
                                            }
                                            if(script != null) {
                                                try {
                                                    SugiliteBlock currentBlock = sugiliteData.getCurrentScriptBlock();
                                                    if(currentBlock == null || sugiliteData.getScriptHead() == null)
                                                        throw new Exception("NULL CURRENT BLOCK OR NULL SCRIPT");
                                                    subscriptBlock.setPreviousBlock(currentBlock);
                                                    if (currentBlock instanceof SugiliteOperationBlock)
                                                        ((SugiliteOperationBlock) currentBlock).setNextBlock(subscriptBlock);
                                                    else if (currentBlock instanceof SugiliteStartingBlock)
                                                        ((SugiliteStartingBlock) currentBlock).setNextBlock(subscriptBlock);
                                                    else if (currentBlock instanceof SugiliteSpecialOperationBlock)
                                                        ((SugiliteSpecialOperationBlock) currentBlock).setNextBlock(subscriptBlock);
                                                    else
                                                        throw new Exception("UNSUPPORTED BLOCK TYPE");

                                                    //subscriptBlock.setDescription(descriptionGenerator.generateReadableDescription(subscriptBlock));
                                                    sugiliteData.setCurrentScriptBlock(subscriptBlock);
                                                    sugiliteScriptDao.save(sugiliteData.getScriptHead());
                                                }
                                                catch (Exception e){
                                                    e.printStackTrace();
                                                }


                                                //run the script
                                                SharedPreferences.Editor prefEditor = sharedPreferences.edit();
                                                prefEditor.putBoolean("recording_in_process", false);
                                                prefEditor.apply();

                                                try {
                                                    subscriptBlock.run(context, sugiliteData, sugiliteScriptDao, sharedPreferences);
                                                }
                                                catch (Exception e){
                                                    e.printStackTrace();
                                                }
                                            }


                                        }
                                    });

                                    Dialog chooseSubscriptDialog = chooseSubscriptDialogBuilder.create();
                                    if(chooseSubscriptDialog.getWindow() != null) {
                                        chooseSubscriptDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
                                    }
                                    chooseSubscriptDialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_box);
                                    chooseSubscriptDialog.show();
                                    break;


                                case "Add a Delay":
                                    SugiliteDelaySpecialOperationBlock delaySpecialOperationBlock = new SugiliteDelaySpecialOperationBlock(10000);
                                    delaySpecialOperationBlock.setDescription("Delay for 10s");

                                    try {
                                        SugiliteBlock currentBlock = sugiliteData.getCurrentScriptBlock();
                                        if(currentBlock == null || sugiliteData.getScriptHead() == null)
                                            throw new Exception("NULL CURRENT BLOCK OR NULL SCRIPT");
                                        delaySpecialOperationBlock.setPreviousBlock(currentBlock);
                                        if (currentBlock instanceof SugiliteOperationBlock)
                                            ((SugiliteOperationBlock) currentBlock).setNextBlock(delaySpecialOperationBlock);
                                        else if (currentBlock instanceof SugiliteStartingBlock)
                                            ((SugiliteStartingBlock) currentBlock).setNextBlock(delaySpecialOperationBlock);
                                        else if (currentBlock instanceof SugiliteSpecialOperationBlock)
                                            ((SugiliteSpecialOperationBlock) currentBlock).setNextBlock(delaySpecialOperationBlock);
                                        else
                                            throw new Exception("UNSUPPORTED BLOCK TYPE");

                                        //subscriptBlock.setDescription(descriptionGenerator.generateReadableDescription(subscriptBlock));
                                        sugiliteData.setCurrentScriptBlock(delaySpecialOperationBlock);
                                        sugiliteScriptDao.save(sugiliteData.getScriptHead());
                                    }
                                    catch (Exception e){
                                        e.printStackTrace();
                                    }
                                    break;


                                case "Get a Text Element on the Screen":
                                    SelectElementWithTextDialog selectElementWithTextDialog = new SelectElementWithTextDialog(context, layoutInflater, sugiliteData);
                                    selectElementWithTextDialog.show();
                                    break;
                                case "Resume Next Step":
                                    if(sugiliteData.storedInstructionQueueForPause.peek() != null && sugiliteData.storedInstructionQueueForPause.peek() instanceof SugiliteOperationBlock)
                                        ((SugiliteOperationBlock) sugiliteData.storedInstructionQueueForPause.peek()).isSetAsABreakPoint = false;
                                    sugiliteData.addInstructions(sugiliteData.storedInstructionQueueForPause);
                                    sugiliteData.storedInstructionQueueForPause.clear();
                                    sugiliteData.setCurrentSystemState(SugiliteData.REGULAR_DEBUG_STATE);
                                    dialog.dismiss();
                                    break;
                                case "Quit Debugging":
                                    sugiliteData.storedInstructionQueueForPause.clear();
                                    sugiliteData.setCurrentSystemState(SugiliteData.DEFAULT_STATE);
                                    dialog.dismiss();
                                    break;

                                case "Turn on verbal instruction":
                                    if(verbalInstructionIconManager != null){
                                        verbalInstructionIconManager.addStatusIcon();
                                    }
                                    break;

                                case "Turn off verbal instruction":
                                    if(verbalInstructionIconManager != null){
                                        verbalInstructionIconManager.removeStatusIcon();
                                    }
                                    break;


                                default:
                                    //do nothing
                            }
                        }
                    });
                    if (duckDialog != null && duckDialog.isShowing()){
                        duckDialog.dismiss();
                    }
                    duckDialog = textDialogBuilder.create();
                    duckDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            if (sugiliteData.getCurrentSystemState() == SugiliteData.PAUSED_FOR_DUCK_MENU_IN_REGULAR_EXECUTION_STATE
                                    || sugiliteData.getCurrentSystemState() == SugiliteData.PAUSED_FOR_DUCK_MENU_IN_DEBUG_MODE) {
                                //restore execution
                                sugiliteData.addInstructions(storedQueue);
                                sugiliteData.setCurrentSystemState(previousState);
                            }
                        }
                    });
                    if(duckDialog.getWindow() != null) {
                        duckDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
                        duckDialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_box);
                    }
                    duckDialog.show();
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

    public ImageView getStatusIcon() {
        return statusIcon;
    }

    @Deprecated
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

    /**
     * class used to display the state overlay shown at the bottom of the screen
     */
    class CurrentStateView extends LinearLayout{
        private TextView currentStateView;
        private TextView nextOperationView;
        private int currentState;

        public CurrentStateView(Context context, int currentState, String nextOperationHTMLDescription){
            super(context);
            this.currentState = currentState;
            currentStateView = new TextView(context);
            nextOperationView = new TextView(context);
            this.setOrientation(VERTICAL);
            if(currentState == SugiliteData.DEFAULT_STATE){
                currentStateView.setTextColor(Color.YELLOW);
                currentStateView.setText("");
                nextOperationView.setTextColor(Color.WHITE);
                nextOperationView.setText("");
            }
            else{
                currentStateView.setText(SugiliteData.getStringforState(currentState));
                currentStateView.setTextColor(Color.YELLOW);
                currentStateView.setBackgroundColor(Const.SEMI_TRANSPARENT_GRAY_BACKGROUND);
                nextOperationView.setTextColor(Color.WHITE);
                nextOperationView.setText("");
                if(currentState == SugiliteData.REGULAR_DEBUG_STATE || currentState == SugiliteData.PAUSED_FOR_DUCK_MENU_IN_DEBUG_MODE) {
                    nextOperationView.setText(Html.fromHtml("<b>Next Operation: </b>" + nextOperationHTMLDescription));
                    nextOperationView.setBackgroundColor(Const.SEMI_TRANSPARENT_GRAY_BACKGROUND);
                }
            }
            this.addView(currentStateView);
            this.addView(nextOperationView);
        }

        public void setCurrentStateView(int currentState, String nextOperationHTMLDescription){
            this.currentState = currentState;
            this.setOrientation(VERTICAL);
            if(currentState == SugiliteData.DEFAULT_STATE){
                currentStateView.setBackground(null);
                currentStateView.setText("");
                nextOperationView.setBackground(null);
                nextOperationView.setText("");
            }
            else{
                currentStateView.setText(SugiliteData.getStringforState(currentState));
                currentStateView.setTextColor(Color.YELLOW);
                currentStateView.setBackgroundColor(Const.SEMI_TRANSPARENT_GRAY_BACKGROUND);
                nextOperationView.setText("");
                nextOperationView.setBackground(null);
                if(currentState == SugiliteData.REGULAR_DEBUG_STATE || currentState == SugiliteData.PAUSED_FOR_DUCK_MENU_IN_DEBUG_MODE || currentState == SugiliteData.PAUSED_FOR_BREAKPOINT_STATE || currentState == SugiliteData.PAUSED_FOR_DUCK_MENU_IN_REGULAR_EXECUTION_STATE) {
                    nextOperationView.setBackgroundColor(Const.SEMI_TRANSPARENT_GRAY_BACKGROUND);
                    nextOperationView.setText(Html.fromHtml("<b>Next Operation: </b>" + nextOperationHTMLDescription));
                }
            }
        }
    }

    private CurrentStateView getViewForCurrentState(String nextOperationHTMLDescription){
        int state = sugiliteData.getCurrentSystemState();
        return new CurrentStateView(context, state, nextOperationHTMLDescription);
    }

    public void setVerbalInstructionIconManager(VerbalInstructionIconManager verbalInstructionIconManager) {
        this.verbalInstructionIconManager = verbalInstructionIconManager;
    }

    public void pauseTestRun(Activity a) {
        SugiliteStartingBlock script = ((ScriptDetailActivity) a).getScript();
        sugiliteData.clearInstructionQueue();
        //sugiliteData.setCurrentSystemState(SugiliteData.DEFAULT_STATE);
        if(storedQueue != null)
            storedQueue.clear();

        //Intent intent = new Intent(context, ScriptDetailActivity.class);
        //intent.putExtra("scriptName", script.getScriptName());
        //intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        //context.startActivity(intent);
        Intent openMainActivity= new Intent(a,ScriptDetailActivity.class);
        openMainActivity.putExtra("scriptName", script.getScriptName());
        openMainActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        openMainActivity.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        context.startActivity(openMainActivity);
        //}
    }
}
