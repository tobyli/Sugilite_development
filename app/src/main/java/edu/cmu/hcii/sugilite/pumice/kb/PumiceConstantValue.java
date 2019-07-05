package edu.cmu.hcii.sugilite.pumice.kb;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import edu.cmu.hcii.sugilite.pumice.communication.SkipPumiceJSONSerialization;

/**
 * @author toby
 * @date 10/30/18
 * @time 3:04 PM
 */
@Deprecated
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
}
