package edu.cmu.hcii.sugilite.model.block;

import java.io.Serializable;

import edu.cmu.hcii.sugilite.model.operation.SugiliteOperation;

/**
 * @author toby
 * @date 6/10/16
 * @time 2:10 PM
 */
public class SugiliteOperationBlock extends SugiliteBlock implements Serializable{
    private SugiliteBlock nextBlock;
    private UIElementMatchingFilter elementMatchingFilter;
    private SugiliteOperation operation;

    public SugiliteOperationBlock(){
        super();
        this.blockType = SugiliteBlock.REGULAR_OPERATION;
        this.setDescription("");
    }
    public void setNextBlock(SugiliteBlock block){
        this.nextBlock = block;
    }
    public void setElementMatchingFilter(UIElementMatchingFilter filter){
        this.elementMatchingFilter = filter;
    }
    public void setOperation(SugiliteOperation operation){
        this.operation = operation;
    }

    public SugiliteBlock getNextBlock(){
        return nextBlock;
    }
    public UIElementMatchingFilter getElementMatchingFilter(){
        return elementMatchingFilter;
    }
    public SugiliteOperation getOperation(){
        return operation;
    }

    @Override
    public boolean run() throws Exception{
        if(operation == null){
            throw new Exception("null operation!");
        }
        if(elementMatchingFilter == null){
            throw new Exception("null element matching filter!");
        }
        if(nextBlock == null){
            throw new Exception("null next block!");
        }
        //
        //perform the operation
        //
        return nextBlock.run();
    }
}
