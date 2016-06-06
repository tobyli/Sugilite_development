package edu.cmu.hcii.sugilite.model;

import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityRecord;

import java.lang.String;import java.sql.Time;
import java.util.List;

/**
 * @author toby
 * @date 11/3/15
 * @time 10:32 AM
 */
public abstract class AbstractTrackingEvent {

    protected String eventType;
    protected String sourcePackageName;
    protected String sourceClassName;
    protected List<String> text;
    protected String viewId;
    protected long eventTime;
    protected String wifiSSID;
    public static final String ABSTRACT_EVENT = "ABSTRACT_EVENT";
    public static final String VIEW_CLICKED = "VIEW_CLICKED";
    public static final String VIEW_FOCUSED = "VIEW_FOCUSED";
    public static final String VIEW_LONG_CLICKED = "VIEW_LONG_CLICKED";
    public static final String VIEW_SELECTED = "VIEW_SELECTED";
    public static final String VIEW_TEXT_CHANGED = "VIEW_TEXT_CHANGED";
    public static final String VIEW_TEXT_SELECTION_CHANGED = "VIEW_TEXT_SELECTION_CHANGED";
    public static final String WINDOW_STATE_CHANGED = "WINDOW_STATE_CHANGED";
    public static final String SCREEN_ON = "SCREEN_ON";
    public static final String SCREEN_OFF = "SCREEN_OFF";
    public AccessibilityEvent accessibilityEvent;

    public String getEventType(){
        return eventType;
    }
    public String getViewId() {return viewId;}
    public void setViewId(String viewId) {this.viewId = viewId;}
    public String getSourceClassName() {return sourceClassName; }
    public String getSourcePackageName(){
        return sourcePackageName;
    }
    public String getWifiSSID() {return wifiSSID;}
    public long getEventTime(){
        return eventTime;
    }
    public abstract String getContentDescription();
    public abstract List<String> getText();



}
