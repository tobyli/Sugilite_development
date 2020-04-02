package edu.cmu.hcii.sugilite.pumice.dialog.intent_handler.parsing_confirmation;

import android.app.Activity;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.TextView;

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
import edu.cmu.hcii.sugilite.model.block.SugiliteBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteOperationBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;
import edu.cmu.hcii.sugilite.model.operation.binary.SugiliteGetProcedureOperation;
import edu.cmu.hcii.sugilite.model.variable.VariableValue;
import edu.cmu.hcii.sugilite.pumice.communication.PumiceSemanticParsingResultPacket;
import edu.cmu.hcii.sugilite.pumice.dialog.PumiceDialogManager;
import edu.cmu.hcii.sugilite.pumice.dialog.intent_handler.PumiceDefaultUtteranceIntentHandler;
import edu.cmu.hcii.sugilite.pumice.dialog.intent_handler.PumiceUtteranceIntentHandler;
import edu.cmu.hcii.sugilite.pumice.kb.PumiceProceduralKnowledge;
import edu.cmu.hcii.sugilite.source_parsing.SugiliteScriptParser;
import edu.cmu.hcii.sugilite.sovite.visual.SoviteScriptVisualThumbnailManager;
import edu.cmu.hcii.sugilite.sovite.conversation.SoviteReturnValueCallbackInterface;
import edu.cmu.hcii.sugilite.sovite.conversation.intent_handler.SoviteIntentClassificationErrorForProceduralKnowledgeIntentHandler;
import edu.cmu.hcii.sugilite.sovite.visual.SoviteVariableUpdateCallback;
import edu.cmu.hcii.sugilite.sovite.visual.SoviteVisualVariableOnClickDialog;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.server_comm.SugiliteVerbalInstructionHTTPQueryInterface;
import edu.cmu.hcii.sugilite.pumice.dialog.intent_handler.parsing_confirmation.PumiceParsingResultWithResolveFnConfirmationHandler.HandleParsingResultPacket;

import static edu.cmu.hcii.sugilite.Const.SQL_SCRIPT_DAO;
import static edu.cmu.hcii.sugilite.pumice.dialog.intent_handler.parsing_confirmation.PumiceChooseParsingDialogNew.getDescriptionForFormula;
import static edu.cmu.hcii.sugilite.pumice.dialog.intent_handler.parsing_confirmation.PumiceParsingResultWithResolveFnConfirmationHandler.getTopParsing;

/**
 * @author toby
 * @date 2/18/19
 * @time 12:13 AM
 */
public class PumiceParsingResultNoResolveConfirmationHandler implements PumiceUtteranceIntentHandler, SugiliteVerbalInstructionHTTPQueryInterface, SoviteReturnValueCallbackInterface<String>, SoviteVariableUpdateCallback {
    //takes in 1. the original parsing query, 2. the parsing result
    private PumiceParsingResultNoResolveConfirmationHandler pumiceParsingResultNoResolveConfirmationHandler;
    private Activity context;
    private PumiceDialogManager pumiceDialogManager;
    private PumiceParsingResultDescriptionGenerator pumiceParsingResultDescriptionGenerator;
    private SugiliteScriptParser sugiliteScriptParser;
    private SoviteScriptVisualThumbnailManager soviteScriptVisualThumbnailManager;
    private SugiliteData sugiliteData;
    private SugiliteScriptDao sugiliteScriptDao;

    private HandleParsingResultPacket parsingResultsToHandle;
    private int failureCount = 0;
    private Set<View> existingVisualViews;

    private String topFormula;


    public PumiceParsingResultNoResolveConfirmationHandler(Activity context, SugiliteData sugiliteData, PumiceDialogManager pumiceDialogManager, int failureCount) {
        this.pumiceParsingResultNoResolveConfirmationHandler = this;
        this.context = context;
        this.sugiliteData = sugiliteData;
        this.pumiceDialogManager = pumiceDialogManager;
        this.pumiceParsingResultDescriptionGenerator = new PumiceParsingResultDescriptionGenerator();
        this.sugiliteScriptParser = new SugiliteScriptParser();
        this.failureCount = failureCount;
        this.soviteScriptVisualThumbnailManager = new SoviteScriptVisualThumbnailManager(context);
        this.existingVisualViews = new HashSet<>();

        if (Const.DAO_TO_USE == SQL_SCRIPT_DAO) {
            this.sugiliteScriptDao = new SugiliteScriptSQLDao(context);
        } else {
            this.sugiliteScriptDao = new SugiliteScriptFileDao(context, sugiliteData);
        }

        //this.parsingResultsToHandle = new Stack<>();
    }


