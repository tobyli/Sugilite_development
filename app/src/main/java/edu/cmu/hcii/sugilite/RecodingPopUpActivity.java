package edu.cmu.hcii.sugilite;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import edu.cmu.hcii.sugilite.model.AccessibilityNodeInfoList;

public class RecodingPopUpActivity extends AppCompatActivity {

    private String packageName, className, text, contentDescription, viewId, boundsInParent, boundsInScreen;
    private long time;
    private int eventType;
    private SharedPreferences sharedPreferences;
    private AccessibilityNodeInfo parentNode;
    private AccessibilityNodeInfoList childNodes;
    private Map<String, String> parentFeatures = new HashMap<>();
    private Map<String, String> childFeatures = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        setContentView(R.layout.activity_recoding_pop_up);
        if(savedInstanceState == null){
            Bundle extras = getIntent().getExtras();
            if(extras == null){

            }
            else{
                packageName = extras.getString("packageName", "NULL");
                className = extras.getString("className", "NULL");
                text = extras.getString("text", "NULL");
                contentDescription = extras.getString("contentDescription", "NULL");
                viewId = extras.getString("viewId", "NULL");
                boundsInParent = extras.getString("boundsInParent", "NULL");
                boundsInScreen = extras.getString("boundsInScreen", "NULL");
                time = extras.getLong("time", -1);
                eventType = extras.getInt("eventType", -1);
                parentNode = (AccessibilityNodeInfo)extras.getSerializable("parentNode");
                childNodes = (AccessibilityNodeInfoList)extras.getSerializable("childrenNodes");
            }
        }
        else{
            packageName = savedInstanceState.getString("packageName", "NULL");
            className = savedInstanceState.getString("className", "NULL");
            text = savedInstanceState.getString("text", "NULL");
            contentDescription = savedInstanceState.getString("contentDescription", "NULL");
            viewId = savedInstanceState.getString("viewId", "NULL");
            boundsInParent = savedInstanceState.getString("boundsInParent", "NULL");
            boundsInScreen = savedInstanceState.getString("boundsInScreen", "NULL");
            time = savedInstanceState.getLong("time", -1);
            eventType = savedInstanceState.getInt("eventType", -1);
            parentNode = (AccessibilityNodeInfo)savedInstanceState.getSerializable("parentNode");
            childNodes = (AccessibilityNodeInfoList)savedInstanceState.getSerializable("childrenNodes");
        }

        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(time);
        SimpleDateFormat dateFormat;
        dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("US/Eastern"));

        ((CheckBox) findViewById(R.id.packageName)).setText("Package Name: " + packageName);
        ((CheckBox)findViewById(R.id.className)).setText("Class Name: " + className);
        ((CheckBox)findViewById(R.id.text)).setText("Text: " + text);
        ((CheckBox)findViewById(R.id.contentDescription)).setText("Content Description: " + contentDescription);
        ((CheckBox)findViewById(R.id.viewId)).setText("ViewId: " + viewId);
        ((CheckBox)findViewById(R.id.boundsInParent)).setText("Bounds in Parent: " + boundsInParent);
        ((CheckBox)findViewById(R.id.boundsInScreen)).setText("Bounds in Screen: " + boundsInScreen);
        ((TextView)findViewById(R.id.time)).setText("Event Time: " + dateFormat.format(c.getTime()));

        //populate parent features
        if(parentNode != null){
            if(parentNode.getText() != null)
                parentFeatures.put("text", parentNode.getText().toString());
            if(parentNode.getContentDescription() != null)
                parentFeatures.put("contentDescription", parentNode.getContentDescription().toString());
            if(parentNode.getViewIdResourceName() != null)
                parentFeatures.put("viewId", parentNode.getViewIdResourceName());
        }

        //populate child features
        for(AccessibilityNodeInfo childNode : childNodes.list){
            if(childNode != null){
                if(childNode.getText() != null)
                    childFeatures.put("text", childNode.getText().toString());
                if(childNode.getContentDescription() != null)
                    childFeatures.put("contentDescription", childNode.getContentDescription().toString());
                if(childNode.getViewIdResourceName() != null)
                    childFeatures.put("viewId", childNode.getViewIdResourceName());
            }
        }

    }

    public void finishActivity(View view){

        finish();
    }
    public void turnOffRecording(View view)
    {

        SharedPreferences.Editor prefEditor = sharedPreferences.edit();
        prefEditor.putBoolean("recording_in_process", false);
        prefEditor.commit();
        finish();
    }

    public void OKButtonOnClick(View view){
        new AlertDialog.Builder(this)
                .setTitle("Operation Recorded")
                .setMessage(generateDescription())
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // continue with delete
                        finish();
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    //TODO: change to check box
    public void entryOnSelect(View view){

        int viewId = view.getId();

        if(viewId == R.id.packageName){
            Toast.makeText(this, "Selected Package Name as Identifying Feature", Toast.LENGTH_SHORT).show();
        }
        if(viewId == R.id.className){
            Toast.makeText(this, "Selected Class Name as Identifying Feature", Toast.LENGTH_SHORT).show();
        }
        if(viewId == R.id.text){
            Toast.makeText(this, "Selected Text as Identifying Feature", Toast.LENGTH_SHORT).show();
        }
        if(viewId == R.id.contentDescription){
            Toast.makeText(this, "Selected Content Description as Identifying Feature", Toast.LENGTH_SHORT).show();
        }
        if(viewId == R.id.viewId){
            Toast.makeText(this, "Selected View ID as Identifying Feature", Toast.LENGTH_SHORT).show();
        }
        if(viewId == R.id.boundsInParent){
            Toast.makeText(this, "Selected Bound in Parent as Identifying Feature", Toast.LENGTH_SHORT).show();
        }
        if(viewId == R.id.boundsInScreen){
            Toast.makeText(this, "Selected Bount in Screen as Identifying Feature", Toast.LENGTH_SHORT).show();
        }
        ((TextView)findViewById(R.id.operationDescription)).setText(generateDescription());
    }

    public String generateDescription(){
        boolean notFirstCondition = false;
        String retVal = "";
        if (eventType == AccessibilityEvent.TYPE_VIEW_CLICKED){
            retVal += "Click ";
        }
        retVal += "on UI element that ";
        if(((CheckBox)findViewById(R.id.packageName)).isChecked()){
            retVal += ((notFirstCondition? "and " : "") + "is within the package \"" + packageName + "\" ");
            notFirstCondition = true;
        }
        if(((CheckBox)findViewById(R.id.className)).isChecked()){
            retVal += ((notFirstCondition? "and " : "") + "is of the class type \"" + className + "\" ");
            notFirstCondition = true;
        }
        if(((CheckBox)findViewById(R.id.text)).isChecked()){
            retVal += ((notFirstCondition? "and " : "") + "has text \"" + text + "\" ");
            notFirstCondition = true;
        }
        if(((CheckBox)findViewById(R.id.contentDescription)).isChecked()){
            retVal += ((notFirstCondition? "and " : "") + "has contentDescription \"" + contentDescription + "\" ");
            notFirstCondition = true;
        }
        if(((CheckBox)findViewById(R.id.viewId)).isChecked()){
            retVal += ((notFirstCondition? "and " : "") + "has view ID \"" + viewId + "\" ");
            notFirstCondition = true;
        }
        if(((CheckBox)findViewById(R.id.boundsInParent)).isChecked()){
            retVal += ((notFirstCondition? "and " : "") + "has location relative to its parent element at \"" + boundsInParent + "\" ");
            notFirstCondition = true;
        }
        if(((CheckBox)findViewById(R.id.boundsInScreen)).isChecked()){
            retVal += ((notFirstCondition? "and " : "") + "has location on screen at \"" + boundsInScreen + "\" ");
            notFirstCondition = true;
        }
        if(notFirstCondition)
            return retVal;
        else
            return "No feature selected!";
    }


}
