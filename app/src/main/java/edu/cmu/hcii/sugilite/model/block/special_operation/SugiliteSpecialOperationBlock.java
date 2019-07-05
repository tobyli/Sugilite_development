package edu.cmu.hcii.sugilite.model.block.special_operation;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.Serializable;

import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptDao;
import edu.cmu.hcii.sugilite.model.block.SugiliteBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteOperationBlock;

/**
 * @author toby
 * @date 10/28/16
 * @time 6:55 PM
 */

/**
 * special operation block that has a run() method -- the run() method will be called when the the block is invoked
 */
public abstract class SugiliteSpecialOperationBlock extends SugiliteBlock implements Serializable {

    public SugiliteSpecialOperationBlock(){
        this.blockType = SPECIAL_OPERATION;
        this.setDescription("Special Operation");
    }

    public void delete(){
        SugiliteBlock previousBlock = getPreviousBlock();
        if(previousBlock instanceof SugiliteStartingBlock)
            ((SugiliteStartingBlock) previousBlock).setNextBlock(null);
        if(previousBlock instanceof SugiliteOperationBlock)
            ((SugiliteOperationBlock) previousBlock).setNextBlock(null);
        if(previousBlock instanceof SugiliteSpecialOperationBlock)
            ((SugiliteSpecialOperationBlock) previousBlock).setNextBlock(null);
    }

    public abstract void run(Context context, SugiliteData sugiliteData, SugiliteScriptDao sugiliteScriptDao, SharedPreferences sharedPreferences) throws Exception;
}
