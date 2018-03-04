package edu.cmu.hcii.sugilite.recording.newrecording.fullscreen_overlay;

import android.content.Context;
import android.graphics.PixelFormat;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;

import java.util.Collection;

import edu.cmu.hcii.sugilite.Const;
import edu.cmu.hcii.sugilite.Node;

/**
 * @author toby
 * @date 2/5/18
 * @time 3:49 PM
 */
public class SugiliteFullScreenOverlayFactory {
    Context context;

    public SugiliteFullScreenOverlayFactory(Context context) {
        this.context = context;
    }

    public View getFullScreenOverlay(DisplayMetrics metrics) {
        View overlay = getRectangleOverlay(context, metrics.widthPixels, metrics.heightPixels, Const.RECORDING_OVERLAY_COLOR);
        return overlay;
    }

    public View getOverlayWithHighlightedBoundingBoxes(DisplayMetrics metrics, Node clickedNode, Collection<Node> confusedNodes){
        View overlay = getRectangleOverlay(context, metrics.widthPixels, metrics.heightPixels, Const.RECORDING_OVERLAY_COLOR);
        //TODO: add the highlights
        return overlay;
    }

    private View getRectangleOverlay(Context context, int width, int height, int color) {
        View overlay = new View(context);
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        layoutParams.width = width;
        layoutParams.height = height;
        overlay.setLayoutParams(layoutParams);
        overlay.setBackgroundColor(color);
        return overlay;
    }

}
