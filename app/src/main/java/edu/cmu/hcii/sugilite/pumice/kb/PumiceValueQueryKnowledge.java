package edu.cmu.hcii.sugilite.pumice.kb;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.automation.ServiceStatusManager;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;
import edu.cmu.hcii.sugilite.model.operation.binary.SugiliteGetOperation;
import edu.cmu.hcii.sugilite.model.operation.binary.SugiliteGetValueOperation;
import edu.cmu.hcii.sugilite.model.value.SugiliteSimpleConstant;
import edu.cmu.hcii.sugilite.model.value.SugiliteValue;
import edu.cmu.hcii.sugilite.model.variable.StringVariable;
import edu.cmu.hcii.sugilite.model.variable.Variable;
import edu.cmu.hcii.sugilite.ontology.description.OntologyDescriptionGenerator;
import edu.cmu.hcii.sugilite.pumice.communication.SkipPumiceJSONSerialization;
import edu.cmu.hcii.sugilite.pumice.dialog.PumiceDialogManager;
import edu.cmu.hcii.sugilite.pumice.dialog.demonstration.PumiceDemonstrationUtil;

import static edu.cmu.hcii.sugilite.Const.HOME_SCREEN_PACKAGE_NAMES;

/**
 * @author toby
 * @date 10/30/18
 * @time 3:02 PM
 */
public class PumiceValueQueryKnowledge<T> implements Serializable {
    public enum ValueType {NUMERICAL, STRING}
    private String valueName;
    private ValueType valueType;
    private String userUtterance = "";

    //holds the value -- can be a get query, a constant or a resolve query.
    @SkipPumiceJSONSerialization
    private SugiliteValue sugiliteValue;

    //the Sugilite block used to obtain the value at runtime - only used when sugiliteValue is null
    //not serialized for GSON
    @SkipPumiceJSONSerialization
    private SugiliteStartingBlock sugiliteStartingBlock;

    //the list of involvedAppNames -> SHOULD be non-null only if sugiliteStartingBlock is non-null
    private List<String> involvedAppNames;

    public PumiceValueQueryKnowledge(){

    }

    public PumiceValueQueryKnowledge(String valueName, ValueType valueType){
        this.valueName = valueName;
        this.valueType = valueType;
    }

    public PumiceValueQueryKnowledge(String valueName, String userUtterance, ValueType valueType, SugiliteValue sugiliteValue){
        this.valueName = valueName;
        this.valueType = valueType;
        this.sugiliteValue = sugiliteValue;
        this.userUtterance = userUtterance;
    }

    public PumiceValueQueryKnowledge(Context context, String valueName, ValueType valueType, SugiliteStartingBlock sugiliteStartingBlock){
        this.valueName = valueName;
        this.valueType = valueType;
        this.sugiliteStartingBlock = sugiliteStartingBlock;
        this.userUtterance = "demonstrate";
        //populate involvedAppNames
        Set<String> involvedAppPackageNames = new HashSet<>();
        Set<String> homeScreenPackageNameSet = new HashSet<>(Arrays.asList(HOME_SCREEN_PACKAGE_NAMES));
        for(String packageName : sugiliteStartingBlock.relevantPackages){
            if (! homeScreenPackageNameSet.contains(packageName)){
                involvedAppPackageNames.add(packageName);
            }
        }
        for(String packageName : involvedAppPackageNames){
            //get app name for package name
            involvedAppNames.add(OntologyDescriptionGenerator.getAppName(context, packageName));
        }
    }

    public void copyFrom(PumiceValueQueryKnowledge pumiceValueQueryKnowledge){
        this.valueName = pumiceValueQueryKnowledge.valueName;
        this.valueType = pumiceValueQueryKnowledge.valueType;
        this.sugiliteStartingBlock = pumiceValueQueryKnowledge.sugiliteStartingBlock;
        this.sugiliteValue = pumiceValueQueryKnowledge.sugiliteValue;
        this.userUtterance = pumiceValueQueryKnowledge.userUtterance;
        this.involvedAppNames = pumiceValueQueryKnowledge.involvedAppNames;
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

    public T getValue(SugiliteData sugiliteData){
        //getting the value using the script stored in sugiliteStartingBlock
        PumiceDialogManager pumiceDialogManager = sugiliteData.pumiceDialogManager;
        if (pumiceDialogManager != null) {
            if (sugiliteValue != null){
                //if there is a sugiliteValue, simply return the result of evaluating it;
                Object result = sugiliteValue.evaluate(sugiliteData);
                try {
                    return (T)result;
                } catch (Exception e){
                    throw new RuntimeException("error in processing the value query -- can't find the target value knowledge");
                }

            } else {
                Activity context = pumiceDialogManager.getContext();
                ServiceStatusManager serviceStatusManager = ServiceStatusManager.getInstance(context);
                LayoutInflater layoutInflater = LayoutInflater.from(context);
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

                //shared variable between threads
                StringBuffer returnValue = new StringBuffer("");

                //this runnable gets executed at the end of the value query script
                Runnable afterExecutionRunnable = new Runnable() {
                    @Override
                    public void run() {
                        System.out.println(sugiliteData.stringVariableMap);
                        if (sugiliteData.stringVariableMap.containsKey(valueName)) {
                            Variable returnVariable = sugiliteData.stringVariableMap.get(valueName);
                            if (returnVariable instanceof StringVariable) {
                                synchronized (returnValue) {
                                    returnValue.append(((StringVariable) returnVariable).getValue());
                                    returnValue.notify();
                                }
                            } else {
                                throw new RuntimeException("error -- wrong type of variable");
                            }
                        } else {
                            throw new RuntimeException("error -- can't find the variable");
                        }

                    }
                };
                PumiceDemonstrationUtil.executeScript(context, serviceStatusManager, sugiliteStartingBlock, sugiliteData, layoutInflater, sharedPreferences, pumiceDialogManager, null, afterExecutionRunnable);

                synchronized (returnValue) {
                    try {
                        System.out.println("waiting for the script to return the value");
                        returnValue.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                //return the value
                pumiceDialogManager.sendAgentMessage("The value of " + valueName + " is " + returnValue.toString(), true, false);

                return (T) (returnValue.toString());
            }
        } else {
            throw new RuntimeException("empty dialog manager!");
        }
    }

    public String getValueDescription(){
        String description = "How to get the value of " + valueName;
        if (sugiliteValue != null && sugiliteValue instanceof SugiliteGetValueOperation){
            description = description + ", which is the value of " + ((SugiliteGetValueOperation) sugiliteValue).getName();
        }

        else if (sugiliteValue != null && sugiliteValue instanceof SugiliteSimpleConstant) {
            description = description + ", which is a constant " + ((SugiliteSimpleConstant) sugiliteValue).toString();
        }

        else if (involvedAppNames != null && involvedAppNames.size() > 0) {
            description = description + " in " + StringUtils.join(involvedAppNames, ",");
        }
        return description;
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

    public SugiliteGetOperation getSugiliteOperation(){
        return new SugiliteGetValueOperation(valueName);
    }
}