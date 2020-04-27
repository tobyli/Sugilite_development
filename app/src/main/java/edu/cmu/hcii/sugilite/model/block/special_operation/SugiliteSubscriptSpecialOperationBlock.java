package edu.cmu.hcii.sugilite.model.block.special_operation;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptDao;
import edu.cmu.hcii.sugilite.model.block.SugiliteSpecialOperationBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;
import edu.cmu.hcii.sugilite.model.variable.Variable;
import edu.cmu.hcii.sugilite.model.variable.VariableValue;
import edu.cmu.hcii.sugilite.pumice.PumiceDemonstrationUtil;
import edu.cmu.hcii.sugilite.ui.dialog.VariableSetValueDialog;

import static edu.cmu.hcii.sugilite.source_parsing.SugiliteScriptExpression.addQuoteToTokenIfNeeded;

/**
 * @author toby
 * @date 10/31/16
 * @time 2:07 PM
 */


public class SugiliteSubscriptSpecialOperationBlock extends SugiliteSpecialOperationBlock {

    private String subscriptName;
    private List<VariableValue<String>> variableValues;

    public SugiliteSubscriptSpecialOperationBlock(String subscriptName){
        super();
        this.subscriptName = subscriptName;
        this.variableValues = new ArrayList<>();
        this.setDescription("Run subscript");
    }

    public SugiliteSubscriptSpecialOperationBlock(){
        super();
        this.variableValues = new ArrayList<>();
        this.setDescription("Run subscript");
    }

    public void setSubscriptName (String subscriptName){
        this.subscriptName = subscriptName;
        this.setDescription("Run subscript: " + subscriptName);
    }

    public String getSubscriptName (){
        return subscriptName;
    }

    public void setVariableValues(List<VariableValue<String>> variableValues) {
        this.variableValues = variableValues;
    }

    public List<VariableValue<String>> getVariableValues() {
        return variableValues;
    }

    @Override
    public void run(Context context, final SugiliteData sugiliteData, SugiliteScriptDao sugiliteScriptDao, final SharedPreferences sharedPreferences) throws Exception{
        final SugiliteStartingBlock script = sugiliteScriptDao.read(subscriptName);

        //send an agent message through pumiceDialogManager if one is available
        if (sugiliteData.pumiceDialogManager != null){
            String parameterizedProcedureName = PumiceDemonstrationUtil.removeScriptExtension(subscriptName).replace("Procedure_", "");
            for (VariableValue<String> variable : variableValues) {
                parameterizedProcedureName = parameterizedProcedureName.replace("[" + variable.getVariableName() + "]", "[" + variable.getVariableValue() + "]");
            }

            sugiliteData.pumiceDialogManager.sendAgentMessage("Executing the procedure: " + parameterizedProcedureName, true, false);
        }

        if (script != null) {
            Handler mainHandler = new Handler(context.getMainLooper());
            final Context finalContext = context;
            Runnable myRunnable = new Runnable() {
                @Override
                public void run() {
                    VariableSetValueDialog variableSetValueDialog = new VariableSetValueDialog(finalContext, sugiliteData, script, sharedPreferences, sugiliteData.getCurrentSystemState(), sugiliteData.pumiceDialogManager, false);
                    if (script.variableNameDefaultValueMap.size() > 0) {
                        //has variable

                        //dump the default values into the active symbolic table first
                        sugiliteData.variableNameVariableValueMap.putAll(script.variableNameDefaultValueMap);

                        //process variableValues
                        Map<String, VariableValue> alreadyExternallyLoadedStringVariableMap = new HashMap<>();

                        //check if variableValue is among alternatives before adding to alreadyLoadedStringVariableMap
                        try {
                            for (VariableValue<String> stringVariable : variableValues) {
                                //TODO: make sure the case matches in variable value
                                if (script.variableNameAlternativeValueMap.containsKey(stringVariable.getVariableName())){
                                    if (script.variableNameAlternativeValueMap.get(stringVariable.getVariableName()).contains(stringVariable)) {
                                        //the variable has alternative values (i.e., spinner type variable), and the alternative values contain the variable value
                                        alreadyExternallyLoadedStringVariableMap.put(stringVariable.getVariableName(), stringVariable);
                                    } else if (script.variableNameAlternativeValueMap.get(stringVariable.getVariableName()).isEmpty()) {
                                        //the variable does not have alternative values (i.e., textbox type variable)
                                        alreadyExternallyLoadedStringVariableMap.put(stringVariable.getVariableName(), stringVariable);
                                    }
                                } else {
                                    throw new Exception("Can't find the loaded variable in the script");
                                }
                            }
                        } catch (Exception e) {
                            //TODO: better handle the exception
                            e.printStackTrace();
                        }

                        //dump the values from alreadyExternallyLoadedStringVariableMap into the active symbolic table
                        sugiliteData.variableNameVariableValueMap.putAll(alreadyExternallyLoadedStringVariableMap);

                        boolean needUserInput = false;
                        for (Map.Entry<String, Variable> entry : script.variableNameVariableObjectMap.entrySet()) {
                            if (entry.getValue().getVariableType() == Variable.USER_INPUT && (!alreadyExternallyLoadedStringVariableMap.containsKey(entry.getKey()))) {
                                needUserInput = true;
                                break;
                            }
                        }

                        variableSetValueDialog.setAlreadyLoadedVariableMap(alreadyExternallyLoadedStringVariableMap);

                        if (needUserInput) {
                            //show the dialog to obtain user input - run getNextBlockToRun() after finish executing the current one
                            variableSetValueDialog.show(getNextBlockToRun(), sugiliteData.afterExecutionRunnable);
                        }

                        else {
                            variableSetValueDialog.executeScript(getNextBlockToRun(), sugiliteData.pumiceDialogManager, sugiliteData.afterExecutionRunnable);
                        }
                    } else {
                        //execute the script without showing the dialog - run getNextBlockToRun() after finish executing the current one
                        variableSetValueDialog.executeScript(getNextBlockToRun(), sugiliteData.pumiceDialogManager, sugiliteData.afterExecutionRunnable);
                    }
                }
            };
            mainHandler.post(myRunnable);
        }
        else {
            //ERROR: CAN'T FIND THE SCRIPT
            System.out.println("Can't find the script " + subscriptName);
            throw new Exception("Can't find the script " + subscriptName);
        }
    }

    @Override
    public String toString() {
        return "(" + "call" + " " + "run_script" + " " + addQuoteToTokenIfNeeded(subscriptName) + ")";
    }

    @Override
    public String getPumiceUserReadableDecription() {
        return String.format("Run the subscript named \"%s\".", subscriptName);
    }
}
