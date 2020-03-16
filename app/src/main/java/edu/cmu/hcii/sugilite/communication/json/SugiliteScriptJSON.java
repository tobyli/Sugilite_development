package edu.cmu.hcii.sugilite.communication.json;

import android.content.Context;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import edu.cmu.hcii.sugilite.model.block.SugiliteOperationBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;
import edu.cmu.hcii.sugilite.model.variable.StringVariable;
import edu.cmu.hcii.sugilite.model.variable.Variable;
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

        for(Map.Entry<String, Variable> entry : startingBlock.variableNameDefaultValueMap.entrySet()){
            if(entry.getValue() instanceof StringVariable)
                this.variableDefaultValues.put(entry.getKey(), ((StringVariable) entry.getValue()).getValue());
        }

        for(Map.Entry<String, Set<String>> entry : startingBlock.variableNameAlternativeValueMap.entrySet()){
            if(entry.getValue() != null)
                this.variableAlternativeValues.put(entry.getKey(), entry.getValue());
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
