package edu.cmu.hcii.sugilite.pumice.visualization;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Pair;
import android.view.View;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import edu.cmu.hcii.sugilite.verbal_instruction_demo.util.NavigationBarUtil;

/**
 * @author toby
 * @date 2/25/19
 * @time 4:57 PM
 */
public class PumiceDemoVisualizationView extends View {
    private Set<Pair<Rect, Integer>> rects;
    private NavigationBarUtil navigationBarUtil;
    private Context context;


    public PumiceDemoVisualizationView(Context context) {
        super(context);
        this.context = context;
        this.rects = new HashSet<>();
        this.navigationBarUtil = new NavigationBarUtil();

    }

    public void clearRects(){
        rects.clear();
    }

    public void addRects(Collection<Rect> rects, int color) {
        for (Rect rect : rects) {
            addRect(rect, color);
        }
    }

    public void addRect(Rect rect, int color){
        int statusBarHeight = navigationBarUtil.getStatusBarHeight(context);
        Rect newRect = Rect.unflattenFromString(rect.flattenToString());
        newRect.set(newRect.left, newRect.top - statusBarHeight, newRect.right, newRect.bottom - statusBarHeight);
        rects.add(new Pair<>(newRect, color));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // TODO Auto-generated method stub
        super.onDraw(canvas);
        for(Pair<Rect, Integer> rectColorPair : rects) {
            Paint paint = new Paint();
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(rectColorPair.second);
            canvas.drawRect(rectColorPair.first, paint);
        }
    }
}

