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

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.cmu.hcii.sugilite.Const;
import edu.cmu.hcii.sugilite.model.Node;
import edu.cmu.hcii.sugilite.SugiliteAccessibilityService;
import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.model.block.util.SugiliteAvailableFeaturePack;
import edu.cmu.hcii.sugilite.model.block.SugiliteOperationBlock;
import edu.cmu.hcii.sugilite.model.operation.SugiliteOperation;
import edu.cmu.hcii.sugilite.model.operation.SugiliteUnaryOperation;
import edu.cmu.hcii.sugilite.ontology.OntologyQuery;
import edu.cmu.hcii.sugilite.ontology.SerializableOntologyQuery;
import edu.cmu.hcii.sugilite.ontology.SugiliteEntity;
import edu.cmu.hcii.sugilite.ontology.SugiliteRelation;
import edu.cmu.hcii.sugilite.ontology.UISnapshot;
import edu.cmu.hcii.sugilite.recording.ReadableDescriptionGenerator;
import edu.cmu.hcii.sugilite.recording.SugiliteScreenshotManager;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.study.SugiliteStudyHandler;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.util.NavigationBarUtil;

/**
 * @author toby
 * @date 2/5/18
 * @time 3:46 PM
 */

//this class creates a full screen overlay over the screen
public class FullScreenRecordingOverlayManager {
    //map between overlays and node
    Map<View, SugiliteEntity<Node>> overlayNodeMap;
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
        this.windowManager = (WindowManager) context.getSystemService(context.WINDOW_SERVICE);
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

    public boolean isShowingOverlay() {
        return showingOverlay;
    }

    private void initOverlay() {
        overlay = overlayFactory.getFullScreenOverlay(displayMetrics);
        setOverlayOnTouchListener(overlay, true);
    }

    public void enableOverlay() {
        removeOverlays();
        //TODO: enable overlay
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
        // set the flag
        showingOverlay = true;
    }

