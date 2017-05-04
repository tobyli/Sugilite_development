package edu.cmu.hcii.sugilite.recording;

import android.graphics.Rect;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.model.block.SugiliteAvailableFeaturePack;
import edu.cmu.hcii.sugilite.model.block.UIElementMatchingFilter;

/**
 * Created by tiffanycai on 3/27/17.
 */

public class SugiliteTextboxHandler {
    private SugiliteData sugiliteData;
    private AccessibilityNodeInfo lastTextbox;
    private SugiliteAvailableFeaturePack featurePack;
    private UIElementMatchingFilter filter;

    public void handle(AccessibilityEvent event, AccessibilityNodeInfo sourceNode){
        lastTextbox = sourceNode;

        if(lastTextbox.getContentDescription() != null) {
            sugiliteData.lastTextboxFeature.contentDescription =
                    lastTextbox.getContentDescription().toString();
        }
        if(lastTextbox.getPackageName() != null) {
            sugiliteData.lastTextboxFeature.packageName =
                    lastTextbox.getPackageName().toString();
        }
        if(lastTextbox.getViewIdResourceName() != null) {
            sugiliteData.lastTextboxFeature.viewId =
                    lastTextbox.getViewIdResourceName().toString();
        }

        Rect textboxRect = new Rect();
        lastTextbox.getBoundsInScreen(textboxRect);
        sugiliteData.lastTextboxFeature.boundsInScreen = textboxRect.flattenToString();

    }



}
