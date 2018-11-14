package edu.cmu.hcii.sugilite.pumice.communication;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

import edu.cmu.hcii.sugilite.pumice.dialog.intent_handler.PumiceUtteranceIntentHandler;
import edu.cmu.hcii.sugilite.pumice.kb.PumiceKnowledgeManager;


/**
 * @author toby
 * @date 10/31/18
 * @time 11:15 PM
 */

public class PumiceInstructionPacket {
    /**
     * Types of Pumice Instruction:
     * 1. USER_INIT_INSTRUCTION: the initial instruction from the user
     */


    private PumiceKnowledgeManager existingKnowledge;
    private String mode;
    private String utteranceType;
    private String userInput;
    private List<List<String>> triples;
    private String entityClassNameFilter;
    private Long queryId;
    private List<String> variableNames;

    public PumiceInstructionPacket(){

    }

    public PumiceInstructionPacket(PumiceKnowledgeManager existingKnowledge, PumiceUtteranceIntentHandler.PumiceIntent pumiceIntent, Long queryId, String userInput, List<List<String>> triples, String entityClassNameFilter){
        this.mode = "USER_COMMAND";
        this.utteranceType = pumiceIntent.name();
        this.queryId = queryId;
        this.existingKnowledge = existingKnowledge;
        this.userInput = userInput;
        this.triples = triples;
        this.entityClassNameFilter = entityClassNameFilter;
    }

    public PumiceInstructionPacket(PumiceKnowledgeManager existingKnowledge, PumiceUtteranceIntentHandler.PumiceIntent pumiceIntent, Long queryId, String userInput){
        this(existingKnowledge, pumiceIntent, queryId, userInput, new ArrayList<>(), "");
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }

    public static PumiceUtteranceIntentHandler.PumiceIntent getPumiceUtteranceIntentFromString(String intent){
        return PumiceUtteranceIntentHandler.PumiceIntent.valueOf(intent);
    }

}
