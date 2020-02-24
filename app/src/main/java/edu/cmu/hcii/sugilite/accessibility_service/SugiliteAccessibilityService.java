package edu.cmu.hcii.sugilite.accessibility_service;

import android.accessibilityservice.AccessibilityService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import android.widget.Toast;

import com.google.gson.*;

import java.io.*;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import edu.cmu.hcii.sugilite.Const;
import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.communication.SugiliteCommunicationController;
import edu.cmu.hcii.sugilite.communication.SugiliteEventBroadcastingActivity;
import edu.cmu.hcii.sugilite.dao.SugiliteAppVocabularyDao;
import edu.cmu.hcii.sugilite.ontology.SerializableUISnapshot;
import edu.cmu.hcii.sugilite.ontology.UISnapshot;
import edu.cmu.hcii.sugilite.ontology.helper.annotator.SugiliteTextParentAnnotator;
import edu.cmu.hcii.sugilite.pumice.PumiceDemonstrationUtil;
import edu.cmu.hcii.sugilite.pumice.visualization.PumiceDemoVisualizationManager;
import edu.cmu.hcii.sugilite.recording.SugiliteScreenshotManager;
import edu.cmu.hcii.sugilite.automation.*;
import edu.cmu.hcii.sugilite.model.block.util.SerializableNodeInfo;
import edu.cmu.hcii.sugilite.model.block.util.SugiliteAvailableFeaturePack;
import edu.cmu.hcii.sugilite.model.block.SugiliteBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteOperationBlock;
import edu.cmu.hcii.sugilite.model.trigger.SugiliteTriggerHandler;
import edu.cmu.hcii.sugilite.recording.TextChangedEventHandler;
import edu.cmu.hcii.sugilite.recording.newrecording.NewDemonstrationHandler;
import edu.cmu.hcii.sugilite.recording.newrecording.fullscreen_overlay.FullScreenRecordingOverlayManager;
import edu.cmu.hcii.sugilite.tracking.SugiliteTrackingHandler;
import edu.cmu.hcii.sugilite.ui.StatusIconManager;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.VerbalInstructionIconManager;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.study.SugiliteStudyHandler;

import static edu.cmu.hcii.sugilite.Const.BROADCASTING_ACCESSIBILITY_EVENT;
import static edu.cmu.hcii.sugilite.Const.BUILDING_VOCAB;
import static edu.cmu.hcii.sugilite.Const.HOME_SCREEN_PACKAGE_NAMES;
import static edu.cmu.hcii.sugilite.Const.INTERVAL_ERROR_CHECKING_ACCESSIBILITY_SERVICE;
import static edu.cmu.hcii.sugilite.Const.INTERVAL_REFRESH_SUGILITE_ICON;
import static edu.cmu.hcii.sugilite.Const.KEEP_ALL_TEXT_LABEL_LIST;
import static edu.cmu.hcii.sugilite.accessibility_service.SugiliteAccessibilityServiceUtil.generateFeaturePack;
import static edu.cmu.hcii.sugilite.accessibility_service.SugiliteAccessibilityServiceUtil.getAllNodesWithText;
import static edu.cmu.hcii.sugilite.accessibility_service.SugiliteAccessibilityServiceUtil.getAlternativeLabels;
import static edu.cmu.hcii.sugilite.accessibility_service.SugiliteAccessibilityServiceUtil.getAvailableAlternativeNodes;

/**
 * the background service for Sugilite
 */
public class SugiliteAccessibilityService extends AccessibilityService {

    protected static final String TAG = SugiliteAccessibilityService.class.getSimpleName();

    private WindowManager windowManager;
    private SharedPreferences sharedPreferences;
    private Automator automator;
    private SugiliteData sugiliteData;
    private StatusIconManager statusIconManager;
    private VerbalInstructionIconManager verbalInstructionIconManager;
    private PumiceDemoVisualizationManager pumiceDemoVisualizationManager;
    private SugiliteScreenshotManager screenshotManager;
    private SugiliteStudyHandler sugiliteStudyHandler;
    private Set<Integer> accessibilityEventSetToHandle, accessibilityEventSetToSend, accessibilityEventSetToTrack;

