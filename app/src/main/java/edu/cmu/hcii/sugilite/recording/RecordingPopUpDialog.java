package edu.cmu.hcii.sugilite.recording;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.inputmethod.InputMethodManager;
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

import edu.cmu.hcii.sugilite.R;
import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.communication.SugiliteBlockJSONProcessor;
import edu.cmu.hcii.sugilite.dao.SugiliteScreenshotManager;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptDao;
import edu.cmu.hcii.sugilite.model.AccessibilityNodeInfoList;
import edu.cmu.hcii.sugilite.model.block.SerializableNodeInfo;
import edu.cmu.hcii.sugilite.model.block.SugiliteAvailableFeaturePack;
import edu.cmu.hcii.sugilite.model.block.SugiliteBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteOperationBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;
import edu.cmu.hcii.sugilite.model.block.UIElementMatchingFilter;
import edu.cmu.hcii.sugilite.model.operation.SugiliteLoadVariableOperation;
import edu.cmu.hcii.sugilite.model.operation.SugiliteOperation;
import edu.cmu.hcii.sugilite.model.operation.SugiliteSetTextOperation;
import edu.cmu.hcii.sugilite.model.variable.StringVariable;
import edu.cmu.hcii.sugilite.model.variable.Variable;
import edu.cmu.hcii.sugilite.ui.ChooseVariableDialog;
import edu.cmu.hcii.sugilite.ui.ReadableDescriptionGenerator;
import edu.cmu.hcii.sugilite.ui.UIElementFeatureRecommender;

/**
 * @author toby
 * @date 7/22/16
 * @time 12:18 PM
 */
public class RecordingPopUpDialog {

    private int triggerMode;
    private SugiliteAvailableFeaturePack featurePack;
    private SharedPreferences sharedPreferences;
    private SugiliteScriptDao sugiliteScriptDao;
    private SugiliteBlockJSONProcessor jsonProcessor;
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
    private SugiliteScreenshotManager screenshotManager;
    private DialogInterface.OnClickListener editCallback;
    private RecordingSkipManager skipManager;
    private String childText = "";



    private Spinner actionSpinner, targetTypeSpinner, withInAppSpinner, readoutParameterSpinner, loadVariableParameterSpinner;
    private CheckBox textCheckbox, contentDescriptionCheckbox, viewIdCheckbox, boundsInParentCheckbox, boundsInScreenCheckbox;
    private EditText setTextEditText, loadVariableVariableDefaultValue, loadVariableVariableName;
    private String textContent, contentDescriptionContent, viewIdContent;
    private LinearLayout actionParameterSection, actionSection, readoutParameterSection, loadVariableParameterSection;
    private View dialogRootView;
    private LayoutInflater layoutInflater;

    private SugiliteStartingBlock originalScript;
    private SugiliteOperationBlock blockToEdit;

    private AlertDialog dialog;

    static final int PICK_CHILD_FEATURE = 1;
    static final int PICK_PARENT_FEATURE = 2;

    public static final int TRIGGERED_BY_NEW_EVENT = 1;
    public static final int TRIGGERED_BY_EDIT = 2;

