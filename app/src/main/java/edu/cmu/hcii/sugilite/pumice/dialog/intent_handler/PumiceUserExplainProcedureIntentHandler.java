package edu.cmu.hcii.sugilite.pumice.dialog.intent_handler;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import edu.cmu.hcii.sugilite.pumice.dialog.PumiceDialogManager;
import edu.cmu.hcii.sugilite.pumice.kb.PumiceProceduralKnowledge;

/**
 * @author toby
 * @date 12/4/18
 * @time 4:15 PM
 */

//class used for handle utterances when the user explain a PumiceProceduralKnowledge
public class PumiceUserExplainProcedureIntentHandler implements PumiceUtteranceIntentHandler {
    private transient Context context;
    Calendar calendar;
    public PumiceUserExplainProcedureIntentHandler(Context context){
        this.context = context;
        this.calendar = Calendar.getInstance();
    }

    @Override
    public void setContext(Context context) {
        this.context = context;
    }

    @Override
    public void handleIntentWithUtterance(PumiceDialogManager dialogManager, PumiceIntent pumiceIntent, PumiceDialogManager.PumiceUtterance utterance) {

        if (pumiceIntent.equals(PumiceIntent.DEFINE_PROCEDURE_EXP)){
            //for situations e.g., redirection
            dialogManager.sendAgentMessage("I have received your explanation: " + utterance.getContent(), true, false);
            //TODO: send out the server query
            //test
            List<String> appList = new ArrayList<>();
            appList.add("Test App 2");
            returnUserExplainProcedureResult(dialogManager, new PumiceProceduralKnowledge(utterance.getContent(), utterance.getContent(), appList));
        }

        else if (pumiceIntent.equals(PumiceIntent.DEFINE_PROCEDURE_DEMONSTATION)){
            dialogManager.runOnMainThread(new Runnable() {
                @Override
                public void run() {
                    AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
                    dialogBuilder.setMessage("Please start demonstrating this procedure")
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    //TODO: handle demonstration
                                    List<String> appList = new ArrayList<>();
                                    appList.add("Test App 2");
                                    returnUserExplainProcedureResult(dialogManager, new PumiceProceduralKnowledge(utterance.getContent(), utterance.getContent(), appList));
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
            return PumiceIntent.DEFINE_PROCEDURE_DEMONSTATION;
        } else {
            return PumiceIntent.DEFINE_PROCEDURE_EXP;
        }
    }

    @Override
    public void handleServerResponse(PumiceDialogManager dialogManager, int responseCode, String result) {
        //TODO: handle server response
        //notify the thread for resolving unknown bool exp that the intent has been fulfilled
    }

    /**
     * return the result PumiceProceduralKnowledge, and release the lock in the original PumiceInitInstructionParsingHandler
     * @param dialogManager
     * @param proceduralKnowledge
     */
    private void returnUserExplainProcedureResult(PumiceDialogManager dialogManager, PumiceProceduralKnowledge proceduralKnowledge){
        synchronized (dialogManager.getPumiceInitInstructionParsingHandler().resolveProcedureLock) {
            dialogManager.getPumiceInitInstructionParsingHandler().resolveProcedureLock.notify();
            dialogManager.getPumiceInitInstructionParsingHandler().resolveProcedureLock = proceduralKnowledge;
        }
    }


}
