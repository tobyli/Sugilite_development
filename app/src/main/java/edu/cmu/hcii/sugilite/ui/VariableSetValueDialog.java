package edu.cmu.hcii.sugilite.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;

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
    private Map<String,EditText> variableEditTextMap;

    public VariableSetValueDialog(final Context context, LayoutInflater inflater, SugiliteData sugiliteData, SugiliteStartingBlock startingBlock){
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        this.context = context;
        View dialogView = inflater.inflate(R.layout.dialog_variable_set_value, null);
        LinearLayout mainLayout = (LinearLayout)dialogView.findViewById(R.id.layout_variable_set_value);
        variableDefaultValueMap = startingBlock.variableNameDefaultValueMap;
        stringVariableMap = sugiliteData.stringVariableMap;
        variableEditTextMap = new HashMap<>();

        for(Map.Entry<String, Variable> entry : variableDefaultValueMap.entrySet()){
            LinearLayout linearLayout = new LinearLayout(context);
            linearLayout.setOrientation(LinearLayout.HORIZONTAL);
            linearLayout.setWeightSum(3);
            TextView variableName = new TextView(context);
            variableName.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            variableName.setWidth(0);
            variableName.setText(entry.getKey());
            EditText variableValue = new EditText(context);
            variableValue.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 2));
            variableValue.setWidth(0);
            if(stringVariableMap.containsKey(entry.getKey()) && stringVariableMap.get(entry.getKey()) instanceof StringVariable)
                variableValue.setText(((StringVariable) stringVariableMap.get(entry.getKey())).getValue());
            else if(entry.getValue() instanceof StringVariable)
                variableValue.setText(((StringVariable) entry.getValue()).getValue());
            linearLayout.addView(variableName);
            linearLayout.addView(variableValue);
            variableEditTextMap.put(entry.getKey(), variableValue);
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
                for(Map.Entry<String, EditText> entry: variableEditTextMap.entrySet()) {
                    if(entry.getValue().getText().toString().length() < 1) {
                        allReady = false;
                        break;
                    }
                }
                if(allReady) {
                    //update all
                    for (Map.Entry<String, EditText> entry : variableEditTextMap.entrySet()) {
                        stringVariableMap.put(entry.getKey(), new StringVariable(entry.getValue().getText().toString()));
                    }
                    dialog.dismiss();
                }
                else {
                    Toast.makeText(context, "Please complete all fields", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

}
