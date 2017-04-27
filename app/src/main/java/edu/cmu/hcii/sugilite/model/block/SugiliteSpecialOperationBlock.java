package edu.cmu.hcii.sugilite.model.block;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.Serializable;

import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptDao;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptSQLDao;

/**
 * @author toby
 * @date 10/28/16
 * @time 6:55 PM
 */
public abstract class SugiliteSpecialOperationBlock extends SugiliteBlock implements Serializable {
    private SugiliteBlock nextBlock;
    abstract public void run(Context context, SugiliteData sugiliteData, SugiliteScriptDao sugiliteScriptDao, SharedPreferences sharedPreferences) throws Exception;

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

    public SugiliteBlock getNextBlock(){
        return nextBlock;
    }

    public void setNextBlock(SugiliteBlock block){
        this.nextBlock = block;
    }
}
