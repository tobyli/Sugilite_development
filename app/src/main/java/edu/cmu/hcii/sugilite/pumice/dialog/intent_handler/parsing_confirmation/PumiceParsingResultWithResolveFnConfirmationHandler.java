package edu.cmu.hcii.sugilite.pumice.dialog.intent_handler.parsing_confirmation;

import android.app.Activity;

import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import edu.cmu.hcii.sugilite.R;
import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.model.block.SugiliteConditionBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteOperationBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;
import edu.cmu.hcii.sugilite.model.block.booleanexp.SugiliteBooleanExpressionNew;
import edu.cmu.hcii.sugilite.model.operation.binary.SugiliteGetProcedureOperation;
import edu.cmu.hcii.sugilite.model.operation.unary.SugiliteResolveProcedureOperation;
import edu.cmu.hcii.sugilite.model.value.SugiliteValue;
import edu.cmu.hcii.sugilite.pumice.communication.PumiceInstructionPacket;
import edu.cmu.hcii.sugilite.pumice.communication.PumiceSemanticParsingResultPacket;
import edu.cmu.hcii.sugilite.pumice.dialog.PumiceDialogManager;
import edu.cmu.hcii.sugilite.pumice.dialog.PumiceUtterance;
import edu.cmu.hcii.sugilite.pumice.dialog.intent_handler.PumiceDefaultUtteranceIntentHandler;
import edu.cmu.hcii.sugilite.pumice.dialog.intent_handler.PumiceUtteranceIntentHandler;
import edu.cmu.hcii.sugilite.source_parsing.SugiliteScriptParser;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.server_comm.SugiliteVerbalInstructionHTTPQueryInterface;

import static edu.cmu.hcii.sugilite.pumice.dialog.intent_handler.parsing_confirmation.PumiceChooseParsingDialogNew.getDescriptionForFormula;


/**
 * @author toby
 * @date 2/18/19
 * @time 12:13 AM
 */
public class PumiceParsingResultWithResolveFnConfirmationHandler implements PumiceUtteranceIntentHandler, SugiliteVerbalInstructionHTTPQueryInterface {
    //takes in 1. the original parsing query, 2. the parsing result
    private Activity context;
    private PumiceDialogManager pumiceDialogManager;
    private PumiceParsingResultDescriptionGenerator pumiceParsingResultDescriptionGenerator;
    private SugiliteScriptParser sugiliteScriptParser;
    private SugiliteData sugiliteData;

    private HandleParsingResultPacket parsingResultsToHandle;
    private int failureCount = 0;


    public PumiceParsingResultWithResolveFnConfirmationHandler(Activity context, SugiliteData sugiliteData, PumiceDialogManager pumiceDialogManager, int failureCount) {
        this.context = context;
        this.sugiliteData = sugiliteData;
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
    public void handleParsingResult(PumiceSemanticParsingResultPacket resultPacket, Runnable runnableForRetry, ConfirmedParseRunnable runnableForConfirmedParse, boolean toAskForConfirmation){
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
            pumiceDialogManager.sendAgentMessage(context.getString(R.string.cant_understand_try_again), true, false);
            //execute the retry runnable
            runnableForRetry.run();
        }

    }

    /**
     * temporary method that prioritize results with more usage of "call get" functions
     * @param resultPacket
     * @return
     */
    public static PumiceSemanticParsingResultPacket.QueryGroundingPair getTopParsing(PumiceSemanticParsingResultPacket resultPacket){
        if (resultPacket != null && resultPacket.queries != null && resultPacket.queries.size() > 0) {
            //default
            Set<String> allAvailableFormula = new HashSet<>();
            PumiceSemanticParsingResultPacket.QueryGroundingPair topScoredQueryGroundingPair = resultPacket.queries.get(0);


            int topGetUsageCount = 0;

            for (PumiceSemanticParsingResultPacket.QueryGroundingPair queryGroundingPair : resultPacket.queries) {
                int numMatches = StringUtils.countMatches(queryGroundingPair.formula, "call get");
                if (numMatches > topGetUsageCount){
                    topGetUsageCount = numMatches;
                }
            }

            for (PumiceSemanticParsingResultPacket.QueryGroundingPair queryGroundingPair : resultPacket.queries) {
                if (StringUtils.countMatches(queryGroundingPair.formula, "call get") == topGetUsageCount){
                    return queryGroundingPair;
                }
            }

            return topScoredQueryGroundingPair;
        } else {
            throw new RuntimeException("failed to get top parsing -- empty result packet?");
        }
    }

