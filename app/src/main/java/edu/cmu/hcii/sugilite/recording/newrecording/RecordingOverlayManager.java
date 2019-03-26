package edu.cmu.hcii.sugilite.recording.newrecording;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.net.Uri;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.cmu.hcii.sugilite.model.Node;
import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.ontology.SugiliteEntity;
import edu.cmu.hcii.sugilite.ontology.UISnapshot;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.util.NavigationBarUtil;

import static edu.cmu.hcii.sugilite.Const.OVERLAY_TYPE;

/**
 * @author toby
 * @date 2/5/18
 * @time 1:19 PM
 */
@Deprecated
public class RecordingOverlayManager {
    private Context context;
    private WindowManager windowManager;
    private LayoutInflater layoutInflater;
    private NavigationBarUtil navigationBarUtil;
    private RecordingOverlayManager recordingOverlayManager;
    private SugiliteData sugiliteData;
    private SharedPreferences sharedPreferences;
    private List<View> overlays;

    //map between overlays and node
    Map<View, SugiliteEntity<Node>> overlayNodeMap;

    //whether overlays are currently shown
    private boolean showingOverlay = false;
    public RecordingOverlayManager(Context context, SugiliteData sugiliteData, SharedPreferences sharedPreferences){
        this.context = context;
        this.windowManager = (WindowManager) context.getSystemService(context.WINDOW_SERVICE);
        this.overlays = new ArrayList<>();
        this.overlayNodeMap = new HashMap<>();
        this.layoutInflater = (LayoutInflater) context.getSystemService( Context.LAYOUT_INFLATER_SERVICE);
        this.navigationBarUtil = new NavigationBarUtil();
        this.sugiliteData = sugiliteData;
        this.sharedPreferences = sharedPreferences;
        this.recordingOverlayManager = this;
    }


    public void addAllOverlaysFromSnapshot(UISnapshot snapshot){
        removeOverlays();
        for(SugiliteEntity<Node> nodeEntity : snapshot.getNodeSugiliteEntityMap().values()){
            Node node = nodeEntity.getEntityValue();
            if(node != null && node.getClickable()) {
                addOverlayFromNode(nodeEntity);
            }
        }
        // set the flag
        showingOverlay = true;
    }


    private void addOverlayFromNode(SugiliteEntity<Node> nodeEntity){
        Node node = nodeEntity.getEntityValue();

        //set the dimension for the overlay
        Rect boundsInScreen = Rect.unflattenFromString(node.getBoundsInScreen());
        View overlay = getRectangleOverlay(context, boundsInScreen.width(), boundsInScreen.height());
        WindowManager.LayoutParams iconParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                OVERLAY_TYPE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        DisplayMetrics displaymetrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(displaymetrics);
        int real_y = boundsInScreen.top;
        int statusBarHeight = navigationBarUtil.getStatusBarHeight(context);
        real_y -= statusBarHeight;

        iconParams.gravity = Gravity.TOP | Gravity.LEFT;
        iconParams.x = boundsInScreen.left;
        iconParams.y = real_y;
        iconParams.width = boundsInScreen.width();
        iconParams.height = boundsInScreen.height();

        addOverlayOnTouchListener(overlay, iconParams, displaymetrics, nodeEntity, windowManager, sugiliteData, sharedPreferences);

        //NEEDED TO BE CONFIGURED AT APPS->SETTINGS-DRAW OVER OTHER APPS on API>=23
        int currentApiVersion = android.os.Build.VERSION.SDK_INT;
        if(currentApiVersion >= 23){
            checkDrawOverlayPermission();
            if(Settings.canDrawOverlays(context)) {
                overlays.add(overlay);
                System.out.println("ADDING OVERLAY TO WINDOW MANAGER");
                windowManager.addView(overlay, iconParams);
            }
        }
        else {
            overlays.add(overlay);
            windowManager.addView(overlay, iconParams);
        }

        overlayNodeMap.put(overlay, nodeEntity);
    }


    private void addOverlayOnTouchListener(final View view, final WindowManager.LayoutParams mPaperParams, DisplayMetrics displayMetrics, SugiliteEntity<Node> nodeEntity,  final WindowManager windowManager, SugiliteData sugiliteData, SharedPreferences sharedPreferences) {
        Node node = nodeEntity.getEntityValue();
        String entityId = String.valueOf(nodeEntity.getEntityId());

        view.setOnTouchListener(new View.OnTouchListener() {
            GestureDetector gestureDetector = new GestureDetector(context, new SingleTapUp());
            @Override
            public boolean onTouch(final View v, MotionEvent event) {
                if (gestureDetector.onTouchEvent(event)) {
                    // gesture is clicking
                    Toast.makeText(context, "Clicked on " + entityId, Toast.LENGTH_SHORT).show();

                    RecordingOverlayOnClickPopup overlayChosenPopupDialog = new RecordingOverlayOnClickPopup(context, layoutInflater, node, sugiliteData, sharedPreferences);
                    overlayChosenPopupDialog.show();

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

    /**
     * remove all overlays from the window manager
     */
    public void removeOverlays(){
        try{
            for(View view : overlays) {
                if (view != null) {
                    windowManager.removeView(view);
                }
            }
            overlays.clear();
            overlayNodeMap.clear();
            showingOverlay = false;
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    private void checkDrawOverlayPermission() {
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

    private View getRectangleOverlay(Context context, int width, int height){
        View overlay = new View(context);
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                OVERLAY_TYPE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        layoutParams.width = width;
        layoutParams.height = height;
        overlay.setLayoutParams(layoutParams);
        overlay.setBackgroundColor(0x80FF0000);
        return overlay;
    }



}
