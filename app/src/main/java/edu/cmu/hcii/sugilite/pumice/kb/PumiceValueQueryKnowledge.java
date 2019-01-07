package edu.cmu.hcii.sugilite.pumice.kb;
import com.google.gson.Gson;

import edu.cmu.hcii.sugilite.model.operation.binary.SugiliteGetOperation;

import static edu.cmu.hcii.sugilite.model.operation.binary.SugiliteGetOperation.VALUE_QUERY_NAME;

/**
 * @author toby
 * @date 10/30/18
 * @time 3:02 PM
 */
public class PumiceValueQueryKnowledge<T> {
    public enum ValueType {NUMERICAL, STRING}
    private String valueName;
    private ValueType valueType;

    public PumiceValueQueryKnowledge(){

    }

    public PumiceValueQueryKnowledge(String valueName, ValueType valueType){
        this.valueName = valueName;
        this.valueType = valueType;
    }

    public void copyFrom(PumiceValueQueryKnowledge pumiceValueQueryKnowledge){
        this.valueName = pumiceValueQueryKnowledge.valueName;
        this.valueType = pumiceValueQueryKnowledge.valueType;
    }

    public String getValueName() {
        return valueName;
    }

    public ValueType getValueType() {
        return valueType;
    }

    public void setValueName(String valueName) {
        this.valueName = valueName;
    }

    T getValue(){
        return null;
    }

    public String getProcedureDescription(){
        return "How to get the value of " + valueName;
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }

    public SugiliteGetOperation getSugiliteOperation(){
        return new SugiliteGetOperation(valueName, VALUE_QUERY_NAME);
    }
}
