package edu.cmu.hcii.sugilite.pumice.kb;
import com.google.gson.Gson;

import edu.cmu.hcii.sugilite.model.block.booleanexp.SugiliteBooleanExpressionNew;
import edu.cmu.hcii.sugilite.model.value.SugiliteValue;

/**
 * @author toby
 * @date 10/29/18
 * @time 11:54 AM
 */
public class PumiceBooleanExpKnowledge {
    private String expName;
    private String utterance;


    //initial version
    private SugiliteValue param1;
    private SugiliteBooleanExpressionNew.BoolOperator comparator;
    private SugiliteValue param2;

    public PumiceBooleanExpKnowledge(){

    }

    public PumiceBooleanExpKnowledge (String expName, String utterance, SugiliteValue param1, SugiliteBooleanExpressionNew.BoolOperator comparator, SugiliteValue param2){
        this.expName = expName;
        this.utterance = utterance;
        this.param1 = param1;
        this.comparator = comparator;
        this.param2 = param2;
    }

    public PumiceBooleanExpKnowledge (String expName, String utterance, SugiliteBooleanExpressionNew sugiliteBooleanExpression){
        this(expName, utterance, sugiliteBooleanExpression.getArg0(), sugiliteBooleanExpression.getBoolOperator(), sugiliteBooleanExpression.getArg1());
    }

    public void copyFrom(PumiceBooleanExpKnowledge pumiceBooleanExpKnowledge){
        this.expName = pumiceBooleanExpKnowledge.expName;
        this.utterance = pumiceBooleanExpKnowledge.utterance;
        this.param1 = pumiceBooleanExpKnowledge.param1;
        this.comparator = pumiceBooleanExpKnowledge.comparator;
        this.param2 = pumiceBooleanExpKnowledge.param2;
    }

    /*
    public boolean evaluate(){
        if(param1.getValueType().equals(PumiceValueQueryKnowledge.ValueType.NUMERICAL) && param2.getValueType().equals(PumiceConstantValue.ValueType.NUMERICAL)){
            switch (comparator){
                case EQUAL:
                    return param1.getValue().equals(param2.getValue());
                case NOT_EQUAL:
                    return (!param1.getValue().equals(param2.getValue()));
                case LESS_THAN:
                    return ((Double)param1.getValue()) < ((Double)param2.getValue());
                case GREATER_THAN:
                    return ((Double)param1.getValue()) > ((Double)param2.getValue());
                case LESS_THAN_OR_EQUAL_TO:
                    return ((Double)param1.getValue()) <= ((Double)param2.getValue());
                case GREATER_THAN_OR_EQUAL_TO:
                    return ((Double)param1.getValue()) >= ((Double)param2.getValue());
            }
        }

        if(param1.getValueType().equals(PumiceValueQueryKnowledge.ValueType.STRING) && param2.getValueType().equals(PumiceConstantValue.ValueType.STRING)){
            switch (comparator){
                case EQUAL:
                    return param1.getValue().equals(param2.getValue());
                case NOT_EQUAL:
                    return (!param1.getValue().equals(param2.getValue()));
                case CONTAINS:
                    return param1.getValue().toString().contains(param2.getValue().toString());
                case NOT_CONTAINS:
                    return (!param1.getValue().toString().contains(param2.getValue().toString()));
            }
        }
        return false;
    }
    */

    public void setExpName(String expName) {
        this.expName = expName;
    }

    public String getProcedureDescription(){
        String description = "How to know whether " + expName;
        if (param1 != null) {
            description = description + " using " + param1.toString();
        }
        if (utterance != null) {
            description = description + " by checking if " + utterance;
        }
        return description;
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }
}
