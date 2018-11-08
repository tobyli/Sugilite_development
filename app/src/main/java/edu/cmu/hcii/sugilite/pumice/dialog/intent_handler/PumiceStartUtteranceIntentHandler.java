package edu.cmu.hcii.sugilite.pumice.dialog.intent_handler;

import android.content.Context;
import android.widget.ImageView;

import edu.cmu.hcii.sugilite.R;
import edu.cmu.hcii.sugilite.pumice.communication.PumiceInstructionPacket;
import edu.cmu.hcii.sugilite.pumice.dialog.PumiceDialogManager;

/**
 * @author toby
 * @date 10/26/18
 * @time 1:33 PM
 */
public class PumiceStartUtteranceIntentHandler implements PumiceUtteranceIntentHandler {
    private transient Context context;
    public PumiceStartUtteranceIntentHandler(Context context){
        this.context = context;
    }

    @Override
    public void setContext(Context context) {
        this.context = context;
    }

    /**
     * detect the intent type from a given user utterance
     * @param utterance
     * @return
     */
    @Override
    public PumiceIntent detectIntentFromUtterance(PumiceDialogManager.PumiceUtterance utterance){
        String text = utterance.getContent().toLowerCase();

        //**test***
        if (text.contains("weather")) {
            return PumiceIntent.TEST_WEATHER;
        }

        if (text.contains("start again") || text.contains("start over")) {
            return PumiceIntent.START_OVER;
        }

        if (text.contains("undo") || text.contains("go back to the last") || text.contains("go back to the previous")) {
            return PumiceIntent.UNDO_STEP;
        }

        if (text.contains("show existing")) {
            return PumiceIntent.SHOW_KNOWLEDGE;
        }

        if (text.contains("show raw")) {
            return PumiceIntent.SHOW_RAW_KNOWLEDGE;
        }

        else {
            return PumiceIntent.INIT_INSTRUCTION;
        }
    }

    @Override
    public void handleIntentWithUtterance(PumiceDialogManager dialogManager, PumiceIntent pumiceIntent, PumiceDialogManager.PumiceUtterance utterance) {
        switch (pumiceIntent) {
            case START_OVER:
                dialogManager.sendAgentMessage("I understand you want to start over: " + utterance.getContent(), true, false);
                dialogManager.startOverState();
                break;
            case UNDO_STEP:
                dialogManager.sendAgentMessage("I understand you want to undo: " + utterance.getContent(), true, false);
                dialogManager.revertToLastState();
                break;
            case TEST_WEATHER:
                ImageView imageView = new ImageView(context);
                imageView.setImageDrawable(context.getResources().getDrawable(R.mipmap.user_avatar));//SHOULD BE R.mipmap.demo_card
                dialogManager.sendAgentViewMessage(imageView, "Here is the weather", true, false);
                break;
            case INIT_INSTRUCTION:
                dialogManager.sendAgentMessage("I have received your instruction: " + utterance.getContent(), true, false);
                dialogManager.sendAgentMessage("Sending out server query: \n\n" + new PumiceInstructionPacket(dialogManager.getPumiceKnowledgeManager(), utterance.getContent()).toString(), false, false);
                break;
            case SHOW_KNOWLEDGE:
                dialogManager.sendAgentMessage("Below are the existing knowledge: \n\n" + dialogManager.getPumiceKnowledgeManager().getKnowledgeInString(), true, false);
                break;
            case SHOW_RAW_KNOWLEDGE:
                dialogManager.sendAgentMessage("Below are the existing knowledge: \n\n" + dialogManager.getPumiceKnowledgeManager().getRawKnowledgeInString(), false, false);
                break;
            default:
                dialogManager.sendAgentMessage("I don't understand this intent", true, false);
                break;
        }
    }
}
