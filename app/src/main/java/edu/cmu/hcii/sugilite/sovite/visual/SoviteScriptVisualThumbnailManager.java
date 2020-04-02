package edu.cmu.hcii.sugilite.sovite.visual;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import edu.cmu.hcii.sugilite.Const;
import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptDao;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptFileDao;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptSQLDao;
import edu.cmu.hcii.sugilite.model.Node;
import edu.cmu.hcii.sugilite.model.block.SugiliteBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteConditionBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteOperationBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;
import edu.cmu.hcii.sugilite.model.operation.binary.SugiliteGetProcedureOperation;
import edu.cmu.hcii.sugilite.model.variable.VariableValue;
import edu.cmu.hcii.sugilite.ontology.SerializableUISnapshot;
import edu.cmu.hcii.sugilite.pumice.dialog.PumiceDialogManager;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.util.NavigationBarUtil;

import static edu.cmu.hcii.sugilite.Const.SQL_SCRIPT_DAO;

/**
 * @author toby
 * @date 2/20/20
 * @time 8:31 PM
 */

public class SoviteScriptVisualThumbnailManager {
    private SugiliteData sugiliteData;
    private SugiliteScriptDao sugiliteScriptDao;
    private Context context;
    private SoviteInteractiveVariableHighlightManager soviteInteractiveVariableHighlightManager;
    final static double SCREENSHOT_SCALE = 1.5;

    public SoviteScriptVisualThumbnailManager(Activity context) {
        this.sugiliteData = (SugiliteData) context.getApplication();
        this.context = context;
        this.soviteInteractiveVariableHighlightManager = new SoviteInteractiveVariableHighlightManager(context);
        if (Const.DAO_TO_USE == SQL_SCRIPT_DAO) {
            this.sugiliteScriptDao = new SugiliteScriptSQLDao(context);
        } else {
            this.sugiliteScriptDao = new SugiliteScriptFileDao(context, sugiliteData);
        }
    }


    public List<View> getVisualThumbnailViewsForBlock(SugiliteBlock block, @Nullable PumiceDialogManager pumiceDialogManager) {
        return getVisualThumbnailViewsForBlock (block, null, pumiceDialogManager, null);
    }

    public List<View> getVisualThumbnailViewsForBlock(SugiliteBlock block, @Nullable SoviteVariableUpdateCallback soviteVariableUpdateCallback, @Nullable PumiceDialogManager pumiceDialogManager) {
        return getVisualThumbnailViewsForBlock (block, soviteVariableUpdateCallback, pumiceDialogManager,null);
    }


