package edu.cmu.hcii.sugilite.communication.json;

import android.content.Context;

import edu.cmu.hcii.sugilite.model.block.SugiliteOperationBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;

/**
 * Created by toby on 7/14/16.
 */
public class SugiliteScriptJSON {
    public SugiliteScriptJSON(SugiliteStartingBlock startingBlock){
        this.scriptName = new String(startingBlock.getScriptName());
        this.scriptName.replace(".SugiliteScript", "");
        if(startingBlock.getNextBlock() != null && startingBlock.getNextBlock() instanceof SugiliteOperationBlock)
            nextBlock = new SugiliteBlockJSON((SugiliteOperationBlock)startingBlock.getNextBlock());
    }

    public SugiliteStartingBlock toSugiliteStartingBlock(Context context){
        SugiliteStartingBlock startingBlock = new SugiliteStartingBlock();
        startingBlock.setScriptName(scriptName);
        if(nextBlock != null)
            startingBlock.setNextBlock(nextBlock.toSugiliteOperationBlock(context));
        return startingBlock;
    }

    String scriptName;
    SugiliteBlockJSON nextBlock;


}
