package edu.cmu.hcii.sugilite.pumice.dialog.intent_handler;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;

import edu.cmu.hcii.sugilite.Const;
import edu.cmu.hcii.sugilite.R;
import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.automation.ServiceStatusManager;
import edu.cmu.hcii.sugilite.model.block.SugiliteConditionBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;
import edu.cmu.hcii.sugilite.pumice.dialog.PumiceDialogManager;
import edu.cmu.hcii.sugilite.pumice.PumiceDemonstrationUtil;
import edu.cmu.hcii.sugilite.pumice.dialog.PumiceUtterance;

/**
 * @author toby
 * @date 12/19/18
 * @time 2:34 PM
 */

/**
 * the intent handler used for confirming whether the user wants to execute the script
 */
public class PumiceScriptExecutingConfirmationIntentHandler implements PumiceUtteranceIntentHandler {
    private SugiliteStartingBlock script;
    private Activity context;
    private PumiceDialogManager pumiceDialogManager;
    private String userUtterance;
    private boolean toAskForConfirmation;
    private SugiliteData sugiliteData;


    public PumiceScriptExecutingConfirmationIntentHandler(PumiceDialogManager pumiceDialogManager, Activity context, SugiliteData sugiliteData, SugiliteStartingBlock script, String userUtterance, boolean toAskForConfirmation){
        this.pumiceDialogManager = pumiceDialogManager;
        this.context = context;
        this.sugiliteData = sugiliteData;
        this.script = script;
        this.userUtterance = userUtterance;
        this.toAskForConfirmation = toAskForConfirmation;
    }

    @Override
    public void handleIntentWithUtterance(PumiceDialogManager dialogManager, PumiceIntent pumiceIntent, PumiceUtterance utterance) {

        if (pumiceIntent.equals(PumiceIntent.EXECUTION_CONFIRM_POSITIVE)) {
            dialogManager.sendAgentMessage(context.getString(R.string.executing_script), true, false);
            ServiceStatusManager serviceStatusManager = dialogManager.getServiceStatusManager();
            SharedPreferences sharedPreferences = dialogManager.getSharedPreferences();

            //load the knowledge manager into SugiliteData


            //===execute the script
            new AlertDialog.Builder(context)
                    .setTitle("Run Script")
                    .setMessage("Are you sure you want to run this script?")
                    .setPositiveButton("Run", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            //execute the script
                            PumiceDemonstrationUtil.executeScript(context, serviceStatusManager, script, sugiliteData, sharedPreferences, false, dialogManager, null, new Runnable() {
                                @Override
                                public void run() {
                                    //after execution runnable - notify the user that the script execution is completed
                                    dialogManager.sendAgentMessage(context.getString(R.string.complete_executing_script), true, false);
                                }
                            });
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // do nothing
                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();


            //go back to the default intent handler
            dialogManager.updateUtteranceIntentHandlerInANewState(new PumiceDefaultUtteranceIntentHandler(dialogManager, context, sugiliteData));

        }

        else if (pumiceIntent.equals(PumiceIntent.EXECUTION_CONFIRM_NEGATIVE)) {
            dialogManager.sendAgentMessage(context.getString(R.string.ok), true, false);
            dialogManager.updateUtteranceIntentHandlerInANewState(new PumiceDefaultUtteranceIntentHandler(dialogManager, context, sugiliteData));
            dialogManager.callSendPromptForTheIntentHandlerForCurrentIntentHandler();
        }

        else if (pumiceIntent.equals(PumiceIntent.UNRECOGNIZED)) {
            pumiceDialogManager.sendAgentMessage(context.getString(R.string.not_recognized_ask_for_binary), true, false);
            sendPromptForTheIntentHandler();
        }
    }

    @Override
    public void sendPromptForTheIntentHandler() {
        if (toAskForConfirmation) {
            //pumiceDialogManager.sendAgentMessage("I've understood your instruction: " +  "\"" + userUtterance + "\"." + " Do you want me to execute it now?", true, true);
            String currentScriptDescription = "";
            if (script.getNextBlock() != null && script.getNextBlock() instanceof SugiliteConditionBlock) {
                currentScriptDescription = script.getNextBlock().getPumiceUserReadableDecription();
            } else if (script instanceof SugiliteStartingBlock && script.getNextBlock() != null && script.getNextBlock().getNextBlock() == null) {
                currentScriptDescription = script.getNextBlock().getPumiceUserReadableDecription();
            } else {
                currentScriptDescription = script.getPumiceUserReadableDecription();
            }
            pumiceDialogManager.getSugiliteVoiceRecognitionListener().setContextPhrases(Const.CONFIRM_CONTEXT_WORDS);
            //pumiceDialogManager.sendAgentMessage(String.format("I've understood your instruction: %s. Do you want me to execute it now?", currentScriptDescription), true, true);
            pumiceDialogManager.sendAgentMessage(context.getString(R.string.ask_execute_now), true, true);

        } else {
            SugiliteData.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    handleIntentWithUtterance(pumiceDialogManager, PumiceIntent.EXECUTION_CONFIRM_POSITIVE, null);
                }
            });
        }

    }

    @Override
    public void setContext(Activity context) {
        this.context = context;
    }

    @Override
    public PumiceIntent detectIntentFromUtterance(PumiceUtterance utterance) {
        String utteranceContent = utterance.getContent().toString();
        if (utteranceContent != null && (utteranceContent.toLowerCase().contains("yes") || utteranceContent.toLowerCase().toLowerCase().contains("ok") || utteranceContent.toLowerCase().contains("yeah"))){
            return PumiceIntent.EXECUTION_CONFIRM_POSITIVE;
        } else if (utteranceContent != null && (utteranceContent.toLowerCase().contains("no"))) {
            return PumiceIntent.EXECUTION_CONFIRM_NEGATIVE;
        } else {
            return PumiceIntent.UNRECOGNIZED;
        }
    }

}
