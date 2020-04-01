package edu.cmu.hcii.sugilite.sovite.conversation.intent_handler;

import android.app.Activity;
import android.view.View;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;
import edu.cmu.hcii.sugilite.ontology.SerializableUISnapshot;
import edu.cmu.hcii.sugilite.pumice.communication.SkipPumiceJSONSerialization;
import edu.cmu.hcii.sugilite.pumice.dialog.PumiceDialogManager;
import edu.cmu.hcii.sugilite.pumice.dialog.intent_handler.PumiceUtteranceIntentHandler;
import edu.cmu.hcii.sugilite.pumice.kb.PumiceProceduralKnowledge;
import edu.cmu.hcii.sugilite.sovite.visual.ScriptVisualThumbnailManager;
import edu.cmu.hcii.sugilite.sovite.communication.SoviteAppResolutionQueryPacket;
import edu.cmu.hcii.sugilite.sovite.communication.SoviteAppResolutionResultPacket;
import edu.cmu.hcii.sugilite.sovite.conversation.dialog.SoviteDisambiguationDemonstrationDialog;
import edu.cmu.hcii.sugilite.sovite.conversation.SoviteReturnValueCallbackInterface;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.server_comm.SugiliteVerbalInstructionHTTPQueryInterface;

/**
 * @author toby
 * @date 3/11/20
 * @time 3:14 PM
 */
public class SoviteDemonstrateRelevantScreenIntentHandler implements PumiceUtteranceIntentHandler, SugiliteVerbalInstructionHTTPQueryInterface {

    private PumiceDialogManager pumiceDialogManager;
    private Activity context;
    private String originalUtterance;
    private String matchedAppPackageName;
    private String matchedAppReadableName;
    private SugiliteData sugiliteData;

    private ScriptVisualThumbnailManager scriptVisualThumbnailManager;
    private SoviteReturnValueCallbackInterface<PumiceProceduralKnowledge> returnValueCallbackObject;
    private SugiliteStartingBlock appReferenceScript;

    public final static String RELEVANT_UTTERANCES_FOR_UI_SNAPSHOT = "RELEVANT_UTTERANCES_FOR_UI_SNAPSHOT";

    public SoviteDemonstrateRelevantScreenIntentHandler (PumiceDialogManager pumiceDialogManager, Activity context, SugiliteData sugiliteData, String originalUtterance, String matchedAppPackageName, String matchedAppReadableName, SoviteReturnValueCallbackInterface<PumiceProceduralKnowledge> returnValueCallbackObject) {
        this.pumiceDialogManager = pumiceDialogManager;
        this.context = context;
        this.sugiliteData = sugiliteData;
        this.scriptVisualThumbnailManager = new ScriptVisualThumbnailManager(context);
        this.originalUtterance = originalUtterance;
        this.matchedAppPackageName = matchedAppPackageName;
        this.matchedAppReadableName = matchedAppReadableName;
        this.returnValueCallbackObject = returnValueCallbackObject;
    }


    @Override
    public void sendPromptForTheIntentHandler() {
        pumiceDialogManager.sendAgentMessage(String.format("Can you show me which screen in %s is more relevant to the task \"%s\"?", matchedAppReadableName, originalUtterance), true, false);
        //show a dialog
        SoviteDisambiguationDemonstrationDialog soviteDisambiguationDemonstrationDialog = new SoviteDisambiguationDemonstrationDialog(context, pumiceDialogManager, originalUtterance, originalUtterance, matchedAppPackageName, matchedAppReadableName, returnValueCallbackObject, this);
        soviteDisambiguationDemonstrationDialog.show();
    }

    @Override
    public void setContext(Activity context) {
        this.context = context;
    }

    @Override
    public void handleIntentWithUtterance(PumiceDialogManager dialogManager, PumiceIntent pumiceIntent, PumiceDialogManager.PumiceUtterance utterance) {
        //do nothing
        if (pumiceIntent == PumiceIntent.UNRECOGNIZED) {
            sendPromptForTheIntentHandler();
        }
    }

    @Override
    public PumiceIntent detectIntentFromUtterance(PumiceDialogManager.PumiceUtterance utterance) {
        return PumiceIntent.UNRECOGNIZED;
    }

