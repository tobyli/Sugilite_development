package edu.cmu.hcii.sugilite.model.block;

import java.io.Serializable;

import edu.cmu.hcii.sugilite.model.block.util.SugiliteAvailableFeaturePack;
import edu.cmu.hcii.sugilite.model.block.util.UIElementMatchingFilter;
import edu.cmu.hcii.sugilite.model.operation.SugiliteOperation;

/**
 * @author toby
 * @date 6/10/16
 * @time 2:10 PM
 */
public class SugiliteOperationBlock extends SugiliteBlock implements Serializable{
    private SugiliteOperation operation;
    private SugiliteAvailableFeaturePack featurePack;
    private SugiliteBlockMetaInfo sugiliteBlockMetaInfo;

    @Deprecated
    private UIElementMatchingFilter elementMatchingFilter;

    public boolean isSetAsABreakPoint = false;

    public SugiliteOperationBlock(){
        super();
        this.init();
        this.blockType = SugiliteBlock.REGULAR_OPERATION;
        this.setDescription("");
    }

    public void setOperation(SugiliteOperation operation){
        this.operation = operation;
    }
    public void setFeaturePack(SugiliteAvailableFeaturePack featurePack){
        this.featurePack = featurePack;
    }


    public SugiliteOperation getOperation(){
        return operation;
    }
    public SugiliteAvailableFeaturePack getFeaturePack(){
        return featurePack;
    }

    public void delete(){
        SugiliteBlock previousBlock = getPreviousBlock();
        if(previousBlock instanceof SugiliteStartingBlock)
            ((SugiliteStartingBlock) previousBlock).setNextBlock(null);
        if(previousBlock instanceof SugiliteOperationBlock)
            ((SugiliteOperationBlock) previousBlock).setNextBlock(null);
        if(previousBlock instanceof SugiliteSpecialOperationBlock)
            ((SugiliteSpecialOperationBlock) previousBlock).setNextBlock(null);
        if(previousBlock instanceof SugiliteConditionBlock)
            ((SugiliteConditionBlock) previousBlock).setNextBlock(null);
    }

    @Override
    public String toString() {
        return operation.toString();
    }

    public void setSugiliteBlockMetaInfo(SugiliteBlockMetaInfo sugiliteBlockMetaInfo) {
        this.sugiliteBlockMetaInfo = sugiliteBlockMetaInfo;
    }

    public SugiliteBlockMetaInfo getSugiliteBlockMetaInfo() {
        return sugiliteBlockMetaInfo;
    }

    @Deprecated
    public void setElementMatchingFilter(UIElementMatchingFilter filter){
        this.elementMatchingFilter = filter;
    }
    @Deprecated
    public UIElementMatchingFilter getElementMatchingFilter(){
        return elementMatchingFilter;
    }

    @Override
    public String getPumiceUserReadableDecription() {
        return operation.getPumiceUserReadableDecription();
    }
}
