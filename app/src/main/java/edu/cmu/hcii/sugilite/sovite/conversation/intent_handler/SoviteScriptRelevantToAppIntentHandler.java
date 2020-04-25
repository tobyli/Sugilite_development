package edu.cmu.hcii.sugilite.sovite.conversation.intent_handler;

import android.app.Activity;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.Pair;
import android.view.View;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.cmu.hcii.sugilite.Const;
import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptDao;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptFileDao;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptSQLDao;
import edu.cmu.hcii.sugilite.model.block.SugiliteOperationBlock;
import edu.cmu.hcii.sugilite.model.operation.binary.SugiliteGetProcedureOperation;
import edu.cmu.hcii.sugilite.model.variable.Variable;
import edu.cmu.hcii.sugilite.model.variable.VariableValue;
import edu.cmu.hcii.sugilite.pumice.dialog.PumiceDialogManager;
import edu.cmu.hcii.sugilite.pumice.dialog.PumiceUtterance;
import edu.cmu.hcii.sugilite.pumice.dialog.intent_handler.PumiceUtteranceIntentHandler;
import edu.cmu.hcii.sugilite.pumice.dialog.intent_handler.parsing_confirmation.PumiceParsingResultNoResolveConfirmationHandler;
import edu.cmu.hcii.sugilite.pumice.kb.PumiceProceduralKnowledge;
import edu.cmu.hcii.sugilite.sovite.visual.SoviteScriptVisualThumbnailManager;
import edu.cmu.hcii.sugilite.sovite.conversation.SoviteReturnValueCallbackInterface;
import edu.cmu.hcii.sugilite.sovite.visual.SoviteVariableUpdateCallback;

import static edu.cmu.hcii.sugilite.Const.SQL_SCRIPT_DAO;

/**
 * @author toby
 * @date 3/3/20
 * @time 8:46 PM
 */
public class SoviteScriptRelevantToAppIntentHandler implements PumiceUtteranceIntentHandler, SoviteVariableUpdateCallback {
    private Activity context;
    private String appPackageName;
    private String appReadableName;
    private String originalUtterance;
    private PumiceDialogManager pumiceDialogManager;
    private SoviteReturnValueCallbackInterface<PumiceProceduralKnowledge> returnValueCallbackObject;
    private SoviteScriptVisualThumbnailManager soviteScriptVisualThumbnailManager;
    private SugiliteData sugiliteData;
    private SugiliteScriptDao sugiliteScriptDao;
    private Set<View> existingVisualViews;


    private List<PumiceProceduralKnowledge> relevantProceduralKnowledgesToTargetApp;
    private PumiceProceduralKnowledge topMatchedKnowledge;
    private Map<String, VariableValue> variableNameVariableValueMap;



    public SoviteScriptRelevantToAppIntentHandler(PumiceDialogManager pumiceDialogManager, Activity context, SugiliteData sugiliteData, String appPackageName, String appReadableName, String originalUtterance, List<PumiceProceduralKnowledge> relevantProceduralKnowledgesToTargetApp, SoviteReturnValueCallbackInterface<PumiceProceduralKnowledge> returnValueCallbackObject) {
        this.context = context;
        this.sugiliteData = sugiliteData;
        this.appPackageName = appPackageName;
        this.appReadableName = appReadableName;
        this.originalUtterance = originalUtterance;
        this.pumiceDialogManager = pumiceDialogManager;
        this.returnValueCallbackObject = returnValueCallbackObject;
        this.relevantProceduralKnowledgesToTargetApp = relevantProceduralKnowledgesToTargetApp;
        this.soviteScriptVisualThumbnailManager = new SoviteScriptVisualThumbnailManager(context);
        this.existingVisualViews = new HashSet<>();
        this.variableNameVariableValueMap = new HashMap<>();

        if (relevantProceduralKnowledgesToTargetApp != null && relevantProceduralKnowledgesToTargetApp.size() > 0) {
            this.topMatchedKnowledge = new PumiceProceduralKnowledge();
            this.topMatchedKnowledge.copyFrom(relevantProceduralKnowledgesToTargetApp.get(0));

        } else {
            this.topMatchedKnowledge = null;
        }

        if (Const.DAO_TO_USE == SQL_SCRIPT_DAO) {
            this.sugiliteScriptDao = new SugiliteScriptSQLDao(context);
        } else {
            this.sugiliteScriptDao = new SugiliteScriptFileDao(context, sugiliteData);
        }

    }