    public RecordingPopUpDialog(SugiliteData sugiliteData, Context applicationContext, SugiliteAvailableFeaturePack featurePack, SharedPreferences sharedPreferences, LayoutInflater inflater, int triggerMode, Set<Map.Entry<String, String>> alternativeLabels){
        this.sharedPreferences = sharedPreferences;
        this.sugiliteData = sugiliteData;
        this.featurePack = featurePack;
        this.triggerMode = triggerMode;
        this.layoutInflater = inflater;
        this.alternativeLabels = new HashSet<>(alternativeLabels);
        this.screenshotManager = new SugiliteScreenshotManager(sharedPreferences, applicationContext);
        this.skipManager = new RecordingSkipManager();
        jsonProcessor = new SugiliteBlockJSONProcessor(applicationContext);
        sugiliteScriptDao = new SugiliteScriptDao(applicationContext);
        readableDescriptionGenerator = new ReadableDescriptionGenerator(applicationContext);
        checkBoxChildEntryMap = new HashMap<>();
        checkBoxParentEntryMap = new HashMap<>();
        identifierCheckboxMap = new HashMap<>();
        dialogRootView = inflater.inflate(R.layout.dialog_recording_pop_up, null);
        ContextThemeWrapper ctw = new ContextThemeWrapper(applicationContext, R.style.AlertDialogCustom);
        AlertDialog.Builder builder = new AlertDialog.Builder(applicationContext);
        builder.setView(dialogRootView)
                .setTitle("Sugilite Recording");
        dialog = builder.create();
        dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_box);
        //fetch the data capsuled in the intent
        //TODO: refactor so the service passes in a feature pack instead
        setupSelections();
    }

    public RecordingPopUpDialog(SugiliteData sugiliteData, Context applicationContext, SugiliteStartingBlock originalScript, SharedPreferences sharedPreferences, SugiliteOperationBlock blockToEdit, LayoutInflater inflater, int triggerMode, DialogInterface.OnClickListener callback){
        this.sharedPreferences = sharedPreferences;
        this.sugiliteData = sugiliteData;
        this.featurePack = blockToEdit.getFeaturePack();
        this.triggerMode = triggerMode;
        this.layoutInflater = inflater;
        this.originalScript = originalScript;
        this.blockToEdit = blockToEdit;
        this.editCallback = callback;
        this.skipManager = new RecordingSkipManager();
        jsonProcessor = new SugiliteBlockJSONProcessor(applicationContext);
        if(blockToEdit.getElementMatchingFilter().alternativeLabels != null)
            this.alternativeLabels = new HashSet<>(blockToEdit.getElementMatchingFilter().alternativeLabels);
        else
            this.alternativeLabels = new HashSet<>();
        this.screenshotManager = new SugiliteScreenshotManager(sharedPreferences, applicationContext);
        sugiliteScriptDao = new SugiliteScriptDao(applicationContext);
        readableDescriptionGenerator = new ReadableDescriptionGenerator(applicationContext);
        checkBoxChildEntryMap = new HashMap<>();
        checkBoxParentEntryMap = new HashMap<>();
        identifierCheckboxMap = new HashMap<>();
        dialogRootView = inflater.inflate(R.layout.dialog_recording_pop_up, null);
        ContextThemeWrapper ctw = new ContextThemeWrapper(applicationContext, R.style.AlertDialogCustom);
        AlertDialog.Builder builder = new AlertDialog.Builder(applicationContext);
        builder.setView(dialogRootView)
                .setTitle("Sugilite Recording");
        dialog = builder.create();
        dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_box);
        //fetch the data capsuled in the intent
        //TODO: refactor so the service passes in a feature pack instead
        setupSelections();

        //check skip status, click on the ok button if skip manager returns true
        if(skipManager.checkSkip(featurePack, triggerMode))
            OKButtonOnClick(null);
    }

    public void show(){
        dialog.show();
        dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
        //dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

    }

    public void finishActivity(View view){
        dialog.dismiss();
    }
    public void turnOffRecording(View view)
    {
        SharedPreferences.Editor prefEditor = sharedPreferences.edit();
        prefEditor.putBoolean("recording_in_process", false);
        prefEditor.commit();
        if(sugiliteData.initiatedExternally == true && sugiliteData.getScriptHead() != null)
            sugiliteData.communicationController.sendRecordingFinishedSignal(sugiliteData.getScriptHead().getScriptName());
            sugiliteData.sendCallbackMsg("FINISHED_RECORDING", jsonProcessor.scriptToJson(sugiliteData.getScriptHead()), sugiliteData.callbackString);
        dialog.dismiss();
    }

    public void OKButtonOnClick(final View view){
        //check if all fields are filled
        String actionSpinnerSelectedItem = actionSpinner.getSelectedItem().toString();
        if (actionSpinnerSelectedItem.contentEquals("Load as Variable")){
            if(loadVariableVariableName.getText().length() < 1){
                //variable name not filled, popup window
                AlertDialog.Builder builder = new AlertDialog.Builder(dialog.getContext());
                builder.setTitle("Variable Name not Set").setMessage("Please set the name of the varilable").setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).show();
            }
            return;
        }
        //TODO: add popup if current selection can't get unique button




        //add head if no one is present && this popup is triggered by new event
        if(triggerMode == TRIGGERED_BY_NEW_EVENT &&
                (sugiliteData.getScriptHead() == null ||
                        (!((SugiliteStartingBlock)sugiliteData.getScriptHead()).getScriptName().contentEquals(sharedPreferences.getString("scriptName", "defaultScript") + ".SugiliteScript")))){
            sugiliteData.setScriptHead(new SugiliteStartingBlock(sharedPreferences.getString("scriptName", "defaultScript") + ".SugiliteScript"));
            sugiliteData.setCurrentScriptBlock(sugiliteData.getScriptHead());
        }

        final SugiliteOperationBlock operationBlock = generateBlock();
        AlertDialog.Builder builder = new AlertDialog.Builder(dialog.getContext());
        //disable the confirmation dialog
        DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //take screenshot
                File screenshot = null;
                if (sharedPreferences.getBoolean("root_enabled", false)) {
                    try {
                        System.out.println("taking screen shot");
                        screenshot = screenshotManager.take(false);
                        operationBlock.setScreenshot(screenshot);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                saveBlock(operationBlock, dialogRootView.getContext());
                //fill in the text box if the operation is of SET_TEXT type
                if (operationBlock.getOperation().getOperationType() == SugiliteOperation.SET_TEXT && triggerMode == TRIGGERED_BY_NEW_EVENT)
                    sugiliteData.addInstruction(operationBlock);
                if (editCallback != null) {
                    System.out.println("calling callback");
                    editCallback.onClick(null, 0);
                }
            }
        };
        if(view == null) {
            builder.setTitle("Save Operation Confirmation").setMessage(Html.fromHtml("Are you sure you want to record the operation: " + readableDescriptionGenerator.generateReadableDescription(operationBlock)));
            builder.setPositiveButton("Yes", onClickListener).
                    setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });
            AlertDialog dialog = builder.create();
            dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
            dialog.show();
        }

        else {
            onClickListener.onClick(null, 0);
        }
    }




    public void setAsAParameterOnClick(View view){
        EditText actionParameter = (EditText)dialogRootView.findViewById(R.id.action_parameter_set_text);
        setAsAParameterOnClick(view, actionParameter, "", "");
    }


    public void setAsAParameterOnClick(View view, TextView actionParameter, String label, String defaultDefaultValue){
        Toast.makeText(view.getContext(), "set as a parameter", Toast.LENGTH_SHORT).show();
        if(actionParameter != null) {
            ChooseVariableDialog dialog;
            switch (triggerMode) {
                case TRIGGERED_BY_NEW_EVENT:
                    dialog = new ChooseVariableDialog(view.getContext(), actionParameter, layoutInflater, sugiliteData, sugiliteData.getScriptHead(), label, defaultDefaultValue);
                    dialog.show();
                    break;
                case TRIGGERED_BY_EDIT:
                    dialog = new ChooseVariableDialog(view.getContext(), actionParameter, layoutInflater, sugiliteData, originalScript, label, defaultDefaultValue);
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
        ListView listView = new ListView(view.getContext());
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(dialog.getContext(), android.R.layout.simple_list_item_single_choice, labelList);
        listView.setAdapter(adapter);
        AlertDialog.Builder builder = new AlertDialog.Builder(dialog.getContext());
        builder.setTitle("Alternative Labels")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setView(listView);
        AlertDialog dialog = builder.create();
        dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        dialog.show();
    }

    private void setupSelections(){

        //set up on click listeners
        ((TextView)dialogRootView.findViewById(R.id.parameterLink)).setText(Html.fromHtml("<p><u>Set as a parameter</u></p>"));
        dialogRootView.findViewById(R.id.okButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                OKButtonOnClick(v);
            }
        });
        dialogRootView.findViewById(R.id.cancelButton).setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View v) {
              finishActivity(v);
          }
      });
        dialogRootView.findViewById(R.id.recordingOffButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                turnOffRecording(v);
            }
        });
        dialogRootView.findViewById(R.id.parameterLink).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setAsAParameterOnClick(v);
            }
        });
        dialogRootView.findViewById(R.id.see_alternative_link).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                seeAlternativeLabelLinkOnClick(v);
            }
        });
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
        LinearLayout identifierLayout = (LinearLayout) dialogRootView.findViewById(R.id.identifier_layout);

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

        if((!featurePack.text.contentEquals("NULL")) && featurePack.text.length() > 0) {
            textCheckbox = new CheckBox(dialogRootView.getContext());
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


        if((!featurePack.contentDescription.contentEquals("NULL")) && featurePack.contentDescription.length() > 0) {
            contentDescriptionCheckbox = new CheckBox(dialogRootView.getContext());
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
            viewIdCheckbox = new CheckBox(dialogRootView.getContext());
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

        boolean hasChildText = false;
        for(Map.Entry<String, String> feature : allChildFeatures){
            if(feature.getKey() != null && feature.getValue() != null && feature.getValue().length() > 0){
                CheckBox childCheckBox = new CheckBox(dialogRootView.getContext());
                childCheckBox.setText(Html.fromHtml(boldify("Child " + feature.getKey() + ": ") + feature.getValue()));
                if(feature.getKey().contains("Text")) {
                    hasChildText = true;
                    childText += feature.getValue();
                    childText += " ";
                }
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


        boundsInParentCheckbox = new CheckBox(dialogRootView.getContext());
        boundsInParentCheckbox.setText(Html.fromHtml(boldify("Bounds in Parent: ") + featurePack.boundsInParent));
        if(autoFillEnabled)
            boundsInParentCheckbox.setChecked(recommender.chooseBoundsInParent());
        identifierLayout.addView(boundsInParentCheckbox, layoutParams);
        identifierCheckboxMap.put("boundsInParent", boundsInParentCheckbox);
        boundsInParentCheckbox.setOnCheckedChangeListener(identiferCheckboxChangeListener);

        boundsInScreenCheckbox = new CheckBox(dialogRootView.getContext());
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


        actionSpinner = (Spinner)dialogRootView.findViewById(R.id.action_dropdown);
        List<String> actionSpinnerItems = new ArrayList<>();
        if(featurePack.isEditable)
            actionSpinnerItems.add("Set Text");
        actionSpinnerItems.add("Click");
        if(featurePack.eventType == AccessibilityEvent.TYPE_VIEW_LONG_CLICKED)
            actionSpinnerItems.add("Long Click");
        if((featurePack.text != null && (! featurePack.text.contentEquals("NULL"))) || hasChildText || (featurePack.contentDescription != null && (! featurePack.contentDescription.contentEquals("NULL")))) {
            actionSpinnerItems.add("Read Out");
            actionSpinnerItems.add("Load as Variable");
        }

        ArrayAdapter<String> actionAdapter = new ArrayAdapter<String>(dialogRootView.getContext(), android.R.layout.simple_spinner_item, actionSpinnerItems);
        actionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        actionSpinner.setAdapter(actionAdapter);
        actionSpinner.setSelection(0);
        actionSpinner.setOnItemSelectedListener(spinnerSelectedListener);



        EditText textBox = (EditText)dialogRootView.findViewById(R.id.action_parameter_set_text);
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
        loadVariableVariableDefaultValue = (EditText)dialogRootView.findViewById(R.id.load_variable_default_value);
        loadVariableVariableName = (EditText)dialogRootView.findViewById(R.id.load_variable_variable_name);

        //set up read out parameter spinner
        readoutParameterSpinner = (Spinner)dialogRootView.findViewById(R.id.text_to_read_out_spinner);
        List<String> readoutParameterItems  = new ArrayList<>();
        if(featurePack.text != null && (!featurePack.text.contentEquals("NULL")))
            readoutParameterItems.add("Text: (" + featurePack.text + ")");
        if(featurePack.contentDescription != null && (!featurePack.contentDescription.contentEquals("NULL")))
            readoutParameterItems.add("Content Description: (" + featurePack.contentDescription + ")");
        if(hasChildText)
            readoutParameterItems.add("Child Text: (" + childText + ")");
        ArrayAdapter<String> readoutAdapter = new ArrayAdapter<String>(dialogRootView.getContext(), android.R.layout.simple_spinner_item, readoutParameterItems);
        readoutParameterSpinner.setAdapter(readoutAdapter);
        readoutParameterSpinner.setSelection(0);
        readoutParameterSpinner.setOnItemSelectedListener(spinnerSelectedListener);

        //set up load variable parameter spinner
        loadVariableParameterSpinner = (Spinner)dialogRootView.findViewById(R.id.element_to_load_variable_spinner);
        List<String> loadVariableParameterItems  = new ArrayList<>();
        if(featurePack.text != null && (!featurePack.text.contentEquals("NULL")))
            loadVariableParameterItems.add("Text: (" + featurePack.text + ")");
        if(featurePack.contentDescription != null && (!featurePack.contentDescription.contentEquals("NULL")))
            loadVariableParameterItems.add("Content Description: (" + featurePack.contentDescription + ")");
        if(hasChildText)
            loadVariableParameterItems.add("Child Text: (" + childText + ")");
        ArrayAdapter<String> loadVariableAdapter = new ArrayAdapter<String>(dialogRootView.getContext(), android.R.layout.simple_spinner_item, loadVariableParameterItems);
        loadVariableParameterSpinner.setAdapter(loadVariableAdapter);
        loadVariableParameterSpinner.setSelection(0);
        loadVariableParameterSpinner.setOnItemSelectedListener(spinnerSelectedListener);


        setTextEditText = (EditText)dialogRootView.findViewById(R.id.action_parameter_set_text);
        final InputMethodManager imm = (InputMethodManager) dialog.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        setTextEditText.setOnClickListener(new View.OnClickListener() {
            //force the keyboard to show when edit text on click
            @Override
            public void onClick(View v) {
                imm.showSoftInput(v, InputMethodManager.SHOW_IMPLICIT);
            }
        });
        actionParameterSection = (LinearLayout)dialogRootView.findViewById(R.id.action_parameter_section);
        readoutParameterSection = (LinearLayout)dialogRootView.findViewById(R.id.read_out_parameter_section);
        loadVariableParameterSection = (LinearLayout)dialogRootView.findViewById(R.id.load_variable_parameter_section);

        actionSection = (LinearLayout)dialogRootView.findViewById(R.id.action_section);
        actionSection.removeView(actionParameterSection);
        actionSection.removeView(readoutParameterSection);
        actionSection.removeView(loadVariableParameterSection);

        //set up target type spinner
        targetTypeSpinner = (Spinner)dialogRootView.findViewById(R.id.target_type_dropdown);
        List<String> targetTypeSpinnerItems = new ArrayList<>();
        targetTypeSpinnerItems.add(featurePack.className);
        targetTypeSpinnerItems.add("Any widget");
        ArrayAdapter<String> targetTypeAdapter = new ArrayAdapter<String>(dialogRootView.getContext(), android.R.layout.simple_spinner_item, targetTypeSpinnerItems);
        targetTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        targetTypeSpinner.setAdapter(targetTypeAdapter);
        targetTypeSpinner.setSelection(0);
        targetTypeSpinner.setOnItemSelectedListener(spinnerSelectedListener);

        //set up within app spinner
        withInAppSpinner = (Spinner)dialogRootView.findViewById(R.id.within_app_dropdown);
        List<String> withinAppSpinnerItems = new ArrayList<>();
        withinAppSpinnerItems.add(readableDescriptionGenerator.getReadableName(featurePack.packageName));
        withinAppSpinnerItems.add("Any app");
        ArrayAdapter<String> withInAppAdapter = new ArrayAdapter<String>(dialogRootView.getContext(), android.R.layout.simple_spinner_item, withinAppSpinnerItems);
        withInAppAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        withInAppSpinner.setAdapter(withInAppAdapter);
        withInAppSpinner.setSelection(0);
        withInAppSpinner.setOnItemSelectedListener(spinnerSelectedListener);

        //((TextView)findViewById(R.id.time)).setText("Event Time: " + dateFormat.format(c.getTime()) + "\nRecording script: " + sharedPreferences.getString("scriptName", "NULL"));
        //((TextView)findViewById(R.id.filteredNodeCount)).setText(generateFilterCount());
        ((TextView) dialogRootView.findViewById(R.id.previewContent)).setText(readableDescriptionGenerator.generateReadableDescription(generateBlock()));



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

        if (actionSpinnerSelectedItem.contentEquals("Read Out") && readoutParameterSection.getParent() == null) {
            actionSection.addView(readoutParameterSection);
        }
        if ((!actionSpinnerSelectedItem.contentEquals("Read Out")) && (readoutParameterSection.getParent() != null))
            actionSection.removeView(readoutParameterSection);

        if (actionSpinnerSelectedItem.contentEquals("Load as Variable") && loadVariableParameterSection.getParent() == null) {
            actionSection.addView(loadVariableParameterSection);
            String selectedTarget = loadVariableParameterSpinner.getSelectedItem().toString();
            if(loadVariableVariableDefaultValue.getText().toString().length() < 1) {
                if (selectedTarget.contains("Text")) {
                    loadVariableVariableDefaultValue.setText(featurePack.text);
                } else if (selectedTarget.contains("Content Description")) {
                    loadVariableVariableDefaultValue.setText(featurePack.contentDescription);
                } else if (selectedTarget.contains("Child Text")) {
                    loadVariableVariableDefaultValue.setText(childText);
                }
            }
        }
        if ((!actionSpinnerSelectedItem.contentEquals("Load as Variable")) && (loadVariableParameterSection.getParent() != null))
            actionSection.removeView(loadVariableParameterSection);

        //refresh "selectedchildren" and "selectedparent"
        selectedParentFeatures.clear();
        selectedChildFeatures.clear();

        for(Map.Entry<String, String> feature : allChildFeatures){
            CheckBox checkBox = checkBoxChildEntryMap.get(feature);
            Map.Entry<String, String> featureToAdd = new AbstractMap.SimpleEntry<String, String>(feature);
            if(checkBox != null && checkBox.isChecked()) {
                String childCheckboxLabel = extractParameter(checkBox.getText().toString());
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




        //use the "***Content" to generate the filter later

        if(textCheckbox != null && textCheckbox.getText() != null) {
            String textCheckboxLabel = extractParameter(textCheckbox.getText().toString());
            if (textCheckboxLabel.contains("@") && variableDefaultValueMap != null && variableDefaultValueMap.keySet().contains(textCheckboxLabel.substring(textCheckboxLabel.indexOf("@") + 1))) {
                textContent = textCheckboxLabel.substring(textCheckboxLabel.indexOf("@"));
            } else {
                textContent = featurePack.text;
            }
        }

        if(contentDescriptionCheckbox != null && contentDescriptionCheckbox.getText() != null) {
            String contentDescriptionCheckboxLabel = extractParameter(contentDescriptionCheckbox.getText().toString());
            if (contentDescriptionCheckboxLabel.contains("@") && variableDefaultValueMap != null && variableDefaultValueMap.keySet().contains(contentDescriptionCheckboxLabel.substring(contentDescriptionCheckboxLabel.indexOf("@") + 1))) {
                contentDescriptionContent = contentDescriptionCheckboxLabel.substring(contentDescriptionCheckboxLabel.indexOf("@"));
            } else {
                contentDescriptionContent = featurePack.contentDescription;
            }
        }

        if(viewIdCheckbox != null && viewIdCheckbox.getText() != null) {
            String viewIdCheckboxLabel = extractParameter(viewIdCheckbox.getText().toString());
            if (viewIdCheckboxLabel.contains("@") && variableDefaultValueMap != null && variableDefaultValueMap.keySet().contains(viewIdCheckboxLabel.substring(viewIdCheckboxLabel.indexOf("@") + 1))) {
                viewIdContent = viewIdCheckboxLabel.substring(viewIdCheckboxLabel.indexOf("@"));
            } else {
                viewIdContent = featurePack.viewId;
            }
        }


        //refresh the operation preview
        ((TextView) dialogRootView.findViewById(R.id.previewContent)).setText(Html.fromHtml(readableDescriptionGenerator.generateReadableDescription(generateBlock())));

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
        dialog.dismiss();
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
    private String extractParameter(String content){
        if(content.contains(":") && content.contains("@"))
            return content.substring(content.indexOf("@"), content.lastIndexOf(":"));
        else
            return content;
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
            filter.setText(extractParameter(textContent));
        }
        if(contentDescriptionCheckbox != null && contentDescriptionCheckbox.isChecked()){
            filter.setContentDescription(extractParameter(contentDescriptionContent));
        }
        if(viewIdCheckbox != null && viewIdCheckbox.isChecked()){
            filter.setViewId(extractParameter(viewIdContent));
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
                    childFilter.setText(extractParameter(entry.getValue()));
                }
                if(entry.getKey().contentEquals("ContentDescription")){
                    childFilter.setContentDescription(extractParameter(entry.getValue()));
                }
                if(entry.getKey().contentEquals("ViewID")){
                    childFilter.setViewId(extractParameter(entry.getValue()));                }
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
        if (actionSpinnerSelectedItem.contentEquals("Read Out")) {
            sugiliteOperation.setOperationType(SugiliteOperation.READ_OUT);
            if(dialogRootView.findViewById(R.id.text_to_read_out_spinner) != null){
                String selectionText = ((Spinner)dialogRootView.findViewById(R.id.text_to_read_out_spinner)).getSelectedItem().toString();
                sugiliteOperation.setParameter(selectionText.substring(0, selectionText.indexOf(":")));
            }
        }
        if (actionSpinnerSelectedItem.contentEquals("Load as Variable")){
            sugiliteOperation = new SugiliteLoadVariableOperation();
            sugiliteOperation.setOperationType(SugiliteOperation.LOAD_AS_VARIABLE);
            if(dialogRootView.findViewById(R.id.element_to_load_variable_spinner) != null){
                String selectionText = ((Spinner)dialogRootView.findViewById(R.id.element_to_load_variable_spinner)).getSelectedItem().toString();
                sugiliteOperation.setParameter(selectionText.substring(0, selectionText.indexOf(":")));
                String variableName = loadVariableVariableName.getText().toString();
                ((SugiliteLoadVariableOperation)sugiliteOperation).setVariableName(variableName);

                //add the variable to the symbol table
                StringVariable stringVariable = new StringVariable(variableName);
                stringVariable.type = Variable.LOAD_RUNTIME;
                String selectedTarget = loadVariableParameterSpinner.getSelectedItem().toString();
                if(loadVariableVariableDefaultValue.getText().toString().length() < 1) {
                    if (selectedTarget.contains("Text")) {
                        stringVariable.setValue(featurePack.text);
                    } else if (selectedTarget.contains("Content Description")) {
                        stringVariable.setValue(featurePack.contentDescription);
                    } else if (selectedTarget.contains("Child Text")) {
                        stringVariable.setValue(childText);
                    }
                }
                if(sugiliteData.stringVariableMap == null)
                    sugiliteData.stringVariableMap = new HashMap<String, Variable>();

                sugiliteData.stringVariableMap.put(variableName, stringVariable);
                if(triggerMode == TRIGGERED_BY_EDIT) {
                    originalScript.variableNameDefaultValueMap.put(variableName, stringVariable);
                }
                else if (triggerMode == TRIGGERED_BY_NEW_EVENT){
                    sugiliteData.getScriptHead().variableNameDefaultValueMap.put(variableName, stringVariable);
                }

            }
        }
        if (actionSpinnerSelectedItem.contentEquals("Set Text")) {
            sugiliteOperation = new SugiliteSetTextOperation();
            //replace set text parameter with parameter
            if(dialogRootView.findViewById(R.id.action_parameter_set_text) != null) {
                String rawText = ((EditText) dialogRootView.findViewById(R.id.action_parameter_set_text)).getText().toString();
                ((SugiliteSetTextOperation) sugiliteOperation).setText(extractParameter(rawText));
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
        LinearLayout linearLayout = new LinearLayout(dialogRootView.getContext());
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        TextView setVariableLink = new TextView(dialogRootView.getContext());
        setVariableLink.setText(Html.fromHtml("<u><i>Set as a parameter</i></u>"));
        setVariableLink.setTextColor(Color.parseColor("#8bb5f8"));
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
