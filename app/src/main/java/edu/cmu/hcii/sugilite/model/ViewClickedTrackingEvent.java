package edu.cmu.hcii.sugilite.model;

import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityRecord;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by toby on 11/3/15.
 */
public class ViewClickedTrackingEvent extends AbstractTrackingEvent {
    //a "clickable" often has "text" if it's text based, or has a content description if it is an icon
    private String contentDescription;
    protected AccessibilityNodeInfo nodeInfo;
    protected List<AccessibilityRecord> recordList;
    public ViewClickedTrackingEvent(AccessibilityEvent event, long time, String wifiSSID){
        text = new ArrayList<>();
        for(CharSequence charSequence : event.getText()) {
            if(charSequence != null)
            text.add(charSequence.toString());
        }


        if(!(event.getContentDescription() == null))
            contentDescription = event.getContentDescription().toString();
        else
            contentDescription = null;

        if(!(event.getClassName() == null))
            this.sourceClassName = event.getClassName().toString();
        else
            this.sourceClassName = null;

        if(!(event.getPackageName() == null))
            this.sourcePackageName = event.getPackageName().toString();
        else
            this.sourcePackageName = null;

        this.eventType = VIEW_CLICKED;

        this.nodeInfo = event.getSource();

        this.eventTime = time;

        recordList = new ArrayList<>();
        for(int i = 0; i < event.getRecordCount(); i++){
            recordList.add(event.getRecord(i));
        }

        this.wifiSSID = wifiSSID;
    }
    @Override
    public List<String> getText(){
        return text;
    }
    @Override
    public String getContentDescription(){
        return contentDescription;
    }
    public AccessibilityNodeInfo getNodeInfo(){
        return nodeInfo;
    }
    public List<AccessibilityRecord> getRecordList(){
        return recordList;
    }



}