    /**
     * detect the intent from the user utterance -> whether the user confirms the parse or not
     * @param utterance
     * @return
     */
    @Override
    public PumiceIntent detectIntentFromUtterance(PumiceUtterance utterance) {
        String utteranceContent = utterance.getContent().toString();
        if (utteranceContent != null && (utteranceContent.toLowerCase().contains("yes") || utteranceContent.toLowerCase().toLowerCase().contains("ok") || utteranceContent.toLowerCase().contains("yeah"))){
            return PumiceIntent.PARSE_CONFIRM_POSITIVE;
        } else if (utteranceContent != null && (utteranceContent.toLowerCase().contains("no"))) {
            return PumiceIntent.PARSE_CONFIRM_NEGATIVE;
        } else {
            return PumiceIntent.UNRECOGNIZED;
        }
    }

    @Override
    public void handleIntentWithUtterance(PumiceDialogManager dialogManager, PumiceIntent pumiceIntent, PumiceUtterance utterance) {
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
            HandleParsingResultPacket parsingResultPacket = parsingResultsToHandle;

            //show a popup to ask the user to choose from parsing results
            PumiceChooseParsingDialogNew pumiceChooseParsingDialog = new PumiceChooseParsingDialogNew(context, dialogManager, parsingResultPacket.resultPacket, parsingResultPacket.runnableForRetry, parsingResultPacket.runnableForConfirmedParse, failureCount);
            pumiceChooseParsingDialog.show();
        }

        else if (pumiceIntent.equals(PumiceIntent.UNRECOGNIZED)) {
            pumiceDialogManager.sendAgentMessage(context.getString(R.string.not_recognized_ask_for_binary), true, false);
            sendPromptForTheIntentHandler();
        }

        //set the intent handler back to the default one
        dialogManager.updateUtteranceIntentHandlerInANewState(new PumiceDefaultUtteranceIntentHandler(pumiceDialogManager, context, sugiliteData));
    }

    public interface ConfirmedParseRunnable {
        void run(String confirmedFormula);
    }

    protected static class HandleParsingResultPacket {
        public PumiceSemanticParsingResultPacket resultPacket;
        public Runnable runnableForRetry;
        public ConfirmedParseRunnable runnableForConfirmedParse;

        public HandleParsingResultPacket(PumiceSemanticParsingResultPacket resultPacket, Runnable runnableForRetry, ConfirmedParseRunnable runnableForConfirmedParse) {
            this.resultPacket = resultPacket;
            this.runnableForRetry = runnableForRetry;
            this.runnableForConfirmedParse = runnableForConfirmedParse;
        }

    }

    @Override
    public void sendPromptForTheIntentHandler() {
        PumiceSemanticParsingResultPacket.QueryGroundingPair topResult = parsingResultsToHandle.resultPacket.queries.get(0);
        String topFormula = topResult.formula;
        pumiceDialogManager.sendAgentMessage(context.getString(R.string.show_guess), true, false);
        pumiceDialogManager.sendAgentMessage(getDescriptionForFormula(topFormula, parsingResultsToHandle.resultPacket.utteranceType), true, false);
        pumiceDialogManager.sendAgentMessage(context.getString(R.string.confirm_question), true, true);
    }


    @Override
    public void setContext(Activity context) {
        this.context = context;
    }

    @Override
    public void resultReceived(int responseCode, String result, String originalQuery) {

    }


}
