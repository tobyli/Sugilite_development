package edu.cmu.hcii.sugilite.pumice.kb;
import com.google.gson.Gson;

/**
 * @author toby
 * @date 10/30/18
 * @time 3:02 PM
 */
public class PumiceValueQueryKnowledge<T> {
    enum ValueType {NUMERICAL, STRING}
    private String valueName;
    private ValueType valueType;

    public PumiceValueQueryKnowledge(String valueName, ValueType valueType){
        this.valueName = valueName;
        this.valueType = valueType;
    }

    public String getValueName() {
        return valueName;
    }

    public ValueType getValueType() {
        return valueType;
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
}
