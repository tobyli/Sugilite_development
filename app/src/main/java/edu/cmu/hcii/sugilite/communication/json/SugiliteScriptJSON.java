package edu.cmu.hcii.sugilite.communication.json;

import android.content.Context;

import java.util.HashMap;
import java.util.Map;

import edu.cmu.hcii.sugilite.model.block.SugiliteOperationBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;
import edu.cmu.hcii.sugilite.model.variable.StringVariable;
import edu.cmu.hcii.sugilite.model.variable.Variable;

/**
 * Created by toby on 7/14/16.
 */
public class SugiliteScriptJSON {
    public SugiliteScriptJSON(SugiliteStartingBlock startingBlock){
        this.scriptName = new String(startingBlock.getScriptName());
        this.scriptName.replace(".SugiliteScript", "");
        variables = new HashMap<>();
        for(Map.Entry<String, Variable> entry : startingBlock.variableNameDefaultValueMap.entrySet()){
            if(entry.getValue() instanceof StringVariable)
                this.variables.put(entry.getKey(), ((StringVariable) entry.getValue()).getValue());
        }
        if(startingBlock.getNextBlock() != null && startingBlock.getNextBlock() instanceof SugiliteOperationBlock)
            nextBlock = new SugiliteOperationBlockJSON((SugiliteOperationBlock)startingBlock.getNextBlock());
        this.createdTime = createdTime;
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
    Map<String, String> variables;
    long createdTime;


}
