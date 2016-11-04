package edu.cmu.hcii.sugilite.ui.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Rect;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.cmu.hcii.sugilite.R;
import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptDao;
import edu.cmu.hcii.sugilite.model.block.SugiliteErrorHandlingForkBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteOperationBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteSpecialOperationBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;
import edu.cmu.hcii.sugilite.model.block.UIElementMatchingFilter;
import edu.cmu.hcii.sugilite.model.operation.SugiliteLoadVariableOperation;
import edu.cmu.hcii.sugilite.model.operation.SugiliteOperation;
import edu.cmu.hcii.sugilite.model.variable.StringVariable;
import edu.cmu.hcii.sugilite.model.variable.Variable;

/**
 * @author toby
 * @date 7/15/16
 * @time 3:20 PM
 */
public class ChooseVariableDialog extends AbstractSugiliteDialog {
    private Context context;
    private AlertDialog dialog;
    private String selectedItemName;
    private SugiliteData sugiliteData;
    private SugiliteStartingBlock startingBlock;
    private final EditText newVariableNameEditText;
    private final EditText defaultValueEditText;
    private final TextView editText;
    private final String label;
    private SugiliteScriptDao sugiliteScriptDao;
    private boolean saveTheBlock;
    private AccessibilityNodeInfo selectedNode;
    private String variableName = "";

