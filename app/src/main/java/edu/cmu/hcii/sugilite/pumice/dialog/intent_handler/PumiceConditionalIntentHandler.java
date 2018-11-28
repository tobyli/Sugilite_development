package edu.cmu.hcii.sugilite.pumice.dialog.intent_handler;

import android.content.Context;

import edu.cmu.hcii.sugilite.pumice.dialog.PumiceDialogManager;
import edu.cmu.hcii.sugilite.ui.ScriptDetailActivity;

public class PumiceConditionalIntentHandler implements PumiceUtteranceIntentHandler {
    private transient Context context;
    public PumiceConditionalIntentHandler(Context context){
        this.context = context;
    }

    @Override
    public void setContext(Context context) {
        this.context = context;
    }

    @Override
    public PumiceIntent detectIntentFromUtterance(PumiceDialogManager.PumiceUtterance utterance) {
        String text = utterance.getContent().toLowerCase();
        System.out.println(text);
        if(text.contains("if") || text.contains("step")) { //will need more than just if they say "if"
            return PumiceIntent.ADD_CONDITIONAL;
        }
        if(text.contains("yes") || text.contains("no")) {
            return PumiceIntent.CHECKING_LOC;
        }
        else {
            return PumiceIntent.USER_INIT_INSTRUCTION;
        }
    }

    @Override
    public void handleIntentWithUtterance(PumiceDialogManager dialogManager, PumiceIntent pumiceIntent, PumiceDialogManager.PumiceUtterance utterance) {
        switch(pumiceIntent) {
            case ADD_CONDITIONAL:
                System.out.println(utterance.getContent());
                ((ScriptDetailActivity) dialogManager.context).determineConditionalLoc();
                break;
            case CHECKING_LOC:
                if(utterance.getContent().contains("yes")) {
                    dialogManager.sendAgentMessage("Great, would you like me to run through the task to make sure the new step works correctly?",true,true);
                }
                else {
                    dialogManager.sendAgentMessage("Ok, after what step should the new step happen?",true,true);
                }
                break;
            default:
                dialogManager.sendAgentMessage("I don't understand this intent", true, false);
                break;
        }
    }

}
