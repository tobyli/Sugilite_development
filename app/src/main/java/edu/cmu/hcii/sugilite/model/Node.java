//package com.microsoft.userappaccessibilityactiontracer.model;
package edu.cmu.hcii.sugilite.model;

import android.graphics.Rect;
import android.view.accessibility.AccessibilityNodeInfo;

//import com.microsoft.userappaccessibilityactiontracer.Const;
//import com.microsoft.userappaccessibilityactiontracer.handler.TextLabelManager;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Created by toby on 5/24/2017.
 */

public class Node implements Serializable {

    private String text;
    private String contentDescription;
    private String viewId;
    private String packageName;
    private String className;
    private String boundsInScreen;
    private String boundsInParent;
//    private List<Node> childNodes;
    private Integer rootNodeDescendantsCount = 0;
    private Boolean isClickable = false;
    private Boolean isLongClickable = false;
    private Boolean isEditable = false;
    private Boolean isChecked = false;
    private Boolean isCheckable = false;
    private Boolean isSelected = false;
    private Boolean isFocused = false;
    private Boolean isEnabled = false;
    private Boolean isScrollable = false;
    private Node parent = null;
    private String eventManagerId; //unique view identifier added to AccessibilityNodeInfo
    private String TAG = Node.class.getCanonicalName();

//    private List<TextDistancePair> childrenTextLabels, nearbyTextLabels, holisticTextLabels;
    private Integer windowZIndex;
    private List<Integer> nodeZIndexSequence = new ArrayList<>();


    public Node(AccessibilityNodeInfo nodeInfo, Integer windowZIndex, List<Integer> parentNodeZIndexSequence){
        this(nodeInfo);
        this.windowZIndex = windowZIndex;
        this.nodeZIndexSequence = new ArrayList<>(parentNodeZIndexSequence);
        Collections.copy(this.nodeZIndexSequence, parentNodeZIndexSequence);
        //NOTE: AccessibilityNodeInfo.getDrawingOrder requires API Level 24 (Android 7.0)
        this.nodeZIndexSequence.add(nodeInfo.getDrawingOrder());
    }

    public Node(AccessibilityNodeInfo nodeInfo){
        if(nodeInfo == null){
            new Exception("null nodeinfo!").printStackTrace();
            return;
        }
        if(nodeInfo.getText() != null)
            text = nodeInfo.getText().toString();
        if(nodeInfo.getContentDescription() != null)
            contentDescription = nodeInfo.getContentDescription().toString();
        if(nodeInfo.getViewIdResourceName() != null)
            viewId = nodeInfo.getViewIdResourceName();
        if(nodeInfo.getPackageName() != null)
            packageName = nodeInfo.getPackageName().toString();
        if(nodeInfo.getClassName() != null)
            className = nodeInfo.getClassName().toString();
//        if(nodeInfo.getEventManagerId() != null)
//            eventManagerId = nodeInfo.getEventManagerId();

        Rect boundsInScreen = new Rect();
        Rect boundsInParent = new Rect();
        nodeInfo.getBoundsInScreen(boundsInScreen);
        nodeInfo.getBoundsInParent(boundsInParent);
        this.boundsInScreen = boundsInScreen.flattenToString();
        this.boundsInParent = boundsInParent.flattenToString();
        this.isClickable = nodeInfo.isClickable();
        this.isLongClickable = nodeInfo.isLongClickable();
        this.isEditable = nodeInfo.isEditable();
        this.isChecked = nodeInfo.isChecked();
        this.isSelected = nodeInfo.isSelected();
        this.isFocused = nodeInfo.isFocused();
        this.isChecked = nodeInfo.isChecked();
        this.isCheckable = nodeInfo.isCheckable();
        this.isScrollable = nodeInfo.isScrollable();
        // currently this causes a infinite loop constructor

//        childNodes = new ArrayList<>();
//        int childCount = nodeInfo.getChildCount();
//        for(int i = 0; i < childCount; i ++){
//            if(nodeInfo.getChild(i) != null)
//            childNodes.add(new Node(nodeInfo.getChild(i)));
//        }
        if(nodeInfo.getParent() != null) {
            parent = new Node(nodeInfo.getParent());
        }
//        TextLabelManager textLabelManager = new TextLabelManager();
//        this.childrenTextLabels = textLabelManager.getChildrenTextLabels(this);
//        if(rootNode != null) {
//            this.nearbyTextLabels = textLabelManager.getNearbyTextLabels(this, rootNode, Const.NEARBY_TEXT_LABEL_LIMIT);
//            rootNodeDescendantsCount = textLabelManager.traverseNode(rootNode).size();
//        }
//        holisticTextLabels = textLabelManager.getHolisticTextLabels(this);
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

    public String getPackageName() {
        return packageName;
    }

    public String getClassName() {
        return className;
    }

    public String getBoundsInScreen() {
        return boundsInScreen;
    }

    public String getBoundsInParent() {
        return boundsInParent;
    }

//    public List<Node> getChildNodes() {
//        return childNodes;
//    }

    public Boolean getClickable() {
        return isClickable;
    }

    public Boolean getEditable() {
        return isEditable;
    }

    public Boolean getChecked() {
        return isChecked;
    }

    public Boolean getCheckable() {
        return isCheckable;
    }

    public Boolean getEnabled() {
        return isEnabled;
    }

    public Boolean getFocused() {
        return isFocused;
    }

    public Boolean getSelected() {
        return isSelected;
    }

    public Boolean getScrollable() {
        return isScrollable;
    }

    public String getViewIdResourceName() {
        return viewId;
    }

    public String getEventManagerId(){
        return eventManagerId;
    }

    public List<Integer> getNodeZIndexSequence() {
        return nodeZIndexSequence;
    }

    public Integer getWindowZIndex() {
        return windowZIndex;
    }

    public Boolean getLongClickable() {
        return isLongClickable;
    }

    public Node getParent() {
        return parent;
    }
//    public List<TextDistancePair> getChildrenTextLabels() {
//        return childrenTextLabels;
//    }
//
//    public List<TextDistancePair> getNearbyTextLabels() {
//        return nearbyTextLabels;
//    }

    @Deprecated
    public boolean sameNode(Node obj) {
        if(packageName.equals(((Node) obj).getPackageName()) &&
                className.equals(((Node) obj).getClassName())){
            if(viewId == null || (viewId.equals(obj.getViewId())))
                return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.packageName, this.className, this.boundsInScreen, this.viewId);
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof Node){
            try {
                if (((this.packageName == null && ((Node) obj).packageName == null) || this.packageName.equals(((Node) obj).packageName)) &&
                        ((this.className == null && ((Node) obj).className == null) || this.className.equals(((Node) obj).className)) &&
                        ((this.boundsInScreen == null && ((Node) obj).boundsInScreen == null) || this.boundsInScreen.equals(((Node) obj).boundsInScreen)) &&
                        ((this.viewId == null && ((Node) obj).viewId == null) || this.viewId.equals(((Node) obj).viewId)))
                    return true;
            }
            catch (NullPointerException e){
                return false;
            }
        }
        return super.equals(obj);
    }
}
