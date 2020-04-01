package edu.cmu.hcii.sugilite.sovite.conversation.intent_handler;

import android.app.Activity;
import android.view.View;

import java.util.List;

import edu.cmu.hcii.sugilite.Const;
import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptDao;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptFileDao;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptSQLDao;
import edu.cmu.hcii.sugilite.model.NewScriptGeneralizer;
import edu.cmu.hcii.sugilite.model.block.SugiliteOperationBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;
import edu.cmu.hcii.sugilite.model.operation.binary.SugiliteGetProcedureOperation;
import edu.cmu.hcii.sugilite.model.variable.VariableValue;
import edu.cmu.hcii.sugilite.pumice.PumiceDemonstrationUtil;
import edu.cmu.hcii.sugilite.pumice.dialog.PumiceDialogManager;
import edu.cmu.hcii.sugilite.pumice.dialog.intent_handler.PumiceUtteranceIntentHandler;
import edu.cmu.hcii.sugilite.pumice.kb.PumiceProceduralKnowledge;
import edu.cmu.hcii.sugilite.sovite.visual.ScriptVisualThumbnailManager;
import edu.cmu.hcii.sugilite.sovite.conversation.SoviteReturnValueCallbackInterface;
import edu.cmu.hcii.sugilite.sovite.conversation.dialog.SoviteNewScriptDemonstrationDialog;
import edu.cmu.hcii.sugilite.sovite.visual.SoviteVariableUpdateCallback;

import static edu.cmu.hcii.sugilite.Const.SQL_SCRIPT_DAO;

/**
 * @author toby
 * @date 3/13/20
 * @time 1:42 PM
 */
public class SoviteScriptMatchedFromScreenIntentHandler implements PumiceUtteranceIntentHandler, SoviteVariableUpdateCallback {
    private Activity context;
    private String appPackageName;
    private String appReadableName;
    private String activityName;
    private String originalUtterance;
    private PumiceDialogManager pumiceDialogManager;
    private SoviteReturnValueCallbackInterface<PumiceProceduralKnowledge> returnValueCallbackObject;
    private ScriptVisualThumbnailManager scriptVisualThumbnailManager;
    private NewScriptGeneralizer newScriptGeneralizer;
    private SugiliteScriptDao sugiliteScriptDao;

    private SugiliteStartingBlock appReferenceScript;
    private List<PumiceProceduralKnowledge> relevantProceduralKnowledgesToTargetApp;
    private PumiceProceduralKnowledge topMatchedKnowledge;

    public SoviteScriptMatchedFromScreenIntentHandler(PumiceDialogManager pumiceDialogManager, Activity context, SugiliteData sugiliteData, SugiliteStartingBlock appReferenceScript, String appPackageName, String appReadableName, String activityName, String originalUtterance, List<PumiceProceduralKnowledge> relevantProceduralKnowledgesToTargetApp, SoviteReturnValueCallbackInterface<PumiceProceduralKnowledge> returnValueCallbackObject) {
        this.context = context;
        this.appReferenceScript = appReferenceScript;
        this.appPackageName = appPackageName;
        this.appReadableName = appReadableName;
        this.activityName = activityName;
        this.originalUtterance = originalUtterance;
        this.pumiceDialogManager = pumiceDialogManager;
        this.returnValueCallbackObject = returnValueCallbackObject;
        this.relevantProceduralKnowledgesToTargetApp = relevantProceduralKnowledgesToTargetApp;
        this.scriptVisualThumbnailManager = new ScriptVisualThumbnailManager(context);
        this.newScriptGeneralizer = new NewScriptGeneralizer(context);
        if (Const.DAO_TO_USE == SQL_SCRIPT_DAO) {
            this.sugiliteScriptDao = new SugiliteScriptSQLDao(context);
        } else {
            this.sugiliteScriptDao = new SugiliteScriptFileDao(context, sugiliteData);
        }

        if (relevantProceduralKnowledgesToTargetApp != null && relevantProceduralKnowledgesToTargetApp.size() > 0) {
            this.topMatchedKnowledge = relevantProceduralKnowledgesToTargetApp.get(0);
        } else {
            this.topMatchedKnowledge = null;
        }
    }

    @Override
    public void sendPromptForTheIntentHandler() {
        if (topMatchedKnowledge != null) {
            pumiceDialogManager.sendAgentMessage(String.format("Relevant to this screen %s in %s, I know %s.", activityName, appReadableName, topMatchedKnowledge.getProcedureDescription(pumiceDialogManager.getPumiceKnowledgeManager(), true)), true, false);
            // thumbnail image
            SugiliteGetProcedureOperation sugiliteGetProcedureOperation = new SugiliteGetProcedureOperation(topMatchedKnowledge.getProcedureName());
            SugiliteOperationBlock sugiliteOperationBlock = new SugiliteOperationBlock();
            sugiliteOperationBlock.setOperation(sugiliteGetProcedureOperation);
            //test sending an image

            List<View> screenshotViews = scriptVisualThumbnailManager.getVisualThumbnailViewsForBlock(sugiliteOperationBlock, this, this.pumiceDialogManager);
            if (screenshotViews != null) {
                for (View screenshotView : screenshotViews) {
                    pumiceDialogManager.sendAgentViewMessage(screenshotView, "SCREENSHOT:" + topMatchedKnowledge.getProcedureDescription(pumiceDialogManager.getPumiceKnowledgeManager(), false), false, false);
                }
            }
            pumiceDialogManager.sendAgentMessage("Is this what you want to do?", true, true);
        } else {
            pumiceDialogManager.sendAgentMessage("Can't find any script relevant to the screen you showed", true, false);
            //should be the same as EXECUTION_CONFIRM_NEGATIVE - ask for demonstration
            handleIntentWithUtterance(pumiceDialogManager, PumiceUtteranceIntentHandler.PumiceIntent.EXECUTION_CONFIRM_NEGATIVE, null);
        }
    }

