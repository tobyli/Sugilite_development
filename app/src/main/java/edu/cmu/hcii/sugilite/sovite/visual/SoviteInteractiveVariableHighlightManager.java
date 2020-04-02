package edu.cmu.hcii.sugilite.sovite.visual;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.util.Pair;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;


import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import edu.cmu.hcii.sugilite.model.Node;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;
import edu.cmu.hcii.sugilite.model.operation.binary.SugiliteGetProcedureOperation;

import edu.cmu.hcii.sugilite.model.variable.VariableValue;
import edu.cmu.hcii.sugilite.pumice.dialog.PumiceDialogManager;

import static android.view.View.GONE;
import static edu.cmu.hcii.sugilite.sovite.visual.SoviteScriptVisualThumbnailManager.SCREENSHOT_SCALE;

/**
 * @author toby
 * @date 4/1/20
 * @time 10:48 PM
 */
public class SoviteInteractiveVariableHighlightManager {

    private Context context;
    public SoviteInteractiveVariableHighlightManager(Context context) {
        this.context = context;
    }

    public View generateInteractiveViewForVariableValueAndScreenshotDrawable(VariableValue variableValue, Drawable screenshot, SugiliteStartingBlock subScript, SugiliteGetProcedureOperation getProcedureOperation, SoviteVariableUpdateCallback soviteVariableUpdateCallback, PumiceDialogManager pumiceDialogManager) {
        if (variableValue.getVariableValueContext() == null || variableValue.getVariableValueContext().getTargetNode() == null) {
            return null;
        }
        RelativeLayout parentLayout = new RelativeLayout(context);
        parentLayout.setGravity(Gravity.TOP | Gravity.LEFT);

        int strokeWidth = 20;
        int halfStrokeWidth = strokeWidth / 2;
        Node node = variableValue.getVariableValueContext().getTargetNode();
        Rect screenBounding = Rect.unflattenFromString(node.getBoundsInScreen());
        screenBounding.set(getScaledCoordinate(screenBounding.left), getScaledCoordinate(screenBounding.top), getScaledCoordinate(screenBounding.right), getScaledCoordinate(screenBounding.bottom));

        ImageView screenshotImageView = new ImageView(context);
        Drawable scaleDrawable = getScaledDrawable(screenshot, getScaledCoordinate(screenshot.getIntrinsicWidth()), getScaledCoordinate(screenshot.getIntrinsicHeight()));
        screenshotImageView.setImageDrawable(scaleDrawable);
        RelativeLayout.LayoutParams imageViewParams = new RelativeLayout.LayoutParams(scaleDrawable.getIntrinsicWidth(), scaleDrawable.getIntrinsicHeight());
        imageViewParams.setMargins(0, 0, 0, 0);
        imageViewParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
        imageViewParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
        imageViewParams.alignWithParent = true;
        screenshotImageView.setForegroundGravity(Gravity.TOP | Gravity.LEFT);
        parentLayout.addView(screenshotImageView, imageViewParams);


        RelativeLayout highlightLayout = new RelativeLayout(context);
        highlightLayout.setGravity(Gravity.TOP | Gravity.LEFT);
        RelativeLayout.LayoutParams highlightLayoutViewParams = new RelativeLayout.LayoutParams(screenBounding.width(), screenBounding.height());
        highlightLayoutViewParams.setMargins(screenBounding.left, screenBounding.top, 0, 0);
        highlightLayoutViewParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
        highlightLayoutViewParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
        parentLayout.addView(highlightLayout, highlightLayoutViewParams);

        //add stroke
        RectShape strokeRectShape = new RectShape();
        ShapeDrawable strokeShapeDrawable = new ShapeDrawable(strokeRectShape);
        strokeShapeDrawable.getPaint().setColor(0x8CFF0000);
        strokeShapeDrawable.getPaint().setStyle(Paint.Style.STROKE);
        strokeShapeDrawable.setIntrinsicHeight(screenBounding.height() - strokeWidth);
        strokeShapeDrawable.setIntrinsicWidth(screenBounding.width() - strokeWidth);
        strokeShapeDrawable.getPaint().setStrokeWidth(strokeWidth);
        ImageView strokeshapeView = new ImageView(context);
        strokeshapeView.setImageDrawable(strokeShapeDrawable);
        RelativeLayout.LayoutParams strokeShapeViewParams = new RelativeLayout.LayoutParams(strokeShapeDrawable.getIntrinsicWidth(), strokeShapeDrawable.getIntrinsicHeight());
        strokeShapeViewParams.setMargins(halfStrokeWidth, halfStrokeWidth, 0, 0);
        strokeShapeViewParams.alignWithParent = true;
        strokeShapeViewParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
        strokeShapeViewParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
        highlightLayout.addView(strokeshapeView, strokeShapeViewParams);


        //add highlight
        RectShape fillRectShape = new RectShape();
        ShapeDrawable fillShapeDrawable = new ShapeDrawable(fillRectShape);
        fillShapeDrawable.getPaint().setColor(0x8CFFFF00);
        fillShapeDrawable.getPaint().setStyle(Paint.Style.FILL);
        fillShapeDrawable.setIntrinsicHeight(screenBounding.height() - 2 * strokeWidth);
        fillShapeDrawable.setIntrinsicWidth(screenBounding.width() - 2 * strokeWidth);
        ImageView fillShapeView = new ImageView(context);
        fillShapeView.setImageDrawable(fillShapeDrawable);
        RelativeLayout.LayoutParams fillShapeViewParams = new RelativeLayout.LayoutParams(fillShapeDrawable.getIntrinsicWidth(), fillShapeDrawable.getIntrinsicHeight());
        fillShapeViewParams.setMargins(strokeWidth, strokeWidth, 0, 0);
        fillShapeViewParams.alignWithParent = true;
        fillShapeViewParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
        fillShapeViewParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
        highlightLayout.addView(fillShapeView, fillShapeViewParams);

        //add onClick listener for the highlight
        addOnClickListenerForVariableHighlight(highlightLayout, variableValue, subScript, getProcedureOperation, soviteVariableUpdateCallback, pumiceDialogManager, parentLayout);

        //add onDrag listener for the highlight
        addOnDragListenerForVariableHighlight(highlightLayout, parentLayout, variableValue, subScript, getProcedureOperation, soviteVariableUpdateCallback, pumiceDialogManager, parentLayout);


        return parentLayout;
    }