    //call back for SoviteDisambiguationDemonstrationDialog
    public void onDemonstrationReady (SugiliteStartingBlock script) {
        //extract the last screen in the script
        appReferenceScript = script;
        //no variable here
        List<View> scriptScreenshots = scriptVisualThumbnailManager.getVisualThumbnailViewsForBlock(script, this.pumiceDialogManager);
        SerializableUISnapshot lastScreenUISnapshot = scriptVisualThumbnailManager.getLastAvailableUISnapshotInSubsequentScript(script, null);
        List<String> allAvailableScriptUtterances = pumiceDialogManager.getPumiceKnowledgeManager().getAllAvailableProcedureKnowledgeUtterances(false);


        //prompt the user of the screen
        if (scriptScreenshots != null) {
            for (View scriptScreenshotView : scriptScreenshots) {
                pumiceDialogManager.sendAgentMessage(String.format("I've learned about the relevant screen in %s", matchedAppReadableName), true, false);
                pumiceDialogManager.sendAgentViewMessage(scriptScreenshotView, String.format("Relevant screen in %s", matchedAppReadableName), false, false);
            }
        }

        //send out a packet to the embedding server
        SoviteAppResolutionQueryPacket queryPacket = new SoviteAppResolutionQueryPacket(RELEVANT_UTTERANCES_FOR_UI_SNAPSHOT);

        queryPacket.setActivity_name(lastScreenUISnapshot.getActivityName());
        queryPacket.setPackage_name(lastScreenUISnapshot.getPackageName());
        queryPacket.setTexts(allAvailableScriptUtterances);
        queryPacket.setUi_snapshot(lastScreenUISnapshot);

        try {
            pumiceDialogManager.getHttpQueryManager().sendSoviteAppResolutionPacketOnASeparateThread(queryPacket, this);
        } catch (Exception e) {
            pumiceDialogManager.sendAgentMessage("Can't read from the server response", true, false);
            pumiceDialogManager.sendAgentMessage("OK. Let's try again.", true, false);
            pumiceDialogManager.updateUtteranceIntentHandlerInANewState(this);
            sendPromptForTheIntentHandler();
            e.printStackTrace();
        }
        //run the returnResultCallback when the result if ready
        //parentIntentHandler.callReturnValueCallback(new PumiceProceduralKnowledge(context, procedureKnowledgeName, procedureKnowledgeName, script));
    }

    @Override
    public void resultReceived(int responseCode, String result, String originalQuery) {
        if (result.contains(RELEVANT_UTTERANCES_FOR_UI_SNAPSHOT)) {
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
                //prompt the user to confirm if the top script relevant to the screen is correct
                SoviteAppResolutionResultPacket resultPacket = gson.fromJson(result, SoviteAppResolutionResultPacket.class);
                Map<String, List<SoviteAppResolutionResultPacket.ResultScorePair>> screenRelevantUtteranceMap = resultPacket.getResult_map();
                List<PumiceProceduralKnowledge> relevantProceduralKnowledgeList = new ArrayList<>();

                Map<String, PumiceProceduralKnowledge> procedureKnowledgeUtteranceProcedureKnowledgeMap = pumiceDialogManager.getPumiceKnowledgeManager().getProcedureKnowledgeUtteranceProcedureKnowledgeMap(false);
                String appActivityName = null;
                for (String activityName : screenRelevantUtteranceMap.keySet()) {
                    appActivityName = activityName;
                    List<SoviteAppResolutionResultPacket.ResultScorePair> relevantProceduralKnowledgeUtteranceList = screenRelevantUtteranceMap.get(activityName);
                    for (SoviteAppResolutionResultPacket.ResultScorePair resultScorePair : relevantProceduralKnowledgeUtteranceList) {
                        if (procedureKnowledgeUtteranceProcedureKnowledgeMap.containsKey(resultScorePair.result_string)) {
                            relevantProceduralKnowledgeList.add(procedureKnowledgeUtteranceProcedureKnowledgeMap.get(resultScorePair.result_string));
                        }
                    }
                }

                SoviteScriptMatchedFromScreenIntentHandler scriptMatchedFromScreenIntentHandler = new SoviteScriptMatchedFromScreenIntentHandler(pumiceDialogManager, context, sugiliteData, appReferenceScript, matchedAppPackageName, matchedAppReadableName, appActivityName, originalUtterance, relevantProceduralKnowledgeList, returnValueCallbackObject);
                pumiceDialogManager.updateUtteranceIntentHandlerInANewState(scriptMatchedFromScreenIntentHandler);
                pumiceDialogManager.callSendPromptForTheIntentHandlerForCurrentIntentHandler();

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