    @Override
    public PumiceUtteranceIntentHandler.PumiceIntent detectIntentFromUtterance(PumiceDialogManager.PumiceUtterance utterance) {
        String utteranceContent = utterance.getContent().toString();
        if (utteranceContent != null && (utteranceContent.toLowerCase().contains("yes") || utteranceContent.toLowerCase().toLowerCase().contains("ok") || utteranceContent.toLowerCase().contains("yeah"))) {
            return PumiceUtteranceIntentHandler.PumiceIntent.EXECUTION_CONFIRM_POSITIVE;
        } else if (utteranceContent != null && (utteranceContent.toLowerCase().contains("no"))) {
            return PumiceUtteranceIntentHandler.PumiceIntent.EXECUTION_CONFIRM_NEGATIVE;
        } else {
            return PumiceUtteranceIntentHandler.PumiceIntent.UNRECOGNIZED;
        }
    }

    @Override
    public void handleIntentWithUtterance(PumiceDialogManager dialogManager, PumiceUtteranceIntentHandler.PumiceIntent pumiceIntent, PumiceDialogManager.PumiceUtterance utterance) {
        //return the matched procedural knowledge
        if (pumiceIntent.equals(PumiceUtteranceIntentHandler.PumiceIntent.EXECUTION_CONFIRM_POSITIVE)) {
            topMatchedKnowledge.isNewlyLearned = false;
            returnValueCallbackObject.callReturnValueCallback(topMatchedKnowledge);
        } else if (pumiceIntent.equals(PumiceUtteranceIntentHandler.PumiceIntent.EXECUTION_CONFIRM_NEGATIVE)) {
            //have the user to continue demonstrating
            SoviteNewScriptDemonstrationDialog soviteNewScriptDemonstrationDialog = new SoviteNewScriptDemonstrationDialog(context, pumiceDialogManager, appReferenceScript, originalUtterance, originalUtterance, appPackageName, appReadableName, returnValueCallbackObject, this);
            soviteNewScriptDemonstrationDialog.show();

        } else if (pumiceIntent.equals(PumiceUtteranceIntentHandler.PumiceIntent.UNRECOGNIZED)) {
            pumiceDialogManager.sendAgentMessage("Can't recognize your response. Please respond with \"Yes\" or \"No\".", true, false);
            sendPromptForTheIntentHandler();
        }
    }

    @Override
    public void setContext(Activity context) {
        this.context = context;
    }

    @Override
    public void onGetProcedureOperationUpdated(SugiliteGetProcedureOperation sugiliteGetProcedureOperation, VariableValue changedNewVariableValue) {
        //TODO: implement
    }

    public void onDemonstrationReady(SugiliteStartingBlock script) {
        //generalize the script
        newScriptGeneralizer.extractParameters(script, PumiceDemonstrationUtil.removeScriptExtension(script.getScriptName()));

        String parameterizedProcedureKnowledgeName = originalUtterance;
        String parameterizedProcedureKnowledgeUtterance = originalUtterance;
        for (VariableValue defaultVariable : script.variableNameDefaultValueMap.values()) {
            if (defaultVariable.getVariableValue() instanceof String) {
                String defaultVariableString = (String) defaultVariable.getVariableValue();
                parameterizedProcedureKnowledgeName = parameterizedProcedureKnowledgeName.toLowerCase().replace(defaultVariableString.toLowerCase(), "[" + defaultVariable.getVariableName() + "]");
                parameterizedProcedureKnowledgeUtterance = parameterizedProcedureKnowledgeName.toLowerCase().replace(defaultVariableString.toLowerCase(), " ");
            }
        }

        final String finalParameterizedProcedureKnowledgeName = parameterizedProcedureKnowledgeName;
        final String finalParameterizedProcedureKnowledgeUtterance = parameterizedProcedureKnowledgeUtterance;

        try {
            sugiliteScriptDao.save(script);
            sugiliteScriptDao.commitSave(new Runnable() {
                @Override
                public void run() {
                    //construct the procedure knowledge
                    PumiceProceduralKnowledge newKnowledge = new PumiceProceduralKnowledge(context, finalParameterizedProcedureKnowledgeName, finalParameterizedProcedureKnowledgeUtterance, script);

                    //run the returnResultCallback when the result if ready
                    newKnowledge.isNewlyLearned = true;
                    returnValueCallbackObject.callReturnValueCallback(newKnowledge);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}