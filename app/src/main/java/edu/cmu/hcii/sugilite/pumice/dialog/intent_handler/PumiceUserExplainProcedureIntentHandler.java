package edu.cmu.hcii.sugilite.pumice.dialog.intent_handler;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import edu.cmu.hcii.sugilite.pumice.dialog.PumiceDialogManager;
import edu.cmu.hcii.sugilite.pumice.kb.PumiceProceduralKnowledge;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.server_comm.SugiliteVerbalInstructionHTTPQueryInterface;

/**
 * @author toby
 * @date 12/4/18
 * @time 4:15 PM
 */

//class used for handle utterances when the user explain a PumiceProceduralKnowledge
public class PumiceUserExplainProcedureIntentHandler implements PumiceUtteranceIntentHandler, SugiliteVerbalInstructionHTTPQueryInterface {
    private transient Context context;
    private transient PumiceDialogManager pumiceDialogManager;
    private String parentKnowledgeName;

    //need to notify this lock when the procedure is resolved, and return the value through this object
    private PumiceProceduralKnowledge resolveProcedureLock;
    Calendar calendar;


    public PumiceUserExplainProcedureIntentHandler(PumiceDialogManager pumiceDialogManager, Context context, PumiceProceduralKnowledge resolveProcedureLock, String parentKnowledgeName){
        this.pumiceDialogManager = pumiceDialogManager;
        this.context = context;
        this.calendar = Calendar.getInstance();
        this.resolveProcedureLock = resolveProcedureLock;
        this.parentKnowledgeName = parentKnowledgeName;
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
            returnUserExplainProcedureResult(dialogManager, new PumiceProceduralKnowledge(parentKnowledgeName, utterance.getContent(), appList));
        }

        else if (pumiceIntent.equals(PumiceIntent.DEFINE_PROCEDURE_DEMONSTATION)){
            dialogManager.runOnMainThread(new Runnable() {
                @Override
                public void run() {
                    AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
                    dialogBuilder.setMessage("Please start demonstrating this procedure.  Click OK to continue.")
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    //TODO: handle demonstration
                                    List<String> appList = new ArrayList<>();
                                    appList.add("The Test App");
                                    returnUserExplainProcedureResult(dialogManager, new PumiceProceduralKnowledge(parentKnowledgeName, utterance.getContent(), appList));
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
            return PumiceIntent.DEFINE_PROCEDURE_DEMONSTATION;
        } else {
            return PumiceIntent.DEFINE_PROCEDURE_EXP;
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
     * return the result PumiceProceduralKnowledge, and release the lock in the original PumiceInitInstructionParsingHandler
     * @param dialogManager
     * @param proceduralKnowledge
     */
    private void returnUserExplainProcedureResult(PumiceDialogManager dialogManager, PumiceProceduralKnowledge proceduralKnowledge){
        synchronized (resolveProcedureLock) {
            resolveProcedureLock.copyFrom(proceduralKnowledge);
            resolveProcedureLock.notify();
        }
    }


}
