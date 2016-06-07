package edu.cmu.hcii.sugilite.model;

import android.view.accessibility.AccessibilityNodeInfo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author toby
 * @date 6/7/16
 * @time 2:20 PM
 */
public class AccessibilityNodeInfoList implements Serializable {
    public List<AccessibilityNodeInfo> list;
    public AccessibilityNodeInfoList(){
        list = new ArrayList<>();
    }
}
