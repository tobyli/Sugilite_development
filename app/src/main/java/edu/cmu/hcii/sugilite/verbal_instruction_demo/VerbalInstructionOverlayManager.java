package edu.cmu.hcii.sugilite.verbal_instruction_demo;

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
import java.util.List;

import edu.cmu.hcii.sugilite.model.Node;
import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.ontology.SerializableUISnapshot;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.server_comm.SugiliteVerbalInstructionHTTPQueryManager;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.server_comm.VerbalInstructionServerResults;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.util.NavigationBarUtil;

import static edu.cmu.hcii.sugilite.Const.OVERLAY_TYPE;

/**
 * @author toby
 * @date 12/10/17
 * @time 2:43 AM
 */
public class VerbalInstructionOverlayManager {
    private Context context;
    private WindowManager windowManager;
    private LayoutInflater layoutInflater;
    private NavigationBarUtil navigationBarUtil;
    private VerbalInstructionOverlayManager verbalInstructionOverlayManager;
    private SugiliteData sugiliteData;
    private SharedPreferences sharedPreferences;
    private SugiliteVerbalInstructionHTTPQueryManager httpQueryManager;
    private List<View> overlays;

    //whether overlays are currently shown
    private boolean showingOverlay = false;

    public VerbalInstructionOverlayManager(Context context, SugiliteData sugiliteData, SharedPreferences sharedPreferences){
        this.context = context;
        this.windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        this.overlays = new ArrayList<>();
        this.layoutInflater = (LayoutInflater) context.getSystemService( Context.LAYOUT_INFLATER_SERVICE);
        this.navigationBarUtil = new NavigationBarUtil();
        this.sugiliteData = sugiliteData;
        this.sharedPreferences = sharedPreferences;
        this.verbalInstructionOverlayManager = this;
    }

    public void addOverlay(Node node, String entityId, VerbalInstructionServerResults.VerbalInstructionResult correspondingResult, List<VerbalInstructionServerResults.VerbalInstructionResult> allResults, SerializableUISnapshot serializableUISnapshot, String utterance){
        Rect boundsInScreen = Rect.unflattenFromString(node.getBoundsInScreen());
        View overlay = getRectangleOverlay(context, 0x80FF0000, boundsInScreen.width(), boundsInScreen.height());
        System.out.println("Creating an overlay with width " + boundsInScreen.width() + " and height " + boundsInScreen.height() +
        " at " + boundsInScreen.left + ", " + boundsInScreen.top);
        WindowManager.LayoutParams iconParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                OVERLAY_TYPE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        DisplayMetrics displaymetrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(displaymetrics);
        int real_y = boundsInScreen.top;
        /*
        if(navigationBarUtil.isNavigationBarShow(windowManager, context)){
            //has navi bar
            int navibarHeight = navigationBarUtil.getNavigationBarHeight(windowManager, context);
            real_y -= navibarHeight;
            System.out.println("Detected a navi bar with height " + navibarHeight);
        }
        */
        int statusBarHeight = navigationBarUtil.getStatusBarHeight(context);
        real_y -= statusBarHeight;
        System.out.println("Detected a status with height " + statusBarHeight);

        iconParams.gravity = Gravity.TOP | Gravity.LEFT;
        iconParams.x = boundsInScreen.left;
        iconParams.y = real_y;
        iconParams.width = boundsInScreen.width();
        iconParams.height = boundsInScreen.height();

        addCrumpledPaperOnTouchListener(overlay, iconParams, displaymetrics, node, entityId, correspondingResult, allResults, serializableUISnapshot, utterance, windowManager, sugiliteData, sharedPreferences);

        //NEEDED TO BE CONFIGURED AT APPS->SETTINGS-DRAW OVER OTHER APPS on API>=23
        int currentApiVersion = android.os.Build.VERSION.SDK_INT;
        if(currentApiVersion >= 23){
            checkDrawOverlayPermission(context);
            overlays.add(overlay);
            windowManager.addView(overlay, iconParams);
        }
        else {
            overlays.add(overlay);
            windowManager.addView(overlay, iconParams);
        }

        showingOverlay = true;
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
            showingOverlay = false;
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void checkDrawOverlayPermission(Context context) {
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
    private void addCrumpledPaperOnTouchListener(final View view, final WindowManager.LayoutParams mPaperParams, DisplayMetrics displayMetrics, Node node, String entityId, VerbalInstructionServerResults.VerbalInstructionResult correspondingResult, List<VerbalInstructionServerResults.VerbalInstructionResult> allResults, SerializableUISnapshot serializableUISnapshot, String utterance, final WindowManager windowManager, SugiliteData sugiliteData, SharedPreferences sharedPreferences) {
        view.setOnTouchListener(new View.OnTouchListener() {

            GestureDetector gestureDetector = new GestureDetector(context, new SingleTapUp());

            @Override
            public boolean onTouch(final View v, MotionEvent event) {
                if (gestureDetector.onTouchEvent(event)) {
                    // gesture is clicking
                    Toast.makeText(context, "Clicked on " + entityId, Toast.LENGTH_SHORT).show();
                    OverlayChosenPopupDialog overlayChosenPopupDialog = new OverlayChosenPopupDialog(context, layoutInflater, verbalInstructionOverlayManager, node,  correspondingResult, allResults, serializableUISnapshot, utterance, sugiliteData, sharedPreferences);
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

    public static View getRectangleOverlay(Context context, int color, int width, int height){
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
        overlay.setBackgroundColor(color);
        return overlay;
    }

}
