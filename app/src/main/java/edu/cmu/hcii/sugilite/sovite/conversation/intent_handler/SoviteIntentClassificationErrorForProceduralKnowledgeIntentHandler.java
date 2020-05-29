package edu.cmu.hcii.sugilite.sovite.conversation.intent_handler;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import edu.cmu.hcii.sugilite.R;
import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.model.block.SugiliteOperationBlock;
import edu.cmu.hcii.sugilite.model.operation.binary.SugiliteGetProcedureOperation;
import edu.cmu.hcii.sugilite.pumice.communication.PumiceInstructionPacket;
import edu.cmu.hcii.sugilite.pumice.communication.PumiceSemanticParsingResultPacket;
import edu.cmu.hcii.sugilite.pumice.communication.SkipPumiceJSONSerialization;
import edu.cmu.hcii.sugilite.pumice.dialog.PumiceDialogManager;
import edu.cmu.hcii.sugilite.pumice.dialog.PumiceUtterance;
import edu.cmu.hcii.sugilite.pumice.dialog.intent_handler.PumiceUtteranceIntentHandler;
import edu.cmu.hcii.sugilite.pumice.kb.PumiceKnowledgeManager;
import edu.cmu.hcii.sugilite.pumice.kb.PumiceProceduralKnowledge;
import edu.cmu.hcii.sugilite.sovite.SoviteAppNameAppInfoManager;
import edu.cmu.hcii.sugilite.sovite.communication.SoviteAppResolutionQueryPacket;
import edu.cmu.hcii.sugilite.sovite.communication.SoviteAppResolutionResultPacket;
import edu.cmu.hcii.sugilite.sovite.conversation.SoviteReturnValueCallbackInterface;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.server_comm.SugiliteVerbalInstructionHTTPQueryInterface;

/**
 * @author toby
 * @date 2/24/20
 * @time 3:11 PM
 */
public class SoviteIntentClassificationErrorForProceduralKnowledgeIntentHandler implements PumiceUtteranceIntentHandler, SugiliteVerbalInstructionHTTPQueryInterface, SoviteReturnValueCallbackInterface<PumiceProceduralKnowledge> {

    private PumiceDialogManager pumiceDialogManager;
    private Activity context;
    private String originalUtterance;
    private PumiceSemanticParsingResultPacket originalSemanticParsingResult;
    private SoviteAppNameAppInfoManager soviteAppNameAppInfoManager;
    private Calendar calendar;
    private SoviteReturnValueCallbackInterface<String> returnValueCallbackObject;
    private SugiliteData sugiliteData;

    public final static String RELEVANT_APPS_FOR_UTTERANCES = "RELEVANT_APPS_FOR_UTTERANCES";
    public final static String RELEVANT_UTTERANCES_FOR_APPS = "RELEVANT_UTTERANCES_FOR_APPS";

    public SoviteIntentClassificationErrorForProceduralKnowledgeIntentHandler(PumiceDialogManager pumiceDialogManager, Activity context, SugiliteData sugiliteData, String originalUtterance, PumiceSemanticParsingResultPacket originalSemanticParsingResult, SoviteReturnValueCallbackInterface<String> returnValueCallbackObject) {
        this.pumiceDialogManager = pumiceDialogManager;
        this.context = context;
        this.sugiliteData = sugiliteData;
        this.calendar = Calendar.getInstance();
        this.originalUtterance = originalUtterance;
        this.originalSemanticParsingResult = originalSemanticParsingResult;
        this.soviteAppNameAppInfoManager = SoviteAppNameAppInfoManager.getInstance(SugiliteData.getAppContext());
        this.returnValueCallbackObject = returnValueCallbackObject;
    }

    @Override
    public void sendPromptForTheIntentHandler() {
        pumiceDialogManager.sendAgentMessage(String.format("What app should I use to %s?", originalUtterance.toLowerCase()), true, true);
    }