    private void addOnClickListenerForVariableHighlight (View highlightView, VariableValue variableValue, SugiliteStartingBlock subScript, SugiliteGetProcedureOperation getProcedureOperation, SoviteVariableUpdateCallback soviteVariableUpdateCallback, @Nullable PumiceDialogManager pumiceDialogManager, @Nullable View originalScreenshotView) {
        highlightView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String variableName = variableValue.getVariableName();
                String currentVariableValueString = variableValue.getVariableValue().toString();
                Set<VariableValue> alternativeValues = subScript.variableNameAlternativeValueMap.get(variableName);
                List<String> alternativeValueStrings = new ArrayList<>();
                if (alternativeValues != null) {
                    alternativeValues.forEach(alternativeVariableValue -> alternativeValueStrings.add(alternativeVariableValue.getVariableValue().toString()));
                }

                SoviteVisualVariableOnClickDialog soviteVisualVariableOnClickDialog = new SoviteVisualVariableOnClickDialog(context, variableValue, subScript, getProcedureOperation, soviteVariableUpdateCallback, originalScreenshotView, false);
                soviteVisualVariableOnClickDialog.show();
                if (pumiceDialogManager != null) {
                    pumiceDialogManager.stopTalking();
                    pumiceDialogManager.stopListening();
                }
            }
        });
    }

    private Drawable getScaledDrawable(Drawable drawable, int width, int height) {
        //scale the drawable so it fits into the dialog
        // Read your drawable from somewhere
        Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
        // Scale it to 50 x 50
        Drawable d = new BitmapDrawable(context.getResources(), Bitmap.createScaledBitmap(bitmap, width, height, true));
        // Set your new, scaled drawable "d"
        return d;
    }

    private int getScaledCoordinate (int coordinate) {
        return (int) (coordinate/SCREENSHOT_SCALE);
    }


    private void addOnDragListenerForVariableHighlight (View highlightView, RelativeLayout rootView, VariableValue variableValue, SugiliteStartingBlock subScript, SugiliteGetProcedureOperation getProcedureOperation, SoviteVariableUpdateCallback soviteVariableUpdateCallback, @Nullable PumiceDialogManager pumiceDialogManager, @Nullable View originalScreenshotView) {
        ScrollView scrollView = null;
        if (pumiceDialogManager != null) {
            scrollView = pumiceDialogManager.getScrollView();
        }

        highlightView.setOnTouchListener(new VariableHighlightOnTouchListener(rootView, scrollView, variableValue, subScript, getProcedureOperation, soviteVariableUpdateCallback, pumiceDialogManager, originalScreenshotView));
    }

    private class VariableHighlightOnTouchListener implements View.OnTouchListener {
        private int xDelta, yDelta;
        private View rootView;
        private ScrollView scrollView;
        private GestureDetector gestureDetector;
        private SugiliteGetProcedureOperation getProcedureOperation;
        private SoviteVariableUpdateCallback soviteVariableUpdateCallback;
        private PumiceDialogManager pumiceDialogManager;
        private View originalScreenshotView;

        private VariableValue currentSelectedVariableValue;
        private SugiliteStartingBlock subScript;


        private VariableHighlightOnTouchListener(RelativeLayout rootView, ScrollView scrollView, VariableValue currentSelectedVariableValue, SugiliteStartingBlock subScript, SugiliteGetProcedureOperation getProcedureOperation, SoviteVariableUpdateCallback soviteVariableUpdateCallback, @Nullable PumiceDialogManager pumiceDialogManager, @Nullable View originalScreenshotView) {
            this.rootView = rootView;
            this.scrollView = scrollView;
            this.currentSelectedVariableValue = currentSelectedVariableValue;
            this.subScript = subScript;
            this.getProcedureOperation = getProcedureOperation;
            this.soviteVariableUpdateCallback = soviteVariableUpdateCallback;
            this.pumiceDialogManager = pumiceDialogManager;
            this.originalScreenshotView = originalScreenshotView;

            gestureDetector = new GestureDetector(context, new SingleTapConfirm());

        }
        @Override
        public boolean onTouch(View view, MotionEvent event) {
            if (pumiceDialogManager != null) {
                pumiceDialogManager.stopListening();
                pumiceDialogManager.stopTalking();
            }

            if (gestureDetector.onTouchEvent(event)) {
                //on click: single tap
                view.performClick();
                return true;
            }

            int[] rootViewCoordinate = new int[2];
            rootView.getLocationOnScreen(rootViewCoordinate);

            // use the relevant position to rootView so that the dragging behavior will not be affected by scrolling
            final int X = (int) event.getRawX() - rootViewCoordinate[0];
            final int Y = (int) event.getRawY() - rootViewCoordinate[1];
            switch (event.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_DOWN:
                    scrollView.requestDisallowInterceptTouchEvent(true);
                    RelativeLayout.LayoutParams lParams = (RelativeLayout.LayoutParams) view.getLayoutParams();
                    xDelta = X - lParams.leftMargin;
                    yDelta = Y - lParams.topMargin;
                    break;
                case MotionEvent.ACTION_UP:
                    scrollView.requestDisallowInterceptTouchEvent(false);
                    onVariableHighlightDragDropped(this, view, event);
                    break;
                case MotionEvent.ACTION_POINTER_DOWN:
                    break;
                case MotionEvent.ACTION_POINTER_UP:
                    break;
                case MotionEvent.ACTION_MOVE:
                    RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) view.getLayoutParams();
                    layoutParams.leftMargin = X - xDelta;
                    layoutParams.topMargin = Y - yDelta;
                    layoutParams.rightMargin = 0;
                    layoutParams.bottomMargin = 0;
                    if (layoutParams.leftMargin >= 0
                            && layoutParams.topMargin >= 0
                            && layoutParams.leftMargin + layoutParams.width <= rootView.getWidth()
                            && layoutParams.topMargin + layoutParams.height <= rootView.getHeight()) {
                        view.setLayoutParams(layoutParams);
                    }
                    break;
            }

            rootView.invalidate();
            return true;
        }

        public VariableValue getCurrentSelectedVariableValue() {
            return currentSelectedVariableValue;
        }

        public SugiliteStartingBlock getSubScript() {
            return subScript;
        }

        public void setCurrentSelectedVariableValue(VariableValue currentSelectedVariableValue) {
            this.currentSelectedVariableValue = currentSelectedVariableValue;
        }
    }

    private void onVariableHighlightDragDropped (VariableHighlightOnTouchListener variableHighlightOnTouchListener, View variableHighlightView, MotionEvent motionEvent) {
        //TODO: implement on drag dropped

        // get all alternative values
        List<VariableValue> allAlternativeValues = new ArrayList<>();
        VariableValue currentSelectedVariableValue = variableHighlightOnTouchListener.getCurrentSelectedVariableValue();
        SugiliteStartingBlock subScript = variableHighlightOnTouchListener.getSubScript();
        allAlternativeValues.addAll(subScript.variableNameAlternativeValueMap.get(currentSelectedVariableValue.getVariableName()));

        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) variableHighlightView.getLayoutParams();
        Rect currentViewBounds = new Rect(layoutParams.leftMargin, layoutParams.topMargin, layoutParams.leftMargin + layoutParams.width, layoutParams.topMargin + layoutParams.height);


        List<Pair<Rect, Integer>> alternativeValueBoundsInParentOverlapList = new ArrayList<>();
        Map<Rect, VariableValue> boundsInParentVariableValueMap = new HashMap<>();

        for (VariableValue variableValue : allAlternativeValues) {
            if (variableValue.getVariableValueContext() == null || variableValue.getVariableValueContext().getTargetNode() == null) {
                continue;
            }
            Rect originalBoundsInScreen = Rect.unflattenFromString(variableValue.getVariableValueContext().getTargetNode().getBoundsInScreen());
            originalBoundsInScreen.set(getScaledCoordinate(originalBoundsInScreen.left), getScaledCoordinate(originalBoundsInScreen.top), getScaledCoordinate(originalBoundsInScreen.right), getScaledCoordinate(originalBoundsInScreen.bottom));
            boundsInParentVariableValueMap.put(originalBoundsInScreen, variableValue);
            alternativeValueBoundsInParentOverlapList.add(new Pair<>(originalBoundsInScreen, getIntersectionArea(originalBoundsInScreen, currentViewBounds)));
        }
        alternativeValueBoundsInParentOverlapList.sort(new Comparator<Pair<Rect, Integer>>() {
            @Override
            public int compare(Pair<Rect, Integer> o1, Pair<Rect, Integer> o2) {
                return o2.second - o1.second;
            }
        });
        VariableValue matchedNewVariableValue = null;
        Rect matchedRect = null;
        if (alternativeValueBoundsInParentOverlapList.size() > 0 && alternativeValueBoundsInParentOverlapList.get(0).second > 0) {
            matchedNewVariableValue = boundsInParentVariableValueMap.get(alternativeValueBoundsInParentOverlapList.get(0).first);
            matchedRect = alternativeValueBoundsInParentOverlapList.get(0).first;
        } else {
            matchedNewVariableValue = currentSelectedVariableValue;
            Rect currentSelectedVariableBoundsInScreen = Rect.unflattenFromString(currentSelectedVariableValue.getVariableValueContext().getTargetNode().getBoundsInScreen());
            currentSelectedVariableBoundsInScreen.set(getScaledCoordinate(currentSelectedVariableBoundsInScreen.left), getScaledCoordinate(currentSelectedVariableBoundsInScreen.top), getScaledCoordinate(currentSelectedVariableBoundsInScreen.right), getScaledCoordinate(currentSelectedVariableBoundsInScreen.bottom));
            matchedRect = currentSelectedVariableBoundsInScreen;
        }
        //update the current selected variable value

        variableHighlightOnTouchListener.setCurrentSelectedVariableValue(matchedNewVariableValue);

        //move the highlight view based on matchedNewVariableValue
        RelativeLayout.LayoutParams newLayoutParams = (RelativeLayout.LayoutParams) variableHighlightView.getLayoutParams();
        layoutParams.leftMargin = matchedRect.left;
        layoutParams.topMargin = matchedRect.top;
        layoutParams.rightMargin = 0;
        layoutParams.bottomMargin = 0;
        variableHighlightView.setLayoutParams(layoutParams);

        VariableValue finalMatchedNewVariableValue = matchedNewVariableValue;
        if (matchedNewVariableValue != currentSelectedVariableValue) {
            //change made

            //update getProcedureOperation
            variableHighlightOnTouchListener.getProcedureOperation.getVariableValues().removeIf(variableValue -> finalMatchedNewVariableValue.getVariableName().equals(variableValue.getVariableName()));
            variableHighlightOnTouchListener.getProcedureOperation.getVariableValues().add(new VariableValue<>(finalMatchedNewVariableValue.getVariableName(), finalMatchedNewVariableValue.getVariableValue().toString()));

            if (variableHighlightOnTouchListener.originalScreenshotView != null && variableHighlightOnTouchListener.originalScreenshotView.getVisibility() == View.VISIBLE) {
                variableHighlightOnTouchListener.originalScreenshotView.setVisibility(GONE);
            }

            if (variableHighlightOnTouchListener.soviteVariableUpdateCallback != null) {
                variableHighlightOnTouchListener.soviteVariableUpdateCallback.onGetProcedureOperationUpdated(variableHighlightOnTouchListener.getProcedureOperation, matchedNewVariableValue, true);
            }
        }

    }

    private int getIntersectionArea(Rect a, Rect b) {
        int left = Math.max(a.left, b.left);
        int right = Math.min(a.right, b.right);
        int top = Math.max(a.top, b.top);
        int bottom = Math.min(a.bottom, b.bottom);

        return (right - left) * (bottom - top);
    }

    private class SingleTapConfirm extends GestureDetector.SimpleOnGestureListener {
        SingleTapConfirm() {

        }
        @Override
        public boolean onSingleTapUp(MotionEvent event) {
            return true;
        }
    }

}
