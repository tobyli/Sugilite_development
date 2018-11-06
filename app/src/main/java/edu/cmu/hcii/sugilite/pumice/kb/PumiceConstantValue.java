package edu.cmu.hcii.sugilite.pumice.kb;

import com.google.gson.Gson;

/**
 * @author toby
 * @date 10/30/18
 * @time 3:04 PM
 */
public class PumiceConstantValue<T> {
    enum ValueType {NUMERICAL, STRING}
    private T value;
    private String unit;
    private ValueType valueType;

    public PumiceConstantValue(T value, String unit){
        this.value = value;
        this.unit = unit;
        if (value instanceof Number){
            this.valueType = ValueType.NUMERICAL;
        } else {
            this.valueType = ValueType.STRING;
        }
    }

    public PumiceConstantValue(T value){
        this.value = value;
        this.valueType = valueType;
        if (value instanceof Number){
            this.valueType = ValueType.NUMERICAL;
        } else {
            this.valueType = ValueType.STRING;
        }
    }

    public T getValue() {
        return value;
    }

    public ValueType getValueType() {
        return valueType;
    }

    public String getUnit() {
        return unit;
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }
}
