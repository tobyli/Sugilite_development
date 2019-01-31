package edu.cmu.hcii.sugilite.pumice.dialog.intent_handler;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

import java.util.Calendar;

import edu.cmu.hcii.sugilite.pumice.dialog.PumiceDialogManager;
import edu.cmu.hcii.sugilite.pumice.dialog.demonstration.PumiceProcedureDemonstrationDialog;
import edu.cmu.hcii.sugilite.pumice.dialog.demonstration.PumiceValueDemonstrationDialog;
import edu.cmu.hcii.sugilite.pumice.kb.PumiceValueQueryKnowledge;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.server_comm.SugiliteVerbalInstructionHTTPQueryInterface;

/**
 * @author toby
 * @date 12/4/18
 * @time 4:15 PM
 */

//class used for handle utterances when the user explain a PumiceValueQueryKnowledge
public class PumiceUserExplainValueIntentHandler implements PumiceUtteranceIntentHandler, SugiliteVerbalInstructionHTTPQueryInterface {
    private transient Activity context;
    private transient PumiceDialogManager pumiceDialogManager;
    private String parentKnowledgeName;

    //need to notify this lock when the value is resolved, and return the value through this object
    private PumiceValueQueryKnowledge resolveValueLock;
    Calendar calendar;

    public PumiceUserExplainValueIntentHandler(PumiceDialogManager pumiceDialogManager, Activity context, PumiceValueQueryKnowledge resolveValueLock, String parentKnowledgeName){
        this.pumiceDialogManager = pumiceDialogManager;
        this.context = context;
        this.calendar = Calendar.getInstance();
        this.resolveValueLock = resolveValueLock;
        this.parentKnowledgeName = parentKnowledgeName;
    }

    @Override
    public void setContext(Activity context) {
        this.context = context;
    }

    @Override
    public void handleIntentWithUtterance(PumiceDialogManager dialogManager, PumiceIntent pumiceIntent, PumiceDialogManager.PumiceUtterance utterance) {

        if (pumiceIntent.equals(PumiceIntent.DEFINE_VALUE_EXP)){
            //branch for situations such as e.g., redirection
            dialogManager.sendAgentMessage("I have received your explanation: " + utterance.getContent(), true, false);
            //TODO: send out the server query
            returnUserExplainValueResult(new PumiceValueQueryKnowledge<String>(parentKnowledgeName, PumiceValueQueryKnowledge.ValueType.STRING));
        }

        else if (pumiceIntent.equals(PumiceIntent.DEFINE_VALUE_DEMONSTRATION)){
            //branch for when the user wants to DEMONSTRATE how to find out the value
            PumiceValueDemonstrationDialog valueDemonstrationDialog = new PumiceValueDemonstrationDialog(context, parentKnowledgeName, utterance.getContent(), dialogManager.getSharedPreferences(), dialogManager.getSugiliteData(), dialogManager.getServiceStatusManager(), this);
            dialogManager.runOnMainThread(new Runnable() {
                @Override
                public void run() {
                    //the show() method for the dialog needs to be called at the main thread
                    valueDemonstrationDialog.show();
                }
            });
            //send out the prompt
            dialogManager.sendAgentMessage("Please start demonstrating how to find out the value of " + parentKnowledgeName +  ". " + "Click OK to continue.",true, false);
        }

        //set the intent handler back to the default one
        dialogManager.updateUtteranceIntentHandlerInANewState(new PumiceDefaultUtteranceIntentHandler(pumiceDialogManager, context));
    }

    @Override
    public PumiceIntent detectIntentFromUtterance(PumiceDialogManager.PumiceUtterance utterance) {
        if (utterance.getContent().contains("demonstrate")){
            return PumiceIntent.DEFINE_VALUE_DEMONSTRATION;
        } else {
            return PumiceIntent.DEFINE_VALUE_EXP;
        }
    }

    @Override
    public void resultReceived(int responseCode, String result) {
        //TODO: handle server response
        //notify the thread for resolving unknown bool exp that the intent has been fulfilled


    }

    @Override
    public void runOnMainThread(Runnable r) {
        pumiceDialogManager.runOnMainThread(r);
    }

    /**
     * return the result PumiceValueQueryKnowledge, and release the lock in the original PumiceInitInstructionParsingHandler
     * @param valueQueryKnowledge
     */
    public void returnUserExplainValueResult(PumiceValueQueryKnowledge valueQueryKnowledge){
        synchronized (resolveValueLock) {
            resolveValueLock.copyFrom(valueQueryKnowledge);
            resolveValueLock.notify();
        }
    }


}
