package edu.cmu.hcii.sugilite;

import android.accessibilityservice.AccessibilityService;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import java.io.File;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.cmu.hcii.sugilite.communication.SugiliteCommunicationController;
import edu.cmu.hcii.sugilite.dao.SugiliteAppVocabularyDao;
import edu.cmu.hcii.sugilite.dao.SugiliteScreenshotManager;
import edu.cmu.hcii.sugilite.model.AccessibilityNodeInfoList;
import edu.cmu.hcii.sugilite.automation.*;
import edu.cmu.hcii.sugilite.model.block.SerializableNodeInfo;
import edu.cmu.hcii.sugilite.model.block.SugiliteAvailableFeaturePack;
import edu.cmu.hcii.sugilite.model.block.SugiliteBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteOperationBlock;
import edu.cmu.hcii.sugilite.recording.RecordingPopUpDialog;
import edu.cmu.hcii.sugilite.recording.mRecordingPopUpActivity;
import edu.cmu.hcii.sugilite.tracking.SugiliteTrackingHandler;
import edu.cmu.hcii.sugilite.ui.StatusIconManager;

public class SugiliteAccessibilityService extends AccessibilityService {
    private WindowManager windowManager;
    private SharedPreferences sharedPreferences;
    private Automator automator;
    private SugiliteData sugiliteData;
    private StatusIconManager statusIconManager;
    private SugiliteScreenshotManager screenshotManager;
    private Set<Integer> accessibilityEventSetToHandle, accessibilityEventSetToSend, accessibilityEventSetToTrack;
    private Thread automatorThread;
    private Context context;
    private SugiliteTrackingHandler sugilteTrackingHandler;
    private SugiliteAppVocabularyDao vocabularyDao;
    final private static boolean BUILDING_VOCAB = false;
    private Handler handler;


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
        automator = new Automator(sugiliteData, this, statusIconManager, sharedPreferences);
        sugilteTrackingHandler = new SugiliteTrackingHandler(sugiliteData, getApplicationContext());
        availableAlternatives = new HashSet<>();
        availableAlternativeNodes = new HashSet<>();
        trackingPackageVocabs = new HashSet<>();
        packageVocabs = new HashSet<>();
        vocabularyDao = new SugiliteAppVocabularyDao(getApplicationContext());
        context = this;
        handler = new Handler();
        try {
            //TODO: periodically check the status of communication controller
            sugiliteData.communicationController = new SugiliteCommunicationController(getApplicationContext(), sugiliteData, sharedPreferences);
            sugiliteData.communicationController.start();
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
                AccessibilityEvent.TYPE_VIEW_LONG_CLICKED,
                AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED};
        Integer[] accessibilityEventArrayToTrack = {
                AccessibilityEvent.TYPE_VIEW_CLICKED,
                AccessibilityEvent.TYPE_VIEW_LONG_CLICKED,
                AccessibilityEvent.TYPE_VIEW_SELECTED,
                AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
        };
        accessibilityEventSetToHandle = new HashSet<>(Arrays.asList(accessibilityEventArrayToHandle));
        accessibilityEventSetToSend = new HashSet<>(Arrays.asList(accessiblityEventArrayToSend));
        accessibilityEventSetToTrack = new HashSet<>(Arrays.asList(accessibilityEventArrayToTrack));

        //end recording

        //set default value for the settings
        SharedPreferences.Editor prefEditor = sharedPreferences.edit();
        prefEditor.putBoolean("recording_in_process", false);
        prefEditor.putBoolean("root_enabled", true);
        prefEditor.putBoolean("auto_fill_enabled", true);
        prefEditor.commit();
        sugiliteData.clearInstructionQueue();
        if(sugiliteData.errorHandler == null){
            sugiliteData.errorHandler = new ErrorHandler(this, sugiliteData, sharedPreferences);
        }
        if(sugiliteData.trackingName.contentEquals("default")){
            sugiliteData.initiateTracking(sugilteTrackingHandler.getDefaultTrackingName());
        }

        try {
            Toast.makeText(this, "Sugilite Accessibility Service Started", Toast.LENGTH_SHORT).show();
            statusIconManager.addStatusIcon();
        }
        catch (Exception e){
            e.printStackTrace();
            //do nothing
        }

        final Handler handler1 = new Handler();
        handler1.postDelayed(new Runnable() {
            //run error handler every 2 seconds if executing
            @Override
            public void run() {
                if(sugiliteData.getInstructionQueueSize() > 0)
                    sugiliteData.errorHandler.checkError(sugiliteData.peekInstructionQueue());
                handler1.postDelayed(this, 2000);
            }
        }, 2000);

