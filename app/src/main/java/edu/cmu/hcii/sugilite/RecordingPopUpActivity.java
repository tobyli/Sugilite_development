package edu.cmu.hcii.sugilite;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.AbstractMap;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import edu.cmu.hcii.sugilite.dao.SugiliteScriptDao;
import edu.cmu.hcii.sugilite.model.AccessibilityNodeInfoList;
import edu.cmu.hcii.sugilite.model.SetMapEntrySerializableWrapper;
import edu.cmu.hcii.sugilite.model.block.SugiliteOperationBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;
import edu.cmu.hcii.sugilite.model.block.UIElementMatchingFilter;
import edu.cmu.hcii.sugilite.model.operation.SugiliteOperation;
import edu.cmu.hcii.sugilite.model.operation.SugiliteSetTextOperation;

public class RecordingPopUpActivity extends AppCompatActivity {

    private String packageName, className, text, contentDescription, viewId, boundsInParent, boundsInScreen;
    private long time;
    private int eventType;
    private SharedPreferences sharedPreferences;
    private AccessibilityNodeInfo parentNode;
    private AccessibilityNodeInfoList childNodes;
    private SugiliteScriptDao sugiliteScriptDao;
    private Set<Map.Entry<String, String>> allParentFeatures = new HashSet<>();
    private Set<Map.Entry<String, String>> allChildFeatures = new HashSet<>();
    private Set<Map.Entry<String, String>> selectedParentFeatures = new HashSet<>();
    private Set<Map.Entry<String, String>> selectedChildFeatures = new HashSet<>();
    private SugiliteData sugiliteData;
    static final int PICK_CHILD_FEATURE = 1;
    static final int PICK_PARENT_FEATURE = 2;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sugiliteData = (SugiliteData)getApplication();
        sugiliteScriptDao = new SugiliteScriptDao(this);
        setContentView(R.layout.activity_recoding_pop_up);
        //fetch the data capsuled in the intent
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
                parentNode = extras.getParcelable("parentNode");
                childNodes = extras.getParcelable("childrenNodes");
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
            parentNode = savedInstanceState.getParcelable("parentNode");
            childNodes = savedInstanceState.getParcelable("childrenNodes");
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
        ((TextView)findViewById(R.id.time)).setText("Event Time: " + dateFormat.format(c.getTime()) + "\nRecording script: " + sharedPreferences.getString("scriptName", "NULL"));

        //populate parent features
        if(parentNode != null){
            if(parentNode.getText() != null)
                allParentFeatures.add(new AbstractMap.SimpleEntry<>("text", parentNode.getText().toString()));
            if(parentNode.getContentDescription() != null)
                allParentFeatures.add(new AbstractMap.SimpleEntry<>("contentDescription", parentNode.getContentDescription().toString()));
            if(parentNode.getViewIdResourceName() != null)
                allParentFeatures.add(new AbstractMap.SimpleEntry<>("viewId", parentNode.getViewIdResourceName()));
        }

