package edu.cmu.hcii.sugilite.model.block;

import android.graphics.Rect;
import android.view.accessibility.AccessibilityNodeInfo;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;

import edu.cmu.hcii.sugilite.automation.Automator;

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
            childText = new HashSet<>();
            childContentDescription = new HashSet<>();
            childViewId = new HashSet<>();
            this.text = (nodeInfo.getText() == null ? null : nodeInfo.getText().toString());
            this.contentDescription = (nodeInfo.getContentDescription() == null ? null : nodeInfo.getContentDescription().toString());
            this.viewId = nodeInfo.getViewIdResourceName();
            this.isClickable = nodeInfo.isClickable();
            Rect parentRect = new Rect(), inScreenRect = new Rect();
            nodeInfo.getBoundsInParent(parentRect);
            nodeInfo.getBoundsInScreen(inScreenRect);
            this.boundsInParent = parentRect.flattenToString();
            this.boundsInScreen = inScreenRect.flattenToString();
            this.className = (nodeInfo.getClassName() == null ? null : nodeInfo.getClassName().toString());
            this.packageName = (nodeInfo.getPackageName() == null ? null : nodeInfo.getPackageName().toString());
            List<AccessibilityNodeInfo> children = Automator.preOrderTraverse(nodeInfo);
            for(AccessibilityNodeInfo node : children){
                if(node.getText() != null)
                    childText.add(node.getText().toString());
                if(node.getContentDescription() != null)
                    childContentDescription.add(node.getContentDescription().toString());
                if(node.getViewIdResourceName() != null)
                    childViewId.add(node.getViewIdResourceName());
            }
        }
    }
    public String text, contentDescription, viewId, boundsInParent, boundsInScreen, className, packageName;
    public boolean isClickable;
    public HashSet<String> childText, childContentDescription, childViewId;
}
