package edu.cmu.hcii.sugilite.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.view.Gravity;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityNodeInfo;

import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.automation.ServiceStatusManager;
import edu.cmu.hcii.sugilite.dao.SugiliteScreenshotManager;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptDao;

/**
 * @author toby
 * @date 7/6/16
 * @time 1:24 PM
 */
public class BoundingBoxManager {
    private HighlightBoundsView boundingBoxOverlay;
    private WindowManager windowManager;
    private Context context;
    private WindowManager.LayoutParams params;
    public BoundingBoxManager(Context context){
        this.context = context;
        windowManager = (WindowManager) context.getSystemService(context.WINDOW_SERVICE);

        boundingBoxOverlay = new HighlightBoundsView(context, null);
         params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.LEFT;
        params.x = 0;
        params.y = 0;
        boundingBoxOverlay.setStrokeWidth(10);
        try {
            windowManager.addView(boundingBoxOverlay, params);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    public void addBoundingBox(AccessibilityNodeInfo node){
        boundingBoxOverlay.clear();
        boundingBoxOverlay.add(node);
    }

}
