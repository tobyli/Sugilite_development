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
    public SugiliteAvailableFeaturePack(SugiliteAvailableFeaturePack featurePack){
        this.packageName = featurePack.packageName;
        this.className = featurePack.className;
        this.text = featurePack.text;
        this.contentDescription = featurePack.contentDescription;
        this.viewId = featurePack.viewId;
        this.boundsInParent = featurePack.boundsInParent;
        this.boundsInScreen = featurePack.boundsInScreen;
        this.isEditable = featurePack.isEditable;
        this.time = featurePack.time;
        this.eventType = featurePack.eventType;
        this.screenshot = featurePack.screenshot;
        this.parentNode = featurePack.parentNode;
        this.childNodes = featurePack.childNodes;
        this.allNodes = featurePack.allNodes;
    }
    public String packageName, className, text, contentDescription, viewId, boundsInParent, boundsInScreen;
    public boolean isEditable;
    public long time;
    public int eventType;
    public File screenshot;
    public AccessibilityNodeInfo parentNode;
    public AccessibilityNodeInfoList childNodes, allNodes;
}
