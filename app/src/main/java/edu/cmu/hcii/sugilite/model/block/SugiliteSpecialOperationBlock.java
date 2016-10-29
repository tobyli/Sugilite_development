package edu.cmu.hcii.sugilite.model.block;

import java.io.Serializable;

/**
 * @author toby
 * @date 10/28/16
 * @time 6:55 PM
 */
public abstract class SugiliteSpecialOperationBlock extends SugiliteBlock implements Serializable {
    private SugiliteBlock nextBlock;
    abstract public void run();
    abstract public void delete();

    public SugiliteBlock getNextBlock(){
        return nextBlock;
    }

    public void setNextBlock(SugiliteBlock block){
        this.nextBlock = block;
    }
}
