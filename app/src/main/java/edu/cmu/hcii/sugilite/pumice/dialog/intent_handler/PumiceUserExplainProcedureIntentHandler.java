package edu.cmu.hcii.sugilite.pumice.dialog.intent_handler;

import android.app.Activity;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import edu.cmu.hcii.sugilite.Const;
import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.pumice.communication.PumiceInstructionPacket;
import edu.cmu.hcii.sugilite.pumice.communication.PumiceSemanticParsingResultPacket;
import edu.cmu.hcii.sugilite.pumice.communication.SkipPumiceJSONSerialization;
import edu.cmu.hcii.sugilite.pumice.dialog.PumiceDialogManager;
import edu.cmu.hcii.sugilite.pumice.dialog.demonstration.PumiceProcedureDemonstrationDialog;
import edu.cmu.hcii.sugilite.pumice.dialog.intent_handler.parsing_confirmation.PumiceParsingResultWithResolveFnConfirmationHandler;
import edu.cmu.hcii.sugilite.pumice.kb.PumiceProceduralKnowledge;
import edu.cmu.hcii.sugilite.sovite.SoviteAppNameAppInfoManager;
import edu.cmu.hcii.sugilite.sovite.conversation.SoviteReturnValueCallbackInterface;
import edu.cmu.hcii.sugilite.sovite.conversation.intent_handler.SoviteIntentClassificationErrorForProceduralKnowledgeIntentHandler;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.server_comm.SugiliteVerbalInstructionHTTPQueryInterface;

import static edu.cmu.hcii.sugilite.sovite.conversation.intent_handler.SoviteIntentClassificationErrorForProceduralKnowledgeIntentHandler.RELEVANT_APPS_FOR_UTTERANCES;
import static edu.cmu.hcii.sugilite.sovite.conversation.intent_handler.SoviteIntentClassificationErrorForProceduralKnowledgeIntentHandler.RELEVANT_UTTERANCES_FOR_APPS;

/**
 * @author toby
 * @date 12/4/18
 * @time 4:15 PM
 */

//class used for handle utterances when the user explain a PumiceProceduralKnowledge
public class PumiceUserExplainProcedureIntentHandler implements PumiceUtteranceIntentHandler, SugiliteVerbalInstructionHTTPQueryInterface, SoviteReturnValueCallbackInterface<PumiceProceduralKnowledge> {
    private Activity context;
    private PumiceDialogManager pumiceDialogManager;
    private String parentKnowledgeName;
    private PumiceUserExplainProcedureIntentHandler pumiceUserExplainProcedureIntentHandler;
    private SoviteAppNameAppInfoManager soviteAppNameAppInfoManager;
    private SugiliteData sugiliteData;

    //need to notify this lock when the procedure is resolved, and return the value through this object
    private PumiceProceduralKnowledge resolveProcedureLock;
    Calendar calendar;


    public PumiceUserExplainProcedureIntentHandler(PumiceDialogManager pumiceDialogManager, Activity context, SugiliteData sugiliteData, PumiceProceduralKnowledge resolveProcedureLock, String parentKnowledgeName) {
        this.pumiceDialogManager = pumiceDialogManager;
        this.context = context;
        this.sugiliteData = sugiliteData;
        this.calendar = Calendar.getInstance();
        this.resolveProcedureLock = resolveProcedureLock;
        this.parentKnowledgeName = parentKnowledgeName;
        this.pumiceUserExplainProcedureIntentHandler = this;
        this.soviteAppNameAppInfoManager = SoviteAppNameAppInfoManager.getInstance(SugiliteData.getAppContext());

    }

    @Override
    public void setContext(Activity context) {
        this.context = context;
    }