        final Handler handler2 = new Handler();
        handler2.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(sugiliteData.getInstructionQueueSize() > 0)
                    statusIconManager.refreshStatusIcon(null, null);
                handler1.postDelayed(this, 500);
            }
        }, 500);


    }


    @Override
    public void onServiceConnected() {
        super.onServiceConnected();

    }

    public void runOnUiThread(Runnable runnable) {
        handler.post(runnable);
    }

    private HashSet<Map.Entry<String, String>> availableAlternatives;
    private HashSet<SerializableNodeInfo> availableAlternativeNodes;
    private HashSet<Map.Entry<String, String>> packageVocabs;

    private HashSet<Map.Entry<String, String>> trackingPackageVocabs;

    Set<String> exceptedPackages = new HashSet<>();
    Set<String> trackingExcludedPackages = new HashSet<>();

    String previousClickText = "NULL", previousClickContentDescription = "NULL", previousClickChildText = "NULL", previousClickChildContentDescription = "NULL", previousClickPackageName = "NULL";
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        //TODO problem: the status of "right after click" (try getParent()?)
        //TODO new rootNode method
        final AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        AccessibilityNodeInfo sourceNode = event.getSource();




        //Type of accessibility events to handle in this function
        //return if the event is not among the accessibilityEventArrayToHandle
        if(!accessibilityEventSetToHandle.contains(Integer.valueOf(event.getEventType()))) {
            return;
        }

        //check communication status

        /* temporarily disable the communication controller for performance optimization
        if(sugiliteData.communicationController != null){
            if(!sugiliteData.communicationController.checkConnectionStatus())
                sugiliteData.communicationController.start();
        }
        */

        //add previous click information for building UI hierachy from vocabs
        if(BUILDING_VOCAB && (!trackingExcludedPackages.contains(event.getPackageName())) && event.getEventType() == AccessibilityEvent.TYPE_VIEW_CLICKED && sourceNode != null){
            if(sourceNode.getText() != null)
                previousClickText = sourceNode.getText().toString();
            else
                previousClickText = "NULL";
            if(sourceNode.getContentDescription() != null)
                previousClickContentDescription = sourceNode.getContentDescription().toString();
            else
                previousClickContentDescription = "NULL";
            if(sourceNode.getPackageName() != null)
                previousClickPackageName = sourceNode.getPackageName().toString();
            else
                previousClickPackageName = "NULL";

            List<AccessibilityNodeInfo> childNodes = Automator.preOrderTraverse(sourceNode);
            Set<String> childTexts = new HashSet<>();
            Set<String> childContentDescriptions = new HashSet<>();
            for(AccessibilityNodeInfo childNode : childNodes){
                if(childNode.getText() != null)
                    childTexts.add(childNode.getText().toString());
                if(childNode.getContentDescription() != null)
                    childContentDescriptions.add(childNode.getContentDescription().toString());
            }
            if(childTexts.size() > 0)
                previousClickChildText = childTexts.toString();
            else
                previousClickChildText = "NULL";
            if(childContentDescriptions.size() > 0)
                previousClickContentDescription = childContentDescriptions.toString();
            else
                previousClickContentDescription = "NULL";
        }


        exceptedPackages.add("edu.cmu.hcii.sugilite");
        exceptedPackages.add("com.android.systemui");
        exceptedPackages.add("edu.cmu.hcii.sugilitecommunicationtest");

        trackingExcludedPackages.add("edu.cmu.hcii.sugilitecommunicationtest");
        trackingExcludedPackages.add("edu.cmu.hcii.sugilite");

        if (sugiliteData.getInstructionQueueSize() > 0 && !sharedPreferences.getBoolean("recording_in_process", true) && !exceptedPackages.contains(event.getPackageName()) && sugiliteData.errorHandler != null){
            //script running in progress
            //invoke the error handler
            sugiliteData.errorHandler.checkError(event, sugiliteData.peekInstructionQueue(), Calendar.getInstance().getTimeInMillis());
        }

        if (sharedPreferences.getBoolean("recording_in_process", false)) {
            //recording in progress

            //add package name to the relevant package set
            if(sugiliteData.getScriptHead() != null && event.getPackageName() != null && (!exceptedPackages.contains(event.getPackageName())))
                sugiliteData.getScriptHead().relevantPackages.add(event.getPackageName().toString());

            //skip internal interactions and interactions on system ui
            availableAlternatives.addAll(getAlternativeLabels(sourceNode, rootNode));
            availableAlternativeNodes.addAll(getAvailableAlternativeNodes(sourceNode, rootNode));

            //if the event is to be recorded, process it
            if (accessibilityEventSetToSend.contains(event.getEventType()) && (!exceptedPackages.contains(event.getPackageName()))) {
                if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED || event.getEventType() == AccessibilityEvent.TYPE_VIEW_FOCUSED){
                    //pop up warning dialog if focus on text box
                    if(sourceNode != null && sourceNode.isEditable()){
                        Toast.makeText(context, "For recording text entry, please type into the Sugilite recording dialog instead of directly in the textbox. Click on the textbox to show the Sugilite recording dialog.", Toast.LENGTH_SHORT).show();
                    }
                }
                else {
                    //temp hack
                    if(sourceNode != null && sourceNode.getClassName() != null && sourceNode.getPackageName() != null && sourceNode.getClassName().toString().contentEquals("android.view.ViewGroup") && sourceNode.getPackageName().equals("com.google.android.googlequicksearchbox"))
                    {/*do nothing (don't show popup)*/}
                    else {
                        //send the event to recording pop up dialog
                        File screenshot = null;
                        if (sharedPreferences.getBoolean("root_enabled", false)) {
                            //take screenshot
                            try {
                        /*
                        System.out.println("taking screen shot");
                        screenshot = screenshotManager.take(false);
                        */
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        //start the popup activity
                        RecordingPopUpDialog recordingPopUpDialog = new RecordingPopUpDialog(sugiliteData, getApplicationContext(), generateFeaturePack(event, rootNode, screenshot, availableAlternativeNodes), sharedPreferences, LayoutInflater.from(getApplicationContext()), RecordingPopUpDialog.TRIGGERED_BY_NEW_EVENT, availableAlternatives);
                        sugiliteData.recordingPopupDialogQueue.add(recordingPopUpDialog);
                        if(!sugiliteData.recordingPopupDialogQueue.isEmpty() && sugiliteData.hasRecordingPopupActive == false) {
                            sugiliteData.hasRecordingPopupActive = true;
                            sugiliteData.recordingPopupDialogQueue.poll().show();
                        }
                    }
                }
            }
            if(BUILDING_VOCAB) {
                //add alternative nodes to the app vocab set
                for (SerializableNodeInfo node : availableAlternativeNodes) {
                    if (node.packageName != null && node.text != null)
                        packageVocabs.add(new AbstractMap.SimpleEntry<String, String>(node.packageName, node.text));
                    if (node.packageName != null && node.childText != null && node.childText.size() > 0) {
                        for (String childText : node.childText)
                            packageVocabs.add(new AbstractMap.SimpleEntry<String, String>(node.packageName, childText));
                    }
                }
            }

            if (accessibilityEventSetToSend.contains(event.getEventType()) && (!exceptedPackages.contains(event.getPackageName()))) {

                if(BUILDING_VOCAB) {
                    for (Map.Entry<String, String> entry : packageVocabs) {
                        try {
                            vocabularyDao.save(entry.getKey(), entry.getValue(), "meh", previousClickText, previousClickContentDescription, previousClickChildText, previousClickChildContentDescription, previousClickPackageName);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }

                availableAlternatives.clear();
                availableAlternativeNodes.clear();
                if(BUILDING_VOCAB)
                    packageVocabs.clear();
            }

        }

        if(sharedPreferences.getBoolean("broadcasting_enabled", false)) {
            if (accessibilityEventSetToTrack.contains(event.getEventType()) && (!trackingExcludedPackages.contains(event.getPackageName()))) {
                sugiliteData.handleBroadcastingEvent(event);
            }
        }

        if (sharedPreferences.getBoolean("tracking_in_process", false)) {
            //background tracking in progress
            if (accessibilityEventSetToTrack.contains(event.getEventType()) && (!trackingExcludedPackages.contains(event.getPackageName()))) {
                sugilteTrackingHandler.handle(event, sourceNode, generateFeaturePack(event, rootNode, null, null));
            }

            //add all seen clickable nodes to package vocab DB
            if(BUILDING_VOCAB) {
                for (SerializableNodeInfo node : getAvailableAlternativeNodes(sourceNode, rootNode)) {
                    if (node.packageName != null && node.text != null)
                        packageVocabs.add(new AbstractMap.SimpleEntry<>(node.packageName, node.text));
                    if (node.packageName != null && node.childText != null && node.childText.size() > 0) {
                        for (String childText : node.childText)
                            packageVocabs.add(new AbstractMap.SimpleEntry<>(node.packageName, childText));
                    }
                }
                //only read/write DB at every click -> to optimize performance
                if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_CLICKED) {
                    for (Map.Entry<String, String> entry : packageVocabs) {
                        try {
                            vocabularyDao.save(entry.getKey(), entry.getValue(), "meh", previousClickText, previousClickContentDescription, previousClickChildText, previousClickChildContentDescription, previousClickPackageName);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    trackingPackageVocabs.clear();
                }
            }
        }

        SugiliteBlock currentBlock = sugiliteData.peekInstructionQueue();
        //refresh status icon
        if(currentBlock instanceof SugiliteOperationBlock) {
            statusIconManager.refreshStatusIcon(rootNode, ((SugiliteOperationBlock) currentBlock).getElementMatchingFilter());
        }
        else{
            statusIconManager.refreshStatusIcon(null, null);
        }

        boolean retVal = false;


        if(sugiliteData.getInstructionQueueSize() > 0) {
            //run automation
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
            sugiliteData.communicationController.unregister();
            sugiliteData.communicationController.stop();
        }
        catch (Exception e){
            e.printStackTrace();
        }

    }




    private SugiliteAvailableFeaturePack generateFeaturePack(AccessibilityEvent event, AccessibilityNodeInfo rootNode, File screenshot, HashSet<SerializableNodeInfo> availableAlternativeNodes){
        SugiliteAvailableFeaturePack featurePack = new SugiliteAvailableFeaturePack();
        AccessibilityNodeInfo sourceNode = event.getSource();
        Rect boundsInParents = new Rect();
        Rect boundsInScreen = new Rect();
        AccessibilityNodeInfo parentNode = null;
        if(sourceNode != null) {
            sourceNode.getBoundsInParent(boundsInParents);
            sourceNode.getBoundsInScreen(boundsInScreen);
            parentNode = sourceNode.getParent();
        }
        //NOTE: NOT ONLY COUNTING THE IMMEDIATE CHILDREN NOW
        ArrayList<AccessibilityNodeInfo> childrenNodes = null;
        if(sourceNode != null && Automator.preOrderTraverse(sourceNode) != null)
            childrenNodes = new ArrayList<>(Automator.preOrderTraverse(sourceNode));
        else
            childrenNodes = new ArrayList<>();
        ArrayList<AccessibilityNodeInfo> allNodes = null;
        if(rootNode != null && Automator.preOrderTraverse(rootNode) != null)
            allNodes = new ArrayList<>(Automator.preOrderTraverse(rootNode));
        else
            allNodes = new ArrayList<>();
        //TODO:AccessibilityNodeInfo is not serializable

        if(sourceNode == null || sourceNode.getPackageName() == null){
            featurePack.packageName = "NULL";
        }
        else
            featurePack.packageName = sourceNode.getPackageName().toString();

        if(sourceNode == null || sourceNode.getClassName() == null){
            featurePack.className = "NULL";
        }
        else
            featurePack.className = sourceNode.getClassName().toString();

        if(sourceNode == null || sourceNode.getText() == null){
            featurePack.text = "NULL";
        }
        else
            featurePack.text = sourceNode.getText().toString();

        if(sourceNode == null || sourceNode.getContentDescription() == null){
            featurePack.contentDescription = "NULL";
        }
        else
            featurePack.contentDescription = sourceNode.getContentDescription().toString();

        if(sourceNode == null || sourceNode.getViewIdResourceName() == null){
            featurePack.viewId = "NULL";
        }
        else
            featurePack.viewId = sourceNode.getViewIdResourceName();

        featurePack.boundsInParent = boundsInParents.flattenToString();
        featurePack.boundsInScreen = boundsInScreen.flattenToString();
        featurePack.time = Calendar.getInstance().getTimeInMillis();
        featurePack.eventType = event.getEventType();
        featurePack.parentNode = new SerializableNodeInfo(parentNode);
        featurePack.childNodes = new AccessibilityNodeInfoList(childrenNodes).getSerializableList();
        featurePack.allNodes = new AccessibilityNodeInfoList(allNodes).getSerializableList();
        if(sourceNode != null)
            featurePack.isEditable = sourceNode.isEditable();
        else
            featurePack.isEditable = false;
        featurePack.screenshot = screenshot;
        if(availableAlternativeNodes != null)
            featurePack.alternativeNodes = new HashSet<>(availableAlternativeNodes);
        else
            featurePack.alternativeNodes = new HashSet<>();

        return featurePack;


    }

    @Deprecated
    private Intent generatePopUpActivityIntentFromEvent(AccessibilityEvent event, AccessibilityNodeInfo rootNode, File screenshot, HashSet<Map.Entry<String, String>> entryHashSet){
        AccessibilityNodeInfo sourceNode = event.getSource();
        Rect boundsInParents = new Rect();
        Rect boundsInScreen = new Rect();
        sourceNode.getBoundsInParent(boundsInParents);
        sourceNode.getBoundsInScreen(boundsInScreen);
        AccessibilityNodeInfo parentNode = sourceNode.getParent();
        //NOTE: NOT ONLY COUNTING THE IMMEDIATE CHILDREN NOW
        ArrayList<AccessibilityNodeInfo> childrenNodes = null;
        if(sourceNode != null && Automator.preOrderTraverse(sourceNode) != null)
             childrenNodes = new ArrayList<>(Automator.preOrderTraverse(sourceNode));
        else
            childrenNodes = new ArrayList<>();
        ArrayList<AccessibilityNodeInfo> allNodes = new ArrayList<>();
        if(rootNode != null)
             allNodes = new ArrayList<>(Automator.preOrderTraverse(rootNode));
        //TODO:AccessibilityNodeInfo is not serializable

        //pop up the selection window
        Intent popUpIntent = new Intent(this, mRecordingPopUpActivity.class);
        popUpIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        popUpIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);

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
        popUpIntent.putExtra("screenshot", screenshot);
        popUpIntent.putExtra("trigger", RecordingPopUpDialog.TRIGGERED_BY_NEW_EVENT);
        popUpIntent.putExtra("alternativeLabels", entryHashSet);
        return popUpIntent;
    }

    private HashSet<Map.Entry<String, String>> getAlternativeLabels (AccessibilityNodeInfo sourceNode, AccessibilityNodeInfo rootNode){
        HashSet<Map.Entry<String, String>> retMap = new HashSet<>();
        List<AccessibilityNodeInfo> allNodes = Automator.preOrderTraverse(rootNode);
        if(allNodes == null)
            return retMap;
        for(AccessibilityNodeInfo node : allNodes){
            if(exceptedPackages.contains(node.getPackageName()))
                continue;
            if(!node.isClickable())
                continue;
            if(!(sourceNode == null || (sourceNode.getClassName() == null && node.getClassName() == null) || (sourceNode.getClassName() != null && node.getClassName() != null && sourceNode.getClassName().toString().contentEquals(node.getClassName()))))
                continue;
            if(node.getText() != null)
                retMap.add(new AbstractMap.SimpleEntry<>("Text", node.getText().toString()));
            if(node.getContentDescription() != null)
                retMap.add(new AbstractMap.SimpleEntry<>("ContentDescription", node.getContentDescription().toString()));
            List<AccessibilityNodeInfo> childNodes = Automator.preOrderTraverse(node);
            if(childNodes == null)
                continue;
            for(AccessibilityNodeInfo childNode : childNodes){
                if(childNode == null)
                    continue;
                if(childNode.getText() != null)
                    retMap.add(new AbstractMap.SimpleEntry<>("Child Text", childNode.getText().toString()));
                if(childNode.getContentDescription() != null)
                    retMap.add(new AbstractMap.SimpleEntry<>("Child ContentDescription", childNode.getContentDescription().toString()));
            }
        }
        return retMap;
    }

    private HashSet<SerializableNodeInfo> getAvailableAlternativeNodes (AccessibilityNodeInfo sourceNode, AccessibilityNodeInfo rootNode){
        List<AccessibilityNodeInfo> allNodes = Automator.preOrderTraverse(rootNode);
        HashSet<SerializableNodeInfo> retSet = new HashSet<>();
        if(allNodes == null)
            return retSet;
        for(AccessibilityNodeInfo node : allNodes){
            if(exceptedPackages.contains(node.getPackageName()))
                continue;
            if(!node.isClickable())
                continue;
            SerializableNodeInfo nodeToAdd = new SerializableNodeInfo(node);
            retSet.add(nodeToAdd);
        }
        return retSet;
    }
}