    @Override
    public void handleIntentWithUtterance(PumiceDialogManager dialogManager, PumiceIntent pumiceIntent, PumiceUtterance utterance) {
        //TODO: add speech recognition bias
        if (pumiceIntent.equals(PumiceIntent.APP_REFERENCE)) {
            // the user has provided an app name
            List<String> availableAppNames = new ArrayList<>(soviteAppNameAppInfoManager.getAllAvailableAppPackageNameReadableNameMap(true).values());
            PumiceInstructionPacket pumiceInstructionPacket = new PumiceInstructionPacket(dialogManager.getPumiceKnowledgeManager(), PumiceIntent.APP_REFERENCE, calendar.getTimeInMillis(), utterance.getContent().toString(), availableAppNames);
            try {
                dialogManager.getHttpQueryManager().sendPumiceInstructionPacketOnASeparateThread(pumiceInstructionPacket, this);
            } catch (Exception e) {
                //TODO: error handling
                e.printStackTrace();
                pumiceDialogManager.sendAgentMessage("Failed to send the query", true, false);
            }


        } else if (pumiceIntent.equals(PumiceIntent.UNRECOGNIZED)) {
            pumiceDialogManager.sendAgentMessage("I can't recognize your response. Please respond with \"Yes\" or \"No\".", true, false);
            sendPromptForTheIntentHandler();
        }
    }

    @Override
    public void setContext(Activity context) {
        this.context = context;
    }

    @Override
    public PumiceIntent detectIntentFromUtterance(PumiceUtterance utterance) {
        //TODO: handle situations where the user can't provide an app name
        return PumiceIntent.APP_REFERENCE;
    }


