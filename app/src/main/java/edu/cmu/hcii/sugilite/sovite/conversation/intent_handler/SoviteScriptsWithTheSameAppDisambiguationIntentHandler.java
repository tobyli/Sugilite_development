package edu.cmu.hcii.sugilite.sovite.conversation.intent_handler;

import android.app.Activity;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.List;

import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.pumice.communication.SkipPumiceJSONSerialization;
import edu.cmu.hcii.sugilite.pumice.dialog.PumiceDialogManager;
import edu.cmu.hcii.sugilite.pumice.dialog.intent_handler.PumiceUtteranceIntentHandler;
import edu.cmu.hcii.sugilite.pumice.kb.PumiceKnowledgeManager;
import edu.cmu.hcii.sugilite.pumice.kb.PumiceProceduralKnowledge;
import edu.cmu.hcii.sugilite.sovite.SoviteAppNameAppInfoManager;
import edu.cmu.hcii.sugilite.sovite.communication.SoviteAppResolutionQueryPacket;
import edu.cmu.hcii.sugilite.sovite.conversation.SoviteReturnValueCallbackInterface;
import edu.cmu.hcii.sugilite.sovite.conversation.dialog.SoviteScriptsWithMatchedAppDialog;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.server_comm.SugiliteVerbalInstructionHTTPQueryInterface;

import static edu.cmu.hcii.sugilite.sovite.conversation.intent_handler.SoviteIntentClassificationErrorForProceduralKnowledgeIntentHandler.RELEVANT_UTTERANCES_FOR_APPS;

/**
 * @author toby
 * @date 2/28/20
 * @time 12:28 PM
 */
public class SoviteScriptsWithTheSameAppDisambiguationIntentHandler implements PumiceUtteranceIntentHandler, SugiliteVerbalInstructionHTTPQueryInterface {
    private Activity context;
    private String appPackageName;
    private String appReadableName;
    private String originalUtterance;
    private PumiceDialogManager pumiceDialogManager;
    private SoviteReturnValueCallbackInterface<PumiceProceduralKnowledge> returnValueCallbackObject;
    private SugiliteData sugiliteData;

    private List<PumiceProceduralKnowledge> proceduralKnowledgesWithMatchedApps;


    public SoviteScriptsWithTheSameAppDisambiguationIntentHandler(PumiceDialogManager pumiceDialogManager, Activity context, SugiliteData sugiliteData, String appPackageName, String appReadableName, String originalUtterance, List<PumiceProceduralKnowledge> proceduralKnowledgesWithMatchedApps, SoviteReturnValueCallbackInterface<PumiceProceduralKnowledge> returnValueCallbackObject) {
        this.pumiceDialogManager = pumiceDialogManager;
        this.context = context;
        this.sugiliteData = sugiliteData;
        this.appPackageName = appPackageName;
        this.appReadableName = appReadableName;
        this.originalUtterance = originalUtterance;
        this.proceduralKnowledgesWithMatchedApps = proceduralKnowledgesWithMatchedApps;
        this.returnValueCallbackObject = returnValueCallbackObject;
    }

    @Override
    public void sendPromptForTheIntentHandler() {
        if (proceduralKnowledgesWithMatchedApps.size() > 1) {
            pumiceDialogManager.sendAgentMessage(String.format("I found the following %d scripts that use %s", proceduralKnowledgesWithMatchedApps.size(), appReadableName), true, false);
            for (PumiceProceduralKnowledge pumiceProceduralKnowledge : proceduralKnowledgesWithMatchedApps) {
                pumiceDialogManager.sendAgentMessage(pumiceProceduralKnowledge.getProcedureName(), true, false);
            }
            pumiceDialogManager.sendAgentMessage("Does any one of these match what you want to do? You can also say \"no\" if none of these is correct.", true, true);
        } else {
            //only one matched app
            SoviteScriptRelevantToAppIntentHandler soviteScriptRelevantToAppIntentHandler = new SoviteScriptRelevantToAppIntentHandler(pumiceDialogManager, context, sugiliteData, appPackageName, appReadableName, originalUtterance, proceduralKnowledgesWithMatchedApps, returnValueCallbackObject);
            pumiceDialogManager.updateUtteranceIntentHandlerInANewState(soviteScriptRelevantToAppIntentHandler);
            pumiceDialogManager.callSendPromptForTheIntentHandlerForCurrentIntentHandler();
        }
    }

