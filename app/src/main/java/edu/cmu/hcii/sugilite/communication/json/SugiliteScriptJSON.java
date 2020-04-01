package edu.cmu.hcii.sugilite.communication.json;

import android.content.Context;

import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import edu.cmu.hcii.sugilite.model.block.SugiliteOperationBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;
import edu.cmu.hcii.sugilite.model.variable.Variable;
import edu.cmu.hcii.sugilite.model.variable.VariableValue;
import edu.cmu.hcii.sugilite.pumice.PumiceDemonstrationUtil;

/**
 * Created by toby on 7/14/16.
 */
public class SugiliteScriptJSON {
    public SugiliteScriptJSON(SugiliteStartingBlock startingBlock){
        this.scriptName = new String(startingBlock.getScriptName());
        this.scriptName = PumiceDemonstrationUtil.removeScriptExtension(scriptName);
        variableDefaultValues = new HashMap<>();
        variableAlternativeValues = new HashMap<>();

        for(Map.Entry<String, VariableValue> entry : startingBlock.variableNameDefaultValueMap.entrySet()){
            if(entry.getValue().getVariableValue() instanceof String)
                this.variableDefaultValues.put(entry.getKey(), ((String) entry.getValue().getVariableValue()));
        }

        for(Map.Entry<String, Set<VariableValue>> entry : startingBlock.variableNameAlternativeValueMap.entrySet()){
            if(entry.getValue() != null) {
                Set<String> alternativeValueStringSet = new HashSet<>();
                entry.getValue().forEach(variableValue -> alternativeValueStringSet.add(variableValue.getVariableValue().toString()));
                this.variableAlternativeValues.put(entry.getKey(), alternativeValueStringSet);
            }
        }

        if(startingBlock.getNextBlockToRun() != null && startingBlock.getNextBlockToRun() instanceof SugiliteOperationBlock)
            nextBlock = new SugiliteOperationBlockJSON((SugiliteOperationBlock)startingBlock.getNextBlockToRun());
        this.JSONCreatedTime = Calendar.getInstance().getTimeInMillis();
        this.createdTime = startingBlock.getCreatedTime();
    }

    public SugiliteStartingBlock toSugiliteStartingBlock(Context context){
        SugiliteStartingBlock startingBlock = new SugiliteStartingBlock();
        startingBlock.setScriptName(scriptName);
        if(nextBlock != null)
            startingBlock.setNextBlock(nextBlock.toSugiliteOperationBlock(context));
        return startingBlock;
    }

    String scriptName;
    SugiliteOperationBlockJSON nextBlock;
    Map<String, String> variableDefaultValues;
    Map<String, Set<String>> variableAlternativeValues;
    long createdTime, JSONCreatedTime;

}
