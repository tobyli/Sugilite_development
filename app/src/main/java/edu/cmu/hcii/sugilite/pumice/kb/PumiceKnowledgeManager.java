package edu.cmu.hcii.sugilite.pumice.kb;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

import edu.cmu.hcii.sugilite.pumice.kb.PumiceBooleanExpKnowledge;
import edu.cmu.hcii.sugilite.pumice.kb.PumiceConstantValue;
import edu.cmu.hcii.sugilite.pumice.kb.PumiceProceduralKnowledge;
import edu.cmu.hcii.sugilite.pumice.kb.PumiceValueQueryKnowledge;

/**
 * @author toby
 * @date 10/29/18
 * @time 11:55 AM
 */
public class PumiceKnowledgeManager {

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
        PumiceProceduralKnowledge testProceduralKnowledge = new PumiceProceduralKnowledge("order a cup of iced cappuccino", "order a cup of iced cappuccino", appNames);
        testProceduralKnowledge.addParameter(new PumiceProceduralKnowledge.PumiceProceduralKnowledgeParameter<>("iced cappuccino", "iced cappuccino"));
        addPumiceProceduralKnowledge(testProceduralKnowledge);

        PumiceValueQueryKnowledge<Double> testValueQueryKnowledge = new PumiceValueQueryKnowledge<>("temperature", PumiceValueQueryKnowledge.ValueType.NUMERICAL);
        addPumiceValueQueryKnowledge(testValueQueryKnowledge);

        PumiceBooleanExpKnowledge testBooleanExpKnowledge = new PumiceBooleanExpKnowledge("it is hot", "it is hot", testValueQueryKnowledge, PumiceBooleanExpKnowledge.Comparator.GREATER_THAN, new PumiceConstantValue(90, "Fahrenheit"));
        addPumiceBooleanExpKnowledge(testBooleanExpKnowledge);
    }

    public String getKnowledgeInString(){
        StringBuilder result = new StringBuilder();
        result.append("Here are the procedures I know: " + "\n");

        for(PumiceProceduralKnowledge proceduralKnowledge : pumiceProceduralKnowledges) {
            result.append(proceduralKnowledge.getProcedureDescription() + "\n");
        }
        result.append("\n");

        result.append("Here are the boolean concepts I know: " + "\n");
        for(PumiceBooleanExpKnowledge booleanExpKnowledge : pumiceBooleanExpKnowledges) {
            result.append(booleanExpKnowledge.getProcedureDescription() + "\n");
        }
        result.append("\n");

        result.append("Here are the value concepts I know: " + "\n");
        for(PumiceValueQueryKnowledge valueQueryKnowledge : pumiceValueQueryKnowledges) {
            result.append(valueQueryKnowledge.getProcedureDescription() + "\n");
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
