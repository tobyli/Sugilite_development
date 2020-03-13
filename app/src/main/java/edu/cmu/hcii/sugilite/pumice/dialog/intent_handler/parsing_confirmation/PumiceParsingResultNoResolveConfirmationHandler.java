package edu.cmu.hcii.sugilite.pumice.dialog.intent_handler.parsing_confirmation;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import java.util.List;

import edu.cmu.hcii.sugilite.model.block.SugiliteOperationBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;
import edu.cmu.hcii.sugilite.model.operation.binary.SugiliteGetProcedureOperation;
import edu.cmu.hcii.sugilite.pumice.communication.PumiceSemanticParsingResultPacket;
import edu.cmu.hcii.sugilite.pumice.dialog.PumiceDialogManager;
import edu.cmu.hcii.sugilite.pumice.dialog.intent_handler.PumiceDefaultUtteranceIntentHandler;
import edu.cmu.hcii.sugilite.pumice.dialog.intent_handler.PumiceUtteranceIntentHandler;
import edu.cmu.hcii.sugilite.pumice.kb.PumiceProceduralKnowledge;
import edu.cmu.hcii.sugilite.source_parsing.SugiliteScriptParser;
import edu.cmu.hcii.sugilite.sovite.ScriptVisualThumbnailManager;
import edu.cmu.hcii.sugilite.sovite.dialog.intent_handler.SoviteReturnValueCallbackInterface;
import edu.cmu.hcii.sugilite.sovite.dialog.intent_handler.SoviteIntentClassificationErrorForProceduralKnowledgeIntentHandler;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.server_comm.SugiliteVerbalInstructionHTTPQueryInterface;
import edu.cmu.hcii.sugilite.pumice.dialog.intent_handler.parsing_confirmation.PumiceParsingResultWithResolveFnConfirmationHandler.HandleParsingResultPacket;

import static edu.cmu.hcii.sugilite.pumice.dialog.intent_handler.parsing_confirmation.PumiceChooseParsingDialogNew.getDescriptionForFormula;
import static edu.cmu.hcii.sugilite.pumice.dialog.intent_handler.parsing_confirmation.PumiceParsingResultWithResolveFnConfirmationHandler.getTopParsing;

/**
 * @author toby
 * @date 2/18/19
 * @time 12:13 AM
 */
public class PumiceParsingResultNoResolveConfirmationHandler implements PumiceUtteranceIntentHandler, SugiliteVerbalInstructionHTTPQueryInterface, SoviteReturnValueCallbackInterface<String> {
    //takes in 1. the original parsing query, 2. the parsing result
    private Activity context;
    private PumiceDialogManager pumiceDialogManager;
    private PumiceParsingResultDescriptionGenerator pumiceParsingResultDescriptionGenerator;
    private SugiliteScriptParser sugiliteScriptParser;
    private ScriptVisualThumbnailManager scriptVisualThumbnailManager;

    private HandleParsingResultPacket parsingResultsToHandle;
    private int failureCount = 0;


    public PumiceParsingResultNoResolveConfirmationHandler(Activity context, PumiceDialogManager pumiceDialogManager, int failureCount) {
        this.context = context;
        this.pumiceDialogManager = pumiceDialogManager;
        this.pumiceParsingResultDescriptionGenerator = new PumiceParsingResultDescriptionGenerator();
        this.sugiliteScriptParser = new SugiliteScriptParser();
        this.failureCount = failureCount;
        this.scriptVisualThumbnailManager = new ScriptVisualThumbnailManager(context);

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
        String utteranceContent = utterance.getContent();
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
                if (parsingResultPacket.resultPacket.queries != null && parsingResultPacket.resultPacket.queries.size() > 0) {
                    PumiceSemanticParsingResultPacket.QueryGroundingPair topResult = parsingResultPacket.resultPacket.queries.get(0);
                    String topFormula = topResult.formula;
                    //run runnableForConfirmedParse on the top formula
                    parsingResultPacket.runnableForConfirmedParse.run(topFormula);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            //set the intent handler back to the default one
            dialogManager.updateUtteranceIntentHandlerInANewState(new PumiceDefaultUtteranceIntentHandler(pumiceDialogManager, context));
        } else if (pumiceIntent.equals(PumiceIntent.PARSE_CONFIRM_NEGATIVE)) {
            // parse is incorrect
            // TODO: need to better handle negative response here
            HandleParsingResultPacket parsingResultPacket = parsingResultsToHandle;

            // TODO: first try to strip the fix part from the user's utterance, and feed it back to the original parser to try again
            SoviteIntentClassificationErrorForProceduralKnowledgeIntentHandler soviteIntentClassificationErrorForProceduralKnowledgeIntentHandler = new SoviteIntentClassificationErrorForProceduralKnowledgeIntentHandler(pumiceDialogManager, context, parsingResultPacket.resultPacket.userUtterance, parsingResultPacket.resultPacket, this);
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

    private void sendBestExecutionConfirmation() {
        PumiceSemanticParsingResultPacket.QueryGroundingPair topResult = parsingResultsToHandle.resultPacket.queries.get(0);
        String topFormula = topResult.formula;
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

        //test sending an image
        Drawable drawable = scriptVisualThumbnailManager.getVisualThumbnailForScript(script, parsingResultsToHandle.resultPacket.userUtterance);
        if (drawable != null) {
            ImageView imageView = new ImageView(context);
            imageView.setImageDrawable(drawable);//SHOULD BE R.mipmap.demo_card
            pumiceDialogManager.sendAgentViewMessage(imageView, "SCREENSHOT:" + description, false, false);
        }

        if (script.getNextBlock() != null
                && script.getNextBlock() instanceof SugiliteOperationBlock
                && ((SugiliteOperationBlock) script.getNextBlock()).getOperation() instanceof SugiliteGetProcedureOperation) {
            SugiliteGetProcedureOperation getProcedureOperation = (SugiliteGetProcedureOperation) ((SugiliteOperationBlock) script.getNextBlock()).getOperation();
            List<PumiceProceduralKnowledge> pumiceProceduralKnowledges = pumiceDialogManager.getPumiceKnowledgeManager().getPumiceProceduralKnowledges();
            for (PumiceProceduralKnowledge pumiceProceduralKnowledge : pumiceProceduralKnowledges) {
                if (pumiceProceduralKnowledge.getProcedureName().equals(getProcedureOperation.getName())) {
                    pumiceDialogManager.sendAgentMessage(String.format("I will %s.", pumiceProceduralKnowledge.getProcedureDescription(pumiceDialogManager.getPumiceKnowledgeManager(), false)), true, false);
                    break;
                }
            }
        }
    }


    @Override
    public void sendPromptForTheIntentHandler() {
        sendBestExecutionConfirmation();
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
        pumiceDialogManager.updateUtteranceIntentHandlerInANewState(new PumiceDefaultUtteranceIntentHandler(pumiceDialogManager, context));
    }
}
