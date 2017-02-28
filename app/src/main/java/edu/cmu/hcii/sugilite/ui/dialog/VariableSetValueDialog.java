package edu.cmu.hcii.sugilite.ui.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.cmu.hcii.sugilite.Const;
import edu.cmu.hcii.sugilite.R;
import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.automation.Automator;
import edu.cmu.hcii.sugilite.model.block.SugiliteBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;
import edu.cmu.hcii.sugilite.model.variable.StringVariable;
import edu.cmu.hcii.sugilite.model.variable.Variable;

import static edu.cmu.hcii.sugilite.Const.SCRIPT_DELAY;

/**
 * @author toby
 * @date 7/15/16
 * @time 8:47 PM
 */
public class VariableSetValueDialog extends AbstractSugiliteDialog{
    private Context context;
    private AlertDialog dialog;
    private Map<String,Variable> variableDefaultValueMap, stringVariableMap;
    private Map<String, Set<String>> variableNameAlternativeValueMap;
    private Map<String, View> variableSelectionViewMap;
    private SharedPreferences sharedPreferences;
    private SugiliteStartingBlock startingBlock;
    private SugiliteData sugiliteData;
    private int state;

    public VariableSetValueDialog(final Context context, LayoutInflater inflater, SugiliteData sugiliteData, SugiliteStartingBlock startingBlock, SharedPreferences sharedPreferences, int state){
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        this.context = context;
        this.sharedPreferences = sharedPreferences;
        this.startingBlock = startingBlock;
        this.sugiliteData = sugiliteData;
        this.state = state;
        View dialogView = inflater.inflate(R.layout.dialog_variable_set_value, null);
        LinearLayout mainLayout = (LinearLayout)dialogView.findViewById(R.id.layout_variable_set_value);
        variableDefaultValueMap = startingBlock.variableNameDefaultValueMap;
        variableNameAlternativeValueMap = startingBlock.variableNameAlternativeValueMap;
        stringVariableMap = sugiliteData.stringVariableMap;
        variableSelectionViewMap = new HashMap<>();


        for(Map.Entry<String, Variable> entry : variableDefaultValueMap.entrySet()){
            if(entry.getValue().type == Variable.LOAD_RUNTIME)
                continue;
            LinearLayout linearLayout = new LinearLayout(context);
            linearLayout.setOrientation(LinearLayout.HORIZONTAL);
            linearLayout.setWeightSum(3);
            TextView variableName = new TextView(context);
            variableName.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            variableName.setWidth(0);
            variableName.setText(entry.getKey());
            linearLayout.addView(variableName);
            //use a spinner if alternatives can be found
            if(variableNameAlternativeValueMap != null && variableNameAlternativeValueMap.containsKey(entry.getKey())){
                //has alternative values stored
                Spinner alternativeValueSpinner = new Spinner(context);
                List<String> spinnerItemList = new ArrayList<>();
                if(entry.getValue() instanceof StringVariable)
                    spinnerItemList.add(((StringVariable) entry.getValue()).getValue());
                for(String alternative : variableNameAlternativeValueMap.get(entry.getKey())){
                    if(alternative.equals(((StringVariable) entry.getValue()).getValue()))
                        continue;
                    spinnerItemList.add(alternative);
                }
                ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, spinnerItemList);
                spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                alternativeValueSpinner.setAdapter(spinnerAdapter);

                alternativeValueSpinner.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2));
                linearLayout.addView(alternativeValueSpinner);
                alternativeValueSpinner.setSelection(0);
                variableSelectionViewMap.put(entry.getKey(), alternativeValueSpinner);

            }
            else {
                //has no alternative values stored - show edit text to prompt the user to enter value
                EditText variableValue = new EditText(context);
                variableValue.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 2));
                variableValue.setWidth(0);
            /*
            this part save the state of the last variable setting
            if(stringVariableMap.containsKey(entry.getKey()) && stringVariableMap.get(entry.getKey()) instanceof StringVariable)
                variableValue.setText(((StringVariable) stringVariableMap.get(entry.getKey())).getValue());
            */
                if (entry.getValue() instanceof StringVariable)
                    variableValue.setText(((StringVariable) entry.getValue()).getValue());
                linearLayout.addView(variableValue);
                variableSelectionViewMap.put(entry.getKey(), variableValue);

            }
            mainLayout.addView(linearLayout);
        }

        builder.setView(dialogView)
                .setTitle(Const.appNameUpperCase + " Parameter Settings")
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

    public void show(){
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                //check if all fields have been set
                boolean allReady = true;
                for(Map.Entry<String, View> entry: variableSelectionViewMap.entrySet()) {
                    if(entry.getValue() instanceof TextView) {
                        if (((TextView)entry.getValue()).getText().toString().length() < 1) {
                            allReady = false;
                            break;
                        }
                    }
                }
                if(allReady) {
                    //update all
                    for (Map.Entry<String, View> entry : variableSelectionViewMap.entrySet()) {
                        if(entry.getValue() instanceof TextView)
                            stringVariableMap.put(entry.getKey(), new StringVariable(entry.getKey(), ((TextView)entry.getValue()).getText().toString()));
                        else if (entry.getValue() instanceof Spinner){
                            stringVariableMap.put(entry.getKey(), new StringVariable(entry.getKey(), ((Spinner) entry.getValue()).getSelectedItem().toString()));
                        }
                    }
                    executeScript(null);
                    dialog.dismiss();
                }
                else {
                    Toast.makeText(context, "Please complete all fields", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * @param afterExecutionOperation @nullable, this operation will be pushed into the queue after the exeution
     * this is used for resume recording
     */
    public void executeScript(SugiliteBlock afterExecutionOperation){
        SharedPreferences.Editor prefEditor = sharedPreferences.edit();
        //turn off the recording before executing
        prefEditor.putBoolean("recording_in_process", false);
        prefEditor.apply();
        //kill all the relevant packages
        for (String packageName : startingBlock.relevantPackages) {
            Automator.killPackage(packageName);
        }
        sugiliteData.runScript(startingBlock, afterExecutionOperation, this.state);
        //need to have this delay to ensure that the killing has finished before we start executing
        try {
            Thread.sleep(SCRIPT_DELAY);
        } catch (Exception e) {
            // do nothing
        }
        //go to home screen for running the automation
        Intent startMain = new Intent(Intent.ACTION_MAIN);
        startMain.addCategory(Intent.CATEGORY_HOME);
        startMain.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        if(!(context instanceof Activity)){
            startMain.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        context.startActivity(startMain);
    }

}
