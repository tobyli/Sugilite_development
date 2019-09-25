package edu.cmu.hcii.sugilite.pumice.dialog.intent_handler.else_statement;

import android.app.Activity;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.Calendar;

import edu.cmu.hcii.sugilite.Const;
import edu.cmu.hcii.sugilite.model.block.SugiliteBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteConditionBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteOperationBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;
import edu.cmu.hcii.sugilite.model.operation.binary.SugiliteGetProcedureOperation;
import edu.cmu.hcii.sugilite.pumice.communication.PumiceInstructionPacket;
import edu.cmu.hcii.sugilite.pumice.communication.PumiceSemanticParsingResultPacket;
import edu.cmu.hcii.sugilite.pumice.communication.SkipPumiceJSONSerialization;
import edu.cmu.hcii.sugilite.pumice.dialog.PumiceDialogManager;
import edu.cmu.hcii.sugilite.pumice.dialog.demonstration.PumiceElseStatementDemonstrationDialog;
import edu.cmu.hcii.sugilite.pumice.dialog.intent_handler.PumiceDefaultUtteranceIntentHandler;
import edu.cmu.hcii.sugilite.pumice.dialog.intent_handler.PumiceUtteranceIntentHandler;
import edu.cmu.hcii.sugilite.pumice.dialog.intent_handler.parsing_confirmation.PumiceParsingConfirmationHandler;
import edu.cmu.hcii.sugilite.pumice.kb.PumiceProceduralKnowledge;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.server_comm.SugiliteVerbalInstructionHTTPQueryInterface;

/**
 * @author toby
 * @date 12/4/18
 * @time 4:15 PM
 */

//class used for handle utterances when the user explain a PumiceProceduralKnowledge
public class PumiceUserExplainElseStatementIntentHandler implements PumiceUtteranceIntentHandler, SugiliteVerbalInstructionHTTPQueryInterface {
    private Activity context;
    private PumiceDialogManager pumiceDialogManager;
    private String boolExpReadableName;
    private PumiceUserExplainElseStatementIntentHandler pumiceUserExplainElseStatementIntentHandler;


    //need to notify this lock when the else statement is resolved, and return the value through this object
    private SugiliteConditionBlock originalConditionBlock;
    Calendar calendar;


    public PumiceUserExplainElseStatementIntentHandler(PumiceDialogManager pumiceDialogManager, Activity context, SugiliteConditionBlock originalConditionBlock, String boolExpReadableName){
        this.pumiceDialogManager = pumiceDialogManager;
        this.context = context;
        this.calendar = Calendar.getInstance();
        this.originalConditionBlock = originalConditionBlock;
        this.boolExpReadableName = boolExpReadableName;
        this.pumiceUserExplainElseStatementIntentHandler = this;
    }

    @Override
    public void setContext(Activity context) {
        this.context = context;
    }

    @Override
    public void handleIntentWithUtterance(PumiceDialogManager dialogManager, PumiceIntent pumiceIntent, PumiceDialogManager.PumiceUtterance utterance) {

        if (pumiceIntent.equals(PumiceIntent.DEFINE_PROCEDURE_EXP)){
            //for situations e.g., redirection
            //dialogManager.sendAgentMessage("I have received your explanation: " + utterance.getContent(), true, false);
            //TODO: send out an OPERATION_INSTRUCTION query to resolve the explanation
            //send out the server query
            PumiceInstructionPacket pumiceInstructionPacket = new PumiceInstructionPacket(dialogManager.getPumiceKnowledgeManager(), "OPERATION_INSTRUCTION", calendar.getTimeInMillis(), utterance.getContent(), utterance.getContent());
            //dialogManager.sendAgentMessage("Sending out the server query below...", true, false);
            //dialogManager.sendAgentMessage(pumiceInstructionPacket.toString(), false, false);
            try {
                dialogManager.getHttpQueryManager().sendPumiceInstructionPacketOnASeparateThread(pumiceInstructionPacket, this);
            } catch (Exception e){
                //TODO: error handling
                e.printStackTrace();
                pumiceDialogManager.sendAgentMessage("Failed to send the query", true, false);
            }
        }

        else if (pumiceIntent.equals(PumiceIntent.DEFINE_PROCEDURE_DEMONSTATION)){
            PumiceElseStatementDemonstrationDialog elseStatementDemonstrationDialog = new PumiceElseStatementDemonstrationDialog(context, boolExpReadableName, utterance.getContent(), dialogManager.getSharedPreferences(), dialogManager.getSugiliteData(), dialogManager.getServiceStatusManager(), this);
            dialogManager.runOnMainThread(new Runnable() {
                @Override
                public void run() {
                    //the show() method for the dialog needs to be called at the main thread
                    elseStatementDemonstrationDialog.show();
                }
            });
            //send out the prompt
            dialogManager.sendAgentMessage("Please start demonstrating what to do when " + boolExpReadableName +  " is not true. " + "Click OK to continue.",true, false);
        }
        //set the intent handler back to the default one
        dialogManager.updateUtteranceIntentHandlerInANewState(new PumiceDefaultUtteranceIntentHandler(pumiceDialogManager, context));
    }

