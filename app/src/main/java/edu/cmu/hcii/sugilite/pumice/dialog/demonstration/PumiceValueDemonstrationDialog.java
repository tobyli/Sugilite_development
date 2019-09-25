package edu.cmu.hcii.sugilite.pumice.dialog.demonstration;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.Nullable;
import android.widget.Toast;

import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.automation.ServiceStatusManager;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptDao;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptFileDao;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;
import edu.cmu.hcii.sugilite.ontology.SugiliteRelation;
import edu.cmu.hcii.sugilite.pumice.PumiceDemonstrationUtil;
import edu.cmu.hcii.sugilite.pumice.dialog.intent_handler.PumiceUserExplainValueIntentHandler;
import edu.cmu.hcii.sugilite.pumice.kb.PumiceValueQueryKnowledge;
import edu.cmu.hcii.sugilite.pumice.ui.PumiceDialogActivity;
import edu.cmu.hcii.sugilite.ui.dialog.SugiliteProgressDialog;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.VerbalInstructionIconManager;

/**
 * @author toby
 * @date 12/17/18
 * @time 2:38 PM
 */

/**
 * dialog used for initiating a user demonstration for obtaining a value - constructed and called from  PumiceUserExplainValueIntentHandler
 */
public class PumiceValueDemonstrationDialog {
    private AlertDialog dialog;
    private Activity context;
    private String valueKnowledgeName;
    private String userUtterance;
    private PumiceUserExplainValueIntentHandler parentIntentHandler;
    private SugiliteScriptDao sugiliteScriptDao;
    private SharedPreferences sharedPreferences;
    private SugiliteData sugiliteData;
    private ServiceStatusManager serviceStatusManager;
    private VerbalInstructionIconManager verbalInstructionIconManager;

    private SugiliteRelation resolveValueQueryOperationSugiliteRelationType;

    public PumiceValueDemonstrationDialog(Activity context, String valueKnowledgeName, String userUtterance, SharedPreferences sharedPreferences, SugiliteData sugiliteData, ServiceStatusManager serviceStatusManager, @Nullable SugiliteRelation resolveValueQueryOperationSugiliteRelationType, PumiceUserExplainValueIntentHandler parentIntentHandler){
        this.context = context;
        this.valueKnowledgeName = valueKnowledgeName;
        this.userUtterance = userUtterance;
        this.parentIntentHandler = parentIntentHandler;
        this.sharedPreferences = sharedPreferences;
        this.sugiliteData = sugiliteData;
        this.verbalInstructionIconManager = sugiliteData.verbalInstructionIconManager;
        this.serviceStatusManager = serviceStatusManager;
        this.resolveValueQueryOperationSugiliteRelationType = resolveValueQueryOperationSugiliteRelationType;
        this.sugiliteScriptDao = new SugiliteScriptFileDao(context, sugiliteData);
        constructDialog();
    }

    private void constructDialog(){
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
        dialogBuilder.setMessage("Please start demonstrating how to find out the value of " + valueKnowledgeName +  ". " + "Click OK to continue.")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String scriptName = "ValueQuery_" + valueKnowledgeName; //determine the script name

                        //create a callback to be called when the recording ends
                        Runnable callback = new Runnable() {
                            @Override
                            public void run() {
                                sugiliteData.currentPumiceValueDemonstrationType = null;
                                SugiliteData.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        //turn off the overlay
                                        if(verbalInstructionIconManager != null){
                                            verbalInstructionIconManager.turnOffCatOverlay();
                                        }

                                        //get the result script
                                        try {
                                            SugiliteStartingBlock script = sugiliteScriptDao.read(scriptName + ".SugiliteScript");
                                            if (script != null) {
                                                onDemonstrationReady(script);
                                            } else {
                                                throw new RuntimeException("can't find the script");
                                            }

                                        } catch (Exception e){
                                            throw new RuntimeException("failed to read the script!");
                                        }
                                    }
                                });
                            }
                        };
                        sugiliteData.valueDemonstrationVariableName = valueKnowledgeName;
                        //set sugiliteData.currentPumiceValueDemonstrationType for displaying Pumice overlays
                        sugiliteData.currentPumiceValueDemonstrationType = resolveValueQueryOperationSugiliteRelationType;
                        PumiceDemonstrationUtil.initiateDemonstration(context, serviceStatusManager, sharedPreferences, scriptName, sugiliteData, callback, sugiliteScriptDao, verbalInstructionIconManager);
                    }
                });
        dialog = dialogBuilder.create();
    }

    //called when the demonstration is ready
    public void onDemonstrationReady(SugiliteStartingBlock script){
        //resume the Sugilite agent activity
        Intent resumeActivity = new Intent(context, PumiceDialogActivity.class);
        resumeActivity.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        context.startActivityIfNeeded(resumeActivity, 0);

        Toast.makeText(context, "Demonstration Ready!", Toast.LENGTH_SHORT).show();

        //run the returnResultCallback when the result if ready
        //TODO: determine the value type
        PumiceValueQueryKnowledge valueQueryKnowledge = new PumiceValueQueryKnowledge(context, valueKnowledgeName, PumiceValueQueryKnowledge.ValueType.NUMERICAL, script);
        parentIntentHandler.returnUserExplainValueResult(valueQueryKnowledge);
    }

    //show the dialog
    public void show() {
        dialog.show();
    }
}