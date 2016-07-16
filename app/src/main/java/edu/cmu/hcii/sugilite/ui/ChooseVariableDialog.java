package edu.cmu.hcii.sugilite.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import edu.cmu.hcii.sugilite.R;
import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;
import edu.cmu.hcii.sugilite.model.variable.StringVariable;
import edu.cmu.hcii.sugilite.model.variable.Variable;

/**
 * @author toby
 * @date 7/15/16
 * @time 3:20 PM
 */
public class ChooseVariableDialog {
    Context context;
    AlertDialog dialog;
    String selectedItemName;
    SugiliteData sugiliteData;
    SugiliteStartingBlock startingBlock;
    final EditText newVariableNameEditText;
    final EditText defaultValueEditText;
    final EditText editText;

    public ChooseVariableDialog(final Context context, final EditText editText, LayoutInflater inflater, SugiliteData sugiliteData, SugiliteStartingBlock startingBlock){
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View dialogView = inflater.inflate(R.layout.dialog_choose_variable, null);
        String names[] ={"name", "drink", "coffee"};
        final ListView variableList = (ListView)dialogView.findViewById(R.id.existing_variable_list);
        if(variableList != null) {
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(context, android.R.layout.simple_list_item_single_choice, names);
            variableList.setAdapter(adapter);
        }
        newVariableNameEditText = (EditText)dialogView.findViewById(R.id.new_variable_name);
        defaultValueEditText = (EditText)dialogView.findViewById(R.id.variable_default_value);
        this.startingBlock = startingBlock;
        this.editText = editText;
        this.context = context;
        this.sugiliteData = sugiliteData;
        variableList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(view instanceof TextView){
                    selectedItemName = ((TextView) view).getText().toString();
                    newVariableNameEditText.setText("");
                    defaultValueEditText.setText("");
                }
            }
        });

        newVariableNameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(s.length() > 0){
                    //reset the listview selection
                    selectedItemName = s.toString();
                    variableList.clearChoices();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });


        builder.setView(dialogView)
                .setTitle("Sugilite Variable Selection")
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
                 if (selectedItemName == null || selectedItemName.length() < 1) {
                     Toast.makeText(context, "No item selected!", Toast.LENGTH_SHORT).show();
                 } else if (newVariableNameEditText.getText().length() > 0 && defaultValueEditText.getText().length() < 1) {
                     Toast.makeText(context, "No default value provided", Toast.LENGTH_SHORT).show();
                 } else {
                     if(newVariableNameEditText.getText().length() > 0){
                         //add the new variable and the new default value to the symbol table
                         String variableName = newVariableNameEditText.getText().toString();
                         String defaultValue = defaultValueEditText.getText().toString();
                         if(sugiliteData.stringVariableMap == null)
                             sugiliteData.stringVariableMap = new HashMap<String, Variable>();
                         sugiliteData.stringVariableMap.put(variableName, new StringVariable(variableName, defaultValue));
                         startingBlock.variableNameDefaultValueMap.put(variableName, new StringVariable(variableName, defaultValue));
                     }
                     editText.setText("@" + selectedItemName);
                     dialog.dismiss();
                 }
             }
         });
     }
}
