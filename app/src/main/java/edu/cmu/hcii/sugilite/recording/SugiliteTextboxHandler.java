package edu.cmu.hcii.sugilite.recording;

import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.model.block.SugiliteAvailableFeaturePack;

/**
 * Created by tiffanycai on 3/27/17.
 */

public class SugiliteTextboxHandler {
    private SugiliteData sugiliteData;
    private AccessibilityNodeInfo lastTextbox;

    public void handle(AccessibilityEvent event, AccessibilityNodeInfo sourceNode){
        lastTextbox = sourceNode;
        save(operationBlock);
    }


    private void save(SugiliteOperationBlock operationBlock){

    }

}
