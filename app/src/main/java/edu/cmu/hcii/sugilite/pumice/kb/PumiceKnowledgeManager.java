package edu.cmu.hcii.sugilite.pumice.kb;

import com.google.gson.annotations.Expose;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import edu.cmu.hcii.sugilite.model.block.booleanexp.SugiliteBooleanExpressionNew;
import edu.cmu.hcii.sugilite.model.value.SugiliteSimpleConstant;

/**
 * @author toby
 * @date 10/29/18
 * @time 11:55 AM
 */
public class PumiceKnowledgeManager implements Serializable {
    private List<PumiceBooleanExpKnowledge> pumiceBooleanExpKnowledges;
    private List<PumiceProceduralKnowledge> pumiceProceduralKnowledges;
    private List<PumiceValueQueryKnowledge> pumiceValueQueryKnowledges;

    public PumiceKnowledgeManager(){
        this.pumiceBooleanExpKnowledges = new ArrayList<>();
        this.pumiceProceduralKnowledges = new ArrayList<>();
        this.pumiceValueQueryKnowledges = new ArrayList<>();
    }

    public List<PumiceBooleanExpKnowledge> getPumiceBooleanExpKnowledges() {
        return pumiceBooleanExpKnowledges;
    }

    public List<PumiceProceduralKnowledge> getPumiceProceduralKnowledges() {
        return pumiceProceduralKnowledges;
    }

    public List<PumiceValueQueryKnowledge> getPumiceValueQueryKnowledges() {
        return pumiceValueQueryKnowledges;
    }

    public void addPumiceBooleanExpKnowledge(PumiceBooleanExpKnowledge pumiceBooleanExpKnowledge){
        this.pumiceBooleanExpKnowledges.add(pumiceBooleanExpKnowledge);
    }

    public void addPumiceProceduralKnowledge(PumiceProceduralKnowledge pumiceProceduralKnowledge){
        this.pumiceProceduralKnowledges.add(pumiceProceduralKnowledge);
    }

    public void addPumiceValueQueryKnowledge(PumiceValueQueryKnowledge pumiceValueQueryKnowledge){
        this.pumiceValueQueryKnowledges.add(pumiceValueQueryKnowledge);
    }

    public void initForTesting(){
        List<String> appNames = new ArrayList<>();
        appNames.add("Starbucks");
        PumiceProceduralKnowledge testProceduralKnowledge = new PumiceProceduralKnowledge("order a cup of iced cappuccino", "order a cup of iced cappuccino", null, appNames);
        testProceduralKnowledge.addParameter(new PumiceProceduralKnowledge.PumiceProceduralKnowledgeParameter<>("iced cappuccino", "iced cappuccino"));
        addPumiceProceduralKnowledge(testProceduralKnowledge);

        PumiceValueQueryKnowledge<Double> testValueQueryKnowledge = new PumiceValueQueryKnowledge<>("price", PumiceValueQueryKnowledge.ValueType.STRING);
        addPumiceValueQueryKnowledge(testValueQueryKnowledge);

        PumiceBooleanExpKnowledge testBooleanExpKnowledge = new PumiceBooleanExpKnowledge("it is hot", "the temperature is above 90 degrees", testValueQueryKnowledge.getSugiliteOperation(), SugiliteBooleanExpressionNew.BoolOperator.GREATER_THAN, new SugiliteSimpleConstant<>(90, "Fahrenheit"));
        testBooleanExpKnowledge.getScenarioArg1Map().put(testProceduralKnowledge.getProcedureName(), testBooleanExpKnowledge.getArg1());

        addPumiceBooleanExpKnowledge(testBooleanExpKnowledge);
    }

    public String getKnowledgeInString(){
        StringBuilder result = new StringBuilder();
        result.append("Here are the procedures I know: " + "\n");

        for(PumiceProceduralKnowledge proceduralKnowledge : pumiceProceduralKnowledges) {
            result.append("- " + proceduralKnowledge.getProcedureDescription(this) + "\n\n");
        }
        result.append("\n========\n");

        result.append("Here are the boolean concepts I know: " + "\n");
        for(PumiceBooleanExpKnowledge booleanExpKnowledge : pumiceBooleanExpKnowledges) {
            result.append("- " + booleanExpKnowledge.getBooleanDescription() + "\n\n");
        }
        result.append("\n========\n");

        result.append("Here are the value concepts I know: " + "\n");
        for(PumiceValueQueryKnowledge valueQueryKnowledge : pumiceValueQueryKnowledges) {
            result.append("- " + valueQueryKnowledge.getValueDescription() + "\n\n");
        }

        return result.toString();
    }

    public String getRawKnowledgeInString(){
        StringBuilder result = new StringBuilder();
        result.append("Here are the procedures I know: " + "\n");
        for(PumiceProceduralKnowledge proceduralKnowledge : pumiceProceduralKnowledges) {
            result.append(proceduralKnowledge.toString() + "\n");
        }
        result.append("\n");

        result.append("Here are the boolean concepts I know: " + "\n");
        for(PumiceBooleanExpKnowledge booleanExpKnowledge : pumiceBooleanExpKnowledges) {
            result.append(booleanExpKnowledge.toString() + "\n");
        }
        result.append("\n");

        result.append("Here are the value concepts I know: " + "\n");
        for(PumiceValueQueryKnowledge valueQueryKnowledge : pumiceValueQueryKnowledges) {
            result.append(valueQueryKnowledge.toString() + "\n");
        }

        return result.toString();
    }
}
