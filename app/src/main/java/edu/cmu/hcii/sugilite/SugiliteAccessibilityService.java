package edu.cmu.hcii.sugilite;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.preference.PreferenceManager;
import android.view.Gravity;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.ImageView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

import edu.cmu.hcii.sugilite.model.AccessibilityNodeInfoList;
import edu.cmu.hcii.sugilite.automation.*;

public class SugiliteAccessibilityService extends AccessibilityService {
    private ImageView statusIcon;
    private WindowManager windowManager;
    private SharedPreferences sharedPreferences;
    private Automator automator;
    private SugiliteData sugiliteData;

    public SugiliteAccessibilityService() {
    }

    @Override
    public void onCreate(){
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sugiliteData = (SugiliteData)getApplication();
        automator = new Automator(sugiliteData);
        try {
            Toast.makeText(this, "Sugilite Accessibility Service Started", Toast.LENGTH_SHORT).show();
            //addStatusIcon();
        }
        catch (Exception e){
            e.printStackTrace();
            //do nothing
        }
    }

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();

    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {

        //Type of accessibility events to handle in this function
        Integer[] accessibilityEventArrayToHandle = {AccessibilityEvent.TYPE_VIEW_CLICKED,
                AccessibilityEvent.TYPE_VIEW_LONG_CLICKED,
                AccessibilityEvent.TYPE_VIEW_SELECTED,
                AccessibilityEvent.TYPE_VIEW_FOCUSED,
                AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED,
                AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED,
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
                AccessibilityEvent.TYPE_WINDOWS_CHANGED,
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED};
        Set<Integer> accessibilityEventSetToHandle = new HashSet<>(Arrays.asList(accessibilityEventArrayToHandle));
        //return if the event is not among the accessibilityEventArrayToHandle
        if(!accessibilityEventSetToHandle.contains(Integer.valueOf(event.getEventType())))
            return;
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (sharedPreferences.getBoolean("recording_in_process", false)) {
            //recording in progress
            AccessibilityNodeInfo sourceNode = event.getSource();
            Set<String> exceptedPackages = new HashSet<>();
            //skip internal interactions and interactions on system ui
            exceptedPackages.add("edu.cmu.hcii.sugilite");
            exceptedPackages.add("com.android.systemui");
            if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_CLICKED && (!exceptedPackages.contains(event.getPackageName()))) {
                //start the popup activity
                startActivity(generatePopUpActivityIntentFromEvent(event));
            }
        }

        if (sharedPreferences.getBoolean("tracking_in_process", false)) {
            //background tracking in progress
        }
        boolean retVal = false;
        if(sugiliteData.getInstructionQueueSize() > 0)
            retVal = automator.handleLiveEvent(this.getRootInActiveWindow(), getApplicationContext());

    }

    @Override
    public void onInterrupt() {
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        Toast.makeText(this, "Sugilite Accessibility Service Stopped", Toast.LENGTH_SHORT).show();
        //windowManager.removeView(statusIcon);
    }

    private void addStatusIcon(){
        statusIcon = new ImageView(this);
        statusIcon.setImageResource(R.mipmap.ic_launcher);
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.TOP | Gravity.LEFT;
        params.x = 0;
        params.y = 100;
        windowManager.addView(statusIcon, params);
    }

    private Intent generatePopUpActivityIntentFromEvent(AccessibilityEvent event){
        AccessibilityNodeInfo sourceNode = event.getSource();
        Rect boundsInParents = new Rect();
        Rect boundsInScreen = new Rect();
        sourceNode.getBoundsInParent(boundsInParents);
        sourceNode.getBoundsInScreen(boundsInScreen);
        AccessibilityNodeInfo parentNode = sourceNode.getParent();
        ArrayList<AccessibilityNodeInfo> childrenNodes = new ArrayList<>();
        for(int i = 0; i < sourceNode.getChildCount(); i++){
            AccessibilityNodeInfo childNode = sourceNode.getChild(i);
            if(childNode != null)
                childrenNodes.add(childNode);
        }
        //TODO:AccessibilityNodeInfo is not serializable

        //pop up the selection window
        Intent popUpIntent = new Intent(this, RecordingPopUpActivity.class);
        popUpIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        popUpIntent.putExtra("packageName", sourceNode.getPackageName());
        popUpIntent.putExtra("className", sourceNode.getClassName());
        popUpIntent.putExtra("text", sourceNode.getText());
        popUpIntent.putExtra("contentDescription", sourceNode.getContentDescription());
        popUpIntent.putExtra("viewId", sourceNode.getViewIdResourceName());
        popUpIntent.putExtra("boundsInParent", boundsInParents.flattenToString());
        popUpIntent.putExtra("boundsInScreen", boundsInScreen.flattenToString());
        popUpIntent.putExtra("time", Calendar.getInstance().getTimeInMillis());
        popUpIntent.putExtra("eventType", event.getEventType());
        popUpIntent.putExtra("parentNode", parentNode);
        popUpIntent.putExtra("childrenNodes", new AccessibilityNodeInfoList(childrenNodes));
        return popUpIntent;
    }
}

