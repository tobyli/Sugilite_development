package edu.cmu.hcii.sugilite.ui;

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

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.cmu.hcii.sugilite.R;
import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;
import edu.cmu.hcii.sugilite.model.variable.StringVariable;
import edu.cmu.hcii.sugilite.model.variable.Variable;

/**
 * @author toby
 * @date 7/15/16
 * @time 8:47 PM
 */
public class VariableSetValueDialog {
    private Context context;
    private AlertDialog dialog;
    private Map<String,Variable> variableDefaultValueMap, stringVariableMap;
    private Map<String, Set<String>> variableNameAlternativeValueMap;
    private Map<String, View> variableSelectionViewMap;
    private SharedPreferences sharedPreferences;
    private SugiliteStartingBlock startingBlock;
    private SugiliteData sugiliteData;
    public static final int SCRIPT_DELAY = 3000;

    public VariableSetValueDialog(final Context context, LayoutInflater inflater, SugiliteData sugiliteData, SugiliteStartingBlock startingBlock, SharedPreferences sharedPreferences){
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        this.context = context;
        this.sharedPreferences = sharedPreferences;
        this.startingBlock = startingBlock;
        this.sugiliteData = sugiliteData;
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
            //TODO: use a spinner if alternatives can be found


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
                ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, spinnerItemList);
                spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                alternativeValueSpinner.setAdapter(spinnerAdapter);

                alternativeValueSpinner.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2));
                linearLayout.addView(alternativeValueSpinner);
                alternativeValueSpinner.setSelection(0);
                variableSelectionViewMap.put(entry.getKey(), alternativeValueSpinner);

            }
            else {
                //has no alternative values stored
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
                .setTitle("Sugilite Parameter Settings")
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
                    executeScript();
                    dialog.dismiss();
                }
                else {
                    Toast.makeText(context, "Please complete all fields", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public void executeScript(){
        SharedPreferences.Editor prefEditor = sharedPreferences.edit();
        //turn off the recording before executing
        prefEditor.putBoolean("recording_in_process", false);
        prefEditor.commit();
        //kill all the relevant packages
        for (String packageName : startingBlock.relevantPackages) {
            try {
                Process sh = Runtime.getRuntime().exec("su", null, null);
                OutputStream os = sh.getOutputStream();
                os.write(("am force-stop " + packageName).getBytes("ASCII"));
                os.flush();
                os.close();
                System.out.println(packageName);
            } catch (Exception e) {
                e.printStackTrace();
                // do nothing, likely this exception is caused by non-rooted device
            }
        }
        sugiliteData.runScript(startingBlock);
        try {
            Thread.sleep(SCRIPT_DELAY);
        } catch (Exception e) {
            // do nothing
        }
        //go to home screen for running the automation
        Intent startMain = new Intent(Intent.ACTION_MAIN);
        startMain.addCategory(Intent.CATEGORY_HOME);
        startMain.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        context.startActivity(startMain);
    }

}
