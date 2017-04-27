package edu.cmu.hcii.sugilite.model.block;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.view.LayoutInflater;

import java.util.Map;

import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptDao;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptSQLDao;
import edu.cmu.hcii.sugilite.model.variable.Variable;
import edu.cmu.hcii.sugilite.ui.dialog.VariableSetValueDialog;

/**
 * @author toby
 * @date 10/31/16
 * @time 2:07 PM
 */
public class SugiliteSubscriptSpecialOperationBlock extends SugiliteSpecialOperationBlock {

    private String subscriptName;

    public SugiliteSubscriptSpecialOperationBlock(String subscriptName){
        super();
        this.subscriptName = subscriptName;
        this.setDescription("Run subscript");
    }

    public SugiliteSubscriptSpecialOperationBlock(){
        super();
        this.setDescription("Run subscript");
    }

    public void setSubscriptName (String subscriptName){
        this.subscriptName = subscriptName;
        this.setDescription("Run subscript: " + subscriptName);
    }

    public String getSubscriptName (){
        return subscriptName;
    }

    @Override
    public void run(Context context, final SugiliteData sugiliteData, SugiliteScriptDao sugiliteScriptDao, final SharedPreferences sharedPreferences) throws Exception{
        final SugiliteStartingBlock script = sugiliteScriptDao.read(subscriptName);
        if(script != null) {
            Handler mainHandler = new Handler(context.getMainLooper());
            final LayoutInflater inflater = (LayoutInflater) context. getSystemService( Context. LAYOUT_INFLATER_SERVICE );
            final Context finalContext = context;
            Runnable myRunnable = new Runnable() {
                @Override
                public void run() {
                    VariableSetValueDialog variableSetValueDialog = new VariableSetValueDialog(finalContext, inflater, sugiliteData, script, sharedPreferences, sugiliteData.getCurrentSystemState());
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
                            variableSetValueDialog.executeScript(getNextBlock());
                    } else {
                        //execute the script without showing the dialog
                        variableSetValueDialog.executeScript(getNextBlock());
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
}
