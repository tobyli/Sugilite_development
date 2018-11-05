package edu.cmu.hcii.sugilite.pumice.kb;

import com.google.gson.Gson;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author toby
 * @date 10/29/18
 * @time 11:54 AM
 */
public class PumiceProceduralKnowledge {
    private String procedureName;
    private String utterance;
    private List<String> involvedAppNames;
    private Map<String, PumiceProceduralKnowledgeParameter> parameterNameParameterMap;

    public PumiceProceduralKnowledge(String procedureName, String utterance, Collection<String> involvedAppNames){
        this.procedureName = procedureName;
        this.utterance = utterance;
        this.involvedAppNames = new ArrayList<>();
        this.involvedAppNames.addAll(involvedAppNames);
        parameterNameParameterMap = new HashMap<>();
    }

    public void addParameter(PumiceProceduralKnowledgeParameter parameter){
        parameterNameParameterMap.put(parameter.parameterName, parameter);
    }

    public void addParameters(Collection<PumiceProceduralKnowledgeParameter> parameters){
        for(PumiceProceduralKnowledgeParameter parameter : parameters){
            addParameter(parameter);
        }
    }

    public void execute(){
        //TODO: implement the execution
    }

    public String getProcedureDescription(){
        String parameterizedUtterance = new String(utterance);
        for(String parameter : parameterNameParameterMap.keySet()){
            parameterizedUtterance = parameterizedUtterance.replace(parameter, "something");
        }

        return "How to " + parameterizedUtterance + " in " + StringUtils.join(involvedAppNames, ",");
    }


    public static class PumiceProceduralKnowledgeParameter<T> {
        //T currently supports numerical and String
        private String parameterName;
        private T parameterDefaultValue;
        private List<T> parameterAlternativeValues;

        public PumiceProceduralKnowledgeParameter(String parameterName, T parameterDefaultValue){
            this.parameterName = parameterName;
            this.parameterDefaultValue = parameterDefaultValue;
            this.parameterAlternativeValues = new ArrayList<>();
        }

        public void AddParameterAlternativeValues (T value){
            parameterAlternativeValues.add(value);
        }
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }

}
