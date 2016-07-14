package edu.cmu.hcii.sugilite;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.preference.PreferenceManager;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.cmu.hcii.sugilite.communication.SugiliteCommunicationController;
import edu.cmu.hcii.sugilite.dao.SugiliteScreenshotManager;
import edu.cmu.hcii.sugilite.model.AccessibilityNodeInfoList;
import edu.cmu.hcii.sugilite.automation.*;
import edu.cmu.hcii.sugilite.model.block.SugiliteBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteOperationBlock;
import edu.cmu.hcii.sugilite.model.block.UIElementMatchingFilter;
import edu.cmu.hcii.sugilite.ui.StatusIconManager;

public class SugiliteAccessibilityService extends AccessibilityService {
    private WindowManager windowManager;
    private SharedPreferences sharedPreferences;
    private Automator automator;
    private SugiliteData sugiliteData;
    private StatusIconManager statusIconManager;
    private SugiliteScreenshotManager screenshotManager;
    private Set<Integer> accessibilityEventSetToHandle, accessibilityEventSetToSend;
    private Thread automatorThread;
    private SugiliteCommunicationController communicationController;

    public SugiliteAccessibilityService() {
    }

    @Override
    public void onCreate(){
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sugiliteData = (SugiliteData)getApplication();
        statusIconManager = new StatusIconManager(this, sugiliteData, sharedPreferences);
        screenshotManager = new SugiliteScreenshotManager(sharedPreferences, getApplicationContext());
        automator = new Automator(sugiliteData, getApplicationContext(), statusIconManager);

        try {
            communicationController = new SugiliteCommunicationController(getApplicationContext());
            communicationController.start();
            communicationController.register();
        }
        catch (Exception e){
            e.printStackTrace();
        }

        Integer[] accessibilityEventArrayToHandle = {AccessibilityEvent.TYPE_VIEW_CLICKED,
                AccessibilityEvent.TYPE_VIEW_LONG_CLICKED,
                AccessibilityEvent.TYPE_VIEW_SELECTED,
                AccessibilityEvent.TYPE_VIEW_FOCUSED,
                AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED,
                AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED,
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
                AccessibilityEvent.TYPE_WINDOWS_CHANGED,
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED};
        Integer[] accessiblityEventArrayToSend = {AccessibilityEvent.TYPE_VIEW_CLICKED,
                AccessibilityEvent.TYPE_VIEW_LONG_CLICKED};
        accessibilityEventSetToHandle = new HashSet<>(Arrays.asList(accessibilityEventArrayToHandle));
        accessibilityEventSetToSend = new HashSet<>(Arrays.asList(accessiblityEventArrayToSend));

        try {
            Toast.makeText(this, "Sugilite Accessibility Service Started", Toast.LENGTH_SHORT).show();
            statusIconManager.addStatusIcon();
        }
        catch (Exception e){
            e.printStackTrace();
            //do nothing
        }
    }


