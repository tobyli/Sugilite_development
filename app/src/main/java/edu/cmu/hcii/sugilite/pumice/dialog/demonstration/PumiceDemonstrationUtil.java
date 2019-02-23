package edu.cmu.hcii.sugilite.pumice.dialog.demonstration;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;

import java.util.Map;

import edu.cmu.hcii.sugilite.Const;
import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.automation.ServiceStatusManager;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptDao;
import edu.cmu.hcii.sugilite.model.block.SugiliteBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;
import edu.cmu.hcii.sugilite.model.variable.Variable;
import edu.cmu.hcii.sugilite.pumice.dialog.PumiceDialogManager;
import edu.cmu.hcii.sugilite.ui.dialog.VariableSetValueDialog;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.VerbalInstructionIconManager;

/**
 * @author toby
 * @date 1/7/19
 * @time 2:44 PM
 */
public class PumiceDemonstrationUtil {
    /**
     * initiate a demonstration recording from a pumice intent handler
     * @param context
     * @param serviceStatusManager
     * @param sharedPreferences
     * @param scriptName
     * @param sugiliteData
     * @param callback
     * @param sugiliteScriptDao
     * @param verbalInstructionIconManager
     */
    public static void initiateDemonstration(Context context, ServiceStatusManager serviceStatusManager, SharedPreferences sharedPreferences, String scriptName, SugiliteData sugiliteData, Runnable callback, SugiliteScriptDao sugiliteScriptDao, VerbalInstructionIconManager verbalInstructionIconManager){
        if(!serviceStatusManager.isRunning()){
            //prompt the user if the accessibility service is not active
            AlertDialog.Builder builder1 = new AlertDialog.Builder(context);
            builder1.setTitle("Service not running")
                    .setMessage("The " + Const.appNameUpperCase + " accessiblity service is not enabled. Please enable the service in the phone settings before recording.")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            serviceStatusManager.promptEnabling();
                            //do nothing
                        }
                    }).show();
        } else {
            //start demonstration
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("scriptName", scriptName);
            editor.putBoolean("recording_in_process", true);
            editor.commit();

            //set the system state
            sugiliteData.setCurrentSystemState(SugiliteData.RECORDING_STATE);


            //set the active script to the newly created script
            sugiliteData.initiateScript(scriptName + ".SugiliteScript", callback); //add the end recording callback
            sugiliteData.initiatedExternally = false;

            //save the newly created script to DB
            try {
                sugiliteScriptDao.save(sugiliteData.getScriptHead());
                sugiliteScriptDao.commitSave();
            }
            catch (Exception e){
                e.printStackTrace();
            }

            //send the phone back to the home screen
            Intent startMain = new Intent(Intent.ACTION_MAIN);
            startMain.addCategory(Intent.CATEGORY_HOME);
            startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            context.startActivity(startMain);

            //turn on the cat overlay to prepare for demonstration
            if(verbalInstructionIconManager != null){
                verbalInstructionIconManager.turnOnCatOverlay();
            }
        }
    }

    /**
     * execute a script from a pumice intent handler --> check the service status and the variable values before doing so
     * @param activityContext
     * @param serviceStatusManager
     * @param script
     * @param sugiliteData
     * @param layoutInflater
     * @param sharedPreferences
     * @param dialogManager
     */
    public static void executeScript(Activity activityContext, ServiceStatusManager serviceStatusManager, SugiliteStartingBlock script, SugiliteData sugiliteData, LayoutInflater layoutInflater, SharedPreferences sharedPreferences, @Nullable PumiceDialogManager dialogManager, @Nullable SugiliteBlock afterExexecutionOperation, @Nullable Runnable afterExecutionRunnable){
        if(!serviceStatusManager.isRunning()){
            //prompt the user if the accessiblity service is not active
            activityContext.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    AlertDialog.Builder builder1 = new AlertDialog.Builder(activityContext);
                    builder1.setTitle("Service not running")
                            .setMessage("The Sugilite accessiblity service is not enabled. Please enable the service in the phone settings before recording.")
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    serviceStatusManager.promptEnabling();
                                    //do nothing
                                }
                            }).show();
                }
            });
        }
        else {
            activityContext.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    VariableSetValueDialog variableSetValueDialog = new VariableSetValueDialog(activityContext, layoutInflater, sugiliteData, script, sharedPreferences, SugiliteData.EXECUTION_STATE, dialogManager);
                    if(script.variableNameDefaultValueMap.size() > 0) {

                        //has variable
                        sugiliteData.stringVariableMap.putAll(script.variableNameDefaultValueMap);
                        boolean needUserInput = false;

                        //check if any of the variables needs user input
                        for(Map.Entry<String, Variable> entry : script.variableNameDefaultValueMap.entrySet()){
                            if(entry.getValue().type == Variable.USER_INPUT){
                                needUserInput = true;
                                break;
                            }
                        }
                        if(needUserInput) {
                            //show the dialog to obtain user input
                            variableSetValueDialog.show();
                        }
                        else {
                            variableSetValueDialog.executeScript(afterExexecutionOperation, dialogManager, afterExecutionRunnable);
                        }
                    }
                    else{
                        //execute the script without showing the dialog
                        variableSetValueDialog.executeScript(afterExexecutionOperation, dialogManager, afterExecutionRunnable);
                    }
                }
            });
        }
    }
}
