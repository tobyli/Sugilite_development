package edu.cmu.hcii.sugilite.pumice.kb;
import android.content.Context;

import com.google.gson.Gson;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;
import edu.cmu.hcii.sugilite.model.variable.Variable;
import edu.cmu.hcii.sugilite.ontology.description.OntologyDescriptionGenerator;

import static edu.cmu.hcii.sugilite.Const.HOME_SCREEN_PACKAGE_NAMES;

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
    transient private SugiliteStartingBlock sugiliteStartingBlock;

    public PumiceProceduralKnowledge(){

    }

    public PumiceProceduralKnowledge(String procedureName, String utterance, Collection<String> involvedAppNames){
        this.procedureName = procedureName;
        this.utterance = utterance;
        this.involvedAppNames = new ArrayList<>();
        this.involvedAppNames.addAll(involvedAppNames);
        this.parameterNameParameterMap = new HashMap<>();
    }

    public PumiceProceduralKnowledge(Context context, String procedureName, String utterance, SugiliteStartingBlock startingBlock){
        this.procedureName = procedureName;
        this.utterance = utterance;
        this.involvedAppNames = new ArrayList<>();
        this.parameterNameParameterMap = new HashMap<>();
        this.sugiliteStartingBlock = startingBlock;

        //populate involvedAppNames
        Set<String> involvedAppPackageNames = new HashSet<>();
        Set<String> homeScreenPackageNameSet = new HashSet<>(Arrays.asList(HOME_SCREEN_PACKAGE_NAMES));
        for(String packageName : startingBlock.relevantPackages){
            if (! homeScreenPackageNameSet.contains(packageName)){
                involvedAppPackageNames.add(packageName);
            }
        }
        for(String packageName : involvedAppPackageNames){
            //TODO: get app name for package name
            involvedAppNames.add(OntologyDescriptionGenerator.getAppName(context, packageName));
        }
        //populate parameterNameParameterMap
        if(startingBlock.variableNameDefaultValueMap != null) {
            for (Map.Entry<String, Variable> variableNameVariable : startingBlock.variableNameDefaultValueMap.entrySet()) {
                if (variableNameVariable.getValue().type == Variable.USER_INPUT){
                    String parameterName = variableNameVariable.getValue().getName();
                    String defaultValue = variableNameVariable.getValue().getName();
                    List<String> alternativeValues = new ArrayList<>();
                    if (startingBlock.variableNameAlternativeValueMap.containsKey(parameterName)){
                        alternativeValues.addAll(startingBlock.variableNameAlternativeValueMap.get(parameterName));
                    }
                    parameterNameParameterMap.put(parameterName, new PumiceProceduralKnowledgeParameter(parameterName, defaultValue, alternativeValues));
                }
            }
        }
    }

    public void copyFrom(PumiceProceduralKnowledge pumiceProceduralKnowledge){
        this.procedureName = pumiceProceduralKnowledge.procedureName;
        this.utterance = pumiceProceduralKnowledge.utterance;
        this.involvedAppNames = pumiceProceduralKnowledge.involvedAppNames;
        this.parameterNameParameterMap = pumiceProceduralKnowledge.parameterNameParameterMap;
        this.sugiliteStartingBlock = pumiceProceduralKnowledge.sugiliteStartingBlock;
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

    public void setProcedureName(String procedureName) {
        this.procedureName = procedureName;
    }

    public void setUtterance(String utterance) {
        this.utterance = utterance;
    }

    public static class PumiceProceduralKnowledgeParameter<T> {
        //T currently supports numerical and String
        private String parameterName;
        private T parameterDefaultValue;
        private List<T> parameterAlternativeValues;

        public PumiceProceduralKnowledgeParameter(String parameterName, T parameterDefaultValue){
            this(parameterName, parameterDefaultValue, null);
        }

        public PumiceProceduralKnowledgeParameter(String parameterName, T parameterDefaultValue, List<T> parameterAlternativeValues){
            this.parameterName = parameterName;
            this.parameterDefaultValue = parameterDefaultValue;
            this.parameterAlternativeValues = new ArrayList<>();
            this.parameterAlternativeValues = parameterAlternativeValues;
        }

        public void addParameterAlternativeValues (T value){
            parameterAlternativeValues.add(value);
        }
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }

}