    /**
     * should support:
     * 1. confirm the current top parsing result
     * 2. view the candidates and choose from one
     * 3. "retry" to try giving a different instruction
     */
    public void handleParsingResult(PumiceSemanticParsingResultPacket resultPacket, Runnable runnableForRetry, PumiceParsingResultWithResolveFnConfirmationHandler.ConfirmedParseRunnable runnableForConfirmedParse, boolean toAskForConfirmation) {
        if (resultPacket.queries != null && resultPacket.queries.size() > 0) {
            //set the parsingResultsToHandle
            parsingResultsToHandle = new HandleParsingResultPacket(resultPacket, runnableForRetry, runnableForConfirmedParse);
            topFormula = getTopParsing(resultPacket).formula;

            if (toAskForConfirmation) {
                // ask about the top formula -- need to switch the intent handler out
                pumiceDialogManager.updateUtteranceIntentHandlerInANewState(this);
                sendPromptForTheIntentHandler();
            } else {
                // choose the top parse without asking
                runnableForConfirmedParse.run(getTopParsing(resultPacket).formula);
            }
        } else {
            //empty result -- no candidate available
            pumiceDialogManager.sendAgentMessage("Can't understand your utterance, please try again", true, false);
            //execute the retry runnable
            runnableForRetry.run();
        }

    }


    /**
     * detect the intent from the user utterance -> whether the user confirms the parse or not
     *
     * @param utterance
     * @return
     */
    @Override
    public PumiceIntent detectIntentFromUtterance(PumiceDialogManager.PumiceUtterance utterance) {
        String utteranceContent = utterance.getContent().toString();
        if (utteranceContent != null && (utteranceContent.toLowerCase().contains("yes") || utteranceContent.toLowerCase().toLowerCase().contains("ok") || utteranceContent.toLowerCase().contains("yeah"))) {
            return PumiceIntent.PARSE_CONFIRM_POSITIVE;
        } else if (utteranceContent != null && (utteranceContent.toLowerCase().contains("no"))) {
            return PumiceIntent.PARSE_CONFIRM_NEGATIVE;
        } else {
            return PumiceIntent.UNRECOGNIZED;
        }
    }

    @Override
    public void handleIntentWithUtterance(PumiceDialogManager dialogManager, PumiceIntent pumiceIntent, PumiceDialogManager.PumiceUtterance utterance) {
        if (pumiceIntent.equals(PumiceIntent.PARSE_CONFIRM_POSITIVE)) {
            // parse is correct
            try {
                HandleParsingResultPacket parsingResultPacket = parsingResultsToHandle;
                parsingResultPacket.runnableForConfirmedParse.run(topFormula);
            } catch (Exception e) {
                e.printStackTrace();
            }
            //set the intent handler back to the default one
            dialogManager.updateUtteranceIntentHandlerInANewState(new PumiceDefaultUtteranceIntentHandler(pumiceDialogManager, context, sugiliteData));
        } else if (pumiceIntent.equals(PumiceIntent.PARSE_CONFIRM_NEGATIVE)) {
            // parse is incorrect
            // TODO: need to better handle negative response here
            HandleParsingResultPacket parsingResultPacket = parsingResultsToHandle;

            // TODO: first try to strip the fix part from the user's utterance, and feed it back to the original parser to try again
            SoviteIntentClassificationErrorForProceduralKnowledgeIntentHandler soviteIntentClassificationErrorForProceduralKnowledgeIntentHandler = new SoviteIntentClassificationErrorForProceduralKnowledgeIntentHandler(pumiceDialogManager, context, sugiliteData, parsingResultPacket.resultPacket.userUtterance, parsingResultPacket.resultPacket, this);
            dialogManager.updateUtteranceIntentHandlerInANewState(soviteIntentClassificationErrorForProceduralKnowledgeIntentHandler);
            dialogManager.callSendPromptForTheIntentHandlerForCurrentIntentHandler();
            //show a popup to ask the user to choose from parsing results
            // PumiceChooseParsingDialogNew pumiceChooseParsingDialog = new PumiceChooseParsingDialogNew(context, dialogManager, parsingResultPacket.resultPacket, parsingResultPacket.runnableForRetry, parsingResultPacket.runnableForConfirmedParse, failureCount);
            // pumiceChooseParsingDialog.show();
        } else if (pumiceIntent.equals(PumiceIntent.UNRECOGNIZED)) {
            pumiceDialogManager.sendAgentMessage("Can't recognize your response. Please respond with \"Yes\" or \"No\".", true, false);
            sendPromptForTheIntentHandler();
        }


    }

    private void sendBestExecutionConfirmationForScript(SugiliteBlock block, boolean toSendImage) {
        //sending the thumbnail images
        if (toSendImage) {
            List<View> views;
            if (block.getNextBlock() != null && block.getNextBlock() instanceof SugiliteOperationBlock && ((SugiliteOperationBlock) block.getNextBlock()).getOperation() instanceof SugiliteGetProcedureOperation) {
                views = soviteScriptVisualThumbnailManager.getVisualThumbnailViewsForBlock(block.getNextBlock(), this, this.pumiceDialogManager);
            } else {
                views = soviteScriptVisualThumbnailManager.getVisualThumbnailViewsForBlock(block, this, this.pumiceDialogManager);
            }

            if (views != null) {
                for (View view : views) {
                    pumiceDialogManager.sendAgentViewMessage(view, "SCREENSHOT", false, false);
                    existingVisualViews.add(view);
                }
            }
        }

        //sending the text description for SugiliteGetProcedureOperation
        SugiliteGetProcedureOperation getProcedureOperation = null;
        if (block.getNextBlock() != null
                && block.getNextBlock() instanceof SugiliteOperationBlock
                && ((SugiliteOperationBlock) block.getNextBlock()).getOperation() instanceof SugiliteGetProcedureOperation) {
            getProcedureOperation = (SugiliteGetProcedureOperation) ((SugiliteOperationBlock) block.getNextBlock()).getOperation();
        }
        if (block instanceof SugiliteOperationBlock && ((SugiliteOperationBlock) block).getOperation() instanceof SugiliteGetProcedureOperation) {
            getProcedureOperation = (SugiliteGetProcedureOperation) ((SugiliteOperationBlock) block).getOperation();
        }
        if (getProcedureOperation != null) {
            List<PumiceProceduralKnowledge> pumiceProceduralKnowledges = pumiceDialogManager.getPumiceKnowledgeManager().getPumiceProceduralKnowledges();
            for (PumiceProceduralKnowledge pumiceProceduralKnowledge : pumiceProceduralKnowledges) {
                if (pumiceProceduralKnowledge.getProcedureName().equals(getProcedureOperation.getName())) {
                    //TODO: make the parameters clickable
                    pumiceDialogManager.sendAgentMessage(TextUtils.concat("I will ", generateParameterClickableDescriptionForGetProcedureOperation(getProcedureOperation)), true, false);
                }
            }
        }
    }

