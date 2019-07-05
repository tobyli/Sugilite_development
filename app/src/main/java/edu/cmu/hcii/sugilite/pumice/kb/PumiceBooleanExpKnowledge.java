package edu.cmu.hcii.sugilite.pumice.kb;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.model.block.booleanexp.SugiliteBooleanExpressionNew;
import edu.cmu.hcii.sugilite.model.value.SugiliteValue;
import edu.cmu.hcii.sugilite.pumice.communication.SkipPumiceJSONSerialization;
import edu.cmu.hcii.sugilite.pumice.dialog.intent_handler.parsing_confirmation.PumiceParsingDifferenceProcessor;

/**
 * @author toby
 * @date 10/29/18
 * @time 11:54 AM
 */
public class PumiceBooleanExpKnowledge implements Serializable {
    private String expName;
    private String utterance;


    //initial version

    //can be a constant, a resolve call or a get call
    private SugiliteValue arg0;
    private SugiliteBooleanExpressionNew.BoolOperator boolOperator;

    //can be a constant, a resolve call or a get call

    //arg1 can be varied depending on the scenario
    private SugiliteValue arg1;

    //for now, use the procedure utterance as the scenario key
    private Map<String, SugiliteValue> scenarioArg1Map;

    public PumiceBooleanExpKnowledge(){
        this.scenarioArg1Map = new HashMap<>();
    }

    public PumiceBooleanExpKnowledge (String expName, String utterance, SugiliteValue arg0, SugiliteBooleanExpressionNew.BoolOperator boolOperator, SugiliteValue arg1){
        this();
        this.expName = expName;
        this.utterance = utterance;
        this.arg0 = arg0;
        this.boolOperator = boolOperator;
        this.arg1 = arg1;
    }

    public PumiceBooleanExpKnowledge (String expName, String utterance, SugiliteBooleanExpressionNew sugiliteBooleanExpression){
        this(expName, utterance, sugiliteBooleanExpression.getArg0(), sugiliteBooleanExpression.getBoolOperator(), sugiliteBooleanExpression.getArg1());
    }

    public void copyFrom(PumiceBooleanExpKnowledge pumiceBooleanExpKnowledge){
        this.expName = pumiceBooleanExpKnowledge.expName;
        this.utterance = pumiceBooleanExpKnowledge.utterance;
        this.arg0 = pumiceBooleanExpKnowledge.arg0;
        this.boolOperator = pumiceBooleanExpKnowledge.boolOperator;
        this.arg1 = pumiceBooleanExpKnowledge.arg1;
    }

    /**
     * evaluate this boolean exp knowledge
     * @return
     */
    public boolean evaluate(SugiliteData sugiliteData){
        return SugiliteBooleanExpressionNew.evaluate(sugiliteData, arg0, arg1, boolOperator);
    }

    public void setExpName(String expName) {
        this.expName = expName;
    }

    public String getExpName() {
        return expName;
    }

    public Map<String, SugiliteValue> getScenarioArg1Map() {
        return scenarioArg1Map;
    }

    public SugiliteValue getArg1() {
        return arg1;
    }

    public void setArg1(SugiliteValue arg1) {
        this.arg1 = arg1;
    }

    public String getBooleanDescription(){
        //support generalization
        String description = String.format("How to know whether %s by checking if ", expName);

        List<String> individualScenarios = new ArrayList<>();

        for(Map.Entry<String, SugiliteValue> scenarioArg1 : scenarioArg1Map.entrySet()) {
            individualScenarios.add(String.format("%s is %s %s when determining whether to %s", arg0.getReadableDescription(), boolOperator.toString().toLowerCase().replace("_", " "), scenarioArg1.getValue().getReadableDescription(), scenarioArg1.getKey()));
        }

        if (individualScenarios.size() == 0 && utterance != null) {
            individualScenarios.add(utterance);
        }

        description = description + PumiceParsingDifferenceProcessor.separateWordsBy(individualScenarios, "and");

        return description;
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

    @Override
    public boolean equals(Object obj) {
        if (obj != null && obj instanceof PumiceBooleanExpKnowledge) {
            return this.toString().equals(obj.toString());
        } else {
            return super.equals(obj);
        }
    }

    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }
}
