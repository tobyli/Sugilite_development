package edu.cmu.hcii.sugilite.model.variable;

import java.io.Serializable;
import java.util.Objects;

/**
 * @author toby
 * @date 7/11/16
 * @time 4:53 PM
 */
public class VariableValue<T> implements Serializable {
    private String variableName;
    private T variableValue;

    private VariableContext variableValueContext;

    public VariableValue(String variableName){
        super();
        this.variableName = variableName;
    }

    public VariableValue(String variableName, T variableValue){
        super();
        this.variableName = variableName;
        this.variableValue = variableValue;
    }

    public void setVariableName(String variableName) {
        this.variableName = variableName;
    }

    public String getVariableName() {
        return variableName;
    }

    public void setVariableValue(T variableValue) {
        this.variableValue = variableValue;
    }

    public T getVariableValue() {
        return variableValue;
    }

    public void setVariableValueContext(VariableContext variableValueContext) {
        this.variableValueContext = variableValueContext;
    }

    public VariableContext getVariableValueContext() {
        return variableValueContext;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof VariableValue) {
            return (this.variableName.equals(((VariableValue) obj).variableName) && this.variableValue.equals(((VariableValue) obj).getVariableValue()));
        } else {
            return super.equals(obj);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(variableName, variableValue);
    }
}
