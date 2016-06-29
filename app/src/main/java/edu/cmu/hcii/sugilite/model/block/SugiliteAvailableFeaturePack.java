package edu.cmu.hcii.sugilite.model.block;

import android.view.accessibility.AccessibilityNodeInfo;

import java.io.File;

import edu.cmu.hcii.sugilite.model.AccessibilityNodeInfoList;

/**
 * Created by toby on 6/28/16.
 */
public class SugiliteAvailableFeaturePack {
    public SugiliteAvailableFeaturePack(){
        //do nothing
    }
    public String packageName, className, text, contentDescription, viewId, boundsInParent, boundsInScreen;
    public boolean isEditable;
    public long time;
    public int eventType;
    public File screenshot;
    public AccessibilityNodeInfo parentNode;
    public AccessibilityNodeInfoList childNodes, allNodes;
}
