package edu.cmu.hcii.sugilite.model.block;

import android.view.accessibility.AccessibilityNodeInfo;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import edu.cmu.hcii.sugilite.model.AccessibilityNodeInfoList;

/**
 * Created by toby on 6/28/16.
 */
public class SugiliteAvailableFeaturePack implements Serializable{
    public SugiliteAvailableFeaturePack(){
        //do nothing
    }
    public SugiliteAvailableFeaturePack(SugiliteAvailableFeaturePack featurePack){
        this.packageName = new String(featurePack.packageName);
        this.className = new String(featurePack.className);
        this.text = new String(featurePack.text);
        this.contentDescription = new String(featurePack.contentDescription);
        this.viewId = new String(featurePack.viewId);
        this.boundsInParent = new String(featurePack.boundsInParent);
        this.boundsInScreen = new String(featurePack.boundsInScreen);
        this.isEditable = featurePack.isEditable;
        this.time = featurePack.time;
        this.eventType = featurePack.eventType;
        this.screenshot = featurePack.screenshot;
        this.parentNode = featurePack.parentNode;
        this.childNodes = new ArrayList<>(featurePack.childNodes);
        this.allNodes = new ArrayList<>(featurePack.allNodes);
        if(featurePack.alternativeChildTextList != null)
            this.alternativeChildTextList = new HashSet<>(featurePack.alternativeChildTextList);
        else
            this.alternativeChildTextList = new HashSet<>();
        if(featurePack.alternativeTextList != null)
            this.alternativeTextList = new HashSet<>(featurePack.alternativeTextList);
        else
            this.alternativeTextList = new HashSet<>();
    }
    public String packageName, className, text, contentDescription, viewId, boundsInParent, boundsInScreen;
    public boolean isEditable;
    public long time;
    public int eventType;
    public File screenshot;
    public SerializableNodeInfo parentNode;
    public ArrayList<SerializableNodeInfo> childNodes, allNodes;

    public Set<String> alternativeTextList;
    public Set<String> alternativeChildTextList;

}
