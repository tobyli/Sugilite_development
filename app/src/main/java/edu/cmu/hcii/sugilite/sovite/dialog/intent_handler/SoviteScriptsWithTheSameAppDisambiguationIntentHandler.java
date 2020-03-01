package edu.cmu.hcii.sugilite.sovite.dialog.intent_handler;

import android.app.Activity;

import java.util.List;

import edu.cmu.hcii.sugilite.pumice.dialog.PumiceDialogManager;
import edu.cmu.hcii.sugilite.pumice.dialog.intent_handler.PumiceUtteranceIntentHandler;
import edu.cmu.hcii.sugilite.pumice.kb.PumiceProceduralKnowledge;

/**
 * @author toby
 * @date 2/28/20
 * @time 12:28 PM
 */
public class SoviteScriptsWithTheSameAppDisambiguationIntentHandler implements PumiceUtteranceIntentHandler {
    private Activity context;
    private String appPackageName;
    private String appReadableName;
    private String originalUtterance;
    PumiceDialogManager pumiceDialogManager;

    private List<PumiceProceduralKnowledge> proceduralKnowledgesWithMatchedApps;


    public SoviteScriptsWithTheSameAppDisambiguationIntentHandler(PumiceDialogManager pumiceDialogManager, Activity context, String appPackageName, String appReadableName, String originalUtterance, List<PumiceProceduralKnowledge> proceduralKnowledgesWithMatchedApps) {
        this.pumiceDialogManager = pumiceDialogManager;
        this.context = context;
        this.appPackageName = appPackageName;
        this.appReadableName = appReadableName;
        this.originalUtterance = originalUtterance;
        this.proceduralKnowledgesWithMatchedApps = proceduralKnowledgesWithMatchedApps;
    }

    @Override
    public void sendPromptForTheIntentHandler() {
        pumiceDialogManager.sendAgentMessage(String.format("I found the following %d scripts that use %s", proceduralKnowledgesWithMatchedApps.size(), appReadableName), true, false);
        for (PumiceProceduralKnowledge pumiceProceduralKnowledge : proceduralKnowledgesWithMatchedApps) {
            pumiceDialogManager.sendAgentMessage(pumiceProceduralKnowledge.getProcedureName(), true, false);
        }
        pumiceDialogManager.sendAgentMessage("Does any one of these match what you want to do? You can also say \"no\" if none of these is correct.", true, true);
    }

    @Override
    public void handleIntentWithUtterance(PumiceDialogManager dialogManager, PumiceIntent pumiceIntent, PumiceDialogManager.PumiceUtterance utterance) {
        if (pumiceIntent.equals(PumiceIntent.PARSE_CONFIRM_POSITIVE)) {
            //TODO: the list contains the script that the user wants to execute
            //probably use a list
        } else if (pumiceIntent.equals(PumiceIntent.PARSE_CONFIRM_NEGATIVE)) {
            //TODO: the list does not include the script that the user wants to execute
            //jump to the intent handler for the situation where there isn't any script with matched apps

        } else {
            pumiceDialogManager.sendAgentMessage("Can't recognize your response. Please respond with \"Yes\" or \"No\".", true, false);
            sendPromptForTheIntentHandler();
        }
    }

    @Override
    public PumiceIntent detectIntentFromUtterance(PumiceDialogManager.PumiceUtterance utterance) {
        String utteranceContent = utterance.getContent();
        if (utteranceContent != null && (utteranceContent.toLowerCase().contains("no"))) {
            return PumiceIntent.PARSE_CONFIRM_NEGATIVE;
        } else {
            return PumiceIntent.PARSE_CONFIRM_POSITIVE;
        }
    }

    @Override
    public void setContext(Activity context) {
        this.context = context;
    }
}
