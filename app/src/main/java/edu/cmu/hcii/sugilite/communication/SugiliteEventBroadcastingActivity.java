package edu.cmu.hcii.sugilite.communication;

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

import edu.cmu.hcii.sugilite.R;
import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.automation.Automator;

public class SugiliteEventBroadcastingActivity extends AppCompatActivity {
    SharedPreferences sharedPreferences;
    static SugiliteData sugiliteData;
    static private Gson gson;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sugilite_event_broadcasting);
        this.sugiliteData = (SugiliteData)getApplication();
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        this.gson = new Gson();
        String messageType = "", arg1 = "";
        if (getIntent().getExtras() != null) {
            messageType = getIntent().getStringExtra("messageType");
            arg1 = getIntent().getStringExtra("arg1");
        }
        finish();
    }

    private void handleRequest(String messageType, String arg1) {
        boolean trackingInProcess = sharedPreferences.getBoolean("tracking_in_process", false);
        switch (messageType) {
            case "REGISTER":
                sugiliteData.registeredBroadcastingListener.add(arg1);
                break;
            case "UNREGISTER":
                if(sugiliteData.registeredBroadcastingListener.contains(arg1))
                    sugiliteData.registeredBroadcastingListener.remove(arg1);
                break;
        }
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Current Registered Listener")
                .setMessage(sugiliteData.registeredBroadcastingListener.toString())
                .create();
        dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
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
            SimpleDateFormat format = new SimpleDateFormat("dd-M-yyyy hh:mm:ss");
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
                for(AccessibilityNodeInfo child : Automator.preOrderTraverse(node)){
                    if(child.getText() != null)
                        childTextSet.add(child.getText().toString());
                    if(child.getContentDescription() != null)
                        childContentDescriptionSet.add(child.getContentDescription().toString());
                }
                if(childTextSet.size() > 0)
                    childText = childTextSet.toString();
                if(childContentDescriptionSet.size() > 0)
                    childContentDescription = childContentDescription.toString();
            }
        }

        public String eventType, packageName, className, text, contentDescription, viewId, boundsInParent, boundsInScreen;
        public String childText, childContentDescription;
        public String time;
    }




}

