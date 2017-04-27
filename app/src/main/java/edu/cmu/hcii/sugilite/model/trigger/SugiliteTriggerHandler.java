package edu.cmu.hcii.sugilite.model.trigger;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.view.LayoutInflater;
import android.widget.Toast;

import java.util.List;
import java.util.Map;

import edu.cmu.hcii.sugilite.Const;
import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptDao;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptFileDao;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptSQLDao;
import edu.cmu.hcii.sugilite.dao.SugiliteTriggerDao;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;
import edu.cmu.hcii.sugilite.model.variable.Variable;
import edu.cmu.hcii.sugilite.ui.dialog.VariableSetValueDialog;

import static edu.cmu.hcii.sugilite.Const.SQL_SCRIPT_DAO;

/**
 * Created by toby on 1/15/17.
 */

public class SugiliteTriggerHandler {
    private Context context;
    private SugiliteTriggerDao sugiliteTriggerDao;
    private SugiliteScriptDao sugiliteScriptDao;
    private List<SugiliteTrigger> allTriggers;
    private LayoutInflater layoutInflater;
    private SugiliteData sugiliteData;
    private SharedPreferences sharedPreferences;
    private String lastTriggerRan;

    public SugiliteTriggerHandler(Context context, SugiliteData sugiliteData, SharedPreferences sharedPreferences){
        this.context = context;
        this.layoutInflater = LayoutInflater.from(context);
        this.sugiliteData = sugiliteData;
        this.sharedPreferences = sharedPreferences;
        lastTriggerRan = "";
        sugiliteTriggerDao = new SugiliteTriggerDao(context);
        if(Const.DAO_TO_USE == SQL_SCRIPT_DAO)
            sugiliteScriptDao = new SugiliteScriptSQLDao(context);
        else
            sugiliteScriptDao = new SugiliteScriptFileDao(context, sugiliteData);
        allTriggers = sugiliteTriggerDao.getAllTriggers();
    }

    public void checkForAppLaunchTrigger(String packageName){
        //update the local trigger set if necessary

        //TODO: BUG here! need to handle this when I got chance
        long triggerSize = sugiliteTriggerDao.size();
        if(allTriggers.size() != triggerSize){
            allTriggers = sugiliteTriggerDao.getAllTriggers();
        }

        for(SugiliteTrigger trigger : allTriggers){
            if(trigger.getType() != SugiliteTrigger.APP_LAUNCH_TRIGGER)
                continue;
            if(!trigger.getAppPackageName().contentEquals(packageName))
                continue;
            if(!lastTriggerRan.contentEquals(trigger.getName())) {
                //trigger activated
                lastTriggerRan = trigger.getName();
                final Handler handler1 = new Handler();
                handler1.postDelayed(new Runnable() {
                    //run handler every 5 seconds if executing
                    @Override
                    public void run() {
                        lastTriggerRan = "";
                    }
                }, 5000);
                Toast.makeText(context, "TRIGGERING SCRIPT " + trigger.getScriptName(), Toast.LENGTH_SHORT).show();
                SugiliteStartingBlock script = null;
                try {
                    script = sugiliteScriptDao.read(trigger.getScriptName());
                }
                catch (Exception e){
                    e.printStackTrace();
                }
                if (script != null) {
                    VariableSetValueDialog variableSetValueDialog = new VariableSetValueDialog(context, layoutInflater, sugiliteData, script, sharedPreferences, SugiliteData.EXECUTION_STATE);
                    if (script.variableNameDefaultValueMap.size() > 0) {
                        //has variable
                        sugiliteData.stringVariableMap.putAll(script.variableNameDefaultValueMap);
                        boolean needUserInput = false;
                        for (Map.Entry<String, Variable> entry : script.variableNameDefaultValueMap.entrySet()) {
                            if (entry.getValue().type == Variable.USER_INPUT) {
                                needUserInput = true;
                                break;
                            }
                        }
                        if (needUserInput)
                            //show the dialog to obtain user input
                            variableSetValueDialog.show();
                        else
                            variableSetValueDialog.executeScript(null);
                    } else {
                        //execute the script without showing the dialog
                        variableSetValueDialog.executeScript(null);
                    }
                }
            }
        }
    }

}
