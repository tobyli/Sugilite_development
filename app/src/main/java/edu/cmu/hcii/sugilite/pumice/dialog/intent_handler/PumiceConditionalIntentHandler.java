package edu.cmu.hcii.sugilite.pumice.dialog.intent_handler;

import android.content.Context;

import edu.cmu.hcii.sugilite.pumice.dialog.PumiceDialogManager;

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
        if(text.contains("if")) { //will need more than just if they say "if"
            return PumiceIntent.ADD_CONDITIONAL;
        }
        else {
            return PumiceIntent.INIT_INSTRUCTION;
        }
    }

    @Override
    public void handleIntentWithUtterance(PumiceDialogManager dialogManager, PumiceIntent pumiceIntent, PumiceDialogManager.PumiceUtterance utterance) {
        switch(pumiceIntent) {
            case ADD_CONDITIONAL:
                //parse conditional
                dialogManager.sendAgentMessage("I have received your instruction: " + utterance.getContent(), true, false);
                break;
            default:
                dialogManager.sendAgentMessage("I don't understand this intent", true, false);
                break;
        }
    }

}