    @Override
    public void handleIntentWithUtterance(PumiceDialogManager dialogManager, PumiceIntent pumiceIntent, PumiceDialogManager.PumiceUtterance utterance) {
        PumiceKnowledgeManager knowledgeManager = pumiceDialogManager.getPumiceKnowledgeManager();
        List<String> allAvailableScriptUtterances = knowledgeManager.getAllAvailableProcedureKnowledgeUtterances(false);



        if (pumiceIntent.equals(PumiceIntent.PARSE_CONFIRM_POSITIVE)) {
            //probably use a list dialog
            SoviteScriptsWithMatchedAppDialog soviteScriptsWithMatchedAppDialog = new SoviteScriptsWithMatchedAppDialog(context, dialogManager, proceduralKnowledgesWithMatchedApps, returnValueCallbackObject);
            soviteScriptsWithMatchedAppDialog.show();

        } else if (pumiceIntent.equals(PumiceIntent.PARSE_CONFIRM_NEGATIVE)) {
            //the list does not include the script that the user wants to execute
            //jump to the intent handler for the situation where there isn't any script with matched apps//2. check if we have other scripts that are similar to the embeddings of this app
            List<String> appPackageNames = new ArrayList<>();
            appPackageNames.add(appPackageName);

            try {
                // query for relevant utterances to the app
                SoviteAppResolutionQueryPacket soviteAppResolutionQueryPacket = new SoviteAppResolutionQueryPacket("playstore", RELEVANT_UTTERANCES_FOR_APPS, allAvailableScriptUtterances, appPackageNames);
                pumiceDialogManager.sendAgentMessage(String.format("OK, I will search for scripts that are relevant to %s.", appReadableName), true, false);
                pumiceDialogManager.getHttpQueryManager().sendSoviteAppResolutionPacketOnASeparateThread(soviteAppResolutionQueryPacket, this);

            } catch (Exception e) {
                pumiceDialogManager.sendAgentMessage("Can't read from the server response", true, false);
                pumiceDialogManager.sendAgentMessage("OK. Let's try again.", true, false);
                pumiceDialogManager.updateUtteranceIntentHandlerInANewState(this);
                sendPromptForTheIntentHandler();
                e.printStackTrace();
            }

        } else {
            pumiceDialogManager.sendAgentMessage("Can't recognize your response. Please respond with \"Yes\" or \"No\".", true, false);
            sendPromptForTheIntentHandler();
        }
    }

    @Override
    public PumiceIntent detectIntentFromUtterance(PumiceDialogManager.PumiceUtterance utterance) {
        String utteranceContent = utterance.getContent();
        if (utteranceContent != null && (utteranceContent.toLowerCase().contains("no"))) {
            return PumiceIntent.PARSE_CONFIRM_NEGATIVE;
        } else {
            return PumiceIntent.PARSE_CONFIRM_POSITIVE;
        }
    }

    @Override
    public void setContext(Activity context) {
        this.context = context;
    }

    @Override
    public void resultReceived(int responseCode, String result, String originalQuery) {
        if (result.contains(RELEVANT_UTTERANCES_FOR_APPS)) {
            //handle queries of getting relevant utterances for apps
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
            try {
                //prompt the user to confirm if the top script relevant to the app
                SoviteIntentClassificationErrorForProceduralKnowledgeIntentHandler.handleRelevantUtterancesForAppsResponse(gson, result, sugiliteData, SoviteAppNameAppInfoManager.getInstance(context), originalUtterance, context, pumiceDialogManager, returnValueCallbackObject);
            } catch (Exception e) {
                pumiceDialogManager.sendAgentMessage("Can't read from the server response", true, false);
                pumiceDialogManager.sendAgentMessage("OK. Let's try again.", true, false);
                pumiceDialogManager.updateUtteranceIntentHandlerInANewState(this);
                sendPromptForTheIntentHandler();
                e.printStackTrace();
            }
        }
    }
}
