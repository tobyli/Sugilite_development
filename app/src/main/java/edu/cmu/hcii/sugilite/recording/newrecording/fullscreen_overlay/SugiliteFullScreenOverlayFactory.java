package edu.cmu.hcii.sugilite.recording.newrecording.fullscreen_overlay;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Region;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import edu.cmu.hcii.sugilite.Const;
import edu.cmu.hcii.sugilite.model.Node;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.util.NavigationBarUtil;

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
        FullScreenWithHighlights overlay = new FullScreenWithHighlights(context, metrics, clickedNode, confusedNodes);
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

    public class FullScreenWithHighlights extends View{
        private Rect clickedNodeRectangle;
        private Paint clickedNodePaint;

        private List<Rect> confusedNodeRectangles;
        private Paint confusedNodePaint;

        private Rect backgroundRectangle;
        private Paint backgroundPaint;

        public FullScreenWithHighlights(Context context, DisplayMetrics metrics, Node clickedNode, Collection<Node> confusedNodes){
            super(context);
            confusedNodeRectangles = new ArrayList<>();
            NavigationBarUtil navigationBarUtil = new NavigationBarUtil();
            int statusBarHeight = navigationBarUtil.getStatusBarHeight(context);

            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_PHONE,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT);
            layoutParams.width = metrics.widthPixels;
            layoutParams.height = metrics.heightPixels;
            setLayoutParams(layoutParams);
            //setBackgroundColor(Const.PREVIEW_OVERLAY_COLOR);

            if(clickedNode.getBoundsInScreen() != null) {
                String[] coordinates = clickedNode.getBoundsInScreen().split(" ");
                if(coordinates.length == 4) {
                    int x1 = Integer.valueOf(coordinates[0]);
                    int y1 = Integer.valueOf(coordinates[1]);
                    int x2 = Integer.valueOf(coordinates[2]);
                    int y2 = Integer.valueOf(coordinates[3]);
                    y1 -= statusBarHeight;
                    y2 -= statusBarHeight;
                    clickedNodeRectangle = new Rect(x1, y1, x2, y2);
                }
            }

            for(Node confusedNode : confusedNodes){
                if(confusedNode.getBoundsInScreen() != null) {
                    String[] coordinates = confusedNode.getBoundsInScreen().split(" ");
                    if(coordinates.length == 4) {
                        int x1 = Integer.valueOf(coordinates[0]);
                        int y1 = Integer.valueOf(coordinates[1]);
                        int x2 = Integer.valueOf(coordinates[2]);
                        int y2 = Integer.valueOf(coordinates[3]);
                        y1 -= statusBarHeight;
                        y2 -= statusBarHeight;
                        confusedNodeRectangles.add(new Rect(x1, y1, x2, y2));
                    }
                }
            }

            backgroundRectangle = new Rect(0, 0, metrics.widthPixels, metrics.hashCode());
            backgroundPaint = new Paint();
            backgroundPaint.setColor(Const.PREVIEW_OVERLAY_COLOR);

            clickedNodePaint = new Paint();
            clickedNodePaint.setColor(0x60FF0000);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            canvas.drawRect(clickedNodeRectangle, clickedNodePaint);
            canvas.clipRect(clickedNodeRectangle, Region.Op.DIFFERENCE);
            for(Rect confusedNodeRectangle : confusedNodeRectangles){
                canvas.clipRect(confusedNodeRectangle, Region.Op.DIFFERENCE);
            }
            canvas.drawRect(backgroundRectangle, backgroundPaint);
        }
    }

}
