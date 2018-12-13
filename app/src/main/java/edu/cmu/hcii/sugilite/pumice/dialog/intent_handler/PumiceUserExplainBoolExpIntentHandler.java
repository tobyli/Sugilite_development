package edu.cmu.hcii.sugilite.pumice.dialog.intent_handler;

import android.content.Context;

import com.google.gson.Gson;

import java.util.Calendar;

import edu.cmu.hcii.sugilite.pumice.communication.PumiceInstructionPacket;
import edu.cmu.hcii.sugilite.pumice.communication.PumiceSemanticParsingResultPacket;
import edu.cmu.hcii.sugilite.pumice.dialog.PumiceDialogManager;
import edu.cmu.hcii.sugilite.pumice.kb.PumiceBooleanExpKnowledge;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.server_comm.SugiliteVerbalInstructionHTTPQueryInterface;

/**
 * @author toby
 * @date 12/4/18
 * @time 4:15 PM
 */

//class used for handle utterances when the user explain a PumiceBooleanExpKnowledge
public class PumiceUserExplainBoolExpIntentHandler implements PumiceUtteranceIntentHandler, SugiliteVerbalInstructionHTTPQueryInterface {
    private transient Context context;
    private transient PumiceDialogManager pumiceDialogManager;
    private String parentKnowledgeName;

    //need to notify this lock when the bool expression is resolved, and return the value through this object
    PumiceBooleanExpKnowledge resolveBoolExpLock;
    Calendar calendar;

    public PumiceUserExplainBoolExpIntentHandler(PumiceDialogManager pumiceDialogManager, Context context, PumiceBooleanExpKnowledge resolveBoolExpLock, String parentKnowledgeName){
        this.pumiceDialogManager = pumiceDialogManager;
        this.context = context;
        this.calendar = Calendar.getInstance();
        this.resolveBoolExpLock = resolveBoolExpLock;
        this.parentKnowledgeName = parentKnowledgeName;
    }

    @Override
    public void setContext(Context context) {
        this.context = context;
    }

    @Override
    public void handleIntentWithUtterance(PumiceDialogManager dialogManager, PumiceIntent pumiceIntent, PumiceDialogManager.PumiceUtterance utterance) {
        if (pumiceIntent.equals(PumiceIntent.DEFINE_BOOL_EXP)){
            dialogManager.sendAgentMessage("I have received your explanation: " + utterance.getContent(), true, false);

            //send out the server query
            PumiceInstructionPacket pumiceInstructionPacket = new PumiceInstructionPacket(dialogManager.getPumiceKnowledgeManager(), "BOOL_EXP_INSTRUCTION", calendar.getTimeInMillis(), utterance.getContent(), parentKnowledgeName);
            dialogManager.sendAgentMessage("Sending out the server query below...", true, false);
            dialogManager.sendAgentMessage(pumiceInstructionPacket.toString(), false, false);
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
        return PumiceIntent.DEFINE_BOOL_EXP;
    }

    @Override
    public void resultReceived(int responseCode, String result) {
        //handle server response from the semantic parsing server
        Gson gson = new Gson();
        try {
            PumiceSemanticParsingResultPacket resultPacket = gson.fromJson(result, PumiceSemanticParsingResultPacket.class);
            if (resultPacket.utteranceType != null) {
                switch (resultPacket.utteranceType) {
                    case "BOOL_EXP_INSTRUCTION":
                        if (resultPacket.queries != null && resultPacket.queries.size() > 0) {
                            PumiceSemanticParsingResultPacket.QueryGroundingPair topResult = resultPacket.queries.get(0);
                            if (topResult.formula != null) {
                                //feedback msg
                                pumiceDialogManager.sendAgentMessage("Received the parsing result from the server: ", true, false);
                                pumiceDialogManager.sendAgentMessage(topResult.formula, false, false);

                                pumiceDialogManager.getExecutorService().submit(new Runnable() {
                                    @Override
                                    public void run() {
                                        //parse and process the server response
                                        PumiceBooleanExpKnowledge pumiceBooleanExpKnowledge = pumiceDialogManager.getPumiceInitInstructionParsingHandler().parseFromBoolExpInstruction(topResult.formula, resultPacket.userUtterance, parentKnowledgeName);

                                        //notify the original thread for resolving unknown bool exp that the intent has been fulfilled
                                        returnUserExplainBoolExpResult(pumiceDialogManager, pumiceBooleanExpKnowledge);
                                    }
                                });
                            } else {
                                throw new RuntimeException("empty formula");
                            }
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
            e.printStackTrace();
        }
    }

    @Override
    public void runOnMainThread(Runnable r) {
        pumiceDialogManager.runOnMainThread(r);
    }

    /**
     * return the result PumiceBooleanExpKnowledge, and release the lock in the original PumiceInitInstructionParsingHandler
     * @param dialogManager
     * @param booleanExpKnowledge
     */
    private void returnUserExplainBoolExpResult(PumiceDialogManager dialogManager, PumiceBooleanExpKnowledge booleanExpKnowledge){
        synchronized (resolveBoolExpLock) {
            resolveBoolExpLock.copyFrom(booleanExpKnowledge);
            resolveBoolExpLock.notify();
        }
    }
}
