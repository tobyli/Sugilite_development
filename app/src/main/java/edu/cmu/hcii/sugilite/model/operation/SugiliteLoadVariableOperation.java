package edu.cmu.hcii.sugilite.model.operation;

import java.io.Serializable;

/**
 * @author toby
 * @date 8/6/16
 * @time 11:42 PM
 */
public class SugiliteLoadVariableOperation extends SugiliteOperation implements Serializable{
    private String variableName;
    private String propertyToSave;
    public SugiliteLoadVariableOperation(){
        super();
        this.setOperationType(LOAD_AS_VARIABLE);
    }
    public String getVariableName(){
        return variableName;
    }
    public void setVariableName(String variableName){
        this.variableName = variableName;
    }

    public String getPropertyToSave() {
        return propertyToSave;
    }

    public void setPropertyToSave(String propertyToSave) {
        this.propertyToSave = propertyToSave;
    }
}
