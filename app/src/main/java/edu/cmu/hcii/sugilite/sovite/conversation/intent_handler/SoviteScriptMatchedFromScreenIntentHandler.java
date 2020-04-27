package edu.cmu.hcii.sugilite.sovite.conversation.intent_handler;

import android.app.Activity;
import android.text.Spanned;
import android.text.TextUtils;
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
import edu.cmu.hcii.sugilite.model.NewScriptGeneralizer;
import edu.cmu.hcii.sugilite.model.block.SugiliteOperationBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;
import edu.cmu.hcii.sugilite.model.operation.binary.SugiliteGetProcedureOperation;
import edu.cmu.hcii.sugilite.model.variable.Variable;
import edu.cmu.hcii.sugilite.model.variable.VariableValue;
import edu.cmu.hcii.sugilite.pumice.PumiceDemonstrationUtil;
import edu.cmu.hcii.sugilite.pumice.dialog.PumiceDialogManager;
import edu.cmu.hcii.sugilite.pumice.dialog.PumiceUtterance;
import edu.cmu.hcii.sugilite.pumice.dialog.intent_handler.PumiceUtteranceIntentHandler;
import edu.cmu.hcii.sugilite.pumice.dialog.intent_handler.parsing_confirmation.PumiceParsingResultNoResolveConfirmationHandler;
import edu.cmu.hcii.sugilite.pumice.kb.PumiceProceduralKnowledge;
import edu.cmu.hcii.sugilite.sovite.visual.SoviteScriptVisualThumbnailManager;
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
    private SoviteScriptVisualThumbnailManager soviteScriptVisualThumbnailManager;
    private NewScriptGeneralizer newScriptGeneralizer;
    private SugiliteScriptDao sugiliteScriptDao;
    private Set<View> existingVisualViews;


    private SugiliteStartingBlock appReferenceScript;
    private List<PumiceProceduralKnowledge> relevantProceduralKnowledgesToTargetApp;
    private PumiceProceduralKnowledge topMatchedKnowledge;
    private Map<String, VariableValue> variableNameVariableValueMap;

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
        this.soviteScriptVisualThumbnailManager = new SoviteScriptVisualThumbnailManager(context);
        this.newScriptGeneralizer = new NewScriptGeneralizer(context);
        this.existingVisualViews = new HashSet<>();
        this.variableNameVariableValueMap = new HashMap<>();


        if (Const.DAO_TO_USE == SQL_SCRIPT_DAO) {
            this.sugiliteScriptDao = new SugiliteScriptSQLDao(context);
        } else {
            this.sugiliteScriptDao = new SugiliteScriptFileDao(context, sugiliteData);
        }

        if (relevantProceduralKnowledgesToTargetApp != null && relevantProceduralKnowledgesToTargetApp.size() > 0) {
            this.topMatchedKnowledge = new PumiceProceduralKnowledge();
            this.topMatchedKnowledge.copyFrom(relevantProceduralKnowledgesToTargetApp.get(0));
        } else {
            this.topMatchedKnowledge = null;
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
            // thumbnail image
            SugiliteGetProcedureOperation sugiliteGetProcedureOperation = new SugiliteGetProcedureOperation(topMatchedKnowledge.getProcedureName());
            sugiliteGetProcedureOperation.setVariableValues(parameterValueList);
            SugiliteOperationBlock sugiliteOperationBlock = new SugiliteOperationBlock();
            sugiliteOperationBlock.setOperation(sugiliteGetProcedureOperation);
            //test sending an image

            Spanned getProcedureOperationParameterizedClickableDescription = PumiceParsingResultNoResolveConfirmationHandler.generateParameterClickableDescriptionForGetProcedureOperation(context, sugiliteGetProcedureOperation, pumiceDialogManager.getSugiliteData(), sugiliteScriptDao, pumiceDialogManager, existingVisualViews, this, originalUtterance);
            pumiceDialogManager.sendAgentMessage(TextUtils.concat("The most relevant intent I know to this screen is to ", getProcedureOperationParameterizedClickableDescription, "."), true, false);

            List<View> screenshotViews = soviteScriptVisualThumbnailManager.getVisualThumbnailViewsForBlock(sugiliteOperationBlock, this, originalUtterance, this.pumiceDialogManager);
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
    public PumiceUtteranceIntentHandler.PumiceIntent detectIntentFromUtterance(PumiceUtterance utterance) {
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
    public void handleIntentWithUtterance(PumiceDialogManager dialogManager, PumiceUtteranceIntentHandler.PumiceIntent pumiceIntent, PumiceUtterance utterance) {
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


    public void onDemonstrationReady(SugiliteStartingBlock script) {
        //generalize the script
        newScriptGeneralizer.extractParameters(script, PumiceDemonstrationUtil.removeScriptExtension(script.getScriptName()), new Runnable() {
            @Override
            public void run() {
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
        });
    }
}