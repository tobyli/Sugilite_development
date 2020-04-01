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
    public Map<String, VariableValue> variableNameVariableValueMap;
    public VariableHelper(Map<String, VariableValue> variableNameVariableValueMap){
        this.variableNameVariableValueMap = variableNameVariableValueMap;
    }


    public String replaceVariableReferencesWithTheirValues(String text){
        if(variableNameVariableValueMap == null) {
            return text;
        }
        String currentText = new String(text);
        for(Map.Entry<String, VariableValue> entry : variableNameVariableValueMap.entrySet()){
            if(entry.getValue().getVariableValue() instanceof String) {
                currentText = currentText.replace("[" + entry.getKey() + "]", entry.getValue().getVariableValue().toString());
            }
        }
        return currentText;
    }

    public static boolean isAVariable(String text){
        return text.startsWith("[") && text.endsWith("]");
    }

    public static String getVariableName(String text){
        if (isAVariable(text)) {
            return text.substring(1, text.length() - 1);
        } else {
            return text;
        }
    }

}
