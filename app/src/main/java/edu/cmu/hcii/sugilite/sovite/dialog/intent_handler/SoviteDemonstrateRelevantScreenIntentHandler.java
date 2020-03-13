package edu.cmu.hcii.sugilite.sovite.dialog.intent_handler;

import android.app.Activity;

import edu.cmu.hcii.sugilite.pumice.dialog.PumiceDialogManager;
import edu.cmu.hcii.sugilite.pumice.dialog.intent_handler.PumiceUtteranceIntentHandler;
import edu.cmu.hcii.sugilite.pumice.kb.PumiceProceduralKnowledge;
import edu.cmu.hcii.sugilite.sovite.dialog.SoviteDisambiguationDemonstrationDialog;

/**
 * @author toby
 * @date 3/11/20
 * @time 3:14 PM
 */
public class SoviteDemonstrateRelevantScreenIntentHandler implements PumiceUtteranceIntentHandler {

    private PumiceDialogManager pumiceDialogManager;
    private Activity context;
    private String originalUtterance;
    private String matchedAppPackageName;
    private String matchedAppReadableName;

    private SoviteReturnValueCallbackInterface<PumiceProceduralKnowledge> returnValueCallbackObject;

    public SoviteDemonstrateRelevantScreenIntentHandler (PumiceDialogManager pumiceDialogManager, Activity context, String originalUtterance, String matchedAppPackageName, String matchedAppReadableName, SoviteReturnValueCallbackInterface<PumiceProceduralKnowledge> returnValueCallbackObject) {
        this.pumiceDialogManager = pumiceDialogManager;
        this.context = context;
        this.originalUtterance = originalUtterance;
        this.matchedAppPackageName = matchedAppPackageName;
        this.matchedAppReadableName = matchedAppReadableName;
        this.returnValueCallbackObject = returnValueCallbackObject;
    }


    @Override
    public void sendPromptForTheIntentHandler() {
        pumiceDialogManager.sendAgentMessage(String.format("Can you show me which screen in %s is more relevant to the task \"%s\"?", matchedAppReadableName, originalUtterance), true, false);
        //show a dialog
        SoviteDisambiguationDemonstrationDialog soviteDisambiguationDemonstrationDialog = new SoviteDisambiguationDemonstrationDialog(context, pumiceDialogManager, originalUtterance, originalUtterance, matchedAppPackageName, matchedAppReadableName, returnValueCallbackObject);
        soviteDisambiguationDemonstrationDialog.show();
    }

    @Override
    public void setContext(Activity context) {
        this.context = context;
    }

    @Override
    public void handleIntentWithUtterance(PumiceDialogManager dialogManager, PumiceIntent pumiceIntent, PumiceDialogManager.PumiceUtterance utterance) {
        //do nothing
        if (pumiceIntent == PumiceIntent.UNRECOGNIZED) {
            sendPromptForTheIntentHandler();
        }
    }

    @Override
    public PumiceIntent detectIntentFromUtterance(PumiceDialogManager.PumiceUtterance utterance) {
        return PumiceIntent.UNRECOGNIZED;
    }
}