    @Override
    public void onServiceConnected() {
        super.onServiceConnected();

    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        //TODO problem: the status of "right after click" (try getParent()?)
        final AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        //Type of accessibility events to handle in this function
        //return if the event is not among the accessibilityEventArrayToHandle
        if(!accessibilityEventSetToHandle.contains(Integer.valueOf(event.getEventType())))
            return;
        if (sharedPreferences.getBoolean("recording_in_process", false)) {
            //recording in progress
            AccessibilityNodeInfo sourceNode = event.getSource();
            Set<String> exceptedPackages = new HashSet<>();
            //skip internal interactions and interactions on system ui
            exceptedPackages.add("edu.cmu.hcii.sugilite");
            exceptedPackages.add("com.android.systemui");
            if (accessibilityEventSetToSend.contains(event.getEventType()) && (!exceptedPackages.contains(event.getPackageName()))) {
                File screenshot = null;
                if(sharedPreferences.getBoolean("root_enabled", false)) {
                    //take screenshot
                    try {
                        screenshot = screenshotManager.take(false);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                //start the popup activity
                startActivity(generatePopUpActivityIntentFromEvent(event, rootNode, screenshot));
            }
        }

        if (sharedPreferences.getBoolean("tracking_in_process", false)) {
            //background tracking in progress
        }
        SugiliteBlock currentBlock = sugiliteData.peekInstructionQueue();
        if(currentBlock instanceof SugiliteOperationBlock) {
            statusIconManager.refreshStatusIcon(rootNode, ((SugiliteOperationBlock) currentBlock).getElementMatchingFilter());
        }
        else{
            statusIconManager.refreshStatusIcon(null, null);
        }

        boolean retVal = false;


        if(sugiliteData.getInstructionQueueSize() > 0) {
            if(automatorThread == null) {
                automatorThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        automator.handleLiveEvent(rootNode, getApplicationContext());
                        automatorThread = null;
                    }
                });
                automatorThread.start();
            }
        }

    }



    @Override
    public void onInterrupt() {
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        Toast.makeText(this, "Sugilite Accessibility Service Stopped", Toast.LENGTH_SHORT).show();
        if(statusIconManager != null)
            try {
                statusIconManager.removeStatusIcon();
            }
            catch (Exception e){
                //failed to remove status icon
                e.printStackTrace();
            }
        //windowManager.removeView(statusIcon);

        try {
            communicationController.unregister();
            communicationController.stop();
        }
        catch (Exception e){
            e.printStackTrace();
        }

    }







    private Intent generatePopUpActivityIntentFromEvent(AccessibilityEvent event, AccessibilityNodeInfo rootNode, File screenshot){
        AccessibilityNodeInfo sourceNode = event.getSource();
        Rect boundsInParents = new Rect();
        Rect boundsInScreen = new Rect();
        sourceNode.getBoundsInParent(boundsInParents);
        sourceNode.getBoundsInScreen(boundsInScreen);
        AccessibilityNodeInfo parentNode = sourceNode.getParent();
        //NOTE: NOT ONLY COUNTING THE IMMEDIATE CHILDREN NOW
        ArrayList<AccessibilityNodeInfo> childrenNodes = new ArrayList<>(Automator.preOrderTraverse(sourceNode));
        ArrayList<AccessibilityNodeInfo> allNodes = new ArrayList<>();
        if(rootNode != null)
             allNodes = new ArrayList<>(Automator.preOrderTraverse(rootNode));
        //TODO:AccessibilityNodeInfo is not serializable

        //pop up the selection window
        Intent popUpIntent = new Intent(this, mRecordingPopUpActivity.class);
        popUpIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        popUpIntent.putExtra("packageName", sourceNode.getPackageName());
        popUpIntent.putExtra("className", sourceNode.getClassName());
        popUpIntent.putExtra("text", sourceNode.getText());
        popUpIntent.putExtra("contentDescription", sourceNode.getContentDescription());
        popUpIntent.putExtra("viewId", sourceNode.getViewIdResourceName());
        popUpIntent.putExtra("boundsInParent", boundsInParents.flattenToString());
        popUpIntent.putExtra("boundsInScreen", boundsInScreen.flattenToString());
        popUpIntent.putExtra("time", Calendar.getInstance().getTimeInMillis());
        popUpIntent.putExtra("eventType", event.getEventType());
        popUpIntent.putExtra("parentNode", parentNode);
        popUpIntent.putExtra("childrenNodes", new AccessibilityNodeInfoList(childrenNodes));
        popUpIntent.putExtra("allNodes", new AccessibilityNodeInfoList(allNodes));
        popUpIntent.putExtra("isEditable", sourceNode.isEditable());
        popUpIntent.putExtra("eventType", event.getEventType());
        popUpIntent.putExtra("screenshot", screenshot);
        popUpIntent.putExtra("trigger", mRecordingPopUpActivity.TRIGGERED_BY_NEW_EVENT);
        return popUpIntent;
    }
}