    @Override
    public void handleIntentWithUtterance(PumiceDialogManager dialogManager, PumiceIntent pumiceIntent, PumiceDialogManager.PumiceUtterance utterance) {

        if (pumiceIntent.equals(PumiceIntent.DEFINE_PROCEDURE_EXPLANATION)) {
            //for situations e.g., redirection
            //dialogManager.sendAgentMessage("I have received your explanation: " + utterance.getTriggerContent(), true, false);
            //TODO: send out an OPERATION_INSTRUCTION query to resolve the explanation
            //send out the server query
            PumiceInstructionPacket pumiceInstructionPacket = new PumiceInstructionPacket(dialogManager.getPumiceKnowledgeManager(), "OPERATION_INSTRUCTION", calendar.getTimeInMillis(), utterance.getContent().toString(), parentKnowledgeName);
            //dialogManager.sendAgentMessage("Sending out the server query below...", true, false);
            //dialogManager.sendAgentMessage(pumiceInstructionPacket.toString(), false, false);
            try {
                dialogManager.getHttpQueryManager().sendPumiceInstructionPacketOnASeparateThread(pumiceInstructionPacket, this);
            } catch (Exception e) {
                //TODO: error handling
                e.printStackTrace();
                pumiceDialogManager.sendAgentMessage("Failed to send the query", true, false);
            }
        } else if (pumiceIntent.equals(PumiceIntent.DEFINE_PROCEDURE_DEMONSTATION)) {
            PumiceProcedureDemonstrationDialog procedureDemonstrationDialog = new PumiceProcedureDemonstrationDialog(context, parentKnowledgeName, utterance.getContent().toString(), dialogManager.getSharedPreferences(), dialogManager.getSugiliteData(), dialogManager.getServiceStatusManager(), this);
            dialogManager.runOnMainThread(new Runnable() {
                @Override
                public void run() {
                    //the show() method for the dialog needs to be called at the main thread
                    procedureDemonstrationDialog.show();
                }
            });
            //send out the prompt
            dialogManager.sendAgentMessage("Please start demonstrating how to " + parentKnowledgeName + ". " + "Click OK to continue.", true, false);
        }
        //set the intent handler back to the default one
        dialogManager.updateUtteranceIntentHandlerInANewState(new PumiceDefaultUtteranceIntentHandler(pumiceDialogManager, context, sugiliteData));
    }