    private Spanned generateParameterClickableDescriptionForGetProcedureOperation (SugiliteGetProcedureOperation getProcedureOperation) {
        String description = getProcedureOperation.getName();
        Map<String, VariableValue> identifiedParameterStringVariableValueMap = new HashMap<>();

        for (VariableValue<String> stringVariable : getProcedureOperation.getVariableValues()) {
            description = description.replace("[" + stringVariable.getVariableName() + "]", "[" + stringVariable.getVariableValue() + "]");
            identifiedParameterStringVariableValueMap.put("[" + stringVariable.getVariableValue() + "]", stringVariable);
        }

        SugiliteStartingBlock subScript = null;
        try {
            String subScriptName = getProcedureOperation.evaluate(sugiliteData);
            subScript = sugiliteScriptDao.read(subScriptName);
            if (subScript != null) {
                for (String variableName : subScript.variableNameVariableObjectMap.keySet()) {
                    VariableValue defaultVariableValue = subScript.variableNameDefaultValueMap.get(variableName);
                    if (defaultVariableValue != null) {
                        identifiedParameterStringVariableValueMap.put("[" + variableName + "]", defaultVariableValue);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        final SugiliteStartingBlock finalSubScript = subScript;

        SpannableString spannableStringDescription = new SpannableString(description);
        for (String identifiedParameterString : identifiedParameterStringVariableValueMap.keySet()) {
            if (description.indexOf(identifiedParameterString) >= 0) {
                spannableStringDescription.setSpan(new ClickableSpan() {
                    @Override
                    public void onClick(View widget) {
                        TextView textView = (TextView) widget;
                        Spanned span = (Spanned) textView.getText();
                        int startIndex = span.getSpanStart(this);
                        int endIndex = span.getSpanEnd(this);
                        String selectedSpanString = span.subSequence(startIndex, endIndex).toString();
                        VariableValue variableValue = identifiedParameterStringVariableValueMap.get(selectedSpanString);
                        SoviteVisualVariableOnClickDialog soviteVisualVariableOnClickDialog = new SoviteVisualVariableOnClickDialog(context, variableValue, finalSubScript, getProcedureOperation, pumiceParsingResultNoResolveConfirmationHandler, null, true);
                        soviteVisualVariableOnClickDialog.show();
                        //stop listening and talking when showing the dialog
                        pumiceDialogManager.stopListening();
                        pumiceDialogManager.stopTalking();

                        for (View visualView : existingVisualViews) {
                            visualView.setVisibility(View.GONE);
                        }
                        //remove all existing visual views

                    }
                }, description.indexOf(identifiedParameterString), description.indexOf(identifiedParameterString) + identifiedParameterString.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }


        return spannableStringDescription;
    }



    @Override
    public void onGetProcedureOperationUpdated(SugiliteGetProcedureOperation sugiliteGetProcedureOperation, VariableValue changedNewVariableValue, boolean toShowNewScreenshot) {
        //the get procedure operation is updated externally using the dialog -- need to reconfirm

        //1. update topFormula
        topFormula = sugiliteGetProcedureOperation.toString();
        SugiliteOperationBlock operationBlock = new SugiliteOperationBlock();
        operationBlock.setOperation(sugiliteGetProcedureOperation);

        //2. show new image
        if (toShowNewScreenshot) {
            List<View> views = soviteScriptVisualThumbnailManager.getVisualThumbnailViewsForBlock(operationBlock, this, this.pumiceDialogManager, changedNewVariableValue.getVariableName());
            if (views != null) {
                for (View view : views) {
                    pumiceDialogManager.sendAgentViewMessage(view, "SCREENSHOT", false, false);
                    existingVisualViews.add(view);
                }
            }
        }

        //3. send out prompt
        //make sure the intent handler is current
        pumiceDialogManager.setPumiceUtteranceIntentHandlerInUse(this);
        pumiceDialogManager.sendAgentMessage(String.format("Updating the value of [%s] to \"%s\"...", changedNewVariableValue.getVariableName(), changedNewVariableValue.getVariableValue()), true, false);
        sendBestExecutionConfirmationForScript(operationBlock, false);
        pumiceDialogManager.sendAgentMessage("Is this correct?", true, true);
    }

    @Override
    public void sendPromptForTheIntentHandler() {
        SugiliteStartingBlock script = null;
        try {
            if (topFormula.length() > 0) {
                script = sugiliteScriptParser.parseBlockFromString(topFormula);
            } else {
                throw new RuntimeException("empty server result!");
            }
            if (script == null) {
                throw new RuntimeException("null script!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        String description = getDescriptionForFormula(topFormula, parsingResultsToHandle.resultPacket.utteranceType);
        pumiceDialogManager.sendAgentMessage(String.format("Here is the parsing result: %s", description), true, false);

        sendBestExecutionConfirmationForScript(script, true);
        pumiceDialogManager.sendAgentMessage("Is this correct?", true, true);
    }


    @Override
    public void setContext(Activity context) {
        this.context = context;
    }

    @Override
    public void resultReceived(int responseCode, String result, String originalQuery) {

    }

    @Override
    public void callReturnValueCallback(String confirmedFormula) {
        try {
            parsingResultsToHandle.runnableForConfirmedParse.run(confirmedFormula);

        } catch (Exception e) {
            e.printStackTrace();
        }
        //set the intent handler back to the default one
        pumiceDialogManager.updateUtteranceIntentHandlerInANewState(new PumiceDefaultUtteranceIntentHandler(pumiceDialogManager, context, sugiliteData));
    }
}