    private SugiliteAccessibilityService context;
    private SugiliteTrackingHandler sugilteTrackingHandler;
    private SugiliteAppVocabularyDao vocabularyDao;
    private SugiliteTriggerHandler triggerHandler;
    private TextChangedEventHandler textChangedEventHandler;
    private String lastPackageName = "";
    private NewDemonstrationHandler newDemonstrationHandler;
    private static Set<String> homeScreenPackageNameSet;
    private SugiliteTextParentAnnotator sugiliteTextParentAnnotator;
    private FullScreenRecordingOverlayManager recordingOverlayManager;

    //this thread is for executing automation
    private static final int AUTOMATOR_THREAD_EXECUTOR_THREAD_COUNT = 3;
    private ExecutorService automatorThreadExecutor = Executors.newFixedThreadPool(AUTOMATOR_THREAD_EXECUTOR_THREAD_COUNT);

    //this exectuor is for handling onAccessibilityEvent events
    private static final int ON_ACCESSIBILITY_EXECUTOR_THREAD_COUNT = 2;
    private ExecutorService executor = Executors.newFixedThreadPool(ON_ACCESSIBILITY_EXECUTOR_THREAD_COUNT);

    //this thread is for generating ui snapshots
    private static final int UI_SNAPSHOT_EXECUTOR_THREAD_COUNT = 3;
    private ExecutorService uiSnapshotGenerationExecutor = Executors.newFixedThreadPool(UI_SNAPSHOT_EXECUTOR_THREAD_COUNT);


    private Set<String> exceptedPackages = new HashSet<>();
    private Set<String> trackingExcludedPackages = new HashSet<>();

    private Handler errorHandlingHandler;
    private Handler refreshIconHandler;

    private String currentAppActivityName;
    private String currentPackageName;

    public SugiliteAccessibilityService() {
        Log.d(TAG, "inside constructor");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "inside onCreate");
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        //Oscar: we need to do this validation in order to avoid Cast Exception when calling Sugilite from Middleware
        if (getApplication() instanceof SugiliteData) {
            sugiliteData = (SugiliteData) getApplication();
        }
        AccessibilityManager accessibilityManager = (AccessibilityManager) this.getSystemService(Context.ACCESSIBILITY_SERVICE);
        context = this;
        sugiliteStudyHandler = new SugiliteStudyHandler(context, this, sugiliteData.getTTS());
        statusIconManager = new StatusIconManager(this, sugiliteData, sharedPreferences, accessibilityManager);
        recordingOverlayManager = new FullScreenRecordingOverlayManager(context, sugiliteData, sharedPreferences, this, sugiliteData.getTTS());
        verbalInstructionIconManager = new VerbalInstructionIconManager(this, sugiliteStudyHandler, sugiliteData, sharedPreferences, recordingOverlayManager, this, sugiliteData.getTTS());
        statusIconManager.setVerbalInstructionIconManager(verbalInstructionIconManager);
        sugiliteData.verbalInstructionIconManager = verbalInstructionIconManager;
        newDemonstrationHandler = NewDemonstrationHandler.getInstance(sugiliteData, sharedPreferences, this);

        screenshotManager = SugiliteScreenshotManager.getInstance(sharedPreferences, sugiliteData);
        sugiliteTextParentAnnotator = SugiliteTextParentAnnotator.getInstance();
        automator = new Automator(sugiliteData, this, statusIconManager, sharedPreferences, sugiliteTextParentAnnotator, sugiliteData.getTTS());
        sugilteTrackingHandler = new SugiliteTrackingHandler(sugiliteData, getApplicationContext());
        availableAlternatives = new HashSet<>();
        availableAlternativeNodes = new HashSet<>();
        trackingPackageVocabs = new HashSet<>();
        packageVocabs = new HashSet<>();
        vocabularyDao = new SugiliteAppVocabularyDao(getApplicationContext());

        triggerHandler = new SugiliteTriggerHandler(context, sugiliteData, sharedPreferences);
        textChangedEventHandler = new TextChangedEventHandler(sugiliteData, context, sharedPreferences, new Handler());

        homeScreenPackageNameSet = new HashSet<>();
        homeScreenPackageNameSet.addAll(Arrays.asList(HOME_SCREEN_PACKAGE_NAMES));

        try {
            //initiate the InMind communication controller
            sugiliteData.communicationController = SugiliteCommunicationController.getInstance(
                    context, sugiliteData, sharedPreferences);
        } catch (Exception e) {
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
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
                AccessibilityEvent.TYPE_ANNOUNCEMENT};
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

