package edu.cmu.hcii.sugilite.sovite.visual;


import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.text.Html;
import android.util.Pair;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;


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

import edu.cmu.hcii.sugilite.model.variable.Variable;
import edu.cmu.hcii.sugilite.model.variable.VariableValue;
import edu.cmu.hcii.sugilite.pumice.dialog.PumiceDialogManager;
import edu.cmu.hcii.sugilite.sovite.visual.text_selection.SoviteSetTextParameterDialog;

import static edu.cmu.hcii.sugilite.pumice.PumiceDemonstrationUtil.getScaledDrawable;
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

    final static int STROKE_WIDTH = 20;
    final static int HALF_STROKE_WIDTH = STROKE_WIDTH / 2;

    public View generateInteractiveViewForVariableValueAndScreenshotDrawable(VariableValue<String> mainVariableValue, List<VariableValue<String>> additionalVariables, Drawable screenshot, SugiliteStartingBlock subScript, SugiliteGetProcedureOperation getProcedureOperation, SoviteVariableUpdateCallback soviteVariableUpdateCallback, String originalUtterance, PumiceDialogManager pumiceDialogManager) {

        RelativeLayout parentLayout = new RelativeLayout(context);
        parentLayout.setGravity(Gravity.TOP | Gravity.LEFT);

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

        List<VariableValue<String>> allVariableValues = new ArrayList<>();
        allVariableValues.add(mainVariableValue);
        if (additionalVariables != null) {
            allVariableValues.addAll(additionalVariables);
        }

        for (VariableValue<String> variableValue : allVariableValues) {
            if (variableValue.getVariableValueContext() == null || variableValue.getVariableValueContext().getTargetNode() == null) {
                continue;
            }

            Node variableTargetNode = variableValue.getVariableValueContext().getTargetNode();
            Rect variableTargetNodeScreenBounding = Rect.unflattenFromString(variableTargetNode.getBoundsInScreen());
            variableTargetNodeScreenBounding.set(getScaledCoordinate(variableTargetNodeScreenBounding.left), getScaledCoordinate(variableTargetNodeScreenBounding.top), getScaledCoordinate(variableTargetNodeScreenBounding.right), getScaledCoordinate(variableTargetNodeScreenBounding.bottom));

            int MININUM_WIDTH = 100, MININUM_HEIGHT = 100;
            int oldleft = variableTargetNodeScreenBounding.left, oldright = variableTargetNodeScreenBounding.right, oldtop = variableTargetNodeScreenBounding.top, oldbottom = variableTargetNodeScreenBounding.bottom;
            if (variableTargetNodeScreenBounding.width() < MININUM_WIDTH) {
                variableTargetNodeScreenBounding.left = (oldleft + oldright) / 2 - (MININUM_WIDTH / 2);
                variableTargetNodeScreenBounding.right = (oldleft + oldright) / 2 + (MININUM_WIDTH / 2);
            }
            if (variableTargetNodeScreenBounding.height() < MININUM_HEIGHT) {
                variableTargetNodeScreenBounding.top = (oldtop + oldbottom) / 2 - (MININUM_HEIGHT / 2);
                variableTargetNodeScreenBounding.bottom = (oldtop + oldbottom) / 2 + (MININUM_HEIGHT / 2);
            }


            //highlightLayout should contain the stroke view and the highlight view
            RelativeLayout highlightLayout = new RelativeLayout(context);
            highlightLayout.setGravity(Gravity.TOP | Gravity.LEFT);
            RelativeLayout.LayoutParams highlightLayoutViewParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            highlightLayoutViewParams.setMargins(variableTargetNodeScreenBounding.left, variableTargetNodeScreenBounding.top, 0, 0);
            highlightLayoutViewParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
            highlightLayoutViewParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
            parentLayout.addView(highlightLayout, highlightLayoutViewParams);

            //add stroke
            RectShape strokeRectShape = new RectShape();
            ShapeDrawable strokeShapeDrawable = new ShapeDrawable(strokeRectShape);
            strokeShapeDrawable.getPaint().setColor(0x8CFF0000);
            strokeShapeDrawable.getPaint().setStyle(Paint.Style.STROKE);
            strokeShapeDrawable.setIntrinsicHeight(variableTargetNodeScreenBounding.height() - STROKE_WIDTH);
            strokeShapeDrawable.setIntrinsicWidth(variableTargetNodeScreenBounding.width() - STROKE_WIDTH);
            strokeShapeDrawable.getPaint().setStrokeWidth(STROKE_WIDTH);
            ImageView strokeshapeView = new ImageView(context);
            strokeshapeView.setImageDrawable(strokeShapeDrawable);
            strokeshapeView.setId(View.generateViewId());
            RelativeLayout.LayoutParams strokeShapeViewParams = new RelativeLayout.LayoutParams(strokeShapeDrawable.getIntrinsicWidth(), strokeShapeDrawable.getIntrinsicHeight());
            strokeShapeViewParams.setMargins(HALF_STROKE_WIDTH, HALF_STROKE_WIDTH, 0, 0);
            strokeShapeViewParams.alignWithParent = true;
            strokeShapeViewParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
            strokeShapeViewParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
            highlightLayout.addView(strokeshapeView, strokeShapeViewParams);


            //add highlight
            RectShape fillRectShape = new RectShape();
            ShapeDrawable fillShapeDrawable = new ShapeDrawable(fillRectShape);
            fillShapeDrawable.getPaint().setColor(0x8CFFFF00);
            fillShapeDrawable.getPaint().setStyle(Paint.Style.FILL);
            fillShapeDrawable.setIntrinsicHeight(variableTargetNodeScreenBounding.height() - 2 * STROKE_WIDTH);
            fillShapeDrawable.setIntrinsicWidth(variableTargetNodeScreenBounding.width() - 2 * STROKE_WIDTH);
            ImageView fillShapeView = new ImageView(context);
            fillShapeView.setImageDrawable(fillShapeDrawable);
            fillShapeView.setId(View.generateViewId());
            RelativeLayout.LayoutParams fillShapeViewParams = new RelativeLayout.LayoutParams(fillShapeDrawable.getIntrinsicWidth(), fillShapeDrawable.getIntrinsicHeight());
            fillShapeViewParams.setMargins(STROKE_WIDTH, STROKE_WIDTH, 0, 0);
            fillShapeViewParams.alignWithParent = true;
            fillShapeViewParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
            fillShapeViewParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
            highlightLayout.addView(fillShapeView, fillShapeViewParams);




            //add text if the variable has no alternative values (EditText type of variable)
            Set<VariableValue> alternativeValues = subScript.variableNameAlternativeValueMap.get(variableValue.getVariableName());
            if (alternativeValues.size() == 0) {
                TextView variableValueTextView = new TextView(context);
                variableValueTextView.setTextColor(Color.BLACK);
                variableValueTextView.setPadding(10, 10, 30, 10);
                variableValueTextView.setText(variableValue.getVariableValue().toString());
                variableValueTextView.setGravity(Gravity.LEFT | Gravity.TOP);
                variableValueTextView.setMaxLines(1);
                if (android.os.Build.VERSION.SDK_INT >= 26) {
                    //allow the textview to auto resize
                    variableValueTextView.setAutoSizeTextTypeWithDefaults(TextView.AUTO_SIZE_TEXT_TYPE_UNIFORM);
                }
                RelativeLayout.LayoutParams variableValueTextViewLayoutParams = new RelativeLayout.LayoutParams(variableTargetNodeScreenBounding.width(), variableTargetNodeScreenBounding.height());
                variableValueTextViewLayoutParams.addRule(RelativeLayout.ALIGN_LEFT, fillShapeView.getId());
                highlightLayout.addView(variableValueTextView, variableValueTextViewLayoutParams);

                //make the background non-transparent
                fillShapeDrawable.getPaint().setColor(0xC0FFFF00);
            } else {
                //add the parameter label
                //don't add parameter labels for text input types
                TextView parameterLabelTextView = new TextView(context);
                parameterLabelTextView.setTextColor(Color.DKGRAY);
                parameterLabelTextView.setTypeface(Typeface.create("serif-monospace", Typeface.BOLD));
                parameterLabelTextView.setText(variableValue.getVariableName());
                parameterLabelTextView.setPadding(1, 1, 1, 1);
                RelativeLayout.LayoutParams parameterLabelTextViewLayoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                parameterLabelTextViewLayoutParams.addRule(RelativeLayout.ALIGN_LEFT, strokeshapeView.getId());
                parameterLabelTextViewLayoutParams.addRule(RelativeLayout.BELOW, strokeshapeView.getId());
                highlightLayout.addView(parameterLabelTextView, parameterLabelTextViewLayoutParams);
            }


            //add onClick listener for the highlight
            addOnClickListenerForVariableHighlight(highlightLayout, variableValue, subScript, getProcedureOperation, soviteVariableUpdateCallback, originalUtterance, pumiceDialogManager, parentLayout);

            //add onDrag listener for the highlight
            List<VariableValue> allVariableValueThatCanBeSwapped = new ArrayList<>();
            for (VariableValue<String> alternativeVariable : allVariableValues) {
                if (!variableValue.getVariableName().equals(alternativeVariable.getVariableName())) {
                    allVariableValueThatCanBeSwapped.add(alternativeVariable);
                }
            }
            addOnDragListenerForVariableHighlight(highlightLayout, parentLayout, variableValue, allVariableValueThatCanBeSwapped, subScript, getProcedureOperation, soviteVariableUpdateCallback, pumiceDialogManager, parentLayout);

        }


        return parentLayout;
    }

    private void addOnClickListenerForVariableHighlight (View highlightView, VariableValue variableValue, SugiliteStartingBlock subScript, SugiliteGetProcedureOperation getProcedureOperation, SoviteVariableUpdateCallback soviteVariableUpdateCallback, String originalUtterance, @Nullable PumiceDialogManager pumiceDialogManager, @Nullable View originalScreenshotView) {
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

                //check if the variable has any alternative values
                if (alternativeValues.size() > 0) {
                    //show spinner type dialog
                    SoviteVisualVariableOnClickDialog soviteVisualVariableOnClickDialog = new SoviteVisualVariableOnClickDialog(context, variableValue, subScript, getProcedureOperation, soviteVariableUpdateCallback, originalScreenshotView, false);
                    soviteVisualVariableOnClickDialog.show();
                } else {
                    //show text selection dialog
                    SoviteSetTextParameterDialog soviteSetTextParameterDialog = new SoviteSetTextParameterDialog(context, pumiceDialogManager.getSugiliteData(), variableValue, originalUtterance, getProcedureOperation, soviteVariableUpdateCallback, originalScreenshotView, false);
                    soviteSetTextParameterDialog.show();
                }

                if (pumiceDialogManager != null) {
                    pumiceDialogManager.stopTalking();
                    pumiceDialogManager.stopListening();
                }
            }
        });
    }



    private int getScaledCoordinate (int coordinate) {
        return (int) (coordinate * SCREENSHOT_SCALE);
    }


    private void addOnDragListenerForVariableHighlight (View highlightView, RelativeLayout rootView, VariableValue variableValue, List<VariableValue> allVariableValueThatCanBeSwapped, SugiliteStartingBlock subScript, SugiliteGetProcedureOperation getProcedureOperation, SoviteVariableUpdateCallback soviteVariableUpdateCallback, @Nullable PumiceDialogManager pumiceDialogManager, @Nullable View originalScreenshotView) {
        ScrollView scrollView = null;
        if (pumiceDialogManager != null) {
            scrollView = pumiceDialogManager.getScrollView();
        }
        VariableHighlightOnTouchListener variableHighlightOnTouchListener = new VariableHighlightOnTouchListener(rootView, scrollView, variableValue, subScript, getProcedureOperation, soviteVariableUpdateCallback, pumiceDialogManager, originalScreenshotView);
        variableHighlightOnTouchListener.setAllVariableValueThatCanBeSwapped(allVariableValueThatCanBeSwapped);
        highlightView.setOnTouchListener(variableHighlightOnTouchListener);

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


        private List<VariableValue> allVariableValueThatCanBeSwapped;


        private VariableHighlightOnTouchListener(RelativeLayout rootView, ScrollView scrollView, VariableValue currentSelectedVariableValue, SugiliteStartingBlock subScript, SugiliteGetProcedureOperation getProcedureOperation, SoviteVariableUpdateCallback soviteVariableUpdateCallback, @Nullable PumiceDialogManager pumiceDialogManager, @Nullable View originalScreenshotView) {
            this.rootView = rootView;
            this.scrollView = scrollView;
            this.currentSelectedVariableValue = currentSelectedVariableValue;
            this.subScript = subScript;
            this.getProcedureOperation = getProcedureOperation;
            this.soviteVariableUpdateCallback = soviteVariableUpdateCallback;
            this.pumiceDialogManager = pumiceDialogManager;
            this.originalScreenshotView = originalScreenshotView;
            this.allVariableValueThatCanBeSwapped = new ArrayList<>();

            gestureDetector = new GestureDetector(context, new SingleTapConfirm());

        }


        public void setAllVariableValueThatCanBeSwapped(List<VariableValue> allVariableValueThatCanBeSwapped) {
            this.allVariableValueThatCanBeSwapped = allVariableValueThatCanBeSwapped;
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
                restoreViewBoundsToTheCurrentSelectedVariable (this, view);
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
        //implement on drag dropped

        // get the bounds of the view for the current variable
        RelativeLayout.LayoutParams currentHighlightViewLayoutParams = (RelativeLayout.LayoutParams) variableHighlightView.getLayoutParams();
        Rect currentHighlightViewBounds = new Rect(currentHighlightViewLayoutParams.leftMargin, currentHighlightViewLayoutParams.topMargin, currentHighlightViewLayoutParams.leftMargin + variableHighlightView.getWidth(), currentHighlightViewLayoutParams.topMargin + variableHighlightView.getHeight());

        // get all alternative values of the currentSelectedVariableValue
        List<VariableValue> allAlternativeValues = new ArrayList<>();
        VariableValue currentSelectedVariableValue = variableHighlightOnTouchListener.getCurrentSelectedVariableValue();
        SugiliteStartingBlock subScript = variableHighlightOnTouchListener.getSubScript();
        allAlternativeValues.addAll(subScript.variableNameAlternativeValueMap.get(currentSelectedVariableValue.getVariableName()));

        if (allAlternativeValues.size() > 0) {
            //"spinner" type variable, can be dragged to an alternative value
            List<Pair<String, Integer>> alternativeValueBoundsInParentOverlapAreaList = new ArrayList<>();
            Map<String, VariableValue> boundsInParentVariableValueMap = new HashMap<>();

            for (VariableValue variableValue : allAlternativeValues) {
                if (variableValue.getVariableValueContext() == null || variableValue.getVariableValueContext().getTargetNode() == null) {
                    continue;
                }
                Rect originalBoundsInScreen = Rect.unflattenFromString(variableValue.getVariableValueContext().getTargetNode().getBoundsInScreen());
                originalBoundsInScreen.set(getScaledCoordinate(originalBoundsInScreen.left), getScaledCoordinate(originalBoundsInScreen.top), getScaledCoordinate(originalBoundsInScreen.right), getScaledCoordinate(originalBoundsInScreen.bottom));
                boundsInParentVariableValueMap.put(originalBoundsInScreen.flattenToString(), variableValue);
                alternativeValueBoundsInParentOverlapAreaList.add(new Pair<>(originalBoundsInScreen.flattenToString(), getIntersectionAreaBetweenTwoRects(originalBoundsInScreen, currentHighlightViewBounds)));
            }

            //sort the alternative values by the area of intersections between their bounds and the current view bounds
            alternativeValueBoundsInParentOverlapAreaList.sort(new Comparator<Pair<String, Integer>>() {
                @Override
                public int compare(Pair<String, Integer> o1, Pair<String, Integer> o2) {
                    return o2.second - o1.second;
                }
            });

            VariableValue matchedNewVariableValue = null;
            Rect matchedRect = null;

            //check if we have any alternative value whose bounds intersects with the current view bounds
            if (alternativeValueBoundsInParentOverlapAreaList.size() > 0 && alternativeValueBoundsInParentOverlapAreaList.get(0).second > 0) {
                matchedNewVariableValue = boundsInParentVariableValueMap.get(alternativeValueBoundsInParentOverlapAreaList.get(0).first);
                matchedRect = Rect.unflattenFromString(alternativeValueBoundsInParentOverlapAreaList.get(0).first);
            } else {
                //restore the view bounds to select the current selected variable value if no intersection
                restoreViewBoundsToTheCurrentSelectedVariable (variableHighlightOnTouchListener, variableHighlightView);
                return;
            }


            //update the current selected variable value
            variableHighlightOnTouchListener.setCurrentSelectedVariableValue(matchedNewVariableValue);

            //match the centroid of variableHighlightView to the centroid of matchedRect
            currentHighlightViewLayoutParams.leftMargin = matchedRect.left + (matchedRect.width() / 2) - (variableHighlightView.getWidth() / 2) - HALF_STROKE_WIDTH;
            currentHighlightViewLayoutParams.topMargin = matchedRect.top + (matchedRect.height() / 2) - (variableHighlightView.getHeight() / 2) + STROKE_WIDTH;

            currentHighlightViewLayoutParams.rightMargin = 0;
            currentHighlightViewLayoutParams.bottomMargin = 0;
            variableHighlightView.setLayoutParams(currentHighlightViewLayoutParams);

            VariableValue finalMatchedNewVariableValue = matchedNewVariableValue;
            if (matchedNewVariableValue != currentSelectedVariableValue && matchedNewVariableValue != null) {
                //when change has been made

                //update getProcedureOperation
                variableHighlightOnTouchListener.getProcedureOperation.getVariableValues().removeIf(variableValue -> finalMatchedNewVariableValue.getVariableName().equals(variableValue.getVariableName()));
                variableHighlightOnTouchListener.getProcedureOperation.getVariableValues().add(new VariableValue<>(finalMatchedNewVariableValue.getVariableName(), finalMatchedNewVariableValue.getVariableValue().toString()));

                //hide the original screenshot
                if (variableHighlightOnTouchListener.originalScreenshotView != null && variableHighlightOnTouchListener.originalScreenshotView.getVisibility() == View.VISIBLE) {
                    variableHighlightOnTouchListener.originalScreenshotView.setVisibility(View.GONE);
                }

                //call the callback (which will also generate a new screeshot based on the new selected variable)
                if (variableHighlightOnTouchListener.soviteVariableUpdateCallback != null) {
                    List<VariableValue> changedVariableValues = new ArrayList<>();
                    changedVariableValues.add(matchedNewVariableValue);
                    variableHighlightOnTouchListener.soviteVariableUpdateCallback.onGetProcedureOperationUpdated(variableHighlightOnTouchListener.getProcedureOperation, changedVariableValues, true);
                }
            }
        } else if (variableHighlightOnTouchListener.allVariableValueThatCanBeSwapped.size() > 0) {
            //"text input" type variable, can be dragged to swap value with another variable
            List<Pair<String, Integer>> swappableVariableValueBoundsInParentOverlapAreaList = new ArrayList<>();
            Map<String, VariableValue> boundsInParentSwappableVariableValueMap = new HashMap<>();

            for (VariableValue variableValue : variableHighlightOnTouchListener.allVariableValueThatCanBeSwapped) {
                if (variableValue.getVariableValueContext() == null || variableValue.getVariableValueContext().getTargetNode() == null) {
                    continue;
                }
                Rect originalBoundsInScreen = Rect.unflattenFromString(variableValue.getVariableValueContext().getTargetNode().getBoundsInScreen());
                originalBoundsInScreen.set(getScaledCoordinate(originalBoundsInScreen.left), getScaledCoordinate(originalBoundsInScreen.top), getScaledCoordinate(originalBoundsInScreen.right), getScaledCoordinate(originalBoundsInScreen.bottom));
                boundsInParentSwappableVariableValueMap.put(originalBoundsInScreen.flattenToString(), variableValue);
                swappableVariableValueBoundsInParentOverlapAreaList.add(new Pair<>(originalBoundsInScreen.flattenToString(), getIntersectionAreaBetweenTwoRects(originalBoundsInScreen, currentHighlightViewBounds)));
            }

            //sort the swappable values by the area of intersections between their bounds and the current view bounds
            swappableVariableValueBoundsInParentOverlapAreaList.sort(new Comparator<Pair<String, Integer>>() {
                @Override
                public int compare(Pair<String, Integer> o1, Pair<String, Integer> o2) {
                    return o2.second - o1.second;
                }
            });

            VariableValue matchedNewVariableValue = null;
            Rect matchedRect = null;

            //check if we have any alternative value whose bounds intersects with the current view bounds
            if (swappableVariableValueBoundsInParentOverlapAreaList.size() > 0 && swappableVariableValueBoundsInParentOverlapAreaList.get(0).second > 0) {
                matchedNewVariableValue = boundsInParentSwappableVariableValueMap.get(swappableVariableValueBoundsInParentOverlapAreaList.get(0).first);
                matchedRect = Rect.unflattenFromString(swappableVariableValueBoundsInParentOverlapAreaList.get(0).first);
            }

            //restore the view bounds to select the current selected variable value if no intersection
            restoreViewBoundsToTheCurrentSelectedVariable (variableHighlightOnTouchListener, variableHighlightView);

            //swap the values of the two variables
            if (matchedNewVariableValue != null) {
                //change has been made
                final VariableValue newValuableCurrentSelected = new VariableValue<String>(currentSelectedVariableValue.getVariableName(), matchedNewVariableValue.getVariableValue().toString());
                final VariableValue newValuableMatched = new VariableValue<String>(matchedNewVariableValue.getVariableName(), currentSelectedVariableValue.getVariableValue().toString());

                //update getProcedureOperation
                variableHighlightOnTouchListener.getProcedureOperation.getVariableValues().removeIf(variableValue -> newValuableCurrentSelected.getVariableName().equals(variableValue.getVariableName()));
                variableHighlightOnTouchListener.getProcedureOperation.getVariableValues().removeIf(variableValue -> newValuableMatched.getVariableName().equals(variableValue.getVariableName()));

                variableHighlightOnTouchListener.getProcedureOperation.getVariableValues().add(newValuableCurrentSelected);
                variableHighlightOnTouchListener.getProcedureOperation.getVariableValues().add(newValuableMatched);

                //hide the original screenshot
                if (variableHighlightOnTouchListener.originalScreenshotView != null && variableHighlightOnTouchListener.originalScreenshotView.getVisibility() == View.VISIBLE) {
                    variableHighlightOnTouchListener.originalScreenshotView.setVisibility(View.GONE);
                }

                //call the callback (which will also generate a new screeshot based on the new selected variable)
                if (variableHighlightOnTouchListener.soviteVariableUpdateCallback != null) {
                    List<VariableValue> changedVariableValues = new ArrayList<>();
                    changedVariableValues.add(newValuableCurrentSelected);
                    changedVariableValues.add(newValuableMatched);
                    variableHighlightOnTouchListener.soviteVariableUpdateCallback.onGetProcedureOperationUpdated(variableHighlightOnTouchListener.getProcedureOperation, changedVariableValues, true);
                }
            }

        } else {
            //can't do anything, restore the highlight view location
            restoreViewBoundsToTheCurrentSelectedVariable (variableHighlightOnTouchListener, variableHighlightView);
        }

    }

    private void restoreViewBoundsToTheCurrentSelectedVariable(VariableHighlightOnTouchListener variableHighlightOnTouchListener, View variableHighlightView) {
        //restore the view bounds to select the current selected variable value
        VariableValue currentSelectedVariableValue = variableHighlightOnTouchListener.getCurrentSelectedVariableValue();

        Rect currentSelectedVariableBoundsInScreen = Rect.unflattenFromString(currentSelectedVariableValue.getVariableValueContext().getTargetNode().getBoundsInScreen());
        currentSelectedVariableBoundsInScreen.set(getScaledCoordinate(currentSelectedVariableBoundsInScreen.left), getScaledCoordinate(currentSelectedVariableBoundsInScreen.top), getScaledCoordinate(currentSelectedVariableBoundsInScreen.right), getScaledCoordinate(currentSelectedVariableBoundsInScreen.bottom));
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) variableHighlightView.getLayoutParams();

        //match the centroid of variableHighlightView to the centroid of matchedRect
        layoutParams.leftMargin = currentSelectedVariableBoundsInScreen.left + (currentSelectedVariableBoundsInScreen.width() / 2) - (variableHighlightView.getWidth() / 2) - HALF_STROKE_WIDTH;
        layoutParams.topMargin = currentSelectedVariableBoundsInScreen.top + (currentSelectedVariableBoundsInScreen.height() / 2) - (variableHighlightView.getHeight() / 2) + STROKE_WIDTH;

        layoutParams.rightMargin = 0;
        layoutParams.bottomMargin = 0;
        variableHighlightView.setLayoutParams(layoutParams);
    }

    private int getIntersectionAreaBetweenTwoRects(Rect a, Rect b) {
        if (a.intersect(b)) {
            int left = Math.max(a.left, b.left);
            int right = Math.min(a.right, b.right);
            int top = Math.max(a.top, b.top);
            int bottom = Math.min(a.bottom, b.bottom);

            return (right - left) * (bottom - top);
        } else {
            return 0;
        }
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
