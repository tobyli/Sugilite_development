package edu.cmu.hcii.sugilite.recording.newrecording.fullscreen_overlay;

import android.content.Context;
import android.graphics.PixelFormat;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;

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
        View overlay = getRectangleOverlay(context, metrics.widthPixels, metrics.heightPixels);
        //TODO: process the overlay
        return overlay;
    }

    private View getRectangleOverlay(Context context, int width, int height) {
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
        overlay.setBackgroundColor(0x20FFFF00);
        return overlay;
    }

}
