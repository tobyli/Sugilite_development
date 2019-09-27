package edu.cmu.hcii.sugilite.ui.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.cmu.hcii.sugilite.R;
import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptDao;
import edu.cmu.hcii.sugilite.dao.SugiliteTriggerDao;
import edu.cmu.hcii.sugilite.model.trigger.SugiliteTrigger;
import edu.cmu.hcii.sugilite.pumice.PumiceDemonstrationUtil;
import edu.cmu.hcii.sugilite.ui.main.FragmentScriptListTab;
import edu.cmu.hcii.sugilite.ui.main.FragmentTriggerListTab;

import static android.view.View.GONE;

/**
 * Created by toby on 1/11/17.
 */

public class AddTriggerDialog implements AbstractSugiliteDialog {

    private Context context;
    private AlertDialog dialog;
    private SugiliteData sugiliteData;
    private SugiliteScriptDao sugiliteScriptDao;
    private Spinner triggerTypeSpinner, chooseTriggerAppSpinner, chooseNotificationTriggerAppSpinner, chooseScriptTriggerSpinner;
    private TextView chooseTriggerAppPromptTextView, chooseNotificationTriggerAppPromptTextView, notificationTriggerContentPromptTextView;
    private EditText notificationTriggerContentEditText, triggerNameEditText;
    private List<ApplicationInfo> packages;
    private SugiliteTriggerDao sugiliteTriggerDao;
    private FragmentTriggerListTab triggerTab;

    private Map<String, String> appNamePackageNameMap;
    private Map<String, String> packageNameAppNameMap;
    private Map<String, Integer> appNameIndexMap;
    private List<String> appNameOrderedList;

    private List<String> scriptReadableNameOrderedList;
    private Map<String, Integer> scriptReadableNameIndexMap;

    private boolean isLoadedFromExistingTrigger = false;
    private String originalTriggerName = null;


    public AddTriggerDialog(final Context context, LayoutInflater inflater, SugiliteData sugiliteData, SugiliteScriptDao sugiliteScriptDao, PackageManager pm, Fragment triggerListTab) throws Exception{
        this.context = context;
        this.sugiliteData = sugiliteData;
        this.sugiliteScriptDao = sugiliteScriptDao;
        if(triggerListTab instanceof FragmentTriggerListTab) {
            this.triggerTab = (FragmentTriggerListTab) triggerListTab;
        }
        sugiliteTriggerDao = new SugiliteTriggerDao(context);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View dialogView = inflater.inflate(R.layout.dialog_add_trigger, null);

        packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);

        //initiate appNamePackageNameMap and appNameOrderedList
        appNamePackageNameMap = new HashMap<>();
        packageNameAppNameMap = new HashMap<>();
        appNameIndexMap = new HashMap<>();
        appNameOrderedList = new ArrayList<>();
        for(ApplicationInfo info : packages){
            appNamePackageNameMap.put(pm.getApplicationLabel(info).toString(), info.packageName);
            packageNameAppNameMap.put(info.packageName, pm.getApplicationLabel(info).toString());

        }

        //filter out those without a readable app name
        for(Map.Entry<String, String> entry : appNamePackageNameMap.entrySet()) {
            if (!entry.getKey().equals(entry.getValue())) {
                appNameOrderedList.add(entry.getKey());
            }
        }

        //sort the appNameList
        Collections.sort(appNameOrderedList);
        for (int i = 0; i < appNameOrderedList.size(); i ++) {
            appNameIndexMap.put(appNameOrderedList.get(i), i);
        }



        triggerTypeSpinner = (Spinner)dialogView.findViewById(R.id.spinner_trigger_type);
        chooseNotificationTriggerAppSpinner = (Spinner)dialogView.findViewById(R.id.spinner_choose_notification_trigger_app);
        chooseTriggerAppSpinner = (Spinner)dialogView.findViewById(R.id.spinner_choose_trigger_app);
        chooseScriptTriggerSpinner = (Spinner)dialogView.findViewById(R.id.spinner_choose_script_to_trigger);
        notificationTriggerContentEditText = (EditText)dialogView.findViewById(R.id.edittext_notification_contains);
        triggerNameEditText = (EditText)dialogView.findViewById(R.id.editText_trigger_name);
        chooseTriggerAppPromptTextView = (TextView)dialogView.findViewById(R.id.textview_choose_trigger_app);
        chooseNotificationTriggerAppPromptTextView = (TextView)dialogView.findViewById(R.id.textview_choose_notification_trigger_app);
        notificationTriggerContentPromptTextView = (TextView)dialogView.findViewById(R.id.textview_notification_contains);

