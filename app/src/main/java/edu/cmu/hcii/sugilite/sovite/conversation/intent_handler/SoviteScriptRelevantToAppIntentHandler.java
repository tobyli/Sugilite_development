package edu.cmu.hcii.sugilite.sovite.conversation.intent_handler;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import java.util.List;

import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.model.block.SugiliteOperationBlock;
import edu.cmu.hcii.sugilite.model.operation.binary.SugiliteGetProcedureOperation;
import edu.cmu.hcii.sugilite.pumice.dialog.PumiceDialogManager;
import edu.cmu.hcii.sugilite.pumice.dialog.intent_handler.PumiceUtteranceIntentHandler;
import edu.cmu.hcii.sugilite.pumice.kb.PumiceProceduralKnowledge;
import edu.cmu.hcii.sugilite.sovite.ScriptVisualThumbnailManager;
import edu.cmu.hcii.sugilite.sovite.conversation.SoviteReturnValueCallbackInterface;

/**
 * @author toby
 * @date 3/3/20
 * @time 8:46 PM
 */
public class SoviteScriptRelevantToAppIntentHandler implements PumiceUtteranceIntentHandler {
    private Activity context;
    private String appPackageName;
    private String appReadableName;
    private String originalUtterance;
    private PumiceDialogManager pumiceDialogManager;
    private SoviteReturnValueCallbackInterface<PumiceProceduralKnowledge> returnValueCallbackObject;
    private ScriptVisualThumbnailManager scriptVisualThumbnailManager;
    private SugiliteData sugiliteData;

    private List<PumiceProceduralKnowledge> relevantProceduralKnowledgesToTargetApp;
    private PumiceProceduralKnowledge topMatchedKnowledge;

    public SoviteScriptRelevantToAppIntentHandler(PumiceDialogManager pumiceDialogManager, Activity context, SugiliteData sugiliteData, String appPackageName, String appReadableName, String originalUtterance, List<PumiceProceduralKnowledge> relevantProceduralKnowledgesToTargetApp, SoviteReturnValueCallbackInterface<PumiceProceduralKnowledge> returnValueCallbackObject) {
        this.context = context;
        this.sugiliteData = sugiliteData;
        this.appPackageName = appPackageName;
        this.appReadableName = appReadableName;
        this.originalUtterance = originalUtterance;
        this.pumiceDialogManager = pumiceDialogManager;
        this.returnValueCallbackObject = returnValueCallbackObject;
        this.relevantProceduralKnowledgesToTargetApp = relevantProceduralKnowledgesToTargetApp;
        this.scriptVisualThumbnailManager = new ScriptVisualThumbnailManager(context);
        if (relevantProceduralKnowledgesToTargetApp != null && relevantProceduralKnowledgesToTargetApp.size() > 0) {
            this.topMatchedKnowledge = relevantProceduralKnowledgesToTargetApp.get(0);
        } else {
            this.topMatchedKnowledge = null;
        }
    }

    @Override
    public void sendPromptForTheIntentHandler() {
        if (topMatchedKnowledge != null) {
            if (topMatchedKnowledge.getInvolvedAppNames(pumiceDialogManager.getPumiceKnowledgeManager()).contains(appReadableName)) {
                pumiceDialogManager.sendAgentMessage(String.format("For the %s app, I know %s.", appReadableName, topMatchedKnowledge.getProcedureDescription(pumiceDialogManager.getPumiceKnowledgeManager(), true)), true, false);
            } else {
                pumiceDialogManager.sendAgentMessage(String.format("I don't know how to %s in %s, but I know %s.", originalUtterance, appReadableName, topMatchedKnowledge.getProcedureDescription(pumiceDialogManager.getPumiceKnowledgeManager(), true)), true, false);
            }
            // thumbnail image
            SugiliteGetProcedureOperation sugiliteGetProcedureOperation = new SugiliteGetProcedureOperation(topMatchedKnowledge.getProcedureName());
            SugiliteOperationBlock sugiliteOperationBlock = new SugiliteOperationBlock();
            sugiliteOperationBlock.setOperation(sugiliteGetProcedureOperation);
            //test sending an image
            Drawable drawable = scriptVisualThumbnailManager.getVisualThumbnailForScript(sugiliteOperationBlock, originalUtterance);
            if (drawable != null) {
                ImageView imageView = new ImageView(context);
                imageView.setImageDrawable(drawable);//SHOULD BE R.mipmap.demo_card
                pumiceDialogManager.sendAgentViewMessage(imageView, "SCREENSHOT:" + topMatchedKnowledge.getProcedureDescription(pumiceDialogManager.getPumiceKnowledgeManager(), false), false, false);
            }
            pumiceDialogManager.sendAgentMessage("Is this what you want to do?", true, true);
        } else {
            pumiceDialogManager.sendAgentMessage(String.format("Can't find any script relevant to %s.", appReadableName), true, false);

            //should be the same as EXECUTION_CONFIRM_NEGATIVE - ask for demonstration
            handleIntentWithUtterance(pumiceDialogManager, PumiceIntent.EXECUTION_CONFIRM_NEGATIVE, null);
        }
    }

    @Override
    public PumiceIntent detectIntentFromUtterance(PumiceDialogManager.PumiceUtterance utterance) {
        String utteranceContent = utterance.getContent();
        if (utteranceContent != null && (utteranceContent.toLowerCase().contains("yes") || utteranceContent.toLowerCase().toLowerCase().contains("ok") || utteranceContent.toLowerCase().contains("yeah"))){
            return PumiceIntent.EXECUTION_CONFIRM_POSITIVE;
        } else if (utteranceContent != null && (utteranceContent.toLowerCase().contains("no"))) {
            return PumiceIntent.EXECUTION_CONFIRM_NEGATIVE;
        } else {
            return PumiceIntent.UNRECOGNIZED;
        }
    }

    @Override
    public void handleIntentWithUtterance(PumiceDialogManager dialogManager, PumiceIntent pumiceIntent, PumiceDialogManager.PumiceUtterance utterance) {
        //return the matched procedural knowledge
        if (pumiceIntent.equals(PumiceIntent.EXECUTION_CONFIRM_POSITIVE)) {
            topMatchedKnowledge.isNewlyLearned = false;
            returnValueCallbackObject.callReturnValueCallback(topMatchedKnowledge);
        }
        else if (pumiceIntent.equals(PumiceIntent.EXECUTION_CONFIRM_NEGATIVE)) {
            //ask for demonstration instead
            dialogManager.sendAgentMessage("OK", true, false);
            SoviteDemonstrateRelevantScreenIntentHandler soviteDemonstrateRelevantScreenIntentHandler = new SoviteDemonstrateRelevantScreenIntentHandler(pumiceDialogManager, context, sugiliteData, originalUtterance, appPackageName, appReadableName, returnValueCallbackObject);
            pumiceDialogManager.updateUtteranceIntentHandlerInANewState(soviteDemonstrateRelevantScreenIntentHandler);
            pumiceDialogManager.callSendPromptForTheIntentHandlerForCurrentIntentHandler();
        }

        else if (pumiceIntent.equals(PumiceIntent.UNRECOGNIZED)) {
            pumiceDialogManager.sendAgentMessage("Can't recognize your response. Please respond with \"Yes\" or \"No\".", true, false);
            sendPromptForTheIntentHandler();
        }
    }

    @Override
    public void setContext(Activity context) {
        this.context = context;
    }
}
