package edu.cmu.hcii.sugilite.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.view.accessibility.AccessibilityNodeInfo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import edu.cmu.hcii.sugilite.model.block.SerializableNodeInfo;

/**
 * @author toby
 * @date 6/7/16
 * @time 2:20 PM
 */
public class AccessibilityNodeInfoList implements Parcelable {
    private ArrayList<AccessibilityNodeInfo> list;
    public AccessibilityNodeInfoList(ArrayList<AccessibilityNodeInfo> data){
        this.list = data;
    }
    public AccessibilityNodeInfoList(){
        this.list = new ArrayList<>();
    }
    public AccessibilityNodeInfoList(Parcel parcel){
        this.list = new ArrayList<>();
        readFromParcel(parcel);
    }
    @Override
    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeTypedList(list);
    }
    private void readFromParcel(Parcel in) {
        in.readTypedList(list, AccessibilityNodeInfo.CREATOR);
    }
    public static final Parcelable.Creator CREATOR =
            new Parcelable.Creator() {
                @Override
                public AccessibilityNodeInfoList[] newArray(int size) {
                    return new AccessibilityNodeInfoList[size];
                }

                @Override
                public AccessibilityNodeInfoList createFromParcel(Parcel source) {
                    return new AccessibilityNodeInfoList(source);
                }
            };
    public ArrayList<AccessibilityNodeInfo> getList(){
        return list;
    }
    public void setList(ArrayList<AccessibilityNodeInfo> list){
        this.list = list;
    }
    public ArrayList<SerializableNodeInfo> getSerializableList(){
        ArrayList<SerializableNodeInfo> retVal = new ArrayList<>();
        for(AccessibilityNodeInfo info : list){
            retVal.add(new SerializableNodeInfo((info.getText() == null ? null : info.getText().toString()), (info.getContentDescription() == null ? null : info.getContentDescription().toString()), info.getViewIdResourceName(), info.isClickable()));
        }
        return retVal;
    }

}
