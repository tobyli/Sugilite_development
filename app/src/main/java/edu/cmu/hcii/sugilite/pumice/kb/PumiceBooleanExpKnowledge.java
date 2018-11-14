package edu.cmu.hcii.sugilite.pumice.kb;
import com.google.gson.Gson;

/**
 * @author toby
 * @date 10/29/18
 * @time 11:54 AM
 */
public class PumiceBooleanExpKnowledge {
    private String expName;
    private String utterance;

    enum Comparator {GREATER_THAN, LESS_THAN, GREATER_THAN_OR_EQUAL_TO, LESS_THAN_OR_EQUAL_TO, EQUAL, NOT_EQUAL, CONTAINS, NOT_CONTAINS}

    //initial version
    private PumiceValueQueryKnowledge param1;
    private Comparator comparator;
    private PumiceConstantValue param2;

    public PumiceBooleanExpKnowledge (String expName, String utterance, PumiceValueQueryKnowledge param1, Comparator comparator, PumiceConstantValue param2){
        this.expName = expName;
        this.utterance = utterance;
        this.param1 = param1;
        this.comparator = comparator;
        this.param2 = param2;
    }

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

    public String getProcedureDescription(){
        return "How to know whether " + utterance + " using " + param1.getValueName();
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }
}