        //populate child features
        for(AccessibilityNodeInfo childNode : childNodes.getList()){
            if(childNode != null){
                if(childNode.getText() != null)
                    allChildFeatures.add(new AbstractMap.SimpleEntry<>("text", childNode.getText().toString()));
                if(childNode.getContentDescription() != null)
                    allChildFeatures.add(new AbstractMap.SimpleEntry<>("contentDescription", childNode.getContentDescription().toString()));
                if(childNode.getViewIdResourceName() != null)
                    allChildFeatures.add(new AbstractMap.SimpleEntry<>("viewId", childNode.getViewIdResourceName()));
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
        //add head if no one is present
        if(sugiliteData.getScriptHead() == null ||
                (!((SugiliteStartingBlock)sugiliteData.getScriptHead()).getScriptName().contentEquals(sharedPreferences.getString("scriptName", "defaultScript") + ".SugiliteScript"))){
            sugiliteData.setScriptHead(new SugiliteStartingBlock(sharedPreferences.getString("scriptName", "defaultScript") + ".SugiliteScript"));
            sugiliteData.setCurrentScriptBlock(sugiliteData.getScriptHead());
        }
        //use the dialog "builder" to ask the type of operation to take
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose Operation");
        String[] operations = {};
        if(className.contains("EditText"))
            operations = new String[]{"CLICK", "RETURN", "SET TEXT"};
        else
            operations = new String[]{"CLICK", "RETURN"};
        final SugiliteOperation sugiliteOperation = new SugiliteOperation();
        sugiliteOperation.setOperationType(SugiliteOperation.CLICK);
        final SugiliteOperationBlock operationBlock = new SugiliteOperationBlock();
        operationBlock.setOperation(sugiliteOperation);
        final Context activityContext = this;
        builder.setItems(operations, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        Toast.makeText(getApplicationContext(), "CLICK REQUESTED", Toast.LENGTH_SHORT).show();
                        sugiliteOperation.setOperationType(SugiliteOperation.CLICK);
                        break;
                    case 1:
                        Toast.makeText(getApplicationContext(), "RETURN REQUESTED", Toast.LENGTH_SHORT).show();
                        sugiliteOperation.setOperationType(SugiliteOperation.RETURN);
                        break;
                    case 2:
                        Toast.makeText(getApplicationContext(), "SET_TEXT REQUESTED", Toast.LENGTH_SHORT).show();
                        sugiliteOperation.setOperationType(SugiliteOperation.SET_TEXT);
                        break;
                }
                operationBlock.setDescription(generateDescription());
                operationBlock.setPreviousBlock(sugiliteData.getCurrentScriptBlock());
                operationBlock.setElementMatchingFilter(generateFilter());
                //genereate the block if the operation is click or return
                if (sugiliteOperation.getOperationType() == SugiliteOperation.CLICK || sugiliteOperation.getOperationType() == SugiliteOperation.RETURN) {
                    operationBlock.setOperation(sugiliteOperation);
                    if(sugiliteData.getCurrentScriptBlock() instanceof SugiliteOperationBlock){
                        ((SugiliteOperationBlock)sugiliteData.getCurrentScriptBlock()).setNextBlock(operationBlock);
                    }
                    if(sugiliteData.getCurrentScriptBlock() instanceof SugiliteStartingBlock){
                        ((SugiliteStartingBlock)sugiliteData.getCurrentScriptBlock()).setNextBlock(operationBlock);
                    }
                    String message = "";
                    if (operationBlock.getOperation().getOperationType() == SugiliteOperation.CLICK){
                        message += "Click ";
                    }
                    if (operationBlock.getOperation().getOperationType() == SugiliteOperation.RETURN){
                        message += "Return";
                    }
                    if(operationBlock.getOperation().getOperationType() == SugiliteOperation.SET_TEXT){
                        message += "Set text to \"" + ((SugiliteSetTextOperation)operationBlock.getOperation()).getText() + "\" ";
                    }
                    message += generateDescription();
                    operationBlock.setDescription(message);
                    sugiliteData.setCurrentScriptBlock(operationBlock);
                    try{
                        sugiliteScriptDao.save((SugiliteStartingBlock)sugiliteData.getScriptHead());
                    }
                    catch (Exception e){
                        e.printStackTrace();
                    }
                    System.out.println("saved block");
                    new AlertDialog.Builder(activityContext)
                            .setTitle("Operation Recorded")
                            .setMessage(message)
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    // continue with delete
                                    finish();
                                }
                            })
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .show();
                }
                // if the operation == text, add a new pop up to ask the text to set the content of the edittext widget to
                //TODO: eliminate the duplicate code for the two branches
                else if (sugiliteOperation.getOperationType() == SugiliteOperation.SET_TEXT) {
                    final SugiliteSetTextOperation setTextOperation = new SugiliteSetTextOperation();
                    AlertDialog.Builder textDialogBuilder = new AlertDialog.Builder(activityContext);
                    textDialogBuilder.setTitle("Set Text Operation").setMessage("Enter the text to set to");
                    final EditText editText = new EditText(activityContext);
                    textDialogBuilder.setView(editText).setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String text = editText.getText().toString();
                            setTextOperation.setText(text);
                            operationBlock.setOperation(setTextOperation);
                            if(sugiliteData.getCurrentScriptBlock() instanceof SugiliteOperationBlock){
                                ((SugiliteOperationBlock)sugiliteData.getCurrentScriptBlock()).setNextBlock(operationBlock);
                            }
                            if(sugiliteData.getCurrentScriptBlock() instanceof SugiliteStartingBlock){
                                ((SugiliteStartingBlock)sugiliteData.getCurrentScriptBlock()).setNextBlock(operationBlock);
                            }
                            System.out.println("saved block");
                            String message = "";
                            if (operationBlock.getOperation().getOperationType() == SugiliteOperation.CLICK){
                                message += "Click ";
                            }
                            if (operationBlock.getOperation().getOperationType() == SugiliteOperation.RETURN){
                                message += "Return";
                            }
                            if(operationBlock.getOperation().getOperationType() == SugiliteOperation.SET_TEXT){
                                message += "Set text to \"" + ((SugiliteSetTextOperation)operationBlock.getOperation()).getText() + "\" ";
                            }
                            message += generateDescription();
                            operationBlock.setDescription(message);
                            sugiliteData.setCurrentScriptBlock(operationBlock);
                            try{
                                sugiliteScriptDao.save((SugiliteStartingBlock)sugiliteData.getScriptHead());
                            }
                            catch (Exception e){
                                e.printStackTrace();
                            }

                            new AlertDialog.Builder(activityContext)
                                    .setTitle("Operation Recorded")
                                    .setMessage(message)
                                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            sugiliteData.addInstruction(operationBlock);
                                            // continue with delete
                                            finish();
                                        }
                                    })
                                    .setIcon(android.R.drawable.ic_dialog_alert)
                                    .show();
                        }
                    });
                    textDialogBuilder.show();

                }
            }
        });
        builder.show();
    }

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
        if(viewId == R.id.childrenCheckbox){
            Toast.makeText(this, "Selected Children as Identifying Feature", Toast.LENGTH_SHORT).show();
            Intent popUpSubMenuIntent = new Intent(this, RecordingPopupSubMenuActivity.class);
            popUpSubMenuIntent.putExtra("allFeatures", new SetMapEntrySerializableWrapper(allChildFeatures));
            popUpSubMenuIntent.putExtra("selectedFeatures", new SetMapEntrySerializableWrapper(selectedChildFeatures));
            startActivityForResult(popUpSubMenuIntent, PICK_CHILD_FEATURE);

        }
        if(viewId == R.id.parentCheckbox){
            Toast.makeText(this, "Selected Parent as Identifying Feature", Toast.LENGTH_SHORT).show();
            Intent popUpSubMenuIntent = new Intent(this, RecordingPopupSubMenuActivity.class);
            popUpSubMenuIntent.putExtra("allFeatures", new SetMapEntrySerializableWrapper(allParentFeatures));
            popUpSubMenuIntent.putExtra("selectedFeatures", new SetMapEntrySerializableWrapper(selectedParentFeatures));
            startActivityForResult(popUpSubMenuIntent, PICK_PARENT_FEATURE);

        }
        ((TextView)findViewById(R.id.operationDescription)).setText(generateDescription());
    }

    //read and load the result from the child/parent sub activity
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == PICK_CHILD_FEATURE){
            //get result from child feature picking
            if(resultCode == RESULT_OK){
                selectedChildFeatures = ((SetMapEntrySerializableWrapper)data.getSerializableExtra("result")).set;
            }
            else{

            }
        }
        else if(requestCode == PICK_PARENT_FEATURE){
            //get result from parent feature picking
            if(resultCode == RESULT_OK){
                selectedParentFeatures = ((SetMapEntrySerializableWrapper)data.getSerializableExtra("result")).set;
            }
            else{

            }
        }
        ((CheckBox)findViewById(R.id.childrenCheckbox)).setChecked(selectedChildFeatures.size() > 0);
        ((CheckBox)findViewById(R.id.parentCheckbox)).setChecked(selectedParentFeatures.size() > 0);
        ((TextView)findViewById(R.id.operationDescription)).setText(generateDescription());
    }
    //show the parent popup when the parent checkbox is checked
    public void showParentPopup(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        for(Map.Entry<String, String> entry : allParentFeatures){
            popup.getMenu().add(entry.getKey() + " is " + entry.getValue()).setCheckable(true).setChecked(false);
        }
        popup.show();
    }
    //show the children popup when the children checkbox is checked
    public void showChildrenPopup(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        for(Map.Entry<String, String> entry : allChildFeatures){
            popup.getMenu().add(entry.getKey() + " is " + entry.getValue()).setCheckable(true).setChecked(false);
        }
        popup.show();
    }

    /**
     * generate a UIElementMatchingFilter based on the selection
     * @return
     */
    public UIElementMatchingFilter generateFilter(){
        UIElementMatchingFilter filter = new UIElementMatchingFilter();
        if(((CheckBox)findViewById(R.id.packageName)).isChecked()){
            filter.setPackageName(packageName);
        }
        if(((CheckBox)findViewById(R.id.className)).isChecked()){
            filter.setClassName(className);
        }
        if(((CheckBox)findViewById(R.id.text)).isChecked()){
            filter.setText(text);
        }
        if(((CheckBox)findViewById(R.id.contentDescription)).isChecked()){
            filter.setContentDescription(contentDescription);
        }
        if(((CheckBox)findViewById(R.id.viewId)).isChecked()){
            filter.setViewId(viewId);
        }
        if(((CheckBox)findViewById(R.id.boundsInParent)).isChecked()){
            filter.setBoundsInParent(Rect.unflattenFromString(boundsInParent));
        }
        if(((CheckBox)findViewById(R.id.boundsInScreen)).isChecked()){
            filter.setBoundsInScreen(Rect.unflattenFromString(boundsInScreen));
        }
        if (selectedChildFeatures.size() > 0){
            UIElementMatchingFilter childFilter = new UIElementMatchingFilter();
            for(Map.Entry<String, String> entry : selectedChildFeatures){
                if(entry.getKey().contentEquals("text")){
                    childFilter.setText(entry.getValue());
                }
                if(entry.getKey().contentEquals("contentDescription")){
                    childFilter.setContentDescription(entry.getValue());
                }
                if(entry.getKey().contentEquals("viewId")){
                    childFilter.setViewId(entry.getValue());
                }
            }
            filter.setChildFilter(childFilter);
        }
        if (selectedParentFeatures.size() > 0){
            UIElementMatchingFilter parentFilter = new UIElementMatchingFilter();
            for(Map.Entry<String, String> entry : selectedParentFeatures){
                if(entry.getKey().contentEquals("text")){
                    parentFilter.setText(entry.getValue());
                }
                if(entry.getKey().contentEquals("contentDescription")){
                    parentFilter.setContentDescription(entry.getValue());
                }
                if(entry.getKey().contentEquals("viewId")){
                    parentFilter.setViewId(entry.getValue());
                }
            }
            filter.setParentFilter(parentFilter);
        }
        return filter;
    }

    /**
     * generate a description string based on the selection
     * @return a description string
     */
    public String generateDescription(){
        boolean notFirstCondition = false;
        String retVal = "";
        /*
        if (eventType == AccessibilityEvent.TYPE_VIEW_CLICKED){
            retVal += "Click ";
        }
        */
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
        if (selectedChildFeatures.size() > 0){
            retVal += ((notFirstCondition? "and " : "") + "has a child that { ");
            boolean notFirst = false;
            for(Map.Entry<String, String> entry :selectedChildFeatures){
                retVal += ((notFirst? "and " : "") + "has " + entry.getKey() + " == \"" + entry.getValue() + "\" ");
                notFirst = true;
            }
            retVal += "} ";
        }
        if (selectedParentFeatures.size() > 0){
            retVal += ((notFirstCondition? "and " : "") + "has the parent that { ");
            boolean notFirst = false;
            for(Map.Entry<String, String> entry :selectedParentFeatures){
                retVal += ((notFirst? "and " : "") + "has " + entry.getKey() + " == \"" + entry.getValue() + "\" ");
                notFirst = true;
            }
            retVal += "} ";
        }
        if(notFirstCondition)
            return retVal;
        else
            return "No feature selected!";
    }


}
