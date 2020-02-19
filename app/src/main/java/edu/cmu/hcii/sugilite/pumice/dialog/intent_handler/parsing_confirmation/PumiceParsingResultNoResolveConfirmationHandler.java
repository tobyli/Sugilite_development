package edu.cmu.hcii.sugilite.pumice.dialog.intent_handler.parsing_confirmation;

import android.app.Activity;

import org.apache.commons.lang3.StringUtils;

import java.util.HashSet;
import java.util.Set;

import edu.cmu.hcii.sugilite.pumice.communication.PumiceSemanticParsingResultPacket;
import edu.cmu.hcii.sugilite.pumice.dialog.PumiceDialogManager;
import edu.cmu.hcii.sugilite.pumice.dialog.intent_handler.PumiceDefaultUtteranceIntentHandler;
import edu.cmu.hcii.sugilite.pumice.dialog.intent_handler.PumiceUtteranceIntentHandler;
import edu.cmu.hcii.sugilite.source_parsing.SugiliteScriptParser;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.server_comm.SugiliteVerbalInstructionHTTPQueryInterface;
import edu.cmu.hcii.sugilite.pumice.dialog.intent_handler.parsing_confirmation.PumiceParsingResultWithResolveFnConfirmationHandler.HandleParsingResultPacket;
import static edu.cmu.hcii.sugilite.pumice.dialog.intent_handler.parsing_confirmation.PumiceParsingResultWithResolveFnConfirmationHandler.getTopParsing;

/**
 * @author toby
 * @date 2/18/19
 * @time 12:13 AM
 */
public class PumiceParsingResultNoResolveConfirmationHandler implements PumiceUtteranceIntentHandler, SugiliteVerbalInstructionHTTPQueryInterface {
    //takes in 1. the original parsing query, 2. the parsing result
    private Activity context;
    private PumiceDialogManager pumiceDialogManager;
    private PumiceParsingResultDescriptionGenerator pumiceParsingResultDescriptionGenerator;
    private SugiliteScriptParser sugiliteScriptParser;

    private HandleParsingResultPacket parsingResultsToHandle;
    private int failureCount = 0;


    public PumiceParsingResultNoResolveConfirmationHandler(Activity context, PumiceDialogManager pumiceDialogManager, int failureCount) {
        this.context = context;
        this.pumiceDialogManager = pumiceDialogManager;
        this.pumiceParsingResultDescriptionGenerator = new PumiceParsingResultDescriptionGenerator();
        this.sugiliteScriptParser = new SugiliteScriptParser();
        this.failureCount = failureCount;

        //this.parsingResultsToHandle = new Stack<>();
    }


    /**
     * should support:
     * 1. confirm the current top parsing result
     * 2. view the candidates and choose from one
     * 3. "retry" to try giving a different instruction
     */
    public void handleParsingResult(PumiceSemanticParsingResultPacket resultPacket, Runnable runnableForRetry, PumiceParsingResultWithResolveFnConfirmationHandler.ConfirmedParseRunnable runnableForConfirmedParse, boolean toAskForConfirmation){
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
        }

        else {
            //empty result -- no candidate available
            pumiceDialogManager.sendAgentMessage("Can't understand your utterance, please try again", true, false);
            //execute the retry runnable
            runnableForRetry.run();
        }

    }


    /**
     * detect the intent from the user utterance -> whether the user confirms the parse or not
     * @param utterance
     * @return
     */
    @Override
    public PumiceIntent detectIntentFromUtterance(PumiceDialogManager.PumiceUtterance utterance) {
        String utteranceContent = utterance.getContent();
        if (utteranceContent != null && (utteranceContent.toLowerCase().contains("yes") || utteranceContent.toLowerCase().toLowerCase().contains("ok") || utteranceContent.toLowerCase().contains("yeah"))){
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
        }



        else if (pumiceIntent.equals(PumiceIntent.PARSE_CONFIRM_NEGATIVE)) {
            // parse is incorrect
            // TODO: need to better handle negative response here
            HandleParsingResultPacket parsingResultPacket = parsingResultsToHandle;

            //show a popup to ask the user to choose from parsing results
            PumiceChooseParsingDialogNew pumiceChooseParsingDialog = new PumiceChooseParsingDialogNew(context, dialogManager, parsingResultPacket.resultPacket, parsingResultPacket.runnableForRetry, parsingResultPacket.runnableForConfirmedParse, failureCount);
            pumiceChooseParsingDialog.show();
        }

        else if (pumiceIntent.equals(PumiceIntent.UNRECOGNIZED)) {
            pumiceDialogManager.sendAgentMessage("Can't recognize your response. Please respond with \"Yes\" or \"No\".", true, false);
            sendPromptForTheIntentHandler();
        }

        //set the intent handler back to the default one
        dialogManager.updateUtteranceIntentHandlerInANewState(new PumiceDefaultUtteranceIntentHandler(pumiceDialogManager, context));
    }



    @Override
    public void sendPromptForTheIntentHandler() {
        PumiceSemanticParsingResultPacket.QueryGroundingPair topResult = parsingResultsToHandle.resultPacket.queries.get(0);
        String topFormula = topResult.formula;
        pumiceDialogManager.sendAgentMessage("Here is the parsing result: ", true, false);
        pumiceDialogManager.sendAgentMessage(getDescriptionForFormula(topFormula, parsingResultsToHandle.resultPacket.utteranceType), true, false);
        pumiceDialogManager.sendAgentMessage("Is this correct?", true, true);
    }


    @Override
    public void setContext(Activity context) {
        this.context = context;
    }

    @Override
    public void resultReceived(int responseCode, String result, String originalQuery) {

    }

    private String getDescriptionForFormula(String formula, String utteranceType) {
        switch (utteranceType) {
            case "USER_INIT_INSTRUCTION":
                return pumiceParsingResultDescriptionGenerator.generateForConditionBlock(formula);
            case "BOOL_EXP_INSTRUCTION":
                return pumiceParsingResultDescriptionGenerator.generateForBoolExp(formula);
            case "OPERATION_INSTRUCTION":
                return pumiceParsingResultDescriptionGenerator.generateForOperationBlock(formula);
            case "VALUE_INSTRUCTION":
                return pumiceParsingResultDescriptionGenerator.generateForValue(formula);
            default:
                throw new RuntimeException("unexpected packet type!");

        }
    }

}
