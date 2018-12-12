package edu.cmu.hcii.sugilite.pumice.dialog.intent_handler;

import android.content.Context;

import java.util.Calendar;

import edu.cmu.hcii.sugilite.pumice.dialog.PumiceDialogManager;
import edu.cmu.hcii.sugilite.pumice.kb.PumiceBooleanExpKnowledge;

/**
 * @author toby
 * @date 12/4/18
 * @time 4:15 PM
 */

//class used for handle utterances when the user explain a PumiceBooleanExpKnowledge
public class PumiceUserExplainBoolExpIntentHandler implements PumiceUtteranceIntentHandler {
    private transient Context context;
    Calendar calendar;
    public PumiceUserExplainBoolExpIntentHandler(Context context){
        this.context = context;
        this.calendar = Calendar.getInstance();
    }

    @Override
    public void setContext(Context context) {
        this.context = context;
    }

    @Override
    public void handleIntentWithUtterance(PumiceDialogManager dialogManager, PumiceIntent pumiceIntent, PumiceDialogManager.PumiceUtterance utterance) {
        if (pumiceIntent.equals(PumiceIntent.DEFINE_BOOL_EXP)){
            dialogManager.sendAgentMessage("I have received your explanation: " + utterance.getContent(), true, false);
            //TODO: send out the server query
            //test
            returnUserExplainBoolExpResult(dialogManager, new PumiceBooleanExpKnowledge(utterance.getContent(), utterance.getContent(), null, null, null));
        }

        //set the intent handler back to the default one
        dialogManager.updateUtteranceIntentHandlerInANewState(new PumiceDefaultUtteranceIntentHandler(context));
    }

    @Override
    public PumiceIntent detectIntentFromUtterance(PumiceDialogManager.PumiceUtterance utterance) {
        return PumiceIntent.DEFINE_BOOL_EXP;
    }

    @Override
    public void handleServerResponse(PumiceDialogManager dialogManager, int responseCode, String result) {
        //TODO: handle server response
        //notify the thread for resolving unknown bool exp that the intent has been fulfilled


    }

    /**
     * return the result PumiceBooleanExpKnowledge, and release the lock in the original PumiceInitInstructionParsingHandler
     * @param dialogManager
     * @param booleanExpKnowledge
     */
    private void returnUserExplainBoolExpResult(PumiceDialogManager dialogManager, PumiceBooleanExpKnowledge booleanExpKnowledge){
        synchronized (dialogManager.getPumiceInitInstructionParsingHandler().resolveBoolExpLock) {
            dialogManager.getPumiceInitInstructionParsingHandler().resolveBoolExpLock.notify();
            dialogManager.getPumiceInitInstructionParsingHandler().resolveBoolExpLock = booleanExpKnowledge;
        }
    }


}
