package edu.cmu.hcii.sugilite.pumice.communication;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import edu.cmu.hcii.sugilite.pumice.dialog.intent_handler.PumiceUtteranceIntentHandler;
import edu.cmu.hcii.sugilite.pumice.kb.PumiceKnowledgeManager;


/**
 * @author toby
 * @date 10/31/18
 * @time 11:15 PM
 */

public class PumiceInstructionPacket implements Serializable {
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
    private List<String> availableAppNames;

    private String parentKnowledgeName;

    public PumiceInstructionPacket(){

    }

    public PumiceInstructionPacket(PumiceKnowledgeManager existingKnowledge, String utteranceType, Long queryId, String userInput, String parentKnowledgeName, List<List<String>> triples, String entityClassNameFilter, List<String> availableAppNames){
        this.mode = "USER_COMMAND";
        this.utteranceType = utteranceType;
        this.queryId = queryId;
        this.existingKnowledge = existingKnowledge;
        this.userInput = userInput;
        this.triples = triples;
        this.entityClassNameFilter = entityClassNameFilter;
        this.parentKnowledgeName = parentKnowledgeName;
        this.availableAppNames = availableAppNames;
    }

    //used in USER_INIT_INSTRUCTION
    public PumiceInstructionPacket(PumiceKnowledgeManager existingKnowledge, PumiceUtteranceIntentHandler.PumiceIntent pumiceIntent, Long queryId, String userInput, String parentKnowledgeName) {
        this(existingKnowledge, pumiceIntent.name(), queryId, userInput, parentKnowledgeName, new ArrayList<>(), "", null);
    }

    //used in DEFINE_BOOL_EXPRESSION_INSTRUCTION, DEFINE_PROCEDURE_EXPLANATION, DEFINE_VALUE_EXPLANATION
    public PumiceInstructionPacket(PumiceKnowledgeManager existingKnowledge, String utteranceType, Long queryId, String userInput, String parentKnowledgeName) {
        this(existingKnowledge, utteranceType, queryId, userInput, parentKnowledgeName, new ArrayList<>(), "", null);
    }

    //used in APP_REFERENCE
    public PumiceInstructionPacket(PumiceKnowledgeManager existingKnowledge, PumiceUtteranceIntentHandler.PumiceIntent pumiceIntent, Long queryId, String userInput, List<String> availableAppNames) {
        this(existingKnowledge, pumiceIntent.name(), queryId, userInput, null, null, "", availableAppNames);
    }



    public String getUserInput() {
        return userInput;
    }

    public String getUtteranceType() {
        return utteranceType;
    }

    @Override
    public String toString() {
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
        return gson.toJson(this);
    }

    public static PumiceUtteranceIntentHandler.PumiceIntent getPumiceUtteranceIntentFromString(String intent){
        return PumiceUtteranceIntentHandler.PumiceIntent.valueOf(intent);
    }

}
