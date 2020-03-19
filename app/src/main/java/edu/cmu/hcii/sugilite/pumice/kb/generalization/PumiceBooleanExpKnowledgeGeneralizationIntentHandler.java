package edu.cmu.hcii.sugilite.pumice.kb.generalization;

import android.app.Activity;

import java.util.Map;
import java.util.concurrent.ExecutorService;

import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.model.value.SugiliteValue;
import edu.cmu.hcii.sugilite.pumice.dialog.PumiceDialogManager;
import edu.cmu.hcii.sugilite.pumice.dialog.intent_handler.PumiceDefaultUtteranceIntentHandler;
import edu.cmu.hcii.sugilite.pumice.dialog.intent_handler.PumiceUtteranceIntentHandler;
import edu.cmu.hcii.sugilite.pumice.kb.PumiceBooleanExpKnowledge;

/**
 * @author toby
 * @date 3/7/19
 * @time 10:56 AM
 */
public class PumiceBooleanExpKnowledgeGeneralizationIntentHandler implements PumiceUtteranceIntentHandler {

    private Activity context;
    private PumiceDialogManager pumiceDialogManager;
    private String userUtteranceForNewScenario;
    private PumiceBooleanExpKnowledge pumiceBooleanExpKnowledge;
    private SugiliteData sugiliteData;

    private String oldScenarioDescrption = null;
    private SugiliteValue oldScenarioArg1Value = null;

    private Runnable positiveRunnable;
    private Runnable negativeRunnable;

    private ExecutorService executorServiceForReturnningResults;


    public PumiceBooleanExpKnowledgeGeneralizationIntentHandler(PumiceDialogManager pumiceDialogManager, Activity context, SugiliteData sugiliteData, PumiceBooleanExpKnowledge pumiceBooleanExpKnowledge, String userUtteranceForNewScenario, Runnable postiveRunnable, Runnable negativeRunnable, ExecutorService executorService){
        this.pumiceDialogManager = pumiceDialogManager;
        this.context = context;
        this.sugiliteData = sugiliteData;
        this.pumiceBooleanExpKnowledge = pumiceBooleanExpKnowledge;
        this.userUtteranceForNewScenario = userUtteranceForNewScenario;
        this.positiveRunnable = postiveRunnable;
        this.negativeRunnable = negativeRunnable;
        this.executorServiceForReturnningResults = executorService;

        for (Map.Entry<String,SugiliteValue> entry : pumiceBooleanExpKnowledge.getScenarioArg1Map().entrySet()) {
            //use the first one as the default
            oldScenarioDescrption = entry.getKey();
            oldScenarioArg1Value = entry.getValue();
            break;
        }

    }


    @Override
    public PumiceIntent detectIntentFromUtterance(PumiceDialogManager.PumiceUtterance utterance) {
        String utteranceContent = utterance.getContent();
        if (utteranceContent != null && (utteranceContent.toLowerCase().contains("yes") || utteranceContent.toLowerCase().toLowerCase().contains("ok") || utteranceContent.toLowerCase().contains("yeah"))){
            return PumiceIntent.EXECUTION_CONFIRM_POSITIVE;
        } else {
            return PumiceIntent.EXECUTION_CONFIRM_NEGATIVE;
        }    }

    @Override
    public void setContext(Activity context) {

    }

    @Override
    public void sendPromptForTheIntentHandler() {
        //ask the question -- I already know ...
        String boolExpDescription = pumiceBooleanExpKnowledge.getExpName();
        pumiceDialogManager.sendAgentMessage(String.format("I already know how to tell whether %s when determining whether to %s. Is it the same here when determining whether to %s?", boolExpDescription, oldScenarioDescrption, userUtteranceForNewScenario), true, true);
    }

    @Override
    public void handleIntentWithUtterance(PumiceDialogManager dialogManager, PumiceIntent pumiceIntent, PumiceDialogManager.PumiceUtterance utterance) {
        if (pumiceIntent.equals(PumiceIntent.EXECUTION_CONFIRM_POSITIVE)) {
            //keep the same
            executorServiceForReturnningResults.execute(positiveRunnable);

        } else {
            executorServiceForReturnningResults.execute(negativeRunnable);
        }
        dialogManager.updateUtteranceIntentHandlerInANewState(new PumiceDefaultUtteranceIntentHandler(dialogManager, context, sugiliteData));
    }
}