    public static void handleAppReferenceResponse(SugiliteData sugiliteData, Gson gson, String result, String originalIntentUtterance, SoviteAppNameAppInfoManager soviteAppNameAppInfoManager, Activity context, PumiceDialogManager pumiceDialogManager, SugiliteVerbalInstructionHTTPQueryInterface caller, SoviteReturnValueCallbackInterface<PumiceProceduralKnowledge> returnValueCallbackObject) throws Exception {
        PumiceSemanticParsingResultPacket resultPacket = gson.fromJson(result, PumiceSemanticParsingResultPacket.class);
        resultPacket.cleanFormula();
        if (resultPacket.utteranceType != null) {
            switch (resultPacket.utteranceType) {
                case "APP_REFERENCE":
                    if (resultPacket.queries != null && resultPacket.queries.size() > 0) {
                        //parse out app names from the top query formula
                        String appName = soviteAppNameAppInfoManager.extractStringFromStringValueFormula(resultPacket.queries.get(0).formula);
                        if (resultPacket.userUtterance.toLowerCase().contains(appName.toLowerCase())) {
                            // pumiceDialogManager.sendAgentMessage(String.format("OK, I will %s in %s", originalUtterance, appName), true, false);

                            //try to match with the app name
                            String packageName = soviteAppNameAppInfoManager.getAppReadableNameAppPackageNameMap(false).get(appName);
                            if (packageName != null) {
                                Drawable icon = soviteAppNameAppInfoManager.getApplicationIconFromPackageName(packageName);
                                if (icon != null) {
                                    ImageView imageView = new ImageView(context);
                                    imageView.setImageDrawable(icon);
                                    //send the icon of the selected app
                                    pumiceDialogManager.sendAgentViewMessage(imageView, PumiceDialogManager.Sender.USER, String.format("ICON: %s", appName), false, false);
                                }

                                //1. check if we have other scripts that use this app
                                pumiceDialogManager.sendAgentMessage(String.format("I'm searching for intents that use %s.", appName), true, false);
                                PumiceKnowledgeManager knowledgeManager = pumiceDialogManager.getPumiceKnowledgeManager();
                                List<PumiceProceduralKnowledge> pumiceProceduralKnowledges = knowledgeManager.getPumiceProceduralKnowledges();
                                List<PumiceProceduralKnowledge> proceduralKnowledgesWithMatchedApps = new ArrayList<>();
                                List<String> allAvailableScriptUtterances = new ArrayList<>();

                                for (PumiceProceduralKnowledge pumiceProceduralKnowledge : pumiceProceduralKnowledges) {
                                    allAvailableScriptUtterances.add(pumiceProceduralKnowledge.getProcedureDescription(knowledgeManager, false));
                                    List<String> involvedAppNames = pumiceProceduralKnowledge.getInvolvedAppNames(knowledgeManager);
                                    if (involvedAppNames != null && involvedAppNames.contains(appName)) {
                                        proceduralKnowledgesWithMatchedApps.add(pumiceProceduralKnowledge);
                                    }
                                }
                                List<String> appPackageNames = new ArrayList<>();
                                appPackageNames.add(packageName);
                                SoviteAppResolutionQueryPacket soviteAppResolutionQueryPacket = new SoviteAppResolutionQueryPacket("playstore", RELEVANT_UTTERANCES_FOR_APPS, allAvailableScriptUtterances, appPackageNames);
                                pumiceDialogManager.getHttpQueryManager().sendSoviteAppResolutionPacketOnASeparateThread(soviteAppResolutionQueryPacket, caller);
                                /*
                                if (proceduralKnowledgesWithMatchedApps.size() > 0) {
                                    // able to find other procedures that use this app
                                    //use a new handler to handle
                                    SoviteScriptsWithTheSameAppDisambiguationIntentHandler soviteScriptsWithTheSameAppDisambiguationIntentHandler = new SoviteScriptsWithTheSameAppDisambiguationIntentHandler(pumiceDialogManager, context, sugiliteData, packageName, appName, originalIntentUtterance, proceduralKnowledgesWithMatchedApps, returnValueCallbackObject);
                                    pumiceDialogManager.updateUtteranceIntentHandlerInANewState(soviteScriptsWithTheSameAppDisambiguationIntentHandler);
                                    pumiceDialogManager.callSendPromptForTheIntentHandlerForCurrentIntentHandler();

                                } else {
                                    // unable to find other procedures that use this app
                                    pumiceDialogManager.sendAgentMessage(String.format("I can't find other intents that use %s.", appName), true, false);
                                    //2. check if we have other scripts that are similar to the embeddings of this app
                                    List<String> appPackageNames = new ArrayList<>();
                                    appPackageNames.add(packageName);

                                    // query for relevant utterances to the app
                                    SoviteAppResolutionQueryPacket soviteAppResolutionQueryPacket = new SoviteAppResolutionQueryPacket("playstore", RELEVANT_UTTERANCES_FOR_APPS, allAvailableScriptUtterances, appPackageNames);
                                    pumiceDialogManager.sendAgentMessage(String.format("I will search for intents that are relevant to %s.", appName), true, false);
                                    pumiceDialogManager.getHttpQueryManager().sendSoviteAppResolutionPacketOnASeparateThread(soviteAppResolutionQueryPacket, caller);
                                    // pumiceDialogManager.sendAgentMessage(String.format("Here are scripts are are relevant to %s", appName), true, false);
                                }
                                */

                                // retrieve the play store description and snapshots for packageName, and calculate the BERT vector
                                        /*
                                        List<String> allPackageNames = new ArrayList<>(soviteAppNameAppInfoManager.getAllAvailableAppPackageNameReadableNameMap(true).keySet());
                                        List<String> utteranceList = new ArrayList<>();
                                        utteranceList.add(originalUtterance);
                                        SoviteAppResolutionQueryPacket soviteAppResolutionQueryPacket = new SoviteAppResolutionQueryPacket("playstore", RELEVANT_APPS_FOR_UTTERANCES, utteranceList, allPackageNames);
                                        pumiceDialogManager.getHttpQueryManager().sendSoviteAppResolutionPacketOnASeparateThread(soviteAppResolutionQueryPacket, this);
                                        */
                            }

                        } else {
                            throw new RuntimeException("the utterance does not contain the app name");
                        }
                    } else {
                        throw new RuntimeException("empty server result");
                    }
                    break;
                default:
                    throw new RuntimeException("wrong type of result");
            }
        }
    }
    public static void handleRelevantAppsForUtterancesResponse(Gson gson, String result, PumiceDialogManager pumiceDialogManager) {
        SoviteAppResolutionResultPacket resultPacket = gson.fromJson(result, SoviteAppResolutionResultPacket.class);
        pumiceDialogManager.sendAgentMessage("Here are the relevant apps for your utterance", false, false);
        pumiceDialogManager.sendAgentMessage(resultPacket.getResult_map().toString(), false, false);
    }
    public static void handleRelevantUtterancesForAppsResponse(Gson gson, String result, SugiliteData sugiliteData, SoviteAppNameAppInfoManager soviteAppNameAppInfoManager, String originalUtterance, Activity context, PumiceDialogManager pumiceDialogManager, SoviteReturnValueCallbackInterface<PumiceProceduralKnowledge> returnValueCallbackObject) {
        SoviteAppResolutionResultPacket resultPacket = gson.fromJson(result, SoviteAppResolutionResultPacket.class);
        Map<String, List<SoviteAppResolutionResultPacket.ResultScorePair>> appRelevantUtteranceMap = resultPacket.getResult_map();
        List<PumiceProceduralKnowledge> relevantProceduralKnowledgeList = new ArrayList<>();

        Map<String, PumiceProceduralKnowledge> procedureKnowledgeUtteranceProcedureKnowledgeMap = pumiceDialogManager.getPumiceKnowledgeManager().getProcedureKnowledgeUtteranceProcedureKnowledgeMap(false);
        String appPackageName = null;
        for (String packageName : appRelevantUtteranceMap.keySet()) {
            appPackageName = packageName;
            List<SoviteAppResolutionResultPacket.ResultScorePair> relevantProceduralKnowledgeUtteranceList = appRelevantUtteranceMap.get(packageName);
            for (SoviteAppResolutionResultPacket.ResultScorePair resultScorePair : relevantProceduralKnowledgeUtteranceList) {
                if (procedureKnowledgeUtteranceProcedureKnowledgeMap.containsKey(resultScorePair.result_string)) {
                    relevantProceduralKnowledgeList.add(procedureKnowledgeUtteranceProcedureKnowledgeMap.get(resultScorePair.result_string));
                }
            }
        }
        String appReadableName = soviteAppNameAppInfoManager.getReadableAppNameForPackageName(appPackageName);
        SoviteScriptRelevantToAppIntentHandler soviteScriptRelevantToAppIntentHandler = new SoviteScriptRelevantToAppIntentHandler(pumiceDialogManager, context, sugiliteData, appPackageName, appReadableName, originalUtterance, relevantProceduralKnowledgeList, returnValueCallbackObject);
        pumiceDialogManager.updateUtteranceIntentHandlerInANewState(soviteScriptRelevantToAppIntentHandler);
        pumiceDialogManager.callSendPromptForTheIntentHandlerForCurrentIntentHandler();
    }
    public static void handleServerResponseError(Context context, Exception e, PumiceDialogManager pumiceDialogManager, PumiceUtteranceIntentHandler intentHandler) {
        //error handling
        if (e.getMessage().contains("empty server result")) {
            pumiceDialogManager.sendAgentMessage("Empty server response", true, false);
        } else {
            pumiceDialogManager.sendAgentMessage(context.getString(R.string.not_able_read_server_response), true, false);
        }
        pumiceDialogManager.sendAgentMessage(context.getString(R.string.try_again), true, false);
        pumiceDialogManager.updateUtteranceIntentHandlerInANewState(intentHandler);
        intentHandler.sendPromptForTheIntentHandler();
        e.printStackTrace();
    }

