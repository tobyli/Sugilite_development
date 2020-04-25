package edu.cmu.hcii.sugilite.pumice.dialog.intent_handler;

import android.app.Activity;
import android.support.annotation.Nullable;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.Calendar;

import edu.cmu.hcii.sugilite.Const;
import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.ontology.SugiliteRelation;
import edu.cmu.hcii.sugilite.pumice.communication.PumiceInstructionPacket;
import edu.cmu.hcii.sugilite.pumice.communication.PumiceSemanticParsingResultPacket;
import edu.cmu.hcii.sugilite.pumice.communication.SkipPumiceJSONSerialization;
import edu.cmu.hcii.sugilite.pumice.dialog.PumiceDialogManager;
import edu.cmu.hcii.sugilite.pumice.dialog.PumiceUtterance;
import edu.cmu.hcii.sugilite.pumice.dialog.demonstration.PumiceValueDemonstrationDialog;
import edu.cmu.hcii.sugilite.pumice.dialog.intent_handler.parsing_confirmation.PumiceParsingResultWithResolveFnConfirmationHandler;
import edu.cmu.hcii.sugilite.pumice.kb.PumiceValueQueryKnowledge;
import edu.cmu.hcii.sugilite.sovite.conversation.SoviteReturnValueCallbackInterface;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.server_comm.SugiliteVerbalInstructionHTTPQueryInterface;

/**
 * @author toby
 * @date 12/4/18
 * @time 4:15 PM
 */

//class used for handle utterances when the user explain a PumiceValueQueryKnowledge
public class PumiceUserExplainValueIntentHandler implements PumiceUtteranceIntentHandler, SugiliteVerbalInstructionHTTPQueryInterface, SoviteReturnValueCallbackInterface<PumiceValueQueryKnowledge> {
    private Activity context;
    private PumiceDialogManager pumiceDialogManager;
    private String parentKnowledgeName;
    private PumiceUserExplainValueIntentHandler pumiceUserExplainValueIntentHandler;
    private SugiliteRelation resolveValueQueryOperationSugiliteRelationType;
    private SugiliteData sugiliteData;

    //need to notify this lock when the value is resolved, and return the value through this object
    private PumiceValueQueryKnowledge resolveValueLock;
    private Calendar calendar;

    public PumiceUserExplainValueIntentHandler(PumiceDialogManager pumiceDialogManager, Activity context, SugiliteData sugiliteData, PumiceValueQueryKnowledge resolveValueLock, String parentKnowledgeName, @Nullable SugiliteRelation resolveValueQueryOperationSugiliteRelationType){
        this.pumiceDialogManager = pumiceDialogManager;
        this.context = context;
        this.sugiliteData = sugiliteData;
        this.calendar = Calendar.getInstance();
        this.resolveValueLock = resolveValueLock;
        this.parentKnowledgeName = parentKnowledgeName;
        this.resolveValueQueryOperationSugiliteRelationType = resolveValueQueryOperationSugiliteRelationType;
        this.pumiceUserExplainValueIntentHandler = this;
    }

    @Override
    public void setContext(Activity context) {
        this.context = context;
    }

