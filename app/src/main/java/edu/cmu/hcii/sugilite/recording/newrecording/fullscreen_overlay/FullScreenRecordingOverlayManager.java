package edu.cmu.hcii.sugilite.recording.newrecording.fullscreen_overlay;

import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.net.Uri;
import android.os.SystemClock;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.cmu.hcii.sugilite.Const;
import edu.cmu.hcii.sugilite.model.Node;
import edu.cmu.hcii.sugilite.accessibility_service.SugiliteAccessibilityService;
import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.model.block.util.SugiliteAvailableFeaturePack;
import edu.cmu.hcii.sugilite.model.block.SugiliteOperationBlock;
import edu.cmu.hcii.sugilite.model.operation.unary.SugiliteClickOperation;
import edu.cmu.hcii.sugilite.model.operation.unary.SugiliteLongClickOperation;
import edu.cmu.hcii.sugilite.model.operation.unary.SugiliteUnaryOperation;
import edu.cmu.hcii.sugilite.ontology.OntologyQuery;
import edu.cmu.hcii.sugilite.ontology.SerializableOntologyQuery;
import edu.cmu.hcii.sugilite.ontology.SugiliteEntity;
import edu.cmu.hcii.sugilite.ontology.SugiliteRelation;
import edu.cmu.hcii.sugilite.ontology.UISnapshot;
import edu.cmu.hcii.sugilite.recording.ReadableDescriptionGenerator;
import edu.cmu.hcii.sugilite.recording.SugiliteScreenshotManager;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.study.SugiliteStudyHandler;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.util.NavigationBarUtil;
import static edu.cmu.hcii.sugilite.Const.OVERLAY_TYPE;

import static edu.cmu.hcii.sugilite.Const.OVERLAY_TYPE;

/**
 * @author toby
 * @date 2/5/18
 * @time 3:46 PM
 */

//this class creates a full screen overlay over the screen
public class FullScreenRecordingOverlayManager {
    //map between overlays and node
    private Map<View, SugiliteEntity<Node>> overlayNodeMap;
    private Context context;
    private WindowManager windowManager;
    private LayoutInflater layoutInflater;
    private NavigationBarUtil navigationBarUtil;
    private FullScreenRecordingOverlayManager recordingOverlayManager;
    private SugiliteData sugiliteData;
    private SharedPreferences sharedPreferences;
    private SugiliteFullScreenOverlayFactory overlayFactory;
    private View overlay;
    private DisplayMetrics displayMetrics;
    private ReadableDescriptionGenerator readableDescriptionGenerator;
    private SugiliteAccessibilityService sugiliteAccessibilityService;
    private SugiliteScreenshotManager sugiliteScreenshotManager;
    private TextToSpeech tts;
    //whether overlays are currently shown
    private boolean showingOverlay = false;

    //latest UI snapshot
    private UISnapshot uiSnapshot = null;

    public FullScreenRecordingOverlayManager(Context context, SugiliteData sugiliteData, SharedPreferences sharedPreferences, SugiliteAccessibilityService sugiliteAccessibilityService, TextToSpeech tts) {
        this.context = context;
        this.sugiliteAccessibilityService = sugiliteAccessibilityService;
        this.windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        this.overlayNodeMap = new HashMap<>();
        this.layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.navigationBarUtil = new NavigationBarUtil();
        this.sugiliteData = sugiliteData;
        this.sharedPreferences = sharedPreferences;
        this.overlayFactory = new SugiliteFullScreenOverlayFactory(context);
        this.recordingOverlayManager = this;
        this.readableDescriptionGenerator = new ReadableDescriptionGenerator(context);
        this.sugiliteScreenshotManager = new SugiliteScreenshotManager(sharedPreferences, context);
        this.tts = tts;

        displayMetrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(displayMetrics);
        initOverlay();
    }