    @Override
    public void onGetProcedureOperationUpdated(SugiliteGetProcedureOperation sugiliteGetProcedureOperation, List<VariableValue> changedNewVariableValues, boolean toShowNewScreenshot) {
        //TODO: fix
        //the get procedure operation is updated externally using the dialog -- need to reconfirm

        //1. update topFormula
        //topFormula = sugiliteGetProcedureOperation.toString();
        SugiliteOperationBlock operationBlock = new SugiliteOperationBlock();
        operationBlock.setOperation(sugiliteGetProcedureOperation);
        for (VariableValue changedNewVariableValue : changedNewVariableValues) {
            variableNameVariableValueMap.put(changedNewVariableValue.getVariableName(), changedNewVariableValue);
        }
        updateProcedureKnowledgeUtterance();

        //2. show new image
        if (toShowNewScreenshot) {
            //hide all existing views
            for (View view : existingVisualViews) {
                if (view != null && view.getVisibility() == View.VISIBLE) {
                    view.setVisibility(View.GONE);
                }
            }
            List<View> views = soviteScriptVisualThumbnailManager.getVisualThumbnailViewsForBlock(operationBlock, this, originalUtterance, this.pumiceDialogManager, null);
            if (views != null) {
                for (View view : views) {
                    pumiceDialogManager.sendAgentViewMessage(view, "SCREENSHOT", false, false);
                    existingVisualViews.add(view);
                }
            }
        }

        //3. send out prompt
        //make sure the intent handler is current
        Spanned getProcedureOperationParameterizedClickableDescription = PumiceParsingResultNoResolveConfirmationHandler.generateParameterClickableDescriptionForGetProcedureOperation(context, sugiliteGetProcedureOperation, pumiceDialogManager.getSugiliteData(), sugiliteScriptDao, pumiceDialogManager, existingVisualViews, this, originalUtterance);

        pumiceDialogManager.setPumiceUtteranceIntentHandlerInUse(this);
        for (VariableValue changedNewVariableValue : changedNewVariableValues) {
            pumiceDialogManager.sendAgentMessage(String.format("Updating the value of [%s] to \"%s\"...", changedNewVariableValue.getVariableName(), changedNewVariableValue.getVariableValue()), true, false);
        }
        pumiceDialogManager.sendAgentMessage(TextUtils.concat("I will ",  getProcedureOperationParameterizedClickableDescription, "."), true, false);

        //sendBestExecutionConfirmationForScript(operationBlock, false);
        pumiceDialogManager.sendAgentMessage("Is this correct?", true, true);
    }

    private void updateProcedureKnowledgeUtterance () {
        String utterance = topMatchedKnowledge.getProcedureName();
        for (VariableValue<String> selectedValue : variableNameVariableValueMap.values()) {
            utterance = utterance.replace("[" + selectedValue.getVariableName() + "]", "[" + selectedValue.getVariableValue() + "]");
        }
        topMatchedKnowledge.setUtterance(utterance);
    }