    @Override
    public void resultReceived(int responseCode, String result, String originalQuery) {
        //handle the parser's result
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
                handleAppReferenceResponse(sugiliteData, gson, result, originalUtterance, soviteAppNameAppInfoManager, context, pumiceDialogManager, this, this);
            } catch (Exception e) {
               handleServerResponseError(context, e, pumiceDialogManager, this);
            }
        } else if (result.contains(RELEVANT_APPS_FOR_UTTERANCES)) {
            //handle queries of getting relevant apps for utterances
            try {
                handleRelevantAppsForUtterancesResponse(gson, result, pumiceDialogManager);
            } catch (Exception e) {
                handleServerResponseError(context, e, pumiceDialogManager, this);
            }
        } else if (result.contains(RELEVANT_UTTERANCES_FOR_APPS)) {
            //handle queries of getting relevant utterances for apps
            try {
               handleRelevantUtterancesForAppsResponse(gson, result, sugiliteData, soviteAppNameAppInfoManager, originalUtterance, context, pumiceDialogManager, this);
            } catch (Exception e) {
                handleServerResponseError(context, e, pumiceDialogManager, this);
            }
        }
    }

    @Override
    public void callReturnValueCallback(PumiceProceduralKnowledge proceduralKnowledge) {
        // save proceduralKnowledge to PumiceKnowledgeManager if it is newly learned
        if (proceduralKnowledge.isNewlyLearned) {
            pumiceDialogManager.getPumiceKnowledgeManager().addPumiceProceduralKnowledge(proceduralKnowledge);
            pumiceDialogManager.savePumiceKnowledgeToDao();
            pumiceDialogManager.sendAgentMessage("OK, I learned " + proceduralKnowledge.getProcedureDescription(pumiceDialogManager.getPumiceKnowledgeManager(), true).toLowerCase() + ".", true, false);
        } else {
            pumiceDialogManager.sendAgentMessage(String.format("OK, I will invoke the existing intent to %s.", proceduralKnowledge.getProcedureDescription(pumiceDialogManager.getPumiceKnowledgeManager(), false)), true, false);
        }

        SugiliteOperationBlock operationBlock = new SugiliteOperationBlock();
        operationBlock.setOperation(new SugiliteGetProcedureOperation(proceduralKnowledge.getProcedureName()));
        returnValueCallbackObject.callReturnValueCallback(operationBlock.toString());
    }
}