    public List<View> getVisualThumbnailViewsForBlock(SugiliteBlock block, @Nullable SoviteVariableUpdateCallback soviteVariableUpdateCallback, @Nullable PumiceDialogManager pumiceDialogManager, @Nullable String onlyVariableNameToShow) {
        List<View> viewList = new ArrayList<>();
        if (block instanceof SugiliteStartingBlock) {
            //handle scripts
            viewList.add(getPlainViewForDrawable(getVisualThumbnailForScript((SugiliteStartingBlock) block)));
            return viewList;
        }

        if (block instanceof SugiliteOperationBlock) {
            if (block.getScreenshot() != null) {
                //return the screenshot for the operation if available
                viewList.add(getPlainViewForDrawable(getDrawableFromFile(block.getScreenshot())));
                return viewList;
            }

            if (((SugiliteOperationBlock) block).getOperation() != null &&
                    ((SugiliteOperationBlock) block).getOperation() instanceof SugiliteGetProcedureOperation) {
                //special handling for SugiliteGetProcedureOperation
                SugiliteGetProcedureOperation getProcedureOperation = (SugiliteGetProcedureOperation) ((SugiliteOperationBlock) block).getOperation();

                List<VariableValue<String>> variableValuesFromGetProcedureOperation = getProcedureOperation.getVariableValues();
                List<VariableValue<String>> matchedVariableValuesFromTheScript = new ArrayList<>();

                //compare variableValuesFromGetProcedureOperation against alternative values in the script
                try {
                    String subScriptName = getProcedureOperation.evaluate(sugiliteData);
                    SugiliteStartingBlock subScript = sugiliteScriptDao.read(subScriptName);
                    Map<String, Set<VariableValue>> variableNameAlternativeValueMap = subScript.variableNameAlternativeValueMap;
                    for (VariableValue<String> variableValueFromGetProcedureOperation : variableValuesFromGetProcedureOperation) {
                        if (variableNameAlternativeValueMap.containsKey(variableValueFromGetProcedureOperation.getVariableName())) {
                            for (VariableValue alternativeValue : variableNameAlternativeValueMap.get(variableValueFromGetProcedureOperation.getVariableName())) {
                                if (alternativeValue.getVariableValue().equals(variableValueFromGetProcedureOperation.getVariableValue())) {
                                    matchedVariableValuesFromTheScript.add(alternativeValue);
                                    break;
                                }
                            }
                        }
                    }

                    if (matchedVariableValuesFromTheScript != null && matchedVariableValuesFromTheScript.size() > 0) {
                        /*
                        Drawable combinedVariableDrawable = getDrawableFromVariableList(variableValues);
                        if (combinedVariableDrawable != null) {
                            return combinedVariableDrawable;
                        }
                        */
                        for (VariableValue variableValue : matchedVariableValuesFromTheScript) {
                            if (onlyVariableNameToShow != null && (! onlyVariableNameToShow.equals(variableValue.getVariableName()))) {
                                //skip if does NOT match the onlyVariableNameToShow
                                continue;
                            }
                            View variableView = getViewFromVariable(variableValue, subScript, getProcedureOperation, soviteVariableUpdateCallback, pumiceDialogManager);
                            if (variableView != null) {
                                viewList.add(variableView);
                            }
                        }
                        if (viewList != null && viewList.size() > 0) {
                            return viewList;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        try {
            //other situations
            File screenshotFile = getLastAvailableScreenshotInSubsequentScript(block, null);
            viewList.add(getPlainViewForDrawable(getDrawableFromFile(screenshotFile)));
            return viewList;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }


    private View getPlainViewForDrawable(Drawable drawable) {
        ImageView imageView = new ImageView(context);
        imageView.setImageDrawable(drawable);
        return imageView;
    }

    private View getViewFromVariable(VariableValue<String> variableValue, SugiliteStartingBlock subScript, SugiliteGetProcedureOperation getProcedureOperation, SoviteVariableUpdateCallback soviteVariableUpdateCallback, PumiceDialogManager pumiceDialogManager) {
            if (variableValue.getVariableValueContext() != null) {
                if (variableValue.getVariableValueContext().getScreenshot() != null &&
                        variableValue.getVariableValueContext().getTargetNode() != null) {
                    File screenshotFile = variableValue.getVariableValueContext().getScreenshot();
                    Drawable screenshotDrawable = getDrawableFromFile(screenshotFile);
                    // add highlights of target node to the screenshot drawable
                    View screenshotWithHighlightView = soviteInteractiveVariableHighlightManager.generateInteractiveViewForVariableValueAndScreenshotDrawable(variableValue, screenshotDrawable, subScript, getProcedureOperation, soviteVariableUpdateCallback, pumiceDialogManager);
                    return screenshotWithHighlightView;
                }
            }
            return null;
    }

    private Drawable getDrawableFromFile(File file) {
        String path = file.getAbsolutePath();
        //Drawable drawable = Drawable.createFromPath(path);

        Bitmap bmp = BitmapFactory.decodeFile(path);
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        bmp.setDensity(dm.densityDpi);
        Drawable drawable = new BitmapDrawable(context.getResources(), bmp);

        return drawable;
    }

    private Drawable getVisualThumbnailForScript(SugiliteStartingBlock script) {
        //1. get the last available screenshot (recursively expand get_procedure calls)
        File screenshotFile = null;

        try {
            screenshotFile = getLastAvailableScreenshotInSubsequentScript(script, null);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (screenshotFile != null) {
            return getDrawableFromFile(screenshotFile);
        }

        return null;
    }

    private File getLastAvailableScreenshotInSubsequentScript(SugiliteBlock script, File lastAvailableScreenshot) throws Exception {
        if (script == null) {
            return lastAvailableScreenshot;
        }

        if (script instanceof SugiliteStartingBlock && ((SugiliteStartingBlock) script).screenshotOnEnd != null) {
            return ((SugiliteStartingBlock) script).screenshotOnEnd;
        }

        if (script.getScreenshot() != null) {
            // update the screenshot if available
            lastAvailableScreenshot = script.getScreenshot();
        }

        if (script instanceof SugiliteConditionBlock) {
            // handle condition block
            File currentLastAvailableScreenshotInThenBlock = getLastAvailableScreenshotInSubsequentScript(((SugiliteConditionBlock) script).getThenBlock(), lastAvailableScreenshot);
            if (currentLastAvailableScreenshotInThenBlock != null) {
                lastAvailableScreenshot = currentLastAvailableScreenshotInThenBlock;
            }
        }
        if (script instanceof SugiliteOperationBlock && ((SugiliteOperationBlock) script).getOperation() instanceof SugiliteGetProcedureOperation) {
            // handle get_procedure calls
            String subScriptName = ((SugiliteGetProcedureOperation) ((SugiliteOperationBlock) script).getOperation()).evaluate(sugiliteData);
            SugiliteStartingBlock subScript = sugiliteScriptDao.read(subScriptName);
            File currentLastAvailableScreenshotInSubBlock = getLastAvailableScreenshotInSubsequentScript(subScript, lastAvailableScreenshot);
            if (currentLastAvailableScreenshotInSubBlock != null) {
                lastAvailableScreenshot = currentLastAvailableScreenshotInSubBlock;
            }

        }
        File currentLastAvailableScreenshotInNextBlock = getLastAvailableScreenshotInSubsequentScript(script.getNextBlock(), lastAvailableScreenshot);
        if (currentLastAvailableScreenshotInNextBlock != null) {
            lastAvailableScreenshot = currentLastAvailableScreenshotInNextBlock;
        }
        return lastAvailableScreenshot;
    }

    public SerializableUISnapshot getLastAvailableUISnapshotInSubsequentScript(SugiliteBlock script, SerializableUISnapshot lastAvailableUISnapshot) {
        if (script instanceof SugiliteStartingBlock && ((SugiliteStartingBlock) script).uiSnapshotOnEnd != null) {
            return ((SugiliteStartingBlock) script).uiSnapshotOnEnd;
        }

        if (script == null) {
            return lastAvailableUISnapshot;
        }

        if (script instanceof SugiliteOperationBlock) {
            if (((SugiliteOperationBlock) script).getSugiliteBlockMetaInfo() != null && ((SugiliteOperationBlock) script).getSugiliteBlockMetaInfo().getUiSnapshot() != null) {
                lastAvailableUISnapshot = ((SugiliteOperationBlock) script).getSugiliteBlockMetaInfo().getUiSnapshot();
            }
        }

        if (script instanceof SugiliteConditionBlock) {
            // handle condition block
            SerializableUISnapshot currentLastAvailableUISnapshotInThenBlock = getLastAvailableUISnapshotInSubsequentScript(((SugiliteConditionBlock) script).getThenBlock(), lastAvailableUISnapshot);
            if (currentLastAvailableUISnapshotInThenBlock != null) {
                lastAvailableUISnapshot = currentLastAvailableUISnapshotInThenBlock;
            }
        }

        if (script instanceof SugiliteOperationBlock && ((SugiliteOperationBlock) script).getOperation() instanceof SugiliteGetProcedureOperation) {
            // handle get_procedure calls
            String subScriptName = ((SugiliteGetProcedureOperation) ((SugiliteOperationBlock) script).getOperation()).evaluate(sugiliteData);
            try {
                SugiliteStartingBlock subScript = sugiliteScriptDao.read(subScriptName);
                SerializableUISnapshot currentLastAvailableUISnapshotInSubBlock = getLastAvailableUISnapshotInSubsequentScript(subScript, lastAvailableUISnapshot);
                if (lastAvailableUISnapshot != null) {
                    lastAvailableUISnapshot = currentLastAvailableUISnapshotInSubBlock;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        SerializableUISnapshot currentLastAvailableUISnapshotInNextBlock = getLastAvailableUISnapshotInSubsequentScript(script.getNextBlock(), lastAvailableUISnapshot);
        if (currentLastAvailableUISnapshotInNextBlock != null) {
            lastAvailableUISnapshot = currentLastAvailableUISnapshotInNextBlock;
        }

        return lastAvailableUISnapshot;
    }


    @Deprecated
    public List<Drawable> getVisualThumbnailDrawablesForBlock(SugiliteBlock block) {
        List<Drawable> drawableList = new ArrayList<>();
        if (block instanceof SugiliteStartingBlock) {
            //handle scripts
            drawableList.add(getVisualThumbnailForScript((SugiliteStartingBlock) block));
            return drawableList;
        }

        if (block instanceof SugiliteOperationBlock) {
            if (block.getScreenshot() != null) {
                //return the screenshot for the operation if available
                drawableList.add(getDrawableFromFile(block.getScreenshot()));
                return drawableList;
            }

            if (((SugiliteOperationBlock) block).getOperation() != null &&
                    ((SugiliteOperationBlock) block).getOperation() instanceof SugiliteGetProcedureOperation) {
                //special handling for SugiliteGetProcedureOperation
                SugiliteGetProcedureOperation getProcedureOperation = (SugiliteGetProcedureOperation) ((SugiliteOperationBlock) block).getOperation();

                List<VariableValue<String>> variableValuesFromGetProcedureOperation = getProcedureOperation.getVariableValues();
                List<VariableValue<String>> variableValues = new ArrayList<>();

                //compare variableValuesFromGetProcedureOperation against alternative values in the script
                try {
                    String subScriptName = getProcedureOperation.evaluate(sugiliteData);
                    SugiliteStartingBlock subScript = sugiliteScriptDao.read(subScriptName);
                    Map<String, Set<VariableValue>> variableNameAlternativeValueMap = subScript.variableNameAlternativeValueMap;
                    for (VariableValue<String> variableValueFromGetProcedureOperation : variableValuesFromGetProcedureOperation) {
                        if (variableNameAlternativeValueMap.containsKey(variableValueFromGetProcedureOperation.getVariableName())) {
                            for (VariableValue alternativeValue : variableNameAlternativeValueMap.get(variableValueFromGetProcedureOperation.getVariableName())) {
                                if (alternativeValue.getVariableValue().equals(variableValueFromGetProcedureOperation.getVariableValue())) {
                                    variableValues.add(alternativeValue);
                                    break;
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }


                if (variableValues != null && variableValues.size() > 0) {
                    /*
                    Drawable combinedVariableDrawable = getDrawableFromVariableList(variableValues);
                    if (combinedVariableDrawable != null) {
                        return combinedVariableDrawable;
                    }
                    */
                    drawableList = getDrawableListFromVariableList(variableValues);
                    if (drawableList != null && drawableList.size() > 0) {
                        return drawableList;
                    }
                }
            }
        }
        try {
            //other situations
            File screenshotFile = getLastAvailableScreenshotInSubsequentScript(block, null);
            drawableList.add(getDrawableFromFile(screenshotFile));
            return drawableList;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    //TODO: generate view instead of Drawable
    @Deprecated
    private Drawable highlightNodeOnDrawable(Node node, Drawable drawable) {
        //visually highlight the node on the drawable
        NavigationBarUtil navigationBarUtil = new NavigationBarUtil();
        int statusBarHeight = navigationBarUtil.getStatusBarHeight(context);
        int navBarHeight = navigationBarUtil.getNavigationBarHeight(context);
        int strokeWidth = 40;
        int halfStrokeWidth = strokeWidth / 2;

        Rect screenBounding = Rect.unflattenFromString(node.getBoundsInScreen());

        RectShape strokeRectShape = new RectShape();
        ShapeDrawable strokeShapeDrawable = new ShapeDrawable(strokeRectShape);
        strokeShapeDrawable.getPaint().setColor(0x8CFF0000);
        strokeShapeDrawable.getPaint().setStyle(Paint.Style.STROKE);
        strokeShapeDrawable.setIntrinsicHeight(screenBounding.height() - strokeWidth);
        strokeShapeDrawable.setIntrinsicWidth(screenBounding.width() - strokeWidth);
        strokeShapeDrawable.getPaint().setStrokeWidth(strokeWidth);

        RectShape fillRectShape = new RectShape();
        ShapeDrawable fillShapeDrawable = new ShapeDrawable(fillRectShape);
        fillShapeDrawable.getPaint().setColor(0x8CFFFF00);
        fillShapeDrawable.getPaint().setStyle(Paint.Style.FILL);
        fillShapeDrawable.setIntrinsicHeight(screenBounding.height() - 2 * strokeWidth);
        fillShapeDrawable.setIntrinsicWidth(screenBounding.width() - 2 * strokeWidth);

        Drawable[] stackingDrawables = new Drawable[3];
        stackingDrawables[0] = drawable;
        stackingDrawables[1] = strokeShapeDrawable;
        stackingDrawables[2] = fillShapeDrawable;

        LayerDrawable layerDrawable = new LayerDrawable(stackingDrawables);
        layerDrawable.setLayerGravity(0, Gravity.LEFT | Gravity.TOP);
        layerDrawable.setLayerGravity(1, Gravity.LEFT | Gravity.TOP);
        layerDrawable.setLayerGravity(2, Gravity.LEFT | Gravity.TOP);


        layerDrawable.setLayerInset(0, 0, 0, 0, 0);
        layerDrawable.setLayerInset(1, screenBounding.left + halfStrokeWidth, screenBounding.top + halfStrokeWidth, 0, 0);
        layerDrawable.setLayerInset(2, screenBounding.left + strokeWidth, screenBounding.top + strokeWidth, 0, 0);

        return layerDrawable;
    }

    @Deprecated
    private List<Drawable> getDrawableListFromVariableList(List<VariableValue<String>> variableValues) {
        List<Drawable> drawableList = new ArrayList<>();
        for (VariableValue<String> variableValue : variableValues) {
            if (variableValue.getVariableValueContext() != null) {
                if (variableValue.getVariableValueContext().getScreenshot() != null &&
                        variableValue.getVariableValueContext().getTargetNode() != null) {
                    File screenshotFile = variableValue.getVariableValueContext().getScreenshot();
                    Drawable screenshotDrawable = getDrawableFromFile(screenshotFile);
                    // add highlights of target node to the screenshot drawable
                    Drawable screenshotWithHighlightDrawable = highlightNodeOnDrawable(variableValue.getVariableValueContext().getTargetNode(), screenshotDrawable);
                    drawableList.add(screenshotWithHighlightDrawable);
                }
            }
        }
        return drawableList;
    }

    @Deprecated
    private Drawable getACombinedDrawableFromVariableList(List<VariableValue<String>> variableValues) {
        List<Drawable> drawableList = getDrawableListFromVariableList(variableValues);
        //create a combined drawable from drawableList
        Drawable[] drawableArray = new Drawable[drawableList.size()];
        drawableArray = drawableList.toArray(drawableArray);
        LayerDrawable layerDrawable = new LayerDrawable(drawableArray);
        int current_l = 0;
        int current_r = 0;
        int padding = 10;

        for (Drawable drawable : drawableList) {
            current_r += drawable.getIntrinsicWidth();
        }
        current_r += (padding * drawableArray.length - 1);


        if (drawableArray.length > 0) {
            for (int i = 0; i < drawableArray.length; i++) {
                current_r -= drawableArray[i].getIntrinsicWidth();
                layerDrawable.setLayerInset(i, current_l, 0, current_r, 0);
                current_l += drawableArray[i].getIntrinsicWidth();
                current_l += padding;
                current_r -= padding;
            }
            return layerDrawable;
        }
        return null;
    }

}