    @Override
    public void sendPromptForTheIntentHandler() {
        if (topMatchedKnowledge != null) {

            //add parameter to topMatchedKnowledge if needed
            List<VariableValue<String>> parameterValueList = new ArrayList<>();
            if (topMatchedKnowledge != null) {
                Map<String, PumiceProceduralKnowledge.PumiceProceduralKnowledgeParameter> parameterMap = topMatchedKnowledge.getParameterNameParameterMap();
                if (parameterMap != null) {
                    for (PumiceProceduralKnowledge.PumiceProceduralKnowledgeParameter parameter : parameterMap.values()) {
                        variableNameVariableValueMap.put(parameter.getParameterName(), new VariableValue<String>(parameter.getParameterName(), parameter.getParameterDefaultValue().toString()));
                        for (Object alternativeValue : parameter.getParameterAlternativeValues()) {
                            if (alternativeValue instanceof String && originalUtterance.toLowerCase().contains(((String) alternativeValue).toLowerCase())) {
                                parameterValueList.add(new VariableValue<String>(parameter.getParameterName(), (String) alternativeValue));
                                variableNameVariableValueMap.put(parameter.getParameterName(), new VariableValue<String>(parameter.getParameterName(), (String) alternativeValue));
                            }
                        }
                    }
                }
            }
            updateProcedureKnowledgeUtterance();

            SugiliteGetProcedureOperation sugiliteGetProcedureOperation = new SugiliteGetProcedureOperation(topMatchedKnowledge.getProcedureName());
            sugiliteGetProcedureOperation.setVariableValues(parameterValueList);
            SugiliteOperationBlock sugiliteOperationBlock = new SugiliteOperationBlock();
            sugiliteOperationBlock.setOperation(sugiliteGetProcedureOperation);

            Spanned getProcedureOperationParameterizedClickableDescription = PumiceParsingResultNoResolveConfirmationHandler.generateParameterClickableDescriptionForGetProcedureOperation(context, sugiliteGetProcedureOperation, sugiliteData, sugiliteScriptDao, pumiceDialogManager, existingVisualViews, this, originalUtterance);
            if (topMatchedKnowledge.getInvolvedAppNames(pumiceDialogManager.getPumiceKnowledgeManager()).contains(appReadableName)) {
                //pumiceDialogManager.sendAgentMessage(String.format("For the %s app, I know %s.", appReadableName, topMatchedKnowledge.getProcedureDescription(pumiceDialogManager.getPumiceKnowledgeManager(), true)), true, false);
                pumiceDialogManager.sendAgentMessage(TextUtils.concat(String.format("For the %s app, I know how to ", appReadableName), getProcedureOperationParameterizedClickableDescription, "."), true, false);
            } else {
                pumiceDialogManager.sendAgentMessage(TextUtils.concat(String.format("I don't know how to %s in %s, but I know how to", originalUtterance, appReadableName), getProcedureOperationParameterizedClickableDescription, "."), true, false);
            }
            // thumbnail image


            //test sending an image
            List<View> screenshotViews = soviteScriptVisualThumbnailManager.getVisualThumbnailViewsForBlock(sugiliteOperationBlock, this, originalUtterance, this.pumiceDialogManager);
            if (screenshotViews != null) {
                for (View screenshotView : screenshotViews) {
                    pumiceDialogManager.sendAgentViewMessage(screenshotView, "SCREENSHOT:" + topMatchedKnowledge.getProcedureDescription(pumiceDialogManager.getPumiceKnowledgeManager(), false), false, false);
                }
            }
            pumiceDialogManager.sendAgentMessage("Is this what you want to do?", true, true);
        } else {
            pumiceDialogManager.sendAgentMessage(String.format("Can't find any script relevant to %s.", appReadableName), true, false);

            //should be the same as EXECUTION_CONFIRM_NEGATIVE - ask for demonstration
            handleIntentWithUtterance(pumiceDialogManager, PumiceIntent.EXECUTION_CONFIRM_NEGATIVE, null);
        }
    }

    @Override
    public PumiceIntent detectIntentFromUtterance(PumiceUtterance utterance) {
        String utteranceContent = utterance.getContent().toString();
        if (utteranceContent != null && (utteranceContent.toLowerCase().contains("yes") || utteranceContent.toLowerCase().toLowerCase().contains("ok") || utteranceContent.toLowerCase().contains("yeah"))){
            return PumiceIntent.EXECUTION_CONFIRM_POSITIVE;
        } else if (utteranceContent != null && (utteranceContent.toLowerCase().contains("no"))) {
            return PumiceIntent.EXECUTION_CONFIRM_NEGATIVE;
        } else {
            return PumiceIntent.UNRECOGNIZED;
        }
    }

    @Override
    public void handleIntentWithUtterance(PumiceDialogManager dialogManager, PumiceIntent pumiceIntent, PumiceUtterance utterance) {
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