        //initiate the trigger type spinner
        List<String> triggerTypeSpinnerItemList = Arrays.asList("App Launch", "Notification Content");
        ArrayAdapter<String> triggerTypeSpinnerAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, triggerTypeSpinnerItemList);
        triggerTypeSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        triggerTypeSpinner.setAdapter(triggerTypeSpinnerAdapter);
        triggerTypeSpinner.setSelection(0);
        triggerTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position){
                    case 0:
                        //App Launch selected
                        chooseTriggerAppSpinner.setVisibility(View.VISIBLE);
                        chooseTriggerAppPromptTextView.setVisibility(View.VISIBLE);
                        chooseNotificationTriggerAppSpinner.setVisibility(GONE);
                        notificationTriggerContentEditText.setVisibility(GONE);
                        chooseNotificationTriggerAppPromptTextView.setVisibility(GONE);
                        notificationTriggerContentPromptTextView.setVisibility(GONE);
                        break;
                    case 1:
                        //Notification content selected
                        chooseNotificationTriggerAppSpinner.setVisibility(View.VISIBLE);
                        notificationTriggerContentEditText.setVisibility(View.VISIBLE);
                        chooseNotificationTriggerAppPromptTextView.setVisibility(View.VISIBLE);
                        notificationTriggerContentPromptTextView.setVisibility(View.VISIBLE);
                        chooseTriggerAppSpinner.setVisibility(GONE);
                        chooseTriggerAppPromptTextView.setVisibility(GONE);
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        //initiate the chooseNotificationTriggerAppSpinner
        List<String> chooseNotificationTriggerAppSpinnerList = new ArrayList<>(appNameOrderedList);
        ArrayAdapter<String> chooseNotificationTriggerAppSpinnerAdapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, chooseNotificationTriggerAppSpinnerList);
        chooseNotificationTriggerAppSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        chooseNotificationTriggerAppSpinner.setAdapter(chooseNotificationTriggerAppSpinnerAdapter);

        //initiate the chooseTriggerAppSpinner
        List<String> chooseTriggerAppSpinnerList = new ArrayList<>(appNameOrderedList);
        ArrayAdapter<String> chooseTriggerAppSpinnerAdapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, chooseTriggerAppSpinnerList);
        chooseTriggerAppSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        chooseTriggerAppSpinner.setAdapter(chooseTriggerAppSpinnerAdapter);

        //initiate the chooseScriptTrigger
        List<String> allScriptNameList = sugiliteScriptDao.getAllNames();
        scriptReadableNameOrderedList = new ArrayList<>();
        //remove the suffix in the script name
        for(String name : allScriptNameList){
            scriptReadableNameOrderedList.add(name.replace(".SugiliteScript", ""));
        }
        Collections.sort(scriptReadableNameOrderedList);
        scriptReadableNameIndexMap = new HashMap<>();
        for (int i = 0; i < scriptReadableNameOrderedList.size(); i ++) {
            scriptReadableNameIndexMap.put(scriptReadableNameOrderedList.get(i), i);
        }

        ArrayAdapter<String> chooseScriptSpinnerAdapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, scriptReadableNameOrderedList);
        chooseScriptSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        chooseScriptTriggerSpinner.setAdapter(chooseScriptSpinnerAdapter);
        builder.setView(dialogView)
                .setTitle("New Trigger")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //intentionally do nothing -- will define later in show()
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });


        dialog = builder.create();
    }

    private void okButtonOnClick () {
        if (triggerTypeSpinner.getSelectedItemPosition() == 0) {
            //app launch trigger selected
            String triggerName = triggerNameEditText.getText().toString();
            String scriptName = chooseScriptTriggerSpinner.getSelectedItem().toString().concat(".SugiliteScript");
            String packageName = appNamePackageNameMap.get(chooseTriggerAppSpinner.getSelectedItem().toString());
            if(scriptName != null && scriptName.length() > 0 && packageName != null && packageName.length() > 0 && triggerName.length() > 0) {
                SugiliteTrigger trigger = new SugiliteTrigger(triggerName, scriptName, "", packageName, SugiliteTrigger.APP_LAUNCH_TRIGGER);
                try {
                    //delete the old script if recover from triggers
                    if (isLoadedFromExistingTrigger) {
                        sugiliteTriggerDao.delete(originalTriggerName);
                    }
                    sugiliteTriggerDao.save(trigger);
                    dialog.dismiss();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            else {
                PumiceDemonstrationUtil.showSugiliteToast("Please finish the form", Toast.LENGTH_SHORT);
            }

        } else if (triggerTypeSpinner.getSelectedItemPosition() == 1) {
            //notification trigger selected
            String triggerName = triggerNameEditText.getText().toString();
            String notificationContent = notificationTriggerContentEditText.getText().toString();
            String scriptName = chooseScriptTriggerSpinner.getSelectedItem().toString().concat(".SugiliteScript");
            String packageName = appNamePackageNameMap.get(chooseNotificationTriggerAppSpinner.getSelectedItem().toString());
            if(scriptName != null && scriptName.length() > 0 && packageName != null && packageName.length() > 0 && triggerName.length() > 0 && notificationContent.length() > 0) {
                SugiliteTrigger trigger = new SugiliteTrigger(triggerName, scriptName, notificationContent, packageName, SugiliteTrigger.NOTIFICATION_TRIGGER);
                try {
                    //delete the old script if recover from triggers
                    if (isLoadedFromExistingTrigger) {
                        sugiliteTriggerDao.delete(originalTriggerName);
                    }
                    sugiliteTriggerDao.save(trigger);
                    dialog.dismiss();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            else {
                PumiceDemonstrationUtil.showSugiliteToast("Please finish the form", Toast.LENGTH_SHORT);
            }

        }
        //refresh the trigger list tab
        if(triggerTab != null) {
            triggerTab.setUpTriggerList();
        }
    }

    public void loadFromExistingTrigger (SugiliteTrigger trigger) throws Exception {
        originalTriggerName = trigger.getName();
        triggerNameEditText.setText(trigger.getName());
        dialog.setTitle("Edit Trigger");

        isLoadedFromExistingTrigger = true;
        if (trigger.getType() == SugiliteTrigger.APP_LAUNCH_TRIGGER) {
            triggerTypeSpinner.setSelection(0);

            if (scriptReadableNameIndexMap.containsKey(trigger.getScriptName().replace(".SugiliteScript", ""))) {
                chooseScriptTriggerSpinner.setSelection(scriptReadableNameIndexMap.get(trigger.getScriptName().replace(".SugiliteScript", "")));
            } else {
                throw new Exception("Can't find the script: " + trigger.getScriptName().replace(".SugiliteScript", ""));
            }

            if (packageNameAppNameMap.containsKey(trigger.getAppPackageName()) && appNameIndexMap.containsKey(packageNameAppNameMap.get(trigger.getAppPackageName()))) {
                chooseTriggerAppSpinner.setSelection(appNameIndexMap.get(packageNameAppNameMap.get(trigger.getAppPackageName())));
            }
            else {
                throw new Exception("Can't find the trigger app: " + trigger.getAppPackageName());
            }
        } else if (trigger.getType() == SugiliteTrigger.NOTIFICATION_TRIGGER) {
            triggerTypeSpinner.setSelection(1);
            if (scriptReadableNameIndexMap.containsKey(trigger.getScriptName().replace(".SugiliteScript", ""))) {
                chooseScriptTriggerSpinner.setSelection(scriptReadableNameIndexMap.get(trigger.getScriptName().replace(".SugiliteScript", "")));
            } else {
                throw new Exception("Can't find the script: " + trigger.getScriptName().replace(".SugiliteScript", ""));
            }
            if (packageNameAppNameMap.containsKey(trigger.getAppPackageName()) && appNameIndexMap.containsKey(packageNameAppNameMap.get(trigger.getAppPackageName()))) {
                chooseNotificationTriggerAppSpinner.setSelection(appNameIndexMap.get(packageNameAppNameMap.get(trigger.getAppPackageName())));
            } else {
                throw new Exception("Can't find the trigger app: " + trigger.getAppPackageName());
            }
            notificationTriggerContentEditText.setText(trigger.getTriggerContent());
        }
    }







    public void show() {
        dialog.show();
        Button b = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                okButtonOnClick();
            }
        });
    }
}