    @Override
    public void handleIntentWithUtterance(PumiceDialogManager dialogManager, PumiceIntent pumiceIntent, PumiceUtterance utterance) {
        if (pumiceIntent.equals(PumiceIntent.DEFINE_VALUE_EXPLANATION)){
            //branch for situations such as e.g., redirection
            //dialogManager.sendAgentMessage("I have received your explanation: " + utterance.getTriggerContent(), true, false);
            //TODO: send out an VALUE_INSTRUCTION query to resolve the explanation
            //send out the server query
            PumiceInstructionPacket pumiceInstructionPacket = new PumiceInstructionPacket(dialogManager.getPumiceKnowledgeManager(), "VALUE_INSTRUCTION", calendar.getTimeInMillis(), utterance.getContent().toString(), parentKnowledgeName);
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

        else if (pumiceIntent.equals(PumiceIntent.DEFINE_VALUE_DEMONSTRATION)){
            //branch for when the user wants to DEMONSTRATE how to find out the value
            PumiceValueDemonstrationDialog valueDemonstrationDialog = new PumiceValueDemonstrationDialog(context, parentKnowledgeName, utterance.getContent().toString(), dialogManager.getSharedPreferences(), dialogManager.getSugiliteData(), dialogManager.getServiceStatusManager(), resolveValueQueryOperationSugiliteRelationType, this);
            dialogManager.runOnMainThread(new Runnable() {
                @Override
                public void run() {
                    //the show() method for the dialog needs to be called at the main thread
                    valueDemonstrationDialog.show();
                }
            });
            //send out the prompt
            dialogManager.sendAgentMessage("Please start demonstrating how to find out the value of " + parentKnowledgeName +  ". " + "Click OK to continue.",true, false);
        }

        //set the intent handler back to the default one
        dialogManager.updateUtteranceIntentHandlerInANewState(new PumiceDefaultUtteranceIntentHandler(pumiceDialogManager, context, sugiliteData));
    }

    @Override
    public PumiceIntent detectIntentFromUtterance(PumiceUtterance utterance) {
        if (utterance.getContent().toString().contains("demonstrate")){
            return PumiceIntent.DEFINE_VALUE_DEMONSTRATION;
        } else {
            return PumiceIntent.DEFINE_VALUE_EXPLANATION;
        }
    }

    @Override
    public void resultReceived(int responseCode, String result, String originalQuery) {
        //handle server response for explaining a value

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
                    case "VALUE_INSTRUCTION":
                        if (resultPacket.queries != null && resultPacket.queries.size() > 0) {
                            PumiceParsingResultWithResolveFnConfirmationHandler parsingConfirmationHandler = new PumiceParsingResultWithResolveFnConfirmationHandler(context, sugiliteData, pumiceDialogManager, 0);
                            parsingConfirmationHandler.handleParsingResult(resultPacket, new Runnable() {
                                @Override
                                public void run() {
                                    //handle retry
                                    pumiceDialogManager.updateUtteranceIntentHandlerInANewState(pumiceUserExplainValueIntentHandler);
                                    sendPromptForTheIntentHandler();
                                }
                            }, new PumiceParsingResultWithResolveFnConfirmationHandler.ConfirmedParseRunnable() {
                                @Override
                                public void run(String confirmedFormula) {
                                    //handle confirmed
                                    pumiceDialogManager.getExecutorService().submit(new Runnable() {
                                        @Override
                                        public void run() {
                                            //parse and process the server response
                                            PumiceValueQueryKnowledge pumiceValueQueryKnowledge = pumiceDialogManager.getPumiceInitInstructionParsingHandler().parseFromValueInstruction(confirmedFormula, resultPacket.userUtterance, parentKnowledgeName, resolveValueQueryOperationSugiliteRelationType, 0);
                                            //notify the original thread for resolving unknown bool exp that the intent has been fulfilled
                                            callReturnValueCallback(pumiceValueQueryKnowledge);
                                        }
                                    });
                                }
                            }, false);
                        } else {
                            throw new RuntimeException("empty server result");
                        }
                        break;
                    default:
                        throw new RuntimeException("wrong type of result");
                }
            }
        } catch (Exception e){
            //TODO: error handling
            pumiceDialogManager.sendAgentMessage("Can't read from the server response", true, false);

            pumiceDialogManager.sendAgentMessage("OK. Let's try again.", true, false);
            pumiceDialogManager.updateUtteranceIntentHandlerInANewState(pumiceUserExplainValueIntentHandler);
            sendPromptForTheIntentHandler();
            e.printStackTrace();
        }


    }

    @Override
    public void sendPromptForTheIntentHandler() {
        pumiceDialogManager.getSugiliteVoiceRecognitionListener().setContextPhrases(Const.DEMONSTRATION_CONTEXT_WORDS);
        pumiceDialogManager.sendAgentMessage("How do I find out the value for " + parentKnowledgeName + "?" + " You can explain, or say \"demonstrate\" to demonstrate", true, true);
    }


    /**
     * return the result PumiceValueQueryKnowledge, and release the lock in the original PumiceInitInstructionParsingHandler
     * @param valueQueryKnowledge
     */
    @Override
    public void callReturnValueCallback(PumiceValueQueryKnowledge valueQueryKnowledge) {
        synchronized (resolveValueLock) {
            resolveValueLock.copyFrom(valueQueryKnowledge);
            resolveValueLock.notify();
        }
    }
}
