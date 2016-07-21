package edu.cmu.hcii.sugilite.model.block;

import android.graphics.Rect;
import android.view.accessibility.AccessibilityNodeInfo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.cmu.hcii.sugilite.automation.Automator;
import edu.cmu.hcii.sugilite.model.variable.Variable;
import edu.cmu.hcii.sugilite.model.variable.VariableHelper;

/**
 * @author toby
 * @date 4/21/16
 * @time 4:48 PM
 */

//NOTE: only use this filter when AccessibilityNodeInfo.find...byText or find...byViewId can't be used, as this filter is slower.
public class UIElementMatchingFilter implements Serializable {

    private String text;
    private String contentDescription;
    private String viewId;
    private String packageName;
    private String className;
    private String boundsInScreen;
    private String boundsInParent;
    private UIElementMatchingFilter parentFilter;
    private UIElementMatchingFilter childFilter;
    private Boolean isClickable;
    public Set<Map.Entry<String, String>> alternativeLabels;

    //TODO: add possible alternatives

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

    public String getText(){
        return text;
    }

    public String getContentDescription(){
        return contentDescription;
    }

    public String getViewId(){
        return viewId;
    }

    public String getPackageName(){
        return packageName;
    }

    public String getClassName(){
        return className;
    }

    public String getBoundsInScreen(){
        return boundsInScreen;
    }

    public String getBoundsInParent(){
        return boundsInParent;
    }

    public UIElementMatchingFilter getParentFilter(){
        return parentFilter;
    }

    public UIElementMatchingFilter getChildFilter(){
        return childFilter;
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
            List<AccessibilityNodeInfo> nodes = Automator.preOrderTraverse(nodeInfo);
            for(AccessibilityNodeInfo node : nodes){
                if(childFilter.filter(node)){
                    childFilterFlag = true;
                    break;
                }
            }
        }
        if(childFilterFlag == false)
            return false;

        return true;
    }

    public boolean filter (SerializableNodeInfo nodeInfo){
        if (nodeInfo == null)
            return false;
        if (text != null && (nodeInfo.text == null || (!text.contentEquals(nodeInfo.text))))
            return false;
        if (contentDescription != null && (nodeInfo.contentDescription == null || (!contentDescription.contentEquals(nodeInfo.contentDescription))))
            return false;
        if (viewId != null && (nodeInfo.viewId == null || (!viewId.contentEquals(nodeInfo.viewId))))
            return false;
        if (isClickable != null && isClickable.booleanValue() != nodeInfo.isClickable)
            return false;

        return true;
    }

    public boolean filter (AccessibilityNodeInfo nodeInfo, VariableHelper helper) {
        if (nodeInfo == null)
            return false;
        if (text != null && (nodeInfo.getText() == null || (!helper.parse(text).contentEquals(nodeInfo.getText().toString()))))
            return false;
        if (contentDescription != null && (nodeInfo.getContentDescription() == null || (!helper.parse(contentDescription).contentEquals(nodeInfo.getContentDescription().toString()))))
            return false;
        if (packageName != null && (nodeInfo.getPackageName() == null || !helper.parse(packageName).contentEquals(nodeInfo.getPackageName().toString())))
            return false;
        if (className != null && (nodeInfo.getClassName() == null || !helper.parse(className).contentEquals(nodeInfo.getClassName().toString())))
            return false;
        if (viewId != null && (nodeInfo.getViewIdResourceName() == null || (!helper.parse(viewId).contentEquals(nodeInfo.getViewIdResourceName().toString()))))
            return false;
        if (parentFilter != null && (!parentFilter.filter(nodeInfo.getParent(), helper)))
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
            List<AccessibilityNodeInfo> nodes = Automator.preOrderTraverse(nodeInfo);
            for(AccessibilityNodeInfo node : nodes){
                if(childFilter.filter(node, helper)){
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

    public static List<AccessibilityNodeInfo> getClickableList (List<AccessibilityNodeInfo> nodeInfos){
        List<AccessibilityNodeInfo> retList = new ArrayList<>();
        for(AccessibilityNodeInfo node : nodeInfos){
            if(node.isClickable())
                retList.add(node);
        }
        return retList;
    }

}