    private static List<SugiliteEntity<Node>> getMatchedNodesFromCoordinate(float x, float y, UISnapshot uiSnapshot, boolean getClickableNodeOnly, boolean getLongClickableNodeOnly) {
        List<SugiliteEntity<Node>> matchedNodeEntities = new ArrayList<>();
        if (uiSnapshot != null) {
            for (SugiliteEntity<Node> entity : uiSnapshot.getNodeSugiliteEntityMap().values()) {
                Node node = entity.getEntityValue();
                if (getClickableNodeOnly) {
                    if (!node.getClickable()) {
                        continue;
                    }
                }
                if (getLongClickableNodeOnly) {
                    if (!node.getLongClickable()) {
                        continue;
                    }
                }
                Rect boundingBox = Rect.unflattenFromString(node.getBoundsInScreen());
                if (boundingBox.contains((int) x, (int) y)) {
                    //contains
                    matchedNodeEntities.add(entity);
                }
            }
        }
        if (matchedNodeEntities.size() > 0) {
            matchedNodeEntities.sort(new Comparator<SugiliteEntity<Node>>() {
                //sort the list<node> based on Z-indexes, so the nodes displayed on top are in front
                @Override
                public int compare(SugiliteEntity<Node> e1, SugiliteEntity<Node> e2) {
                    Node o1 = e1.getEntityValue();
                    Node o2 = e2.getEntityValue();
                    if (o1.getWindowZIndex() != null && o2.getWindowZIndex() != null && (!o1.getWindowZIndex().equals(o2.getWindowZIndex()))) {
                        if (o1.getWindowZIndex() > o2.getWindowZIndex()) {
                            return -1;
                        } else {
                            return 1;
                        }
                    }
                    if (o1.getNodeZIndexSequence() != null && o2.getNodeZIndexSequence() != null) {
                        int length = o1.getNodeZIndexSequence().size() > o2.getNodeZIndexSequence().size() ? o1.getNodeZIndexSequence().size() : o2.getNodeZIndexSequence().size();
                        for (int i = 0; i < length; i++) {
                            if (i >= o1.getNodeZIndexSequence().size()) {
                                return 1;
                            }
                            if (i >= o2.getNodeZIndexSequence().size()) {
                                return -1;
                            }
                            if (o1.getNodeZIndexSequence().get(i) > o2.getNodeZIndexSequence().get(i)) {
                                return -1;
                            }
                            if (o1.getNodeZIndexSequence().get(i) < o2.getNodeZIndexSequence().get(i)) {
                                return 1;
                            }
                        }
                    }
                    return 0;
                }
            });
            //print matched nodes
            System.out.println("Matched " + matchedNodeEntities.size() + " objects!");

            //choose the top-layer matched node
            return matchedNodeEntities;
        } else {
            return null;
        }
    }

    public boolean isShowingOverlay() {
        return showingOverlay;
    }

    private void initOverlay() {
        overlay = overlayFactory.getFullScreenOverlay(displayMetrics);
    }

    public void enableOverlay() {
        removeOverlays();
        //enable overlay
        WindowManager.LayoutParams layoutParams = updateLayoutParams(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);

        //NEEDED TO BE CONFIGURED AT APPS->SETTINGS-DRAW OVER OTHER APPS on API>=23
        int currentApiVersion = android.os.Build.VERSION.SDK_INT;
        if (currentApiVersion >= 23) {
            checkDrawOverlayPermission();
            if (Settings.canDrawOverlays(context)) {
                System.out.println("ADDING OVERLAY TO WINDOW MANAGER");
                windowManager.addView(overlay, layoutParams);
            }
        } else {
            windowManager.addView(overlay, layoutParams);
        }
        // set the listener
        setOverlayOnTouchListener(overlay, true);

        // set the flag
        showingOverlay = true;
    }

    private WindowManager.LayoutParams updateLayoutParams(int flag) {
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                OVERLAY_TYPE,
                flag,
                PixelFormat.TRANSLUCENT);

        DisplayMetrics displaymetrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(displaymetrics);
        int real_y = 0;
        int statusBarHeight = navigationBarUtil.getStatusBarHeight(context);
        real_y -= statusBarHeight;