    /**
     *
     * @param context
     * @param @nullable editText
     * @param inflater
     * @param sugiliteData
     * @param startingBlock
     * @param @nullable label
     * @param defaultDefaultValue
     * @param saveTheBlock whether to save this block at the end
     * @param @nullable sugiliteScriptDao
     * @param @nullable selectedNode
     */
    public ChooseVariableDialog(final Context context, final TextView editText, LayoutInflater inflater, SugiliteData sugiliteData, SugiliteStartingBlock startingBlock, String label, String defaultDefaultValue, boolean saveTheBlock, SugiliteScriptDao sugiliteScriptDao, AccessibilityNodeInfo selectedNode){
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View dialogView = inflater.inflate(R.layout.dialog_choose_variable, null);
        List<String> existingVariables = new ArrayList<>();
        for(Map.Entry<String, Variable> entry : startingBlock.variableNameDefaultValueMap.entrySet()){
            if(entry.getValue() instanceof StringVariable){
                existingVariables.add(entry.getKey() + ": (" + ((StringVariable) entry.getValue()).getValue() + ")");
            }
        }
        final ListView variableList = (ListView)dialogView.findViewById(R.id.existing_variable_list);
        if(variableList != null) {
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(context, android.R.layout.simple_list_item_single_choice, existingVariables);
            variableList.setAdapter(adapter);
        }
        View emptyView = dialogView.findViewById(R.id.empty);
        variableList.setEmptyView(emptyView);

        newVariableNameEditText = (EditText)dialogView.findViewById(R.id.new_variable_name);
        defaultValueEditText = (EditText)dialogView.findViewById(R.id.variable_default_value);
        defaultValueEditText.setText(defaultDefaultValue);
        this.startingBlock = startingBlock;
        this.editText = editText;
        this.context = context;
        this.sugiliteData = sugiliteData;
        this.label = label;
        this.saveTheBlock = saveTheBlock;
        this.sugiliteScriptDao = sugiliteScriptDao;
        this.selectedNode = selectedNode;
        variableList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(view instanceof TextView){
                    String entry = ((TextView) view).getText().toString();
                    selectedItemName = entry.substring(0, entry.indexOf(":"));
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
                if (s.length() > 0) {
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
        dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

    }

    public void show(){

         dialog.show();
         dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener()
         {
             @Override
             public void onClick(View v)
             {
                 String defaultValueToShow = "";
                 if (selectedItemName == null || selectedItemName.length() < 1) {
                     Toast.makeText(context, "No item selected!", Toast.LENGTH_SHORT).show();
                 } else if (newVariableNameEditText.getText().length() > 0 && defaultValueEditText.getText().length() < 1) {
                     Toast.makeText(context, "No default value provided", Toast.LENGTH_SHORT).show();
                 } else {
                     if(newVariableNameEditText.getText().length() > 0){
                         //add the new variable and the new default value to the symbol table
                         variableName = newVariableNameEditText.getText().toString();
                         String defaultValue = defaultValueEditText.getText().toString();
                         defaultValueToShow = defaultValue;
                         if(sugiliteData.stringVariableMap == null)
                             sugiliteData.stringVariableMap = new HashMap<String, Variable>();
                         sugiliteData.stringVariableMap.put(variableName, new StringVariable(variableName, defaultValue));
                         startingBlock.variableNameDefaultValueMap.put(variableName, new StringVariable(variableName, defaultValue));
                     }
                     else {
                         //TODO: user has selected an existing variable
                         Variable defaultVariableValue = startingBlock.variableNameDefaultValueMap.get(selectedItemName);
                         if(defaultVariableValue != null && defaultVariableValue instanceof StringVariable)
                             defaultValueToShow = ((StringVariable) defaultVariableValue).getValue();
                     }

                     if(editText != null && label != null) {
                         if (label.length() > 0) {
                             //choosing variable for a generated checkbox row
                             editText.setText(Html.fromHtml("<b>" + label + ":</b> " + "@" + selectedItemName + ": (" + defaultValueToShow + ")"));
                         } else
                             editText.setText("@" + selectedItemName + ": (" + defaultValueToShow + ")");
                     }
                     if(saveTheBlock)
                         saveBlock(selectedNode, variableName);
                     dialog.dismiss();
                 }
             }
         });

     }

    /**
     * this method should create a "load as a variable" block and add the block to the current recording
     */
    private void saveBlock(AccessibilityNodeInfo selectedNode, String variableName){
        SugiliteOperationBlock operationBlock = new SugiliteOperationBlock();
        SugiliteOperation sugiliteOperation = new SugiliteLoadVariableOperation();
        sugiliteOperation.setOperationType(SugiliteOperation.LOAD_AS_VARIABLE);
        sugiliteOperation.setParameter("text");
        ((SugiliteLoadVariableOperation)sugiliteOperation).setVariableName(variableName);
        UIElementMatchingFilter filter = new UIElementMatchingFilter();
        if(selectedNode.getPackageName() != null)
            filter.setPackageName(selectedNode.getPackageName().toString());
        if(selectedNode.getClassName() != null)
            filter.setClassName(selectedNode.getClassName().toString());
        Rect boundsInScreen = new Rect();
        selectedNode.getBoundsInScreen(boundsInScreen);
        filter.setBoundsInScreen(boundsInScreen);

        operationBlock.setOperation(sugiliteOperation);
        operationBlock.setElementMatchingFilter(filter);
        operationBlock.setDescription("Load the text at (" + boundsInScreen.toShortString() + ") to the variable " + variableName);
        System.out.println("CREATE LOAD_AS_VARIABLE BLOCK FOR " + selectedNode.getText());
        //save the block
        operationBlock.setPreviousBlock(sugiliteData.getCurrentScriptBlock());
        if (sugiliteData.getCurrentScriptBlock() instanceof SugiliteOperationBlock) {
            ((SugiliteOperationBlock) sugiliteData.getCurrentScriptBlock()).setNextBlock(operationBlock);
        }
        else if (sugiliteData.getCurrentScriptBlock() instanceof SugiliteStartingBlock) {
            ((SugiliteStartingBlock) sugiliteData.getCurrentScriptBlock()).setNextBlock(operationBlock);
        }
        else if (sugiliteData.getCurrentScriptBlock() instanceof SugiliteErrorHandlingForkBlock){
            ((SugiliteErrorHandlingForkBlock) sugiliteData.getCurrentScriptBlock()).setAlternativeNextBlock(operationBlock);
        }
        else if (sugiliteData.getCurrentScriptBlock() instanceof SugiliteSpecialOperationBlock){
            ((SugiliteSpecialOperationBlock) sugiliteData.getCurrentScriptBlock()).setNextBlock(operationBlock);
        }
        else{
            throw new RuntimeException("Unsupported Block Type!");
        }
        sugiliteData.setCurrentScriptBlock(operationBlock);
        try {
            sugiliteScriptDao.save(sugiliteData.getScriptHead());
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("saved read out block");

    }
}
