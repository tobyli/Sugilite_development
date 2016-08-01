package edu.cmu.hcii.sugilite;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Rect;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import edu.cmu.hcii.sugilite.communication.SugiliteCommunicationController;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptDao;
import edu.cmu.hcii.sugilite.model.AccessibilityNodeInfoList;
import edu.cmu.hcii.sugilite.model.block.SerializableNodeInfo;
import edu.cmu.hcii.sugilite.model.block.SugiliteAvailableFeaturePack;
import edu.cmu.hcii.sugilite.model.block.SugiliteBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteOperationBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;
import edu.cmu.hcii.sugilite.model.block.UIElementMatchingFilter;
import edu.cmu.hcii.sugilite.model.operation.SugiliteOperation;
import edu.cmu.hcii.sugilite.model.operation.SugiliteSetTextOperation;
import edu.cmu.hcii.sugilite.model.variable.StringVariable;
import edu.cmu.hcii.sugilite.model.variable.Variable;
import edu.cmu.hcii.sugilite.ui.ChooseVariableDialog;
import edu.cmu.hcii.sugilite.ui.ReadableDescriptionGenerator;
import edu.cmu.hcii.sugilite.ui.UIElementFeatureRecommender;
@Deprecated
public class mRecordingPopUpActivity extends AppCompatActivity {

    private int triggerMode;
    private SugiliteAvailableFeaturePack featurePack;
    private SharedPreferences sharedPreferences;
    private SugiliteScriptDao sugiliteScriptDao;
    private Set<Map.Entry<String, String>> allParentFeatures = new HashSet<>();
    private Set<Map.Entry<String, String>> allChildFeatures = new HashSet<>();
    private Set<Map.Entry<String, String>> selectedParentFeatures = new HashSet<>();
    private Set<Map.Entry<String, String>> selectedChildFeatures = new HashSet<>();
    private SugiliteData sugiliteData;
    private ReadableDescriptionGenerator readableDescriptionGenerator;
    private UIElementFeatureRecommender recommender;
    private Map<Map.Entry<String, String>, CheckBox> checkBoxChildEntryMap;
    private Map<Map.Entry<String, String>, CheckBox> checkBoxParentEntryMap;
    private Map<String, CheckBox> identifierCheckboxMap;
    private Set<Map.Entry<String, String>> alternativeLabels;



    private Spinner actionSpinner, targetTypeSpinner, withInAppSpinner;
    private CheckBox textCheckbox, contentDescriptionCheckbox, viewIdCheckbox, boundsInParentCheckbox, boundsInScreenCheckbox;
    private String textContent, contentDescriptionContent, viewIdContent;
    private LinearLayout actionParameterSection, actionSection;


    private SugiliteStartingBlock originalScript;
    private SugiliteOperationBlock blockToEdit;

    static final int PICK_CHILD_FEATURE = 1;
    static final int PICK_PARENT_FEATURE = 2;

