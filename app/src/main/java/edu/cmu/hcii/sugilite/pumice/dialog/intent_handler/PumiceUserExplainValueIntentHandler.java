package edu.cmu.hcii.sugilite.pumice.dialog.intent_handler;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

import java.util.Calendar;

import edu.cmu.hcii.sugilite.pumice.dialog.PumiceDialogManager;
import edu.cmu.hcii.sugilite.pumice.kb.PumiceValueQueryKnowledge;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.server_comm.SugiliteVerbalInstructionHTTPQueryInterface;

/**
 * @author toby
 * @date 12/4/18
 * @time 4:15 PM
 */

//class used for handle utterances when the user explain a PumiceValueQueryKnowledge
public class PumiceUserExplainValueIntentHandler implements PumiceUtteranceIntentHandler, SugiliteVerbalInstructionHTTPQueryInterface {
    private transient Context context;
    private transient PumiceDialogManager pumiceDialogManager;
    private String parentKnowledgeName;

    //need to notify this lock when the value is resolved, and return the value through this object
    private PumiceValueQueryKnowledge resolveValueLock;
    Calendar calendar;

    public PumiceUserExplainValueIntentHandler(PumiceDialogManager pumiceDialogManager, Context context, PumiceValueQueryKnowledge resolveValueLock, String parentKnowledgeName){
        this.pumiceDialogManager = pumiceDialogManager;
        this.context = context;
        this.calendar = Calendar.getInstance();
        this.resolveValueLock = resolveValueLock;
        this.parentKnowledgeName = parentKnowledgeName;
    }

    @Override
    public void setContext(Context context) {
        this.context = context;
    }

    @Override
    public void handleIntentWithUtterance(PumiceDialogManager dialogManager, PumiceIntent pumiceIntent, PumiceDialogManager.PumiceUtterance utterance) {

        if (pumiceIntent.equals(PumiceIntent.DEFINE_VALUE_EXP)){
            //for situations e.g., redirection
            dialogManager.sendAgentMessage("I have received your explanation: " + utterance.getContent(), true, false);
            //TODO: send out the server query
            //test
            returnUserExplainValueResult(dialogManager, new PumiceValueQueryKnowledge<String>(parentKnowledgeName, PumiceValueQueryKnowledge.ValueType.STRING));
        }

        else if (pumiceIntent.equals(PumiceIntent.DEFINE_VALUE_DEMONSTRATION)){
            dialogManager.runOnMainThread(new Runnable() {
                @Override
                public void run() {
                    AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
                    dialogBuilder.setMessage("Please start demonstrating finding this value. Click OK to continue.")
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    //TODO: handle demonstration
                                    returnUserExplainValueResult(dialogManager, new PumiceValueQueryKnowledge<String>(parentKnowledgeName, PumiceValueQueryKnowledge.ValueType.STRING));
                                }
                            }).show();
                }
            });
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
     * @param dialogManager
     * @param valueQueryKnowledge
     */
    private void returnUserExplainValueResult(PumiceDialogManager dialogManager, PumiceValueQueryKnowledge valueQueryKnowledge){
        synchronized (resolveValueLock) {
            resolveValueLock.copyFrom(valueQueryKnowledge);
            resolveValueLock.notify();
        }
    }


}
