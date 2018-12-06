package edu.cmu.hcii.sugilite.pumice.dialog.intent_handler;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

import java.util.Calendar;

import edu.cmu.hcii.sugilite.pumice.dialog.PumiceDialogManager;
import edu.cmu.hcii.sugilite.pumice.kb.PumiceValueQueryKnowledge;

/**
 * @author toby
 * @date 12/4/18
 * @time 4:15 PM
 */

//class used for handle utterances when the user explain a PumiceValueQueryKnowledge
public class PumiceUserExplainValueIntentHandler implements PumiceUtteranceIntentHandler {
    private transient Context context;
    Calendar calendar;
    public PumiceUserExplainValueIntentHandler(Context context){
        this.context = context;
        this.calendar = Calendar.getInstance();
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
            returnUserExplainValueResult(dialogManager, new PumiceValueQueryKnowledge<String>(utterance.getContent(), PumiceValueQueryKnowledge.ValueType.STRING));
        }

        else if (pumiceIntent.equals(PumiceIntent.DEFINE_VALUE_DEMONSTRATION)){
            dialogManager.runOnMainThread(new Runnable() {
                @Override
                public void run() {
                    AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
                    dialogBuilder.setMessage("Please start demonstrating finding this value")
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    //TODO: handle demonstration
                                    returnUserExplainValueResult(dialogManager, new PumiceValueQueryKnowledge<String>(utterance.getContent(), PumiceValueQueryKnowledge.ValueType.STRING));
                                }
                            }).show();
                }
            });
        }

        //set the intent handler back to the default one
        dialogManager.updateUtteranceIntentHandlerInANewState(new PumiceDefaultUtteranceIntentHandler(context));
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
    public void handleServerResponse(PumiceDialogManager dialogManager, int responseCode, String result) {
        //TODO: handle server response
        //notify the thread for resolving unknown bool exp that the intent has been fulfilled


    }

    /**
     * return the result PumiceValueQueryKnowledge, and release the lock in the original PumiceInitInstructionParsingHandler
     * @param dialogManager
     * @param valueQueryKnowledge
     */
    private void returnUserExplainValueResult(PumiceDialogManager dialogManager, PumiceValueQueryKnowledge valueQueryKnowledge){
        synchronized (dialogManager.getPumiceInitInstructionParsingHandler().resolveValueLock) {
            dialogManager.getPumiceInitInstructionParsingHandler().resolveValueLock.notify();
            dialogManager.getPumiceInitInstructionParsingHandler().resolveValueLock = valueQueryKnowledge;
        }
    }


}
