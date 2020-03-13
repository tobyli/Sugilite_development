package edu.cmu.hcii.sugilite.communication;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.google.gson.Gson;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

import edu.cmu.hcii.sugilite.Const;
import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.automation.AutomatorUtil;

import static edu.cmu.hcii.sugilite.Const.OVERLAY_TYPE;

public class SugiliteEventBroadcastingActivity extends AppCompatActivity {
    SharedPreferences sharedPreferences;
    static SugiliteData sugiliteData;
    static private Gson gson;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.sugiliteData = (SugiliteData)getApplication();
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        this.gson = new Gson();
        int messageType = 0;
        String arg1 = "";
        if (getIntent().getExtras() != null) {
            messageType = getIntent().getIntExtra("messageType", 0);
            arg1 = getIntent().getStringExtra("arg1");
            handleRequest(messageType, arg1);
        }
        finish();
    }

    private void handleRequest(int messageType, String arg1) {
        boolean broadcastingEnabled = sharedPreferences.getBoolean("broadcasting_enabled", false);
        switch (messageType) {
            case SugiliteCommunicationHelper.REGISTER:
                sugiliteData.registeredBroadcastingListener.add(arg1);
                Intent returnIntent = new Intent();
                returnIntent.putExtra("result", sugiliteData.registeredBroadcastingListener.toString());
                setResult(Activity.RESULT_OK, returnIntent);
                break;
            case SugiliteCommunicationHelper.UNREGISTER:
                if(sugiliteData.registeredBroadcastingListener.contains(arg1)) {
                    sugiliteData.registeredBroadcastingListener.remove(arg1);
                    Intent returnIntent2 = new Intent();
                    returnIntent2.putExtra("result", sugiliteData.registeredBroadcastingListener.toString());
                    setResult(Activity.RESULT_OK, returnIntent2);
                }
                break;
        }
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Current Registered Listener")
                .setMessage(sugiliteData.registeredBroadcastingListener.toString())
                .create();
        dialog.getWindow().setType(OVERLAY_TYPE);
        dialog.show();
    }

    static public class BroadcastingEvent implements Serializable{
        public BroadcastingEvent(AccessibilityEvent event){
            switch (event.getEventType()){
                case AccessibilityEvent.TYPE_VIEW_CLICKED:
                    eventType = "TYPE_VIEW_CLICKED";
                    break;
                case AccessibilityEvent.TYPE_VIEW_LONG_CLICKED:
                    eventType = "TYPE_VIEW_LONG_CLICKED";
                    break;
                case AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED:
                    eventType = "TYPE_VIEW_TEXT_CHANGED";
                    break;
                case AccessibilityEvent.TYPE_VIEW_FOCUSED:
                    eventType = "TYPE_VIEW_FOCUSED";
                    break;
            }
            Calendar calendar = Calendar.getInstance();
            SimpleDateFormat format = new SimpleDateFormat("dd-M-yyyy HH:mm:ss");
            time = format.format(calendar.getTime());
            //time
            AccessibilityNodeInfo node = event.getSource();
            if(node != null){
                if(node.getPackageName() != null)
                    packageName = node.getPackageName().toString();
                if(node.getClassName() != null)
                    className = node.getClassName().toString();
                if(node.getText() != null)
                    text = node.getText().toString();
                if(node.getContentDescription() != null)
                    contentDescription = node.getContentDescription().toString();
                if(node.getViewIdResourceName() != null)
                    viewId = node.getViewIdResourceName().toString();
                Rect boundsInParentRect = new Rect(), boundsInScreenRect = new Rect();
                node.getBoundsInParent(boundsInParentRect);
                node.getBoundsInScreen(boundsInScreenRect);
                boundsInParent = boundsInParentRect.flattenToString();
                boundsInScreen = boundsInScreenRect.flattenToString();
                Set<String> childTextSet = new HashSet<>();
                Set<String> childContentDescriptionSet = new HashSet<>();
                for(AccessibilityNodeInfo child : AutomatorUtil.preOrderTraverse(node)){
                    if(child.getText() != null)
                        childTextSet.add(child.getText().toString());
                    if(child.getContentDescription() != null)
                        childContentDescriptionSet.add(child.getContentDescription().toString());
                }
                if(childTextSet.size() > 0)
                    childText = childTextSet.toString();
                if(childContentDescriptionSet.size() > 0)
                    childContentDescription = childContentDescriptionSet.toString();
            }
        }

        public String eventType, packageName, className, text, contentDescription, viewId, boundsInParent, boundsInScreen;
        public String childText, childContentDescription;
        public String time;
    }




}

