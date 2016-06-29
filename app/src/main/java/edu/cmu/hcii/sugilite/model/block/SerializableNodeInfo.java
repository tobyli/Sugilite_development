package edu.cmu.hcii.sugilite.model.block;

import android.view.accessibility.AccessibilityNodeInfo;

import java.io.Serializable;

/**
 * @author toby
 * @date 6/29/16
 * @time 12:24 PM
 */
public class SerializableNodeInfo implements Serializable {
    public SerializableNodeInfo(){
        //nothing
    }
    public SerializableNodeInfo(String text, String contentDescription, String viewId, boolean isClickable){
        this.text = text;
        this.contentDescription = contentDescription;
        this.viewId = viewId;
        this.isClickable = isClickable;
    }
    public SerializableNodeInfo(AccessibilityNodeInfo nodeInfo){
        if(nodeInfo != null) {
            this.text = (nodeInfo.getText() == null ? null : nodeInfo.getText().toString());
            this.contentDescription = (nodeInfo.getContentDescription() == null ? null : nodeInfo.getContentDescription().toString());
            this.viewId = nodeInfo.getViewIdResourceName();
            this.isClickable = nodeInfo.isClickable();
        }
    }
    public String text, contentDescription, viewId;
    public boolean isClickable;
}
