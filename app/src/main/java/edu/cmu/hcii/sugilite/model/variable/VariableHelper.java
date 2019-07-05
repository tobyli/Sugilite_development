package edu.cmu.hcii.sugilite.model.variable;

import java.util.Map;
import java.util.Set;

/**
 * @author toby
 * @date 7/11/16
 * @time 5:03 PM
 */
public class VariableHelper {
    //TODO: variable helper should be able to parse a string, return the original string if can't match a variable, return the variable value if matched with a variable
    //clear before every launch, used to store the value for variables
    public Map<String, Variable> stringVariableMap;
    public VariableHelper(Map<String, Variable> stringVariableMap){
        this.stringVariableMap = stringVariableMap;
    }
    public String parse (String text){
        if(stringVariableMap == null)
            return text;
        String currentText = new String(text);
        for(Map.Entry<String, Variable> entry : stringVariableMap.entrySet()){
            if(entry.getValue() instanceof StringVariable)
                currentText = currentText.replace("@" + entry.getKey(), ((StringVariable) entry.getValue()).getValue());
        }
        return currentText;
    }

}
