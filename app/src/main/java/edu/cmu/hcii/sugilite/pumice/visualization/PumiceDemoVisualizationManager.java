package edu.cmu.hcii.sugilite.pumice.visualization;

import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Pair;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import java.util.Set;


import edu.cmu.hcii.sugilite.model.Node;
import edu.cmu.hcii.sugilite.ontology.SugiliteEntity;
import edu.cmu.hcii.sugilite.ontology.SugiliteRelation;
import edu.cmu.hcii.sugilite.ontology.SugiliteTriple;
import edu.cmu.hcii.sugilite.ontology.UISnapshot;
import edu.cmu.hcii.sugilite.ontology.helper.annotator.SugiliteTextParentAnnotator;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.VerbalInstructionOverlayManager;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.util.NavigationBarUtil;

import static edu.cmu.hcii.sugilite.Const.OVERLAY_TYPE;

/**
 * @author toby
 * @date 2/24/19
 * @time 7:58 PM
 */

/**
 * class used for showing visualization in pumice for value demonstration (e.g., when the user demonstrates something that's comparable to "30 degrees fahrenheit", highlight screen values that represent temperatures)
 */
public class PumiceDemoVisualizationManager {
    private Context context;
    private Set<View> currentDisplayedOverlays;
    private WindowManager windowManager;
    private NavigationBarUtil navigationBarUtil;
    private PumiceDemoVisualizationView pumiceDemoVisualizationView;

    public PumiceDemoVisualizationManager(Context context) {
        this.context = context;
        this.currentDisplayedOverlays = new HashSet<>();
        this.windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        this.navigationBarUtil = new NavigationBarUtil();
        this.pumiceDemoVisualizationView = new PumiceDemoVisualizationView(context);

        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                OVERLAY_TYPE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT);

        DisplayMetrics displaymetrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(displaymetrics);

        layoutParams.gravity = Gravity.TOP | Gravity.LEFT;
        layoutParams.x = 0;
        layoutParams.y = 0;
        layoutParams.width = displaymetrics.widthPixels;
        layoutParams.height = displaymetrics.heightPixels;
        addOverlay(new Pair<>(pumiceDemoVisualizationView, layoutParams));
    }

    /**
     * refresh the overlays based on a new UISnapshot
     *
     * @param uiSnapshot
     */
    public void refreshBasedOnSnapshotAndRelationType(UISnapshot uiSnapshot, SugiliteRelation sugiliteRelation) {
        pumiceDemoVisualizationView.clearRects();
        pumiceDemoVisualizationView.addRects(getOverlaysBasedOnSnapshotAndRelationType(uiSnapshot, sugiliteRelation), 0x80FF0000);
        pumiceDemoVisualizationView.invalidate();
    }

    private List<Rect> getOverlaysBasedOnSnapshotAndRelationType(UISnapshot uiSnapshot, SugiliteRelation sugiliteRelation) {
        List<Rect> results = new ArrayList<>();

        //first annotate entities in the UISnapshot
        uiSnapshot.annotateStringEntitiesIfNeeded();

        Set<SugiliteTriple> matchedTriples = uiSnapshot.getPredicateTriplesMap().get(sugiliteRelation.getRelationId());
        if (matchedTriples != null) {
            for (SugiliteTriple stringRelationTriple : matchedTriples) {
                if (stringRelationTriple.getSubject() != null && stringRelationTriple.getSubject() instanceof SugiliteEntity && stringRelationTriple.getSubject().getEntityValue() instanceof Node) {
                    //get node entities that contain the target string entities
                    SugiliteEntity nodeEntity = stringRelationTriple.getSubject();

                    Set<SugiliteTriple> hasTextTriplesFromNodeEntity = uiSnapshot.getSubjectPredicateTriplesMap().get(new AbstractMap.SimpleEntry<>(nodeEntity.getEntityId(), SugiliteRelation.HAS_TEXT.getRelationId()));
                    if (hasTextTriplesFromNodeEntity == null || hasTextTriplesFromNodeEntity.size() == 0) {
                        continue;
                    }
                    boolean relationFoundFlag = false;
                    for (SugiliteTriple hasTextTriple : hasTextTriplesFromNodeEntity) {
                        SugiliteEntity textEntity = hasTextTriple.getObject();
                        if (textEntity != null && textEntity.getEntityValue() != null && textEntity.getEntityValue() instanceof String) {
                            String stringValue = textEntity.getEntityValue().toString();
                            if (checkIfStringHasRelation(stringValue, sugiliteRelation)) {
                                relationFoundFlag = true;
                                break;
                            }
                        }
                    }
                    if (relationFoundFlag) {
                        Node matchedNode = (Node) nodeEntity.getEntityValue();
                        if (matchedNode.getBoundsInScreen() != null) {
                            results.add(Rect.unflattenFromString(matchedNode.getBoundsInScreen()));
                        }
                    }
                }
            }
        }
        return results;
    }

    private boolean checkIfStringHasRelation(String string, SugiliteRelation sugiliteRelation) {
        SugiliteTextParentAnnotator sugiliteTextParentAnnotator = SugiliteTextParentAnnotator.getInstance();
        List<SugiliteTextParentAnnotator.AnnotatingResult> annotatingResults = sugiliteTextParentAnnotator.annotate(string);
        for (SugiliteTextParentAnnotator.AnnotatingResult annotatingResult : annotatingResults) {
            if (annotatingResult.getRelation().equals(sugiliteRelation)) {
                return true;
            }
        }
        return false;
    }

    private void removeAllOverlays() {
        removeOverlays(currentDisplayedOverlays);
        currentDisplayedOverlays.clear();
    }

    private void removeOverlays(Collection<View> overlays) {
        for (View view : overlays) {
            if (view != null && view.getWindowToken() != null) {
                windowManager.removeView(view);
            }
        }
    }

    @Deprecated
    private Pair<View, WindowManager.LayoutParams> getOverlayForMatchedNode(SugiliteEntity nodeEntity, Node matchedNode) {
        //create an overlay view for the node
        Rect boundsInScreen = Rect.unflattenFromString(matchedNode.getBoundsInScreen());
        View rectOverlayView = VerbalInstructionOverlayManager.getRectangleOverlay(context, 0x80FF0000, boundsInScreen.width(), boundsInScreen.height());

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
        System.out.println("Detected a status with height " + statusBarHeight);

        iconParams.gravity = Gravity.TOP | Gravity.LEFT;
        iconParams.x = boundsInScreen.left;
        iconParams.y = real_y;
        iconParams.width = boundsInScreen.width();
        iconParams.height = boundsInScreen.height();

        //TODO: need to add an onTouch listener to the overlay
        //addCrumpledPaperOnTouchListener(overlay, iconParams, displaymetrics, node, entityId, correspondingResult, allResults, serializableUISnapshot, utterance, windowManager, sugiliteData, sharedPreferences);

        return new Pair<>(rectOverlayView, iconParams);
    }

    private void addOverlay(Pair<View, WindowManager.LayoutParams> overlay) {
        //NEEDED TO BE CONFIGURED AT APPS->SETTINGS-DRAW OVER OTHER APPS on API>=23
        int currentApiVersion = android.os.Build.VERSION.SDK_INT;

        //add the overlay
        if (currentApiVersion >= 23) {
            VerbalInstructionOverlayManager.checkDrawOverlayPermission(context);
            if (Settings.canDrawOverlays(context)) {
                currentDisplayedOverlays.add(overlay.first);
                windowManager.addView(overlay.first, overlay.second);
            }
        } else {
            currentDisplayedOverlays.add(overlay.first);
            windowManager.addView(overlay.first, overlay.second);
        }
    }

}