    private WindowManager.LayoutParams updateLayoutParams(int flag) {
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
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
    public void setPassThroughOnTouchListener(final View view) {
        view.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return false;
            }
        });
        try {
            windowManager.updateViewLayout(view, updateLayoutParams(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE));
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void setOverlayOnTouchListener(final View view, final boolean toConsumeEvent) {
        try {
            windowManager.updateViewLayout(view, updateLayoutParams(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE));
        } catch (Exception e) {
            e.printStackTrace();
        }

        view.setOnTouchListener(new View.OnTouchListener() {
            GestureDetector singleTapUpGestureDetector = new GestureDetector(context, new SingleTapUp());
            GestureDetector scrollGestureDetector = new GestureDetector(context, new Scroll());
            GestureDetector flingGestureDetector = new GestureDetector(context, new Fling());

            @Override
            public boolean onTouch(final View v, MotionEvent event) {
                if (singleTapUpGestureDetector.onTouchEvent(event)) {
                    //single tap up detected
                    System.out.println("Single Tap detected");
                    float rawX = event.getRawX();
                    float rawY = event.getRawY();
                    handleClick(rawX, rawY);
                } else if (scrollGestureDetector.onTouchEvent(event)) {
                    //scroll detected
                    System.out.println("Scroll detected");
                } else if (flingGestureDetector.onTouchEvent(event)) {
                    //fling detected
                    System.out.println("Fling detected");
                }
                return true;
            }

            class SingleTapUp extends GestureDetector.SimpleOnGestureListener {
                @Override
                public boolean onSingleTapUp(MotionEvent event) {
                    return true;
                }
            }

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

            class Fling extends GestureDetector.SimpleOnGestureListener {
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
                        }
                        catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                    return true;
                }
            }


        });
    }


    /**
     * remove all overlays from the window manager
     */
    public void removeOverlays() {
        try {
            if (overlay != null) {
                windowManager.removeView(overlay);

            }
            showingOverlay = false;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private synchronized UISnapshot getUiSnapshot() {
        return uiSnapshot;
    }

    public synchronized void setUiSnapshot(UISnapshot uiSnapshot) {
        this.uiSnapshot = uiSnapshot;
    }

    private void checkDrawOverlayPermission() {
        /** check if we already  have permission to draw over other apps */
        int currentApiVersion = android.os.Build.VERSION.SDK_INT;
        if (currentApiVersion >= 23) {
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

    private void handleClick(float x, float y) {
        List<SugiliteEntity<Node>> matchedNodeEntities = new ArrayList<>();
        if (getUiSnapshot() != null) {
            for (SugiliteEntity<Node> entity : getUiSnapshot().getNodeSugiliteEntityMap().values()) {
                Node node = entity.getEntityValue();
                if (!node.getClickable()) {
                    continue;
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
                    if (o1.getWindowZIndex() != null && o2.getWindowZIndex() != null && o1.getWindowZIndex() != o2.getWindowZIndex()) {
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
            System.out.println("Matched " + matchedNodeEntities.size() + " clickable objects!");
            Gson gson = new Gson();

            //choose the top matched node
            SugiliteEntity<Node> node = matchedNodeEntities.get(0);


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
                studyHandler.handleEvent(new SugiliteAvailableFeaturePack(node.getEntityValue(), getUiSnapshot()), getUiSnapshot(), path, fileName);
            } else {
                OverlayClickedDialog overlayClickedDialog = new OverlayClickedDialog(context, node, getUiSnapshot(), x, y, this, overlay, sugiliteData, layoutInflater, sharedPreferences, tts);
                overlayClickedDialog.show();
            }
        } else {
            Toast.makeText(context, "No node matched!", Toast.LENGTH_SHORT).show();
            System.out.println("No node matched!");
        }
    }

    public void clickWithRootPermission(float x, float y, Runnable uiThreadRunnable, Node alternativeNode) {
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
                        }
                        catch (Exception e){
                            e.printStackTrace();
                            recordingOverlayManager.addSugiliteOperationBlockBasedOnNode(alternativeNode);
                        }
                    }
                });
                clickThread.start();
                try {
                    clickThread.join();
                }
                catch (Exception e){
                    e.printStackTrace();
                }

                //run the callback - this eliminates the latency between changing passthroughbility
                uiThreadRunnable.run();
            }
        });
    }

    public void performFlingWithRootPermission(MotionEvent event1, MotionEvent event2, Runnable uiThreadRunnable) {
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
                        if(event1 != null && event2 != null) {
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

                            try {
                                m_Instrumentation.sendPointerSync(motionEvent1);
                                m_Instrumentation.sendPointerSync(motionEvent2);
                            }
                            catch (Exception e){
                                e.printStackTrace();
                            }
                            //sugiliteAccessibilityService.runOnUiThread(uiThreadRunnable);
                        }
                    }
                });
                flingThread.start();
                try {
                    flingThread.join();
                }
                catch (Exception e){
                    e.printStackTrace();
                }
                //run the callback
                uiThreadRunnable.run();
            }
        });
    }


    public void addSugiliteOperationBlockBasedOnNode(Node node) {
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
            SugiliteOperationBlock operationBlock = generateBlock(parentQuery, parentQuery.toString());


            //add the operation block to the instruction queue specified in sugiliteData
            sugiliteData.addInstruction(operationBlock);
        }


    }

    private SugiliteOperationBlock generateBlock(OntologyQuery query, String formula) {
        //generate the sugilite operation
        SugiliteUnaryOperation sugiliteOperation = new SugiliteUnaryOperation();
        //assume it's click for now -- need to expand to more types of operations
        sugiliteOperation.setOperationType(SugiliteOperation.CLICK);
        SerializableOntologyQuery serializedQuery = new SerializableOntologyQuery(query);

        SugiliteOperationBlock operationBlock = new SugiliteOperationBlock();
        operationBlock.setOperation(sugiliteOperation);
        operationBlock.setFeaturePack(null);
        operationBlock.setElementMatchingFilter(null);
        operationBlock.setScreenshot(null);
        operationBlock.setQuery(serializedQuery);
        operationBlock.setDescription(readableDescriptionGenerator.generateDescriptionForVerbalBlock(operationBlock, formula, "UTTERANCE"));
        return operationBlock;
    }


}
