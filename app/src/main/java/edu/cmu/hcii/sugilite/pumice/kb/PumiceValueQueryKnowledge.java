package edu.cmu.hcii.sugilite.pumice.kb;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;

import com.google.gson.Gson;

import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.automation.ServiceStatusManager;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;
import edu.cmu.hcii.sugilite.model.operation.binary.SugiliteGetOperation;
import edu.cmu.hcii.sugilite.model.operation.binary.SugiliteGetValueOperation;
import edu.cmu.hcii.sugilite.model.variable.StringVariable;
import edu.cmu.hcii.sugilite.model.variable.Variable;
import edu.cmu.hcii.sugilite.pumice.dialog.PumiceDialogManager;
import edu.cmu.hcii.sugilite.pumice.dialog.demonstration.PumiceDemonstrationUtil;

/**
 * @author toby
 * @date 10/30/18
 * @time 3:02 PM
 */
public class PumiceValueQueryKnowledge<T> {
    public enum ValueType {NUMERICAL, STRING}
    private String valueName;
    private ValueType valueType;

    // the Sugilite block used to obtain the value at runtime
    transient private SugiliteStartingBlock sugiliteStartingBlock;


    public PumiceValueQueryKnowledge(){

    }

    public PumiceValueQueryKnowledge(String valueName, ValueType valueType){
        this.valueName = valueName;
        this.valueType = valueType;
    }

    public PumiceValueQueryKnowledge(Context context, String valueName, ValueType valueType, SugiliteStartingBlock sugiliteStartingBlock){
        this.valueName = valueName;
        this.valueType = valueType;
        this.sugiliteStartingBlock = sugiliteStartingBlock;
    }

    public void copyFrom(PumiceValueQueryKnowledge pumiceValueQueryKnowledge){
        this.valueName = pumiceValueQueryKnowledge.valueName;
        this.valueType = pumiceValueQueryKnowledge.valueType;
        this.sugiliteStartingBlock = pumiceValueQueryKnowledge.sugiliteStartingBlock;
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
                    if (sugiliteData.stringVariableMap.containsKey(valueName)){
                        Variable returnVariable = sugiliteData.stringVariableMap.get(valueName);
                        if (returnVariable instanceof StringVariable){
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
                } catch (InterruptedException e){
                    e.printStackTrace();
                }
            }

            //return the value
            pumiceDialogManager.sendAgentMessage("The value of " + valueName + " is " + returnValue.toString(), true, false);

            return (T)(returnValue.toString());
        } else {
            throw new RuntimeException("empty dialog manager!");
        }
    }

    public String getProcedureDescription(){
        return "How to get the value of " + valueName;
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }

    public SugiliteGetOperation getSugiliteOperation(){
        return new SugiliteGetValueOperation(valueName);
    }
}