        prefEditor.apply();

        //packages within the excepted package will be totally excepted from the accessibility service tracking
        exceptedPackages.addAll(Arrays.asList(Const.ACCESSIBILITY_SERVICE_EXCEPTED_PACKAGE_NAMES));
        exceptedPackages.addAll(Arrays.asList(Const.INPUT_METHOD_PACKAGE_NAMES));
        trackingExcludedPackages.addAll(Arrays.asList(Const.ACCESSIBILITY_SERVICE_TRACKING_EXCLUDED_PACKAGE_NAMES));

        init();

        PumiceDemonstrationUtil.showSugiliteToast("Sugilite Accessibility Service Created", Toast.LENGTH_SHORT);
    }

    private void init() {
        sugiliteData.clearInstructionQueue();

        if (sugiliteData.errorHandler == null) {
            sugiliteData.errorHandler = new ErrorHandler(this, sugiliteData, sharedPreferences);
        }
        if (sugiliteData.trackingName != null && sugiliteData.trackingName.contentEquals("default")) {
            sugiliteData.initiateTracking(sugilteTrackingHandler.getDefaultTrackingName());
        }

        errorHandlingHandler = new Handler();
        errorHandlingHandler.postDelayed(new Runnable() {
            //run error handler every 2 seconds if executing
            @Override
            public void run() {
                if (sugiliteData.getInstructionQueueSize() > 0)
                    sugiliteData.errorHandler.checkError(sugiliteData.peekInstructionQueue());
                errorHandlingHandler.postDelayed(this, INTERVAL_ERROR_CHECKING_ACCESSIBILITY_SERVICE);
            }
        }, INTERVAL_ERROR_CHECKING_ACCESSIBILITY_SERVICE);

        refreshIconHandler = new Handler();
        refreshIconHandler.postDelayed(new Runnable() {
            //refresh status icon every 1 second
            @Override
            public void run() {
                if (sugiliteData.getInstructionQueueSize() > 0)
                    statusIconManager.refreshStatusIcon(null, null, true);
                refreshIconHandler.postDelayed(this, INTERVAL_REFRESH_SUGILITE_ICON);
            }
        }, INTERVAL_REFRESH_SUGILITE_ICON);

        try {
            if (!statusIconManager.isShowingIcon()) {
                statusIconManager.addStatusIcon();
            }

        } catch (Exception e) {
            e.printStackTrace();
            //do nothing
        }

        pumiceDemoVisualizationManager = new PumiceDemoVisualizationManager(context);
        PumiceDemonstrationUtil.showSugiliteToast("Sugilite Accessibility Service Started", Toast.LENGTH_SHORT);
    }

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        Log.d(TAG, "inside onServiceCreated");

    }


    private HashSet<Map.Entry<String, String>> availableAlternatives;
    private HashSet<SerializableNodeInfo> availableAlternativeNodes;
    private HashSet<Map.Entry<String, String>> packageVocabs;
    private HashSet<Map.Entry<String, String>> trackingPackageVocabs;
    private String previousClickText = "NULL", previousClickContentDescription = "NULL", previousClickChildText = "NULL", previousClickChildContentDescription = "NULL", previousClickPackageName = "NULL";



    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        //TODO problem: the status of "right after click" (try getParent()?)
        //TODO new rootNode method
        final AccessibilityNodeInfo sourceNode = event.getSource();
        final String eventPackageName = String.valueOf(event.getPackageName());
        final int eventType = event.getEventType();


        AccessibilityNodeInfo rootNode = null;
        List<AccessibilityNodeInfo> preOrderTraverseSourceNode = null;
        List<AccessibilityNodeInfo> preOrderTraverseRootNode = null;
        List<AccessibilityNodeInfo> preOrderTraverseSibNode = null;


        //Type of accessibility events to handle in this function
        //return if the event is not among the accessibilityEventArrayToHandle
        if (!accessibilityEventSetToHandle.contains(eventType)) {
            return;
        }

        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            if (event.getPackageName() != null && event.getClassName() != null) {
                ComponentName componentName = new ComponentName(
                        event.getPackageName().toString(),
                        event.getClassName().toString()
                );

                try {
                    ActivityInfo activityInfo = getPackageManager().getActivityInfo(componentName, 0);
                    Log.i("CurrentActivity", activityInfo.packageName + " : " + activityInfo.name + " : " + AccessibilityEvent.eventTypeToString(event.getEventType()));
                    currentAppActivityName = activityInfo.name;
                    currentPackageName = activityInfo.packageName;
                } catch (PackageManager.NameNotFoundException e) {
                    //e.printStackTrace();
                    Log.e(this.getClass().getName(), "Failed to get the activity name for: " + componentName);
                }
            }
        }


        //check for the trigger, see if an app launch trigger should be triggered
        if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            if (sourceNode != null && sourceNode.getPackageName() != null && (!lastPackageName.contentEquals(sourceNode.getPackageName()))) {
                triggerHandler.checkForAppLaunchTrigger(eventPackageName);
                //lastPackageName used to avoid sync issue between threads
                lastPackageName = sourceNode.getPackageName().toString();

            }
        }

        //check communication status

        /* temporarily disable the communication controller for performance optimization
        if(sugiliteData.communicationController != null){
            if(!sugiliteData.communicationController.checkConnectionStatus())
                sugiliteData.communicationController.start();
        }
        */

        //add previous click information for building UI hierachy from vocabs
        if (BUILDING_VOCAB && (!trackingExcludedPackages.contains(eventPackageName)) && eventType == AccessibilityEvent.TYPE_VIEW_CLICKED && sourceNode != null) {
            if (sourceNode.getText() != null) {
                previousClickText = sourceNode.getText().toString();
            } else {
                previousClickText = "NULL";
            }
            if (sourceNode.getContentDescription() != null) {
                previousClickContentDescription = sourceNode.getContentDescription().toString();
            } else {
                previousClickContentDescription = "NULL";
            }
            if (sourceNode.getPackageName() != null) {
                previousClickPackageName = sourceNode.getPackageName().toString();
            } else {
                previousClickPackageName = "NULL";
            }
            if (preOrderTraverseSourceNode == null) {
                preOrderTraverseSourceNode = AutomatorUtil.preOrderTraverse(sourceNode);
            }

            Set<String> childTexts = new HashSet<>();
            Set<String> childContentDescriptions = new HashSet<>();
            for (AccessibilityNodeInfo childNode : preOrderTraverseSourceNode) {
                if (childNode.getText() != null) {
                    childTexts.add(childNode.getText().toString());
                }
                if (childNode.getContentDescription() != null) {
                    childContentDescriptions.add(childNode.getContentDescription().toString());
                }
            }
            // TODO: add sibling stuff here?
            if (childTexts.size() > 0) {
                previousClickChildText = childTexts.toString();
            }
            else {
                previousClickChildText = "NULL";
            }
            if (childContentDescriptions.size() > 0) {
                previousClickContentDescription = childContentDescriptions.toString();
            }
            else {
                previousClickContentDescription = "NULL";
            }
        }


        if (sugiliteData.getInstructionQueueSize() > 0 && !sharedPreferences.getBoolean("recording_in_process", true) && !exceptedPackages.contains(event.getPackageName()) && sugiliteData.errorHandler != null) {
            //if the script running is in progress, invoke the error handler
            sugiliteData.errorHandler.checkError(event, sugiliteData.peekInstructionQueue(), Calendar.getInstance().getTimeInMillis());
        }


        if (sharedPreferences.getBoolean("recording_in_process", false)) {
            //if recording is in progress

            //add package name to the relevant package set
            if (sugiliteData.getScriptHead() != null && eventPackageName != null && (!eventPackageName.equals("null")) && (!exceptedPackages.contains(eventPackageName))) {
                sugiliteData.getScriptHead().relevantPackages.add(eventPackageName);
            }

            //if the event is to be recorded, process it
            if (accessibilityEventSetToSend.contains(eventType) &&
                    (!exceptedPackages.contains(eventPackageName)) &&
                    (!recordingOverlayManager.isShowingOverlay())) {
                //ignore events if the recording overlay is on

                rootNode = getRootInActiveWindow();
                final AccessibilityNodeInfo rootNodeForRecording = rootNode;

                preOrderTraverseSourceNode = AutomatorUtil.preOrderTraverse(sourceNode);
                preOrderTraverseRootNode = AutomatorUtil.preOrderTraverse(rootNode);
                preOrderTraverseSibNode = AutomatorUtil.preOrderTraverseSiblings(sourceNode);

                if (preOrderTraverseSibNode != null) {
                    preOrderTraverseSibNode.removeIf(n -> n.getText() == null);
                }

                final List<AccessibilityNodeInfo> preOrderTraverseSourceNodeForRecording = preOrderTraverseSourceNode;
                final List<AccessibilityNodeInfo> preOrderTracerseRootNodeForRecording = preOrderTraverseRootNode;
                final List<AccessibilityNodeInfo> preOrderTraverseSibNodeForRecording = preOrderTraverseSibNode;


                //skip internal interactions and interactions on system ui
                availableAlternatives.addAll(getAlternativeLabels(sourceNode, rootNodeForRecording, preOrderTracerseRootNodeForRecording, exceptedPackages));
                availableAlternativeNodes.addAll(getAvailableAlternativeNodes(sourceNode, rootNodeForRecording, preOrderTracerseRootNodeForRecording, exceptedPackages));

                //refresh the elementsWithTextLabels list
                if (KEEP_ALL_TEXT_LABEL_LIST && eventPackageName != null && (!exceptedPackages.contains(eventPackageName))) {
                    List<AccessibilityNodeInfo> nodes = getAllNodesWithText(rootNodeForRecording, preOrderTracerseRootNodeForRecording);
                    boolean toRefresh = true;
                    //hack used to avoid getting items in the duck popup
                    if (nodes.size() > 10)
                        toRefresh = true;
                    else {
                        for (AccessibilityNodeInfo node : nodes) {
                            if (node.getText() != null && node.getText().toString().contains("Quit Sugilite")) {
                                toRefresh = false;
                                break;
                            }
                        }
                        if (nodes.size() <= 0)
                            toRefresh = false;
                    }
                    if (toRefresh) {
                        sugiliteData.elementsWithTextLabels.clear();
                        sugiliteData.elementsWithTextLabels.addAll(nodes);
                    }
                }

                //ignore automatically generated events from the recording overlay
                long currentTime = Calendar.getInstance().getTimeInMillis();
                boolean toSkipRecording = false;

                //used for skipping events programmatically generated from the recording overlay
                while (sugiliteData.NodeToIgnoreRecordingBoundsInScreenTimeStampQueue.size() > 0) {
                    long timeStamp = sugiliteData.NodeToIgnoreRecordingBoundsInScreenTimeStampQueue.peek().getValue();
                    if (currentTime - timeStamp > 3000) {
                        sugiliteData.NodeToIgnoreRecordingBoundsInScreenTimeStampQueue.remove();
                    } else {
                        String boundsInScreen = sugiliteData.NodeToIgnoreRecordingBoundsInScreenTimeStampQueue.poll().getKey();
                        Rect rectForEvent = new Rect();
                        if (sourceNode != null) {
                            sourceNode.getBoundsInScreen(rectForEvent);
                        }
                        if (boundsInScreen != null && boundsInScreen.equals(rectForEvent.flattenToString())) {
                            toSkipRecording = true;
                        }
                        break;
                    }
                }

                AccessibilityNodeInfo new_root = rootNode;

                //generate the uiSnapshot
                if (sourceNode != null) {
                    new_root = sourceNode;
                    while (new_root != null && new_root.getParent() != null) {
                        new_root = new_root.getParent();
                    }
                }
                final AccessibilityNodeInfo final_root = new_root;


                if (sharedPreferences.getBoolean("recording_in_process", false)) {
                    //==== the thread of handling recording
                    Runnable handleRecording = new Runnable() {
                        @Override
                        public void run() {
                            Log.i(TAG, "Entering handle recording thread");
                            UISnapshot uiSnapshot = new UISnapshot(windowManager.getDefaultDisplay(), final_root, true, sugiliteTextParentAnnotator, true, currentPackageName, currentAppActivityName);
                            System.out.printf("UI Snapshot Constructed for Recording!");
                            //temp hack for ViewGroup in Google Now Launcher
                            if (sourceNode != null && sourceNode.getClassName() != null && sourceNode.getPackageName() != null && sourceNode.getClassName().toString().contentEquals("android.view.ViewGroup") && homeScreenPackageNameSet.contains(sourceNode.getPackageName().toString())) {
                                /*do nothing (don't show popup for ViewGroup in home screen)*/
                            } else {
                                File screenshot = null;
                                if (sharedPreferences.getBoolean("root_enabled", false)) {
                                    //1. take screenshot
                                    try {
                                    /*
                                    System.out.println("taking screen shot");
                                    screenshot = screenshotManager.take(false);
                                    */
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }


                                //2. send the event to recording pop up dialog
                                SugiliteAvailableFeaturePack featurePack = generateFeaturePack(event, sourceNode, rootNodeForRecording, screenshot, availableAlternativeNodes, preOrderTraverseSourceNodeForRecording, preOrderTracerseRootNodeForRecording, preOrderTraverseSibNodeForRecording, new SerializableUISnapshot(uiSnapshot));

                                if (featurePack.isEditable) {
                                    //3. handle text entry
                                    if (eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) {
                                        //TODO: add TextChangedEventHandlerHere
                                        textChangedEventHandler.handle(featurePack, availableAlternatives);
                                    }
                                } else {
                                    System.out.println("flush from service");
                                    //flush the text changed event handler
                                    SugiliteData.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            textChangedEventHandler.flush();
                                        }
                                    });
                                    newDemonstrationHandler.handleEventInOldAccessiblityRecording(featurePack, availableAlternatives, uiSnapshot);
                                }
                            }


                            if (BUILDING_VOCAB) {
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

                            if (accessibilityEventSetToSend.contains(eventType) && (!exceptedPackages.contains(eventPackageName))) {

                                if (BUILDING_VOCAB) {
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
                                if (BUILDING_VOCAB)
                                    packageVocabs.clear();
                            }

                        }
                    };
                    if (!toSkipRecording) {
                        executor.execute(handleRecording);
                        //new Thread(handleRecording).run();
                    }
                }
                /*
                else if(sugiliteStudyHandler.isToRecordNextOperation()){
                    executor.execute(new Runnable() {
                        @Override
                        public void run() {
                            UISnapshot uiSnapshot = new UISnapshot(final_root, true, sugiliteTextAnnotator);
                            SugiliteAvailableFeaturePack featurePack = generateFeaturePack(event, sourceNode, rootNodeForRecording, null, availableAlternativeNodes, preOrderTraverseSourceNodeForRecording, preOrderTracerseRootNodeForRecording, preOrderTraverseSibNodeForRecording);
                            sugiliteStudyHandler.handleEventInOldAccessiblityRecording(featurePack, uiSnapshot);
                        }
                    });
                }
                */

            }

        }

        if (sharedPreferences.getBoolean("broadcasting_enabled", false)) {
            if (accessibilityEventSetToTrack.contains(eventType) && (!trackingExcludedPackages.contains(eventPackageName))) {
                sugiliteData.handleBroadcastingEvent(event);
            }
        }

        ////// broadcast the accessibility event received, for any app that may want to listen

        if (BROADCASTING_ACCESSIBILITY_EVENT) { // if broadcasting is enabled
            try {
                if (accessibilityEventSetToTrack.contains(eventType) && (!trackingExcludedPackages.contains(eventPackageName))) {
                    SugiliteEventBroadcastingActivity.BroadcastingEvent broadcastingEvent = new SugiliteEventBroadcastingActivity.BroadcastingEvent(event);
                    Gson gson = new Gson();

                    // what is teh event ? find the properties.
                    String desc = broadcastingEvent.contentDescription;
                    String pkg = broadcastingEvent.packageName;
                    String event_type = broadcastingEvent.eventType;

                    // filter on what kinds of events should to be broadcasted ...
                    //if(desc.contentEquals("Home") && event_type.contentEquals("TYPE_VIEW_CLICKED") && pkg.contentEquals("com.android.systemui"))
                    if (true) {
                        String messageToSend = gson.toJson(broadcastingEvent);

                        // send info to middleware
//                        sugiliteData.communicationController.sendMessage( Const.RESPONSE,
//                                Const.ACCESSIBILITY_EVENT, messageToSend);

                        // broadcast info for receivers of other apps
                        Intent intent = new Intent();
                        intent.setAction("edu.cmu.hcii.sugilite.SUGILITE_ACC_EVENT");
                        intent.putExtra("accEvent", messageToSend);
                        sendBroadcast(intent);
                    }
                }
            } catch (Exception e) {
                //empty catch block
            }
        }

        if (sharedPreferences.getBoolean("tracking_in_process", false)) {
            if (rootNode == null)
                rootNode = getRootInActiveWindow();

            if (preOrderTraverseSourceNode == null)
                preOrderTraverseSourceNode = AutomatorUtil.preOrderTraverse(sourceNode);

            if (preOrderTraverseRootNode == null)
                preOrderTraverseRootNode = AutomatorUtil.preOrderTraverse(rootNode);

            if (preOrderTraverseSibNode == null) {
                preOrderTraverseSibNode = AutomatorUtil.preOrderTraverseSiblings(sourceNode);
            }
            if (preOrderTraverseSibNode != null) {
                preOrderTraverseSibNode.removeIf(n -> n.getText() == null);
            }

            final AccessibilityNodeInfo rootNodeForTracking = rootNode;
            final List<AccessibilityNodeInfo> preOrderTraverseSourceNodeForTracking = preOrderTraverseSourceNode;
            final List<AccessibilityNodeInfo> preOrderTracerseRootNodeForTracking = preOrderTraverseRootNode;
            final List<AccessibilityNodeInfo> preOrderTraverseSibNodeForTracking = preOrderTraverseSibNode;


            Runnable handleTracking = new Runnable() {
                @Override
                public void run() {
                    //background tracking in progress
                    if (accessibilityEventSetToTrack.contains(eventType) && (!trackingExcludedPackages.contains(eventPackageName))) {
                        sugilteTrackingHandler.handle(event, sourceNode, generateFeaturePack(event, sourceNode, rootNodeForTracking, null, null, preOrderTraverseSourceNodeForTracking, preOrderTracerseRootNodeForTracking, preOrderTraverseSibNodeForTracking, null));
                    }

                    //add all seen clickable nodes to package vocab DB
                    if (BUILDING_VOCAB) {
                        for (SerializableNodeInfo node : getAvailableAlternativeNodes(sourceNode, rootNodeForTracking, preOrderTracerseRootNodeForTracking, exceptedPackages)) {
                            if (node.packageName != null && node.text != null)
                                packageVocabs.add(new AbstractMap.SimpleEntry<>(node.packageName, node.text));
                            if (node.packageName != null && node.childText != null && node.childText.size() > 0) {
                                for (String childText : node.childText)
                                    packageVocabs.add(new AbstractMap.SimpleEntry<>(node.packageName, childText));
                            }
                        }
                        //only read/write DB at every click -> to optimize performance
                        if (eventType == AccessibilityEvent.TYPE_VIEW_CLICKED) {
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
            };
            //new Thread(handleTracking).run();
            executor.execute(handleTracking);

        }


        SugiliteBlock currentBlock = sugiliteData.peekInstructionQueue();
        //refresh status icon
        if (currentBlock instanceof SugiliteOperationBlock) {
            if (rootNode == null) {
                rootNode = getRootInActiveWindow();
            }
            //TODO: fix the moving duck
            statusIconManager.refreshStatusIcon(rootNode, ((SugiliteOperationBlock) currentBlock).getElementMatchingFilter(), true);
        } else {
            statusIconManager.refreshStatusIcon(null, null, false);
        }


        //disable running automation from accessibility events

        /*
        if(sugiliteData.getInstructionQueueSize() > 0) {
            //System.out.println("attemping to run automation on the rootnode " + rootNode);
            //run automation
            if(rootNode == null)
                rootNode = getRootInActiveWindow();
            final AccessibilityNodeInfo finalRootNode = rootNode;
            if(automatorThreadExecutor == null) {
                automatorThreadExecutor = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        automator.handleLiveEvent(finalRootNode, getApplicationContext());
                        automatorThreadExecutor = null;
                    }
                });
                automatorThreadExecutor.start();
            }
        }
        */
    }


    /**
     * method used for updating the stored UI snapshot in the verbal instruction manager -- called by VerbalInstructionIconManager
     */
    public void updateUISnapshotInVerbalInstructionManager(Runnable runnableOnUpdateFinishesOnUIThread) {
        try {
            //for profiling purpose
            long startTime = System.currentTimeMillis();
            List<AccessibilityWindowInfo> windows = getWindows();
            verbalInstructionIconManager.rotateStatusIcon();

            if (windows.size() > 0) {
                Set<String> rootNodePackageNames = new HashSet<>();
                for (AccessibilityWindowInfo window : windows) {
                    if (window.getRoot() != null && window.getRoot().getPackageName() != null) {
                        rootNodePackageNames.add(window.getRoot().getPackageName().toString());
                    }
                }
                long finishGettingWindowsTime = System.currentTimeMillis();
                Log.v(TAG, String.format("Finished getting windows -- Takes %s ms.", String.valueOf(finishGettingWindowsTime - startTime)));

                //if(!rootNodePackageNames.contains("edu.cmu.hcii.sugilite")) {
                if (true) {
                    try {
                        uiSnapshotGenerationExecutor.execute(new Runnable() {
                            @Override
                            public void run() {
                                // currentAppActivityName might not be correct at this point in time?
                                try {
                                    UISnapshot uiSnapshot = new UISnapshot(windowManager.getDefaultDisplay(), windows, true, sugiliteTextParentAnnotator, false, currentPackageName, currentAppActivityName);
                                    if (uiSnapshot.getNodeAccessibilityNodeInfoMap().size() >= 5) {
                                        //filter out (mostly) empty ui snapshots
                                        verbalInstructionIconManager.setLatestUISnapshot(uiSnapshot);
                                        long stopTime = System.currentTimeMillis();
                                        Log.v(TAG, "Updated UI Snapshot! -- Takes " + String.valueOf(stopTime - startTime) + "ms");
                                        if (runnableOnUpdateFinishesOnUIThread != null) {
                                            SugiliteData.runOnUiThread(runnableOnUpdateFinishesOnUIThread);
                                        }
                                    }
                                } catch (Exception e) {
                                    //do nothing
                                    Log.e("SugiliteAccessibilityService", "Error when updating the UI snapshot");
                                    e.printStackTrace();
                                }
                            }
                        });
                    } catch (Exception e) {
                        //do nothing
                        e.printStackTrace();
                    }
                }
            }
            //for profiling purpose
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * method used for checking if an automation action can be performed in script execution -- called by VerbalInstructionIconManager
     */
    public void checkIfAutomationCanBePerformed() {
        //in check whether an automation action can be performed
        if (sugiliteData.getInstructionQueueSize() > 0) {
            //run automation
            List<AccessibilityWindowInfo> windows = getWindows();

            automatorThreadExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    List<AccessibilityNodeInfo> allNodes = AutomatorUtil.getAllNodesFromWindows(windows);
                    // getCurrentAppActivityName might not yield correct activity name at this moment
                    UISnapshot uiSnapshot = new UISnapshot(windowManager.getDefaultDisplay(), windows, true, sugiliteTextParentAnnotator, true, currentPackageName, currentAppActivityName);
                    automator.handleLiveEvent(uiSnapshot, getApplicationContext(), allNodes);
                }
            });
        }
    }

    public void updatePumiceOverlay(UISnapshot uiSnapshot) {
        if (sharedPreferences.getBoolean("recording_in_process", false)) {
            //if recording is in progress
            if (sugiliteData.currentPumiceValueDemonstrationType != null) {
                pumiceDemoVisualizationManager.refreshBasedOnSnapshotAndRelationType(uiSnapshot, sugiliteData.currentPumiceValueDemonstrationType);
            }
        } else {
            sugiliteData.currentPumiceValueDemonstrationType = null;
        }
    }

    public StatusIconManager getDuckIconManager() {
        return statusIconManager;
    }

    public VerbalInstructionIconManager getVerbalInstructionIconManager() {
        return verbalInstructionIconManager;
    }

    public SugiliteStudyHandler getSugiliteStudyHandler() {
        return sugiliteStudyHandler;
    }


    @Override
    public void onInterrupt() {
        System.out.print("Interrupt");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        PumiceDemonstrationUtil.showSugiliteToast("Sugilite Accessibility Service Stopped", Toast.LENGTH_SHORT);

        if (statusIconManager != null) {
            try {
                statusIconManager.removeStatusIcon();
            } catch (Exception e) {
                //failed to remove status icon
                e.printStackTrace();
            }
        }
        if (verbalInstructionIconManager != null) {
            try {
                verbalInstructionIconManager.removeStatusIcon();
            } catch (Exception e) {
                //failed to remove status icon
                e.printStackTrace();
            }
        }
        if (errorHandlingHandler != null) {
            errorHandlingHandler.removeMessages(0);
        }
        if (refreshIconHandler != null) {
            refreshIconHandler.removeMessages(0);
        }

    }
}