    @Override
    public PumiceIntent detectIntentFromUtterance(PumiceDialogManager.PumiceUtterance utterance) {
        if (utterance.getContent().toString().contains("demonstrate")) {
            return PumiceIntent.DEFINE_PROCEDURE_DEMONSTATION;
        } else {
            return PumiceIntent.DEFINE_PROCEDURE_EXPLANATION;
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
                .addSerializationExclusionStrategy(new ExclusionStrategy() {
                    @Override
                    public boolean shouldSkipField(FieldAttributes f) {
                        return f.getAnnotation(SkipPumiceJSONSerialization.class) != null;
                    }

                    @Override
                    public boolean shouldSkipClass(Class<?> clazz) {
                        return false;
                    }
                })
                .create();
        if (result.contains(PumiceIntent.APP_REFERENCE.name())) {
            try {
                SoviteIntentClassificationErrorForProceduralKnowledgeIntentHandler.handleAppReferenceResponse(sugiliteData, gson, result, parentKnowledgeName, soviteAppNameAppInfoManager, context, pumiceDialogManager, this, this);
            } catch (Exception e) {
                SoviteIntentClassificationErrorForProceduralKnowledgeIntentHandler.handleServerResponseError(e, pumiceDialogManager, this);
            }
        } else if (result.contains(RELEVANT_APPS_FOR_UTTERANCES)) {
            //handle queries of getting relevant apps for utterances
            try {
                SoviteIntentClassificationErrorForProceduralKnowledgeIntentHandler.handleRelevantAppsForUtterancesResponse(gson, result, pumiceDialogManager);

            } catch (Exception e) {
                SoviteIntentClassificationErrorForProceduralKnowledgeIntentHandler.handleServerResponseError(e, pumiceDialogManager, this);
            }
        } else if (result.contains(RELEVANT_UTTERANCES_FOR_APPS)) {
            //handle queries of getting relevant utterances for apps
            try {
                SoviteIntentClassificationErrorForProceduralKnowledgeIntentHandler.handleRelevantUtterancesForAppsResponse(gson, result, sugiliteData, soviteAppNameAppInfoManager, parentKnowledgeName, context, pumiceDialogManager, this);
            } catch (Exception e) {
                SoviteIntentClassificationErrorForProceduralKnowledgeIntentHandler.handleServerResponseError(e, pumiceDialogManager, this);
            }
        } else {
            //handle PUMICE server response
            try {
                PumiceSemanticParsingResultPacket resultPacket = gson.fromJson(result, PumiceSemanticParsingResultPacket.class);
                resultPacket.cleanFormula();
                if (resultPacket.utteranceType != null) {
                    switch (resultPacket.utteranceType) {
                        case "OPERATION_INSTRUCTION":
                            if (resultPacket.queries != null && resultPacket.queries.size() > 0) {
                                if (resultPacket.queries.get(0).formula.contains("get")) {
                                    PumiceParsingResultWithResolveFnConfirmationHandler parsingConfirmationHandler = new PumiceParsingResultWithResolveFnConfirmationHandler(context, sugiliteData, pumiceDialogManager, 0);
                                    parsingConfirmationHandler.handleParsingResult(resultPacket, new Runnable() {
                                        @Override
                                        public void run() {
                                            //handle retry
                                            pumiceDialogManager.updateUtteranceIntentHandlerInANewState(pumiceUserExplainProcedureIntentHandler);
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
                                                    PumiceProceduralKnowledge pumiceProceduralKnowledge = pumiceDialogManager.getPumiceInitInstructionParsingHandler().parseFromProcedureInstruction(confirmedFormula, resultPacket.userUtterance, parentKnowledgeName, 0);
                                                    //notify the original thread for resolving unknown bool exp that the intent has been fulfilled
                                                    callReturnValueCallback(pumiceProceduralKnowledge);
                                                }
                                            });
                                        }
                                    }, false);
                                } else {
                                    // the user has provided an app name
                                    List<String> availableAppNames = new ArrayList<>(soviteAppNameAppInfoManager.getAllAvailableAppPackageNameReadableNameMap(true).values());
                                    PumiceInstructionPacket pumiceInstructionPacket = new PumiceInstructionPacket(pumiceDialogManager.getPumiceKnowledgeManager(), PumiceIntent.APP_REFERENCE, calendar.getTimeInMillis(), resultPacket.userUtterance, availableAppNames);
                                    try {
                                        pumiceDialogManager.getHttpQueryManager().sendPumiceInstructionPacketOnASeparateThread(pumiceInstructionPacket, this);
                                    } catch (Exception e) {
                                        //TODO: error handling
                                        e.printStackTrace();
                                        pumiceDialogManager.sendAgentMessage("Failed to send the query", true, false);
                                    }
                                }

                            } else {
                                throw new RuntimeException("empty server result");
                            }
                            break;

                        default:
                            throw new RuntimeException("wrong type of result");
                    }
                }
            } catch (Exception e) {
                //TODO: error handling
                pumiceDialogManager.sendAgentMessage("Can't read from the server response", true, false);

                pumiceDialogManager.sendAgentMessage("OK. Let's try again.", true, false);
                pumiceDialogManager.updateUtteranceIntentHandlerInANewState(pumiceUserExplainProcedureIntentHandler);
                sendPromptForTheIntentHandler();
                e.printStackTrace();
            }
        }
    }

    @Override
    public void sendPromptForTheIntentHandler() {
        pumiceDialogManager.getSugiliteVoiceRecognitionListener().setContextPhrases(Const.DEMONSTRATION_CONTEXT_WORDS);
        pumiceDialogManager.sendAgentMessage("How do I " + parentKnowledgeName + "?" + " You can explain, or say \"demonstrate\" to demonstrate", true, true);
    }


    /**
     * return the result PumiceProceduralKnowledge, and release the lock in the original PumiceInitInstructionParsingHandler
     *
     * @param proceduralKnowledge
     */
    @Override
    public void callReturnValueCallback(PumiceProceduralKnowledge proceduralKnowledge) {
        synchronized (resolveProcedureLock) {
            resolveProcedureLock.copyFrom(proceduralKnowledge);
            resolveProcedureLock.notify();
        }
    }
}
