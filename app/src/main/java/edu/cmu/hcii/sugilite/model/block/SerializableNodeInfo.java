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
        childText = new HashSet<>();
        childContentDescription = new HashSet<>();
        childViewId = new HashSet<>();

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

    public boolean isTheSameNode(SerializableNodeInfo node){
        if((this.text != null && node.text != null && !this.text.contentEquals(node.text)) ||
                (this.text == null && node.text != null) ||
                (this.text != null && node.text == null))
            return false;
        if((this.contentDescription != null && node.contentDescription != null && !this.contentDescription.contentEquals(node.contentDescription)) ||
                (this.contentDescription == null && node.contentDescription != null) ||
                (this.contentDescription != null && node.contentDescription == null))
            return false;
        if((this.viewId != null && node.viewId != null && !this.viewId.contentEquals(node.viewId)) ||
                (this.viewId == null && node.viewId != null) ||
                (this.viewId != null && node.viewId == null))
            return false;
        if((this.packageName != null && node.packageName != null && !this.packageName.contentEquals(node.packageName)) ||
                (this.packageName == null && node.packageName != null) ||
                (this.packageName != null && node.packageName == null))
            return false;
        if((this.className != null && node.className != null && !this.className.contentEquals(node.className)) ||
                (this.className == null && node.className != null) ||
                (this.className != null && node.className == null))
            return false;
        if(this.isClickable != node.isClickable)
            return false;
        for(String text : this.childText){
            if(!node.childText.contains(text))
                return false;
        }
        for(String contentDescription : this.childContentDescription){
            if(!node.childContentDescription.contains(contentDescription))
                return false;
        }
        for(String viewId : this.childViewId){
            if(!node.childViewId.contains(viewId))
                return false;
        }
        if(this.childText.size() != node.childText.size())
            return false;
        if(this.childContentDescription.size() != node.childContentDescription.size())
            return false;
        if(this.childViewId.size() != node.childViewId.size())
            return false;
        Rect boundsInParentRect = Rect.unflattenFromString(boundsInParent);
        Rect boundsInScreenRect = Rect.unflattenFromString(boundsInScreen);

        if((boundsInParentRect.height() != Rect.unflattenFromString(node.boundsInParent).height()) || (boundsInParentRect.width() != Rect.unflattenFromString(node.boundsInParent).width())) {
            if(!(boundsInParentRect.contains(Rect.unflattenFromString(node.boundsInParent)) || Rect.unflattenFromString(node.boundsInParent).contains(boundsInParentRect)))
                return false;

        }

        if((boundsInScreenRect.height() != Rect.unflattenFromString(node.boundsInScreen).height()) || (boundsInScreenRect.width() != Rect.unflattenFromString(node.boundsInScreen).width())) {
            if(!(boundsInScreenRect.contains(Rect.unflattenFromString(node.boundsInScreen)) || Rect.unflattenFromString(node.boundsInScreen).contains(boundsInScreenRect)))
                return false;
        }


        return true;
    }

    public String text, contentDescription, viewId, boundsInParent, boundsInScreen, className, packageName;
    public boolean isClickable;
    public HashSet<String> childText, childContentDescription, childViewId;
}