    static final int TRIGGERED_BY_NEW_EVENT = 1;
    static final int TRIGGERED_BY_EDIT = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sugiliteData = (SugiliteData)getApplication();
        sugiliteScriptDao = new SugiliteScriptDao(this);
        readableDescriptionGenerator = new ReadableDescriptionGenerator(getApplicationContext());
        checkBoxChildEntryMap = new HashMap<>();
        checkBoxParentEntryMap = new HashMap<>();
        identifierCheckboxMap = new HashMap<>();
        setContentView(R.layout.activity_m_recording_pop_up);
        featurePack = new SugiliteAvailableFeaturePack();
        ((TextView)findViewById(R.id.parameterLink)).setText(Html.fromHtml("<p><u>Set as a parameter</u></p>"));
        setTitle("Sugilite Script Recording");
        //fetch the data capsuled in the intent
        //TODO: refactor so the service passes in a feature pack instead
        if(savedInstanceState == null){
            loadFeaturePackFromIntent();
        }
        else{
            loadFeaturePackFromSavedInstance(savedInstanceState);
        }
        setupSelections();
    }

    public void finishActivity(View view){
        setResult(RESULT_CANCELED);
        finish();
    }
    public void turnOffRecording(View view)
    {
        SharedPreferences.Editor prefEditor = sharedPreferences.edit();
        prefEditor.putBoolean("recording_in_process", false);
        prefEditor.commit();
        if(sugiliteData.initiatedExternally == true && sugiliteData.getScriptHead() != null) {
            sugiliteData.communicationController.sendRecordingFinishedSignal(sugiliteData.getScriptHead().getScriptName());
            sugiliteData.sendCallbackMsg("FINISHED_RECORDING", sugiliteData.getScriptHead().getScriptName(), sugiliteData.callbackString);
        }
        setResult(RESULT_CANCELED);
        finish();
    }

    public void OKButtonOnClick(View view){
        //add head if no one is present && this popup is triggered by new event
        if(triggerMode == TRIGGERED_BY_NEW_EVENT &&
                (sugiliteData.getScriptHead() == null ||
                        (!((SugiliteStartingBlock)sugiliteData.getScriptHead()).getScriptName().contentEquals(sharedPreferences.getString("scriptName", "defaultScript") + ".SugiliteScript")))){
            sugiliteData.setScriptHead(new SugiliteStartingBlock(sharedPreferences.getString("scriptName", "defaultScript") + ".SugiliteScript"));
            sugiliteData.setCurrentScriptBlock(sugiliteData.getScriptHead());
        }
        final Context activityContext = this;
        final SugiliteOperationBlock operationBlock = generateBlock();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Save Operation Confirmation").setMessage(Html.fromHtml("Are you sure you want to record the operation: " + readableDescriptionGenerator.generateReadableDescription(operationBlock)));
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                saveBlock(operationBlock, activityContext);
                //fill in the text box if the operation is of SET_TEXT type
                if(operationBlock.getOperation().getOperationType() == SugiliteOperation.SET_TEXT && triggerMode == TRIGGERED_BY_NEW_EVENT)
                    sugiliteData.addInstruction(operationBlock);
            }
        }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        }).show();
    }

    public void setAsAParameterOnClick(View view){
        EditText actionParameter = (EditText)findViewById(R.id.action_parameter_set_text);
        setAsAParameterOnClick(view, actionParameter, "", "");
    }

    public void setAsAParameterOnClick(View view, TextView actionParameter, String label, String defaultDefaultValue){
        Toast.makeText(this, "set as a parameter", Toast.LENGTH_SHORT).show();
        if(actionParameter != null) {
            ChooseVariableDialog dialog;
            switch (triggerMode) {
                case TRIGGERED_BY_NEW_EVENT:
                    dialog = new ChooseVariableDialog(this, actionParameter, getLayoutInflater(), sugiliteData, sugiliteData.getScriptHead(), label, defaultDefaultValue);
                    dialog.show();
                    break;
                case TRIGGERED_BY_EDIT:
                    dialog = new ChooseVariableDialog(this, actionParameter, getLayoutInflater(), sugiliteData, originalScript, label, defaultDefaultValue);
                    dialog.show();
                    break;
            }
        }
        //prompt new parameter, open the parameter management popup, click on a parameter
    }

    public void seeAlternativeLabelLinkOnClick(View view) {
        if(alternativeLabels == null)
            return;
        List<String> labelList = new ArrayList<>();
        for(Map.Entry<String, String> entry : alternativeLabels){
            labelList.add(entry.getKey() + ": " + entry.getValue());
        }
        ListView listView = new ListView(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_single_choice, labelList);
        listView.setAdapter(adapter);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Alternative Labels")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setView(listView)
                .show();
    }

    private void setupSelections(){
        //populate parent features
        if(featurePack.parentNode != null){
            if(featurePack.parentNode.text != null)
                allParentFeatures.add(new AbstractMap.SimpleEntry<>("Text", featurePack.parentNode.text.toString()));
            if(featurePack.parentNode.contentDescription != null)
                allParentFeatures.add(new AbstractMap.SimpleEntry<>("ContentDescription", featurePack.parentNode.contentDescription.toString()));
            if(featurePack.parentNode.viewId != null)
                allParentFeatures.add(new AbstractMap.SimpleEntry<>("ViewID", featurePack.parentNode.viewId));
        }



        //populate child features
        for(SerializableNodeInfo childNode : featurePack.childNodes){
            if(childNode != null){
                if(childNode.text != null)
                    allChildFeatures.add(new AbstractMap.SimpleEntry<>("Text", childNode.text.toString()));
                if(childNode.contentDescription != null)
                    allChildFeatures.add(new AbstractMap.SimpleEntry<>("ContentDescription", childNode.contentDescription.toString()));
                if(childNode.viewId != null)
                    allChildFeatures.add(new AbstractMap.SimpleEntry<>("ViewID", childNode.viewId));
            }
        }

        recommender = new UIElementFeatureRecommender(featurePack.packageName, featurePack.className, featurePack.text, featurePack.contentDescription, featurePack.viewId, featurePack.boundsInParent, featurePack.boundsInScreen, featurePack.isEditable, featurePack.time, featurePack.eventType, allParentFeatures, allChildFeatures);

        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(featurePack.time);
        SimpleDateFormat dateFormat;
        dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("US/Eastern"));
        boolean autoFillEnabled = sharedPreferences.getBoolean("auto_fill_enabled", true);
        LinearLayout identifierLayout = (LinearLayout) findViewById(R.id.identifier_layout);

        CompoundButton.OnCheckedChangeListener identiferCheckboxChangeListener = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                refreshAfterChange();
            }
        };
        Set<String> existingFeatureValues = new HashSet<>();

        //setup identifier
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(20, 0, 0 ,0);
        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                refreshAfterChange();
            }
        };

        if(!featurePack.text.contentEquals("NULL")) {
            textCheckbox = new CheckBox(this);
            textCheckbox.setText(Html.fromHtml(boldify("Text: ") + featurePack.text));
            existingFeatureValues.add(featurePack.text);
            textContent = new String(featurePack.text);
            if(autoFillEnabled)
                textCheckbox.setChecked(recommender.chooseText());
            identifierLayout.addView(generateRow(textCheckbox, "text", featurePack.text), layoutParams);
            identifierCheckboxMap.put("text", textCheckbox);
            textCheckbox.setOnCheckedChangeListener(identiferCheckboxChangeListener);
            textCheckbox.addTextChangedListener(textWatcher);
            //TODO: fix the text label of the checkbox
        }


        if(!featurePack.contentDescription.contentEquals("NULL")) {
            contentDescriptionCheckbox = new CheckBox(this);
            contentDescriptionCheckbox.setText(Html.fromHtml(boldify("ContentDescription: ") + featurePack.contentDescription));
            existingFeatureValues.add(featurePack.contentDescription);
            contentDescriptionContent = new String(featurePack.contentDescription);
            if(autoFillEnabled)
                contentDescriptionCheckbox.setChecked(recommender.chooseContentDescription());
            identifierLayout.addView(generateRow(contentDescriptionCheckbox, "contentDescription", featurePack.contentDescription), layoutParams);
            identifierCheckboxMap.put("contentDescription", contentDescriptionCheckbox);
            contentDescriptionCheckbox.setOnCheckedChangeListener(identiferCheckboxChangeListener);
            contentDescriptionCheckbox.addTextChangedListener(textWatcher);
        }

        if(!featurePack.viewId.contentEquals("NULL")) {
            viewIdCheckbox = new CheckBox(this);
            viewIdCheckbox.setText(Html.fromHtml(boldify("ViewId: ") + featurePack.viewId));
            existingFeatureValues.add(featurePack.viewId);
            viewIdContent = new String(featurePack.viewId);
            if(autoFillEnabled)
                viewIdCheckbox.setChecked(recommender.chooseViewId());
            identifierLayout.addView(generateRow(viewIdCheckbox, "viewId", featurePack.viewId), layoutParams);
            identifierCheckboxMap.put("viewId", viewIdCheckbox);
            viewIdCheckbox.setOnCheckedChangeListener(identiferCheckboxChangeListener);
            viewIdCheckbox.addTextChangedListener(textWatcher);
        }

        if(autoFillEnabled){
            selectedChildFeatures.addAll(recommender.chooseChildFeatures());
            selectedParentFeatures.addAll(recommender.chooseParentFeatures());
        }

        for(Map.Entry<String, String> feature : allChildFeatures){
            if(feature.getKey() != null && feature.getValue() != null){
                CheckBox childCheckBox = new CheckBox(this);
                childCheckBox.setText(Html.fromHtml(boldify("Child " + feature.getKey() + ": ") + feature.getValue()));
                if(!existingFeatureValues.contains(feature.getValue())) {
                    if (selectedChildFeatures.contains(feature))
                        childCheckBox.setChecked(true);
                    existingFeatureValues.add(feature.getValue());
                    identifierLayout.addView(generateRow(childCheckBox, "Child " + feature.getKey(), feature.getValue()), layoutParams);
                    checkBoxChildEntryMap.put(feature, childCheckBox);
                    childCheckBox.setOnCheckedChangeListener(identiferCheckboxChangeListener);
                    childCheckBox.addTextChangedListener(textWatcher);
                }
                else
                    continue;
            }
        }

        //NOTE: temporarily remove parent features

        /*
        for(Map.Entry<String, String> feature : allParentFeatures){
            if(feature.getKey() != null && feature.getValue() != null){
                CheckBox parentCheckBox = new CheckBox(this);
                parentCheckBox.setText(Html.fromHtml(boldify("Parent " + feature.getKey() + ": ") + feature.getValue()));
                if(!existingFeatureValues.contains(feature.getValue())) {
                    if (selectedParentFeatures.contains(feature))
                        parentCheckBox.setChecked(true);
                    existingFeatureValues.add(feature.getValue());
                    identifierLayout.addView(parentCheckBox, layoutParams);
                    checkBoxParentEntryMap.put(feature, parentCheckBox);
                    parentCheckBox.setOnCheckedChangeListener(identiferCheckboxChangeListener);
                }
                else
                    continue;
            }
        }
        */


        boundsInParentCheckbox = new CheckBox(this);
        boundsInParentCheckbox.setText(Html.fromHtml(boldify("Bounds in Parent: ") + featurePack.boundsInParent));
        if(autoFillEnabled)
            boundsInParentCheckbox.setChecked(recommender.chooseBoundsInParent());
        identifierLayout.addView(boundsInParentCheckbox, layoutParams);
        identifierCheckboxMap.put("boundsInParent", boundsInParentCheckbox);
        boundsInParentCheckbox.setOnCheckedChangeListener(identiferCheckboxChangeListener);

        boundsInScreenCheckbox = new CheckBox(this);
        boundsInScreenCheckbox.setText(Html.fromHtml(boldify("Bounds in Screen: ") + featurePack.boundsInScreen));
        if(autoFillEnabled)
            boundsInScreenCheckbox.setChecked(recommender.chooseBoundsInScreen());
        identifierLayout.addView(boundsInScreenCheckbox, layoutParams);
        identifierCheckboxMap.put("boundsInScreen", boundsInScreenCheckbox);
        boundsInScreenCheckbox.setOnCheckedChangeListener(identiferCheckboxChangeListener);

        AdapterView.OnItemSelectedListener spinnerSelectedListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                refreshAfterChange();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                refreshAfterChange();
            }
        };

        //set up action
        actionSpinner = (Spinner)findViewById(R.id.action_dropdown);
        List<String> actionSpinnerItems = new ArrayList<>();
        if(featurePack.isEditable)
            actionSpinnerItems.add("Set Text");
        actionSpinnerItems.add("Click");
        if(featurePack.eventType == AccessibilityEvent.TYPE_VIEW_LONG_CLICKED)
            actionSpinnerItems.add("Long Click");
        ArrayAdapter<String> actionAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, actionSpinnerItems);
        actionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        actionSpinner.setAdapter(actionAdapter);
        actionSpinner.setSelection(0);
        actionSpinner.setOnItemSelectedListener(spinnerSelectedListener);

        EditText textBox = (EditText)findViewById(R.id.action_parameter_set_text);
        textBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                refreshAfterChange();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        actionParameterSection = (LinearLayout)findViewById(R.id.action_parameter_section);
        actionSection = (LinearLayout)findViewById(R.id.action_section);
        actionSection.removeView(actionParameterSection);

        //set up target type
        targetTypeSpinner = (Spinner)findViewById(R.id.target_type_dropdown);
        List<String> targetTypeSpinnerItems = new ArrayList<>();
        targetTypeSpinnerItems.add(featurePack.className);
        targetTypeSpinnerItems.add("Any widget");
        ArrayAdapter<String> targetTypeAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, targetTypeSpinnerItems);
        targetTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        targetTypeSpinner.setAdapter(targetTypeAdapter);
        targetTypeSpinner.setSelection(0);
        targetTypeSpinner.setOnItemSelectedListener(spinnerSelectedListener);

        //set up within app
        withInAppSpinner = (Spinner)findViewById(R.id.within_app_dropdown);
        List<String> withinAppSpinnerItems = new ArrayList<>();
        withinAppSpinnerItems.add(readableDescriptionGenerator.getReadableName(featurePack.packageName));
        withinAppSpinnerItems.add("Any app");
        ArrayAdapter<String> withInAppAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, withinAppSpinnerItems);
        withInAppAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        withInAppSpinner.setAdapter(withInAppAdapter);
        withInAppSpinner.setSelection(0);
        withInAppSpinner.setOnItemSelectedListener(spinnerSelectedListener);

        //((TextView)findViewById(R.id.time)).setText("Event Time: " + dateFormat.format(c.getTime()) + "\nRecording script: " + sharedPreferences.getString("scriptName", "NULL"));
        //((TextView)findViewById(R.id.filteredNodeCount)).setText(generateFilterCount());
        ((TextView) findViewById(R.id.previewContent)).setText(readableDescriptionGenerator.generateReadableDescription(generateBlock()));
    }

    private void refreshAfterChange(){
        //collapse and expand the action parameter
        Map<String, Variable> variableDefaultValueMap = null;
        switch (triggerMode) {
            case TRIGGERED_BY_NEW_EVENT:
                variableDefaultValueMap = sugiliteData.getScriptHead().variableNameDefaultValueMap;
                break;
            case TRIGGERED_BY_EDIT:
                variableDefaultValueMap = originalScript.variableNameDefaultValueMap;
                break;
        }
        String actionSpinnerSelectedItem = actionSpinner.getSelectedItem().toString();
        if (actionSpinnerSelectedItem.contentEquals("Set Text") && actionParameterSection.getParent() == null) {
            actionSection.addView(actionParameterSection);
        }
        if ((!actionSpinnerSelectedItem.contentEquals("Set Text")) && (actionParameterSection.getParent() != null))
            actionSection.removeView(actionParameterSection);

        //refresh "selectedchildren" and "selectedparent"
        selectedParentFeatures.clear();
        selectedChildFeatures.clear();

        for(Map.Entry<String, String> feature : allChildFeatures){
            CheckBox checkBox = checkBoxChildEntryMap.get(feature);
            Map.Entry<String, String> featureToAdd = new AbstractMap.SimpleEntry<String, String>(feature);
            if(checkBox != null && checkBox.isChecked()) {
                String childCheckboxLabel = checkBox.getText().toString();
                if(childCheckboxLabel.contains("@") && variableDefaultValueMap != null && variableDefaultValueMap.keySet().contains(childCheckboxLabel.substring(childCheckboxLabel.indexOf("@") + 1))){
                    featureToAdd.setValue(childCheckboxLabel.substring(childCheckboxLabel.indexOf("@")));
                }
                selectedChildFeatures.add(featureToAdd);
            }
        }

        for(Map.Entry<String, String> feature : allParentFeatures){
            CheckBox checkBox = checkBoxParentEntryMap.get(feature);
            if(checkBox != null && checkBox.isChecked())
                selectedParentFeatures.add(feature);
        }



        //refresh content according to checkbox label

        if(textCheckbox != null && textCheckbox.getText() != null) {
            String textCheckboxLabel = textCheckbox.getText().toString();
            if (textCheckboxLabel.contains("@") && variableDefaultValueMap != null && variableDefaultValueMap.keySet().contains(textCheckboxLabel.substring(textCheckboxLabel.indexOf("@") + 1))) {
                textContent = textCheckboxLabel.substring(textCheckboxLabel.indexOf("@"));
            } else {
                textContent = featurePack.text;
            }
        }

        if(contentDescriptionCheckbox != null && contentDescriptionCheckbox.getText() != null) {
            String contentDescriptionCheckboxLabel = contentDescriptionCheckbox.getText().toString();
            if (contentDescriptionCheckboxLabel.contains("@") && variableDefaultValueMap != null && variableDefaultValueMap.keySet().contains(contentDescriptionCheckboxLabel.substring(contentDescriptionCheckboxLabel.indexOf("@") + 1))) {
                contentDescriptionContent = contentDescriptionCheckboxLabel.substring(contentDescriptionCheckboxLabel.indexOf("@"));
            } else {
                contentDescriptionContent = featurePack.contentDescription;
            }
        }

        if(viewIdCheckbox != null && viewIdCheckbox.getText() != null) {
            String viewIdCheckboxLabel = viewIdCheckbox.getText().toString();
            if (viewIdCheckboxLabel.contains("@") && variableDefaultValueMap != null && variableDefaultValueMap.keySet().contains(viewIdCheckboxLabel.substring(viewIdCheckboxLabel.indexOf("@") + 1))) {
                viewIdContent = viewIdCheckboxLabel.substring(viewIdCheckboxLabel.indexOf("@"));
            } else {
                viewIdContent = featurePack.viewId;
            }
        }


        //refresh the operation preview
        ((TextView) findViewById(R.id.previewContent)).setText(Html.fromHtml(readableDescriptionGenerator.generateReadableDescription(generateBlock())));
    }

    private void loadFeaturePackFromIntent(){
        Bundle extras = getIntent().getExtras();
        if(extras == null){
            //something wrong here!
        }
        else{
            triggerMode = extras.getInt("trigger");
            switch (triggerMode) {
                case TRIGGERED_BY_NEW_EVENT:
                    featurePack.packageName = extras.getString("packageName", "NULL");
                    featurePack.className = extras.getString("className", "NULL");
                    featurePack.text = extras.getString("text", "NULL");
                    featurePack.contentDescription = extras.getString("contentDescription", "NULL");
                    featurePack.viewId = extras.getString("viewId", "NULL");
                    featurePack.boundsInParent = extras.getString("boundsInParent", "NULL");
                    featurePack.boundsInScreen = extras.getString("boundsInScreen", "NULL");
                    featurePack.time = extras.getLong("time", -1);
                    featurePack.eventType = extras.getInt("eventType", -1);
                    featurePack.parentNode = new SerializableNodeInfo((AccessibilityNodeInfo)extras.getParcelable("parentNode"));
                    featurePack.childNodes = ((AccessibilityNodeInfoList)extras.getParcelable("childrenNodes")).getSerializableList();
                    featurePack.allNodes = ((AccessibilityNodeInfoList)extras.getParcelable("allNodes")).getSerializableList();
                    featurePack.isEditable = extras.getBoolean("isEditable");
                    featurePack.eventType = extras.getInt("eventType");
                    featurePack.screenshot = (File) extras.getSerializable("screenshot");
                    alternativeLabels = (HashSet<Map.Entry<String, String>>) extras.getSerializable("alternativeLabels");
                    break;
                case TRIGGERED_BY_EDIT:
                    originalScript = (SugiliteStartingBlock)extras.getSerializable("originalScript");
                    blockToEdit = (SugiliteOperationBlock)extras.getSerializable("blockToEdit");
                    featurePack = new SugiliteAvailableFeaturePack(blockToEdit.getFeaturePack());
                    alternativeLabels = blockToEdit.getElementMatchingFilter().alternativeLabels;
                    break;
            }
        }
    }

    private void loadFeaturePackFromSavedInstance(Bundle savedInstanceState){
        featurePack.packageName = savedInstanceState.getString("packageName", "NULL");
        featurePack.className = savedInstanceState.getString("className", "NULL");
        featurePack.text = savedInstanceState.getString("text", "NULL");
        featurePack.contentDescription = savedInstanceState.getString("contentDescription", "NULL");
        featurePack.viewId = savedInstanceState.getString("viewId", "NULL");
        featurePack.boundsInParent = savedInstanceState.getString("boundsInParent", "NULL");
        featurePack.boundsInScreen = savedInstanceState.getString("boundsInScreen", "NULL");
        featurePack.time = savedInstanceState.getLong("time", -1);
        featurePack.eventType = savedInstanceState.getInt("eventType", -1);
        featurePack.parentNode = savedInstanceState.getParcelable("parentNode");
        featurePack.childNodes = savedInstanceState.getParcelable("childrenNodes");
        featurePack.allNodes = savedInstanceState.getParcelable("allNodes");
        featurePack.isEditable = savedInstanceState.getBoolean("isEditable");
        featurePack.eventType = savedInstanceState.getInt("eventType");
        featurePack.screenshot = (File)savedInstanceState.getSerializable("screenshot");
        alternativeLabels = (HashSet<Map.Entry<String, String>>)savedInstanceState.getSerializable("alternativeLabels");
    }

    /**
     * save the block appropriately according the the trigger mode
     * 1. set the "next block" of the previous block to the current block
     * 2. set the "previous block" of the current block to the previous block
     *
     * @param operationBlock
     * @param activityContext
     */
    private void saveBlock(SugiliteOperationBlock operationBlock, Context activityContext){
        boolean success = false;
        switch (triggerMode){
            case TRIGGERED_BY_NEW_EVENT:
                operationBlock.setPreviousBlock(sugiliteData.getCurrentScriptBlock());
                if (sugiliteData.getCurrentScriptBlock() instanceof SugiliteOperationBlock) {
                    ((SugiliteOperationBlock) sugiliteData.getCurrentScriptBlock()).setNextBlock(operationBlock);
                }
                if (sugiliteData.getCurrentScriptBlock() instanceof SugiliteStartingBlock) {
                    ((SugiliteStartingBlock) sugiliteData.getCurrentScriptBlock()).setNextBlock(operationBlock);
                }
                sugiliteData.setCurrentScriptBlock(operationBlock);
                try {
                    sugiliteData.getScriptHead().relevantPackages.add(featurePack.packageName);
                    sugiliteScriptDao.save(sugiliteData.getScriptHead());
                    success = true;
                } catch (Exception e) {
                    e.printStackTrace();
                    success = false;
                }
                System.out.println("saved block");
                break;

            case TRIGGERED_BY_EDIT:
                SugiliteBlock currentBlock = originalScript;
                SugiliteBlock previousBlock = null;
                while(true){
                    if(currentBlock == null) {
                        new Exception("can't find the block to edit").printStackTrace();
                        break;
                    }
                    else if(currentBlock instanceof SugiliteStartingBlock) {
                        previousBlock = currentBlock;
                        currentBlock = ((SugiliteStartingBlock) currentBlock).getNextBlock();
                    }
                    else if (currentBlock instanceof SugiliteOperationBlock){
                        if(currentBlock.getBlockId() == blockToEdit.getBlockId()){
                            //matched
                            if(previousBlock instanceof SugiliteOperationBlock){
                                ((SugiliteOperationBlock)previousBlock).setNextBlock(operationBlock);
                                operationBlock.setPreviousBlock(previousBlock);
                                operationBlock.setNextBlock(((SugiliteOperationBlock) currentBlock).getNextBlock());
                                try {
                                    originalScript.relevantPackages.add(featurePack.packageName);
                                    sugiliteScriptDao.save(originalScript);
                                    success = true;
                                }
                                catch (Exception e){
                                    e.printStackTrace();
                                    success = false;
                                }
                                break;
                            }
                            else if(currentBlock.getPreviousBlock() instanceof SugiliteStartingBlock){
                                ((SugiliteStartingBlock)previousBlock).setNextBlock(operationBlock);
                                operationBlock.setPreviousBlock(previousBlock);
                                operationBlock.setNextBlock(((SugiliteOperationBlock) currentBlock).getNextBlock());
                                try {
                                    sugiliteScriptDao.save(originalScript);
                                    success = true;
                                }
                                catch (Exception e){
                                    success = false;
                                    e.printStackTrace();
                                }
                                break;
                            }
                            else {
                                //something wrong here
                            }
                        }
                        else{
                            previousBlock = currentBlock;
                            currentBlock = ((SugiliteOperationBlock) currentBlock).getNextBlock();
                        }
                    }
                }

                break;
        }
        setResult((success ? RESULT_OK : RESULT_CANCELED));
        finish();
        /*
        new AlertDialog.Builder(activityContext)
                .setTitle("Operation Recorded")
                .setMessage(Html.fromHtml(readableDescriptionGenerator.generateReadableDescription(operationBlock)))
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // continue
                        setResult((retVal ? RESULT_OK : RESULT_CANCELED));
                        finish();
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
       */

    }

    /**
     * generate a UIElementMatchingFilter based on the selection
     * @return
     */
    public UIElementMatchingFilter generateFilter(){
        //TODO: support variable here
        UIElementMatchingFilter filter = new UIElementMatchingFilter();
        if(withInAppSpinner.getSelectedItem().toString().contentEquals(readableDescriptionGenerator.getReadableName(featurePack.packageName))){
            filter.setPackageName(featurePack.packageName);
        }
        if(targetTypeSpinner.getSelectedItem().toString().contentEquals(featurePack.className)){
            filter.setClassName(featurePack.className);
        }
        if(textCheckbox != null && textCheckbox.isChecked()){
            filter.setText(textContent);
        }
        if(contentDescriptionCheckbox != null && contentDescriptionCheckbox.isChecked()){
            filter.setContentDescription(contentDescriptionContent);
        }
        if(viewIdCheckbox != null && viewIdCheckbox.isChecked()){
            filter.setViewId(viewIdContent);
        }
        if(boundsInParentCheckbox != null && boundsInParentCheckbox.isChecked()){
            filter.setBoundsInParent(Rect.unflattenFromString(featurePack.boundsInParent));
        }
        if(boundsInScreenCheckbox != null && boundsInScreenCheckbox.isChecked()){
            filter.setBoundsInScreen(Rect.unflattenFromString(featurePack.boundsInScreen));
        }

        if (selectedChildFeatures.size() > 0){
            UIElementMatchingFilter childFilter = new UIElementMatchingFilter();
            for(Map.Entry<String, String> entry : selectedChildFeatures){
                if(entry.getKey().contentEquals("Text")){
                    childFilter.setText(entry.getValue());
                }
                if(entry.getKey().contentEquals("ContentDescription")){
                    childFilter.setContentDescription(entry.getValue());
                }
                if(entry.getKey().contentEquals("ViewID")){
                    childFilter.setViewId(entry.getValue());                }
            }
            filter.setChildFilter(childFilter);
        }
        if (selectedParentFeatures.size() > 0){
            UIElementMatchingFilter parentFilter = new UIElementMatchingFilter();
            for(Map.Entry<String, String> entry : selectedParentFeatures){
                if(entry.getKey().contentEquals("Text")){
                    parentFilter.setText(entry.getValue());
                }
                if(entry.getKey().contentEquals("ContentDescription")){
                    parentFilter.setContentDescription(entry.getValue());
                }
                if(entry.getKey().contentEquals("ViewID")){
                    parentFilter.setViewId(entry.getValue());
                }
            }
            filter.setParentFilter(parentFilter);
        }

        if(alternativeLabels != null)
            filter.alternativeLabels = new HashSet<>(alternativeLabels);

        return filter;
    }

    private SugiliteOperationBlock generateBlock(){
        SugiliteOperation sugiliteOperation = new SugiliteOperation();
        String actionSpinnerSelectedItem = actionSpinner.getSelectedItem().toString();
        if (actionSpinnerSelectedItem.contentEquals("Click"))
            sugiliteOperation.setOperationType(SugiliteOperation.CLICK);
        if (actionSpinnerSelectedItem.contentEquals("Long Click"))
            sugiliteOperation.setOperationType(SugiliteOperation.LONG_CLICK);
        if (actionSpinnerSelectedItem.contentEquals("Set Text")) {
            sugiliteOperation = new SugiliteSetTextOperation();
            //replace set text parameter with parameter
            if(findViewById(R.id.action_parameter_set_text) != null) {
                String rawText = ((EditText) findViewById(R.id.action_parameter_set_text)).getText().toString();
                ((SugiliteSetTextOperation) sugiliteOperation).setText(rawText);
            }
            /*
            switch (triggerMode) {
                case TRIGGERED_BY_NEW_EVENT:
                    ((SugiliteSetTextOperation)sugiliteOperation).setText(textVariableParse(rawText, sugiliteData.getScriptHead().variableNameSet, sugiliteData.stringVariableMap));
                    break;
                case TRIGGERED_BY_EDIT:
                    ((SugiliteSetTextOperation)sugiliteOperation).setText(textVariableParse(rawText, originalScript.variableNameSet, sugiliteData.stringVariableMap));
                    break;
            }
            */
        }

        final SugiliteOperationBlock operationBlock = new SugiliteOperationBlock();
        operationBlock.setOperation(sugiliteOperation);
        operationBlock.setFeaturePack(featurePack);
        operationBlock.setElementMatchingFilter(generateFilter());
        operationBlock.setScreenshot(featurePack.screenshot);
        operationBlock.setDescription(readableDescriptionGenerator.generateReadableDescription(operationBlock));
        return operationBlock;
    }



    private String boldify(String text){
        return "<b>" + text + "</b>";
    }
    private LinearLayout generateRow(final CheckBox checkBox, final String label, final String defaultDefaultValue){
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        TextView setVariableLink = new TextView(this);
        setVariableLink.setText(Html.fromHtml("<u><i>Set as a parameter</i></u>"));
        setVariableLink.setTextColor(Color.parseColor("#3377dc"));
        setVariableLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setAsAParameterOnClick(v, checkBox, label, defaultDefaultValue);
            }
        });
        LinearLayout.LayoutParams checkBoxParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        linearLayout.addView(checkBox, checkBoxParams);
        LinearLayout.LayoutParams linkParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        linkParams.setMargins(50, 0, 0, 0);
        setVariableLink.setPadding(0, 0 ,0 ,0);
        linearLayout.addView(setVariableLink, linkParams);
        return linearLayout;
    }

}