        layoutParams.gravity = Gravity.TOP | Gravity.LEFT;
        layoutParams.x = 0;
        layoutParams.y = real_y;
        layoutParams.width = displayMetrics.widthPixels;
        layoutParams.height = displayMetrics.heightPixels;
        return layoutParams;
    }

    /**
     * set view to be not touchble (so it will pass through touch events)
     *
     * @param view
     */
    private void setPassThroughOnTouchListener(final View view) {
        view.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return false;
            }
        });
        view.setBackgroundColor(Const.RECORDING_OVERLAY_COLOR_STOP);
        view.invalidate();
        try {
            windowManager.updateViewLayout(view, updateLayoutParams(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE));
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void setOverlayOnTouchListener(final View view, final boolean toConsumeEvent) {
        try {
            windowManager.updateViewLayout(view, updateLayoutParams(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE));
            view.setBackgroundColor(Const.RECORDING_OVERLAY_COLOR);
            view.invalidate();
        } catch (Exception e) {
            e.printStackTrace();
        }

        view.setOnTouchListener(new View.OnTouchListener() {
            GestureDetector myGestureDetector = new GestureDetector(context, new MyGestureDetector());
            ;

            @Override
            public boolean onTouch(final View v, MotionEvent event) {
                return myGestureDetector.onTouchEvent(event);
            }

            class MyGestureDetector extends GestureDetector.SimpleOnGestureListener {
                @Override
                public boolean onSingleTapConfirmed(MotionEvent event) {
                    //single tap up detected
                    System.out.println("Single tap detected");
                    float rawX = event.getRawX();
                    float rawY = event.getRawY();
                    handleClick(rawX, rawY);
                    return true;
                }

                @Override
                public void onLongPress(MotionEvent event) {
                    System.out.println("Context click detected");
                    float rawX = event.getRawX();
                    float rawY = event.getRawY();
                    handleContextClick(rawX, rawY, tts);
                    return;
                }

                @Override
                public boolean onContextClick(MotionEvent e) {
                    System.out.println("Context click detected");
                    return super.onContextClick(e);
                }

                @Override
                public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                    if (Const.ROOT_ENABLED) {
                        recordingOverlayManager.setPassThroughOnTouchListener(overlay);
                        try {
                            recordingOverlayManager.performFlingWithRootPermission(e1, e2, new Runnable() {
                                @Override
                                public void run() {
                                    //allow the overlay to get touch event after finishing the simulated gesture
                                    recordingOverlayManager.setOverlayOnTouchListener(overlay, true);
                                }
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    return true;
                }
            }

            /*
            class Scroll extends GestureDetector.SimpleOnGestureListener {
                @Override
                public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                    if (Const.ROOT_ENABLED) {
                        recordingOverlayManager.setPassThroughOnTouchListener(overlay);
                        try {
                            recordingOverlayManager.performFlingWithRootPermission(e1, e2, new Runnable() {
                                @Override
                                public void run() {
                                    //allow the overlay to get touch event after finishing the simulated gesture
                                    recordingOverlayManager.setOverlayOnTouchListener(overlay, true);
                                }
                            });
                        }
                        catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                    return true;
                }
            }
            */

        });
    }

    /**
     * remove all overlays from the window manager
     */
    public void removeOverlays() {
        try {
            if (overlay != null && overlay.getWindowToken() != null) {
                windowManager.removeView(overlay);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            showingOverlay = false;
        }
    }

    private UISnapshot getUiSnapshotAndAnnotateStringEntitiesIfNeeded() {
        synchronized (this) {
            //annotate string entities in the ui snapshot if needed
            uiSnapshot.annotateStringEntitiesIfNeeded();
            return uiSnapshot;
        }
    }

    public void setUiSnapshot(UISnapshot uiSnapshot) {
        synchronized (this) {
            this.uiSnapshot = uiSnapshot;
        }
    }

    private void checkDrawOverlayPermission() {
        /* check if we already  have permission to draw over other apps */
        int currentApiVersion = android.os.Build.VERSION.SDK_INT;
        if (currentApiVersion >= 23) {
            if (!Settings.canDrawOverlays(context)) {
                /* if not construct intent to request permission */
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + context.getPackageName()));
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                /* request permission via start activity for result */
                context.startActivity(intent);

            }
        }
    }

    /**
     * handle when the overlay detects a click at (x, y) -> should determine the UI object to match this click event to, and create an OverlayClickedDialog
     *
     * @param x
     * @param y
     */
    private void handleClick(float x, float y) {
        SugiliteEntity<Node> node = null;
        UISnapshot uiSnapshot = getUiSnapshotAndAnnotateStringEntitiesIfNeeded();
        if (uiSnapshot != null) {
            List<SugiliteEntity<Node>> matchedNodeEntities = getMatchedNodesFromCoordinate(x, y, uiSnapshot, true, false);
            if (matchedNodeEntities != null) {
                node = matchedNodeEntities.get(0);
            }
        }
        if (node != null) {
            if (sugiliteAccessibilityService.getSugiliteStudyHandler().isToRecordNextOperation()) {
                //save a study packet
                Date time = Calendar.getInstance().getTime();
                String timeString = Const.dateFormat.format(time);
                String path = "/sdcard/Download/sugilite_study_packets";
                String fileName = "packet_" + timeString;
                try {
                    sugiliteScreenshotManager.take(true, path, fileName + ".png");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                SugiliteStudyHandler studyHandler = sugiliteAccessibilityService.getSugiliteStudyHandler();
                studyHandler.handleEvent(new SugiliteAvailableFeaturePack(node.getEntityValue(), uiSnapshot), uiSnapshot, path, fileName);
            } else {
                OverlayClickedDialog overlayClickedDialog = new OverlayClickedDialog(context, node, uiSnapshot, x, y, this, overlay, sugiliteData, layoutInflater, sharedPreferences, tts);
                overlayClickedDialog.show();
            }
        } else {
            Toast.makeText(context, "No node matched!", Toast.LENGTH_SHORT).show();
            System.out.println("No node matched!");
        }
    }

    private void handleContextClick(float x, float y, TextToSpeech tts) {
        UISnapshot uiSnapshot = getUiSnapshotAndAnnotateStringEntitiesIfNeeded();
        if (uiSnapshot != null) {
            SugiliteEntity<Node> topLongClickableNode = null;
            List<SugiliteEntity<Node>> matchedLongClickableNodeEntities = getMatchedNodesFromCoordinate(x, y, uiSnapshot, false, true);
            if (matchedLongClickableNodeEntities != null) {
                topLongClickableNode = matchedLongClickableNodeEntities.get(0);
            }
            List<SugiliteEntity<Node>> matchedAllNodeEntities = getMatchedNodesFromCoordinate(x, y, uiSnapshot, false, false);
            if (matchedAllNodeEntities != null) {
                RecordingOverlayContextClickDialog recordingOverlayContextClickDialog = new RecordingOverlayContextClickDialog(context, this, topLongClickableNode, matchedAllNodeEntities, uiSnapshot, sugiliteData.valueDemonstrationVariableName, sugiliteData, tts, x, y);
                recordingOverlayContextClickDialog.show();
            } else {
                Toast.makeText(context, "No node matched!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void clickWithRootPermission(float x, float y, Runnable uiThreadRunnable, Node alternativeNode, boolean isLongClick) {
        Instrumentation m_Instrumentation = new Instrumentation();
        overlay.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            /**
             * the simulated touch event is send once the layout change has happend (when the view is set to not touchable)
             * @param v
             * @param left
             * @param top
             * @param right
             * @param bottom
             * @param oldLeft
             * @param oldTop
             * @param oldRight
             * @param oldBottom
             */
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                overlay.removeOnLayoutChangeListener(this);
                System.out.println("layout changed");
                Thread clickThread = new Thread(new Runnable() {
                    @Override
                    public synchronized void run() {
                        try {
                            m_Instrumentation.sendPointerSync(MotionEvent.obtain(
                                    SystemClock.uptimeMillis(),
                                    SystemClock.uptimeMillis(),
                                    MotionEvent.ACTION_DOWN, x, y, 0));
                            m_Instrumentation.sendPointerSync(MotionEvent.obtain(
                                    SystemClock.uptimeMillis(),
                                    SystemClock.uptimeMillis(),
                                    MotionEvent.ACTION_UP, x, y, 0));
                            //sugiliteAccessibilityService.runOnUiThread(uiThreadRunnable);
                        } catch (Exception e) {
                            e.printStackTrace();
                            recordingOverlayManager.addSugiliteOperationBlockBasedOnNode(alternativeNode, isLongClick);
                        }
                    }
                });
                clickThread.start();
                try {
                    clickThread.join();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                //run the callback - this eliminates the latency between changing passthroughbility
                uiThreadRunnable.run();
            }
        });
    }

    private void performFlingWithRootPermission(MotionEvent event1, MotionEvent event2, Runnable uiThreadRunnable) {
        Instrumentation m_Instrumentation = new Instrumentation();
        overlay.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                overlay.removeOnLayoutChangeListener(this);
                System.out.println("layout changed");
                Thread flingThread = new Thread(new Runnable() {
                    @Override
                    public synchronized void run() {
                        //obtain event1
                        if (event1 != null && event2 != null) {
                            /*
                            MotionEvent.PointerProperties[] pointerProperties1 = new MotionEvent.PointerProperties[event1.getPointerCount()];
                            MotionEvent.PointerCoords[] pointerCoordses1 = new MotionEvent.PointerCoords[event1.getPointerCount()];
                            for (int i = 0; i < event1.getPointerCount(); i++) {
                                pointerProperties1[i] = new MotionEvent.PointerProperties();
                                pointerCoordses1[i] = new MotionEvent.PointerCoords();
                                event1.getPointerProperties(i, pointerProperties1[i]);
                                event1.getPointerCoords(i, pointerCoordses1[i]);
                            }
                            MotionEvent motionEvent1 = MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(),
                                    event1.getAction(), event1.getPointerCount(), pointerProperties1, pointerCoordses1, event1.getMetaState(),
                                    event1.getButtonState(), event1.getXPrecision(), event1.getYPrecision(), event1.getDeviceId(), event1.getEdgeFlags(), event1.getSource(), event1.getFlags());
                            System.out.println("EVENT1: " + motionEvent1.toString());

                            //obtain event2
                            MotionEvent.PointerProperties[] pointerProperties2 = new MotionEvent.PointerProperties[event2.getPointerCount()];
                            MotionEvent.PointerCoords[] pointerCoordses2 = new MotionEvent.PointerCoords[event2.getPointerCount()];
                            for (int i = 0; i < event2.getPointerCount(); i++) {
                                pointerProperties2[i] = new MotionEvent.PointerProperties();
                                pointerCoordses2[i] = new MotionEvent.PointerCoords();
                                event2.getPointerProperties(i, pointerProperties2[i]);
                                event2.getPointerCoords(i, pointerCoordses2[i]);
                            }
                            MotionEvent motionEvent2 = MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(),
                                    event2.getAction(), event2.getPointerCount(), pointerProperties2, pointerCoordses2, event2.getMetaState(),
                                    event2.getButtonState(), event2.getXPrecision(), event2.getYPrecision(), event2.getDeviceId(), event2.getEdgeFlags(), event2.getSource(), event2.getFlags());
                            System.out.println("EVENT2: " + motionEvent2.toString());
                            */

                            int x1 = (int) event1.getRawX();
                            int y1 = (int) event1.getRawY();
                            int x2 = (int) event2.getRawX();
                            int y2 = (int) event2.getRawY();
                            long duration = event2.getEventTime() - event1.getEventTime();
                            String command = "input swipe " + x1 + " " + y1 + " " + x2 + " " + y2 + " " + duration;


                            try {
                                /*
                                m_Instrumentation.sendPointerSync(motionEvent1);
                                m_Instrumentation.sendPointerSync(motionEvent2);
                                */
                                Process sh = Runtime.getRuntime().exec("su", null, null);
                                OutputStream os = sh.getOutputStream();
                                os.write((command).getBytes("ASCII"));
                                os.flush();
                                os.close();
                                System.out.println("SWIPING: " + command);
                                Thread.sleep(400 + duration);

                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            //sugiliteAccessibilityService.runOnUiThread(uiThreadRunnable);

                        }
                    }
                });
                flingThread.start();
                try {
                    flingThread.join();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                //run the callback
                uiThreadRunnable.run();
            }
        });
    }

    public View getOverlay() {
        return overlay;
    }

    void addSugiliteOperationBlockBasedOnNode(Node node, boolean isLongClick) {
        if (node.getBoundsInScreen() != null) {
            OntologyQuery parentQuery = new OntologyQuery(OntologyQuery.relationType.AND);
            OntologyQuery screenBoundsQuery = new OntologyQuery(OntologyQuery.relationType.nullR);
            screenBoundsQuery.addObject(new SugiliteEntity<>(-1, String.class, node.getBoundsInScreen()));
            screenBoundsQuery.setQueryFunction(SugiliteRelation.HAS_SCREEN_LOCATION);
            parentQuery.addSubQuery(screenBoundsQuery);

            if (node.getClassName() != null) {
                OntologyQuery classQuery = new OntologyQuery(OntologyQuery.relationType.nullR);
                classQuery.addObject(new SugiliteEntity<>(-1, String.class, node.getClassName()));
                classQuery.setQueryFunction(SugiliteRelation.HAS_CLASS_NAME);
                parentQuery.addSubQuery(classQuery);
            }

            if (node.getPackageName() != null) {
                OntologyQuery packageQuery = new OntologyQuery(OntologyQuery.relationType.nullR);
                packageQuery.addObject(new SugiliteEntity<>(-1, String.class, node.getPackageName()));
                packageQuery.setQueryFunction(SugiliteRelation.HAS_PACKAGE_NAME);
                parentQuery.addSubQuery(packageQuery);
            }
            SugiliteOperationBlock operationBlock = generateBlock(parentQuery, parentQuery.toString(), isLongClick);


            //add the operation block to the instruction queue specified in sugiliteData
            sugiliteData.addInstruction(operationBlock);
        }


    }

    private SugiliteOperationBlock generateBlock(OntologyQuery query, String formula, boolean isLongClick) {
        //generate the sugilite operation
        SugiliteUnaryOperation sugiliteOperation = isLongClick ? new SugiliteLongClickOperation() : new SugiliteClickOperation();
        //assume it's click for now -- need to expand to more types of operations
        SerializableOntologyQuery serializedQuery = new SerializableOntologyQuery(query);

        SugiliteOperationBlock operationBlock = new SugiliteOperationBlock();
        operationBlock.setOperation(sugiliteOperation);
        operationBlock.setFeaturePack(null);
        operationBlock.setElementMatchingFilter(null);
        operationBlock.setScreenshot(null);
        if (sugiliteOperation instanceof SugiliteClickOperation) {
            ((SugiliteClickOperation) sugiliteOperation).setQuery(serializedQuery);
        }
        if (sugiliteOperation instanceof SugiliteLongClickOperation) {
            ((SugiliteLongClickOperation) sugiliteOperation).setQuery(serializedQuery);
        }
        operationBlock.setDescription(readableDescriptionGenerator.generateDescriptionForVerbalBlock(operationBlock, formula, "UTTERANCE"));
        return operationBlock;
    }

    void clickNode(Node node, float x, float y, View overlay, boolean isLongClick) {
        if (Const.ROOT_ENABLED) {
            //on a rooted phone, should directly simulate the click itself
            recordingOverlayManager.setPassThroughOnTouchListener(overlay);
            try {
                recordingOverlayManager.clickWithRootPermission(x, y, new Runnable() {
                    @Override
                    public void run() {
                        //allow the overlay to get touch event after finishing the simulated click
                        recordingOverlayManager.setOverlayOnTouchListener(overlay, true);
                    }
                }, node, isLongClick);
            } catch (Exception e) {
                //do nothing
            }
        } else {
            recordingOverlayManager.addSugiliteOperationBlockBasedOnNode(node, isLongClick);
        }
    }


}
