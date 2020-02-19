package edu.cmu.hcii.sugilite.pumice.dialog.intent_handler;

import android.app.Activity;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.Calendar;

import edu.cmu.hcii.sugilite.Const;
import edu.cmu.hcii.sugilite.pumice.communication.PumiceInstructionPacket;
import edu.cmu.hcii.sugilite.pumice.communication.PumiceSemanticParsingResultPacket;
import edu.cmu.hcii.sugilite.pumice.communication.SkipPumiceJSONSerialization;
import edu.cmu.hcii.sugilite.pumice.dialog.PumiceDialogManager;
import edu.cmu.hcii.sugilite.pumice.dialog.intent_handler.parsing_confirmation.PumiceParsingResultWithResolveFnConfirmationHandler;
import edu.cmu.hcii.sugilite.pumice.kb.PumiceBooleanExpKnowledge;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.server_comm.SugiliteVerbalInstructionHTTPQueryInterface;

/**
 * @author toby
 * @date 12/4/18
 * @time 4:15 PM
 */

//class used for handle utterances when the user explain a PumiceBooleanExpKnowledge
public class PumiceUserExplainBoolExpIntentHandler implements PumiceUtteranceIntentHandler, SugiliteVerbalInstructionHTTPQueryInterface {
    private Activity context;
    private PumiceDialogManager pumiceDialogManager;
    private String parentKnowledgeName;
    private PumiceUserExplainBoolExpIntentHandler pumiceUserExplainBoolExpIntentHandler;
    private int failureCount = 0;

    //need to notify this lock when the bool expression is resolved, and return the value through this object
    PumiceBooleanExpKnowledge resolveBoolExpLock;
    Calendar calendar;

    public PumiceUserExplainBoolExpIntentHandler(PumiceDialogManager pumiceDialogManager, Activity context, PumiceBooleanExpKnowledge resolveBoolExpLock, String parentKnowledgeName, int failureCount){
        this.pumiceDialogManager = pumiceDialogManager;
        this.context = context;
        this.calendar = Calendar.getInstance();
        this.resolveBoolExpLock = resolveBoolExpLock;
        this.parentKnowledgeName = parentKnowledgeName;
        this.pumiceUserExplainBoolExpIntentHandler = this;
        this.failureCount = failureCount;
    }

    @Override
    public void setContext(Activity context) {
        this.context = context;
    }

    @Override
    public void handleIntentWithUtterance(PumiceDialogManager dialogManager, PumiceIntent pumiceIntent, PumiceDialogManager.PumiceUtterance utterance) {
        if (pumiceIntent.equals(PumiceIntent.BOOL_EXP_INSTRUCTION)){
            //dialogManager.sendAgentMessage("I have received your explanation: " + utterance.getTriggerContent(), true, false);

            //send out the server query
            PumiceInstructionPacket pumiceInstructionPacket = new PumiceInstructionPacket(dialogManager.getPumiceKnowledgeManager(), PumiceIntent.BOOL_EXP_INSTRUCTION.name(), calendar.getTimeInMillis(), utterance.getContent(), parentKnowledgeName);
            //dialogManager.sendAgentMessage("Sending out the server query below...", true, false);
            //dialogManager.sendAgentMessage(pumiceInstructionPacket.toString(), false, false);
            try {
                dialogManager.getHttpQueryManager().sendPumiceInstructionPacketOnASeparateThread(pumiceInstructionPacket, this);
            } catch (Exception e){
                //TODO: error handling
                e.printStackTrace();
                pumiceDialogManager.sendAgentMessage("Failed to send the query", true, false);
            }
            System.out.println(pumiceInstructionPacket.toString());
        }

        //set the intent handler back to the default one
        dialogManager.updateUtteranceIntentHandlerInANewState(new PumiceDefaultUtteranceIntentHandler(dialogManager, context));
    }

    @Override
    public PumiceIntent detectIntentFromUtterance(PumiceDialogManager.PumiceUtterance utterance) {
        return PumiceIntent.BOOL_EXP_INSTRUCTION;
    }

    @Override
    public void resultReceived(int responseCode, String result, String originalQuery) {
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
                    case "BOOL_EXP_INSTRUCTION":
                        if (resultPacket.queries != null && resultPacket.queries.size() > 0) {
                            PumiceParsingResultWithResolveFnConfirmationHandler parsingConfirmationHandler = new PumiceParsingResultWithResolveFnConfirmationHandler(context, pumiceDialogManager, failureCount);
                            parsingConfirmationHandler.handleParsingResult(resultPacket, new Runnable() {
                                        @Override
                                        public void run() {
                                            //handle retry
                                            pumiceUserExplainBoolExpIntentHandler.recordFailure();

                                            pumiceDialogManager.updateUtteranceIntentHandlerInANewState(pumiceUserExplainBoolExpIntentHandler);

                                            sendPromptForTheIntentHandler();
                                        }
                                    }, new PumiceParsingResultWithResolveFnConfirmationHandler.ConfirmedParseRunnable() {
                                        @Override
                                        public void run(String confirmedFormula) {
                                            //handle confirmed
                                            pumiceDialogManager.getExecutorService().submit(new Runnable() {
                                                @Override
                                                public void run() {
                                                    //got the parse confirmed
                                                    System.out.println("bool exp parse is confirmed");
                                                    pumiceUserExplainBoolExpIntentHandler.clearFailure();

                                                    //parse and process the server response
                                                    PumiceBooleanExpKnowledge pumiceBooleanExpKnowledge = pumiceDialogManager.getPumiceInitInstructionParsingHandler().parseFromBoolExpInstruction(confirmedFormula, resultPacket.userUtterance, parentKnowledgeName);

                                                    //notify the original thread for resolving unknown bool exp that the intent has been fulfilled
                                                    returnUserExplainBoolExpResult(pumiceBooleanExpKnowledge);
                                                }
                                            });
                                        }
                                    }, true);
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
            pumiceDialogManager.updateUtteranceIntentHandlerInANewState(pumiceUserExplainBoolExpIntentHandler);
            sendPromptForTheIntentHandler();
            e.printStackTrace();
        }
    }

    public void recordFailure(){
        failureCount = failureCount + 1;
    }

    public void clearFailure(){
        failureCount = 0;
    }

    @Override
    public void sendPromptForTheIntentHandler() {
        pumiceDialogManager.getSugiliteVoiceRecognitionListener().setContextPhrases(Const.INIT_INSTRUCTION_CONTEXT_WORDS);
        pumiceDialogManager.sendAgentMessage("How do I tell whether " + parentKnowledgeName + "?", true, true);
    }


    /**
     * return the result PumiceBooleanExpKnowledge, and release the lock in the original PumiceInitInstructionParsingHandler
     * @param booleanExpKnowledge
     */
    public void returnUserExplainBoolExpResult(PumiceBooleanExpKnowledge booleanExpKnowledge){
        synchronized (resolveBoolExpLock) {
            resolveBoolExpLock.copyFrom(booleanExpKnowledge);
            resolveBoolExpLock.notify();
        }
    }
}
