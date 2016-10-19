package edu.cmu.hcii.sugilite.model.block;

import java.io.Serializable;

/**
 * @author toby
 * @date 10/10/16
 * @time 5:05 PM
 */
public class SugiliteSubscriptOperationBlock extends SugiliteBlock implements Serializable{
    private SugiliteBlock nextBlock;
    private String subscriptName;

    public SugiliteSubscriptOperationBlock(){
        super();
        this.blockType = SugiliteBlock.SUBSCRIPT;
        this.setDescription("Run subscript");
    }

    public SugiliteSubscriptOperationBlock(String subscriptName){
        super();
        this.blockType = SugiliteBlock.SUBSCRIPT;
        this.setDescription("Run subscript");
        this.subscriptName = subscriptName;
    }

    public void setSubscriptName (String subscriptName){
        this.subscriptName = subscriptName;
    }

    public String getSubscriptName (){
        return subscriptName;
    }

    public SugiliteBlock getNextBlock(){
        return nextBlock;
    }

    public void setNextBlock(SugiliteBlock nextBlock){
        this.nextBlock = nextBlock;
    }

    public void delete(){
        SugiliteBlock previousBlock = getPreviousBlock();
        if(previousBlock instanceof SugiliteStartingBlock)
            ((SugiliteStartingBlock) previousBlock).setNextBlock(null);
        if(previousBlock instanceof SugiliteOperationBlock)
            ((SugiliteOperationBlock) previousBlock).setNextBlock(null);
        if(previousBlock instanceof SugiliteSubscriptOperationBlock)
            ((SugiliteSubscriptOperationBlock) previousBlock).setNextBlock(null);
    }





}
