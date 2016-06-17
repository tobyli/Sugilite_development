package edu.cmu.hcii.sugilite.model.block;

import android.graphics.Rect;
import android.view.accessibility.AccessibilityNodeInfo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author toby
 * @date 4/21/16
 * @time 4:48 PM
 */

//NOTE: only use this filter when AccessibilityNodeInfo.find...byText or find...byViewId can't be used, as this filter is slower.

public class UIElementMatchingFilter implements Serializable {

    String text;
    String contentDescription;
    String viewId;
    String packageName;
    String className;
    String boundsInScreen;
    String boundsInParent;
    UIElementMatchingFilter parentFilter;
    UIElementMatchingFilter childFilter;
    Boolean isClickable;

    public UIElementMatchingFilter(){


    }

    /**
     *
     * @param text true if the content of text of the UI element equals to text
     * @return
     */
    public UIElementMatchingFilter setText(String text){
        this.text = text;
        return this;
    }

    /**
     *
     * @param contentDescription true if the content of the contentDescription of the UI element equals to contentDescription
     * @return
     */
    public UIElementMatchingFilter setContentDescription(String contentDescription){
        this.contentDescription = contentDescription;
        return this;
    }

    /**
     *
     * @param viewId true if the content of the view id of the UI element equals to viewId
     * @return
     */
    public UIElementMatchingFilter setViewId(String viewId){
        this.viewId = viewId;
        return this;
    }

    /**
     *
     * @param parentFilter true if parentFilter returns true for the parent of the UI element
     * @return
     */
    public UIElementMatchingFilter setParentFilter(UIElementMatchingFilter parentFilter){
        this.parentFilter = parentFilter;
        return this;
    }

    /**
     *
     * @param clickable true if the clickable property of the UI element equals to clickable
     * @return
     */
    public UIElementMatchingFilter setIsClickable(boolean clickable){
        this.isClickable = clickable;
        return this;
    }

    /**
     *
     * @param childFilter true if childFilter returns true for ANY of the children of the UI element
     * @return
     */
    public UIElementMatchingFilter setChildFilter(UIElementMatchingFilter childFilter){
        this.childFilter = childFilter;
        return this;
    }

    /**
     *
     * @param packageName true if the package name of the UI element equals to packageName
     * @return
     */
    public UIElementMatchingFilter setPackageName(String packageName){
        this.packageName = packageName;
        return this;
    }

    /**
     *
     * @param className true if the class name of the UI element equals to className
     * @return
     */
    public UIElementMatchingFilter setClassName(String className){
        this.className = className;
        return this;
    }
    //TODO: implement contains & intersect
    /**
     *
     * @param boundsInParent true if the bounds in parent of the UI element equals to boundsInParent
     * @return
     */
    public UIElementMatchingFilter setBoundsInParent(Rect boundsInParent){
        this.boundsInParent = boundsInParent.flattenToString();
        return this;
    }

    /**
     *
     * @param boundsInScreen true if the bounds in screen of the UI element equals to boundsInScreen
     * @return
     */
    public UIElementMatchingFilter setBoundsInScreen (Rect boundsInScreen){
        this.boundsInScreen = boundsInScreen.flattenToString();
        return this;
    }

    public boolean filter (AccessibilityNodeInfo nodeInfo) {
        if (nodeInfo == null)
            return false;
        if (text != null && (nodeInfo.getText() == null || (!text.contentEquals(nodeInfo.getText()))))
            return false;
        if (contentDescription != null && (nodeInfo.getContentDescription() == null || (!contentDescription.contentEquals(nodeInfo.getContentDescription()))))
            return false;
        if (packageName != null && (nodeInfo.getPackageName() == null || !packageName.contentEquals(nodeInfo.getPackageName())))
            return false;
        if (className != null && (nodeInfo.getClassName() == null || !className.contentEquals(nodeInfo.getClassName())))
            return false;
        if (viewId != null && (nodeInfo.getViewIdResourceName() == null || (!viewId.contentEquals(nodeInfo.getViewIdResourceName()))))
            return false;
        if (parentFilter != null && (!parentFilter.filter(nodeInfo.getParent())))
            return false;
        if (isClickable != null && isClickable.booleanValue() != nodeInfo.isClickable())
            return false;
        Rect boundsInParent = new Rect(), boundsInScreen = new Rect();
        nodeInfo.getBoundsInParent(boundsInParent);
        nodeInfo.getBoundsInScreen(boundsInScreen);

        if (this.boundsInParent != null && (boundsInParent == null || (!this.boundsInParent.contentEquals(boundsInParent.flattenToString()))))
            return false;
        if (this.boundsInScreen != null && (boundsInScreen == null || (!this.boundsInScreen.contentEquals(boundsInScreen.flattenToString()))))
            return false;


        boolean childFilterFlag = false;
        if(childFilter == null){
            childFilterFlag = true;
        }
        else {
            int childCouint = nodeInfo.getChildCount();
            for (int i = 0; i < childCouint; i++) {
                if (childFilter.filter(nodeInfo.getChild(i))) {
                    childFilterFlag = true;
                    break;
                }
            }
        }
        if(childFilterFlag == false)
            return false;

        return true;
    }

    public List<AccessibilityNodeInfo> filter (List<AccessibilityNodeInfo> nodeInfos){
        List<AccessibilityNodeInfo> retList = new ArrayList<>();
        for(AccessibilityNodeInfo node : nodeInfos){
            if(filter(node))
                retList.add(node);
        }
        return retList;
    }

}