    @Override
    public PumiceIntent detectIntentFromUtterance(PumiceDialogManager.PumiceUtterance utterance) {
        if (utterance.getContent().contains("demonstrate")){
            return PumiceIntent.DEFINE_PROCEDURE_DEMONSTATION;
        } else {
            return PumiceIntent.DEFINE_PROCEDURE_EXP;
        }
    }

    public PumiceDialogManager getPumiceDialogManager() {
        return pumiceDialogManager;
    }

    @Override
    public void resultReceived(int responseCode, String result, String originalQuery) {
        //handle server response for explaining an operation

        //notify the thread for resolving unknown bool exp that the intent has been fulfilled
        //handle server response from the semantic parsing server
        Gson gson = new GsonBuilder()
                .addSerializationExclusionStrategy(new ExclusionStrategy()
                {
                    @Override
                    public boolean shouldSkipField(FieldAttributes f)
                    {
                        return f.getAnnotation(SkipPumiceJSONSerialization.class) != null;
                    }

                    @Override
                    public boolean shouldSkipClass(Class<?> clazz)
                    {
                        return false;
                    }
                })
                .create();
        try {
            PumiceSemanticParsingResultPacket resultPacket = gson.fromJson(result, PumiceSemanticParsingResultPacket.class);
            resultPacket.cleanFormula();
            if (resultPacket.utteranceType != null) {
                switch (resultPacket.utteranceType) {
                    case "OPERATION_INSTRUCTION":
                        if (true) {
                            //TODO: bypass the else statement
                            returnUserExplainElseStatementResult(null);
                            break;
                        }
                        else {
                            if (resultPacket.queries != null && resultPacket.queries.size() > 0) {
                            PumiceParsingConfirmationHandler parsingConfirmationHandler = new PumiceParsingConfirmationHandler(context, pumiceDialogManager, 0);
                            parsingConfirmationHandler.handleParsingResult(resultPacket, new Runnable() {
                                @Override
                                public void run() {
                                    //handle retry
                                    pumiceDialogManager.updateUtteranceIntentHandlerInANewState(pumiceUserExplainElseStatementIntentHandler);
                                    sendPromptForTheIntentHandler();

                                }
                            }, new PumiceParsingConfirmationHandler.ConfirmedParseRunnable() {
                                @Override
                                public void run(String confirmedFormula) {
                                    //handle confirmed
                                    pumiceDialogManager.getExecutorService().submit(new Runnable() {
                                        @Override
                                        public void run() {
                                            //parse and process the server response
                                            PumiceProceduralKnowledge pumiceProceduralKnowledge = pumiceDialogManager.getPumiceInitInstructionParsingHandler().parseFromProcedureInstruction(confirmedFormula, resultPacket.userUtterance, resultPacket.userUtterance, 0);

                                            //pumiceProceduralKnowledge should have a target procedure;
                                            String targetProcedureName = pumiceProceduralKnowledge.getTargetProcedureKnowledgeName();
                                            SugiliteGetProcedureOperation getProcedureOperation = new SugiliteGetProcedureOperation(targetProcedureName);
                                            SugiliteOperationBlock elseBlock = new SugiliteOperationBlock();
                                            elseBlock.setOperation(getProcedureOperation);

                                            //notify the original thread for resolving else block that the intent has been fulfilled
                                            returnUserExplainElseStatementResult(elseBlock);
                                        }
                                    });
                                }
                            }, false);
                        } else {
                            throw new RuntimeException("empty server result");
                        }
                        break;
                    }
                    default:
                        throw new RuntimeException("wrong type of result");

                }
            }
        } catch (Exception e){
            //TODO: error handling
            pumiceDialogManager.sendAgentMessage("Can't read from the server response", true, false);

            pumiceDialogManager.sendAgentMessage("OK. Let's try again.", true, false);
            pumiceDialogManager.updateUtteranceIntentHandlerInANewState(pumiceUserExplainElseStatementIntentHandler);
            sendPromptForTheIntentHandler();
            e.printStackTrace();
        }
    }

    @Override
    public void sendPromptForTheIntentHandler() {
        pumiceDialogManager.getSugiliteVoiceRecognitionListener().setContextPhrases(Const.INIT_INSTRUCTION_CONTEXT_WORDS);
        pumiceDialogManager.sendAgentMessage(String.format("What should I do if %s is not true?", boolExpReadableName.replace(" is true", "")), true, true);
    }


    /**
     * return the result newBlock by adding it to originalConditionBlock, and release the lock in the original PumiceInitInstructionParsingHandler
     * @param newBlock
     */
    public void returnUserExplainElseStatementResult(SugiliteBlock newBlock){
        pumiceDialogManager.sendAgentMessage(String.format("OK, I know what to do when %s is not true.", boolExpReadableName.replace(" is true", "")), true, false);
        synchronized (originalConditionBlock) {
            //add new block as the else block for the original condition block
            if (newBlock != null) {
                SugiliteBlock blockToAdd = newBlock;
                if (blockToAdd instanceof SugiliteStartingBlock) {
                    blockToAdd = blockToAdd.getNextBlockToRun();
                }
                originalConditionBlock.setElseBlock(blockToAdd);
                blockToAdd.setParentBlock(originalConditionBlock);

            }
            originalConditionBlock.notify();
        }
    }



}
