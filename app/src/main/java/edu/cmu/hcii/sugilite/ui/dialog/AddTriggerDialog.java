package edu.cmu.hcii.sugilite.ui.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.cmu.hcii.sugilite.R;
import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptDao;

import static android.view.View.GONE;

/**
 * Created by toby on 1/11/17.
 */

public class AddTriggerDialog extends AbstractSugiliteDialog {

    private Context context;
    private AlertDialog dialog;
    private SugiliteData sugiliteData;
    private SugiliteScriptDao sugiliteScriptDao;
    private Spinner triggerTypeSpinner, chooseTriggerAppSpinner, chooseNotificationTriggerAppSpinner, chooseScriptTrigger;
    private TextView chooseTriggerAppTextView, chooseNotificationTriggerAppTextView, notificationTriggerContentTextView;
    private EditText notificationTriggerContentEditText;
    private List<ApplicationInfo> packages;



    public AddTriggerDialog(Context context, LayoutInflater inflater, SugiliteData sugiliteData, SugiliteScriptDao sugiliteScriptDao, PackageManager pm){
        this.context = context;
        this.sugiliteData = sugiliteData;
        this.sugiliteScriptDao = sugiliteScriptDao;
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View dialogView = inflater.inflate(R.layout.dialog_add_trigger, null);

        packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        Map<String, String> appNamePackageNameMap = new HashMap<>();
        for(ApplicationInfo info : packages){
            appNamePackageNameMap.put(pm.getApplicationLabel(info).toString(), info.packageName);
        }

        triggerTypeSpinner = (Spinner)dialogView.findViewById(R.id.spinner_trigger_type);
        chooseNotificationTriggerAppSpinner = (Spinner)dialogView.findViewById(R.id.spinner_choose_notification_trigger_app);
        chooseTriggerAppSpinner = (Spinner)dialogView.findViewById(R.id.spinner_choose_trigger_app);
        chooseScriptTrigger = (Spinner)dialogView.findViewById(R.id.spinner_choose_script_to_trigger);
        notificationTriggerContentEditText = (EditText)dialogView.findViewById(R.id.edittext_notification_contains);
        chooseTriggerAppTextView = (TextView)dialogView.findViewById(R.id.textview_choose_trigger_app);
        chooseNotificationTriggerAppTextView = (TextView)dialogView.findViewById(R.id.textview_choose_notification_trigger_app);
        notificationTriggerContentTextView = (TextView)dialogView.findViewById(R.id.textview_notification_contains);

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
                        chooseTriggerAppTextView.setVisibility(View.VISIBLE);
                        chooseNotificationTriggerAppSpinner.setVisibility(GONE);
                        notificationTriggerContentEditText.setVisibility(GONE);
                        chooseNotificationTriggerAppTextView.setVisibility(GONE);
                        notificationTriggerContentTextView.setVisibility(GONE);
                        chooseTriggerAppSpinner.setSelection(0);
                        break;
                    case 1:
                        //Notification content selected
                        chooseNotificationTriggerAppSpinner.setVisibility(View.VISIBLE);
                        notificationTriggerContentEditText.setVisibility(View.VISIBLE);
                        chooseNotificationTriggerAppTextView.setVisibility(View.VISIBLE);
                        notificationTriggerContentTextView.setVisibility(View.VISIBLE);
                        chooseNotificationTriggerAppSpinner.setSelection(0);
                        notificationTriggerContentEditText.setText("");
                        chooseTriggerAppSpinner.setVisibility(GONE);
                        chooseTriggerAppTextView.setVisibility(GONE);
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        //initiate the chooseNotificationTriggerAppSpinner
        List<String> chooseNotificationTriggerAppSpinnerList = new ArrayList<>(appNamePackageNameMap.keySet());
        ArrayAdapter<String> chooseNotificationTriggerAppSpinnerAdapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, chooseNotificationTriggerAppSpinnerList);
        chooseNotificationTriggerAppSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        chooseNotificationTriggerAppSpinner.setAdapter(chooseNotificationTriggerAppSpinnerAdapter);

        //initiate the chooseTriggerAppSpinner
        List<String> chooseTriggerAppSpinnerList = new ArrayList<>(appNamePackageNameMap.keySet());
        ArrayAdapter<String> chooseTriggerAppSpinnerAdapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, chooseTriggerAppSpinnerList);
        chooseTriggerAppSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        chooseTriggerAppSpinner.setAdapter(chooseTriggerAppSpinnerAdapter);

        //initiate the chooseScriptTrigger
        List<String> allScriptNameList = sugiliteScriptDao.getAllNames();

        //remove the suffix in the script name
        for(String name : allScriptNameList){
            name = name.replace(".Sugilite", "");
        }
        ArrayAdapter<String> chooseScriptSpinnerAdapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, allScriptNameList);
        chooseScriptSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        chooseScriptTrigger.setAdapter(chooseScriptSpinnerAdapter);

        builder.setView(dialogView)
                .setTitle("Add Trigger")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

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


    public void show() {
        dialog.show();
    }
}
