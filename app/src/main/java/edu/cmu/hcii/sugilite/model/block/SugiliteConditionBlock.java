package edu.cmu.hcii.sugilite.model.block;

import java.io.Serializable;

import edu.cmu.hcii.sugilite.SugiliteData;

import edu.cmu.hcii.sugilite.model.block.operation.special_operation.SugiliteSpecialOperationBlock;
import edu.cmu.hcii.sugilite.model.operator.SugiliteOperator;
import edu.cmu.hcii.sugilite.model.variable.StringVariable;

import static edu.cmu.hcii.sugilite.source_parsing.SugiliteScriptExpression.addQuoteToTokenIfNeeded;

/**
 * Created by toby on 8/11/16.
 */

public class SugiliteConditionBlock extends SugiliteBlock implements Serializable {
    private static final long serialVersionUID = -5272239376931158724L;

    private SugiliteBlock ifBlock;
    //private SugiliteBlock nextBlock;


    //optional
    private SugiliteBlock elseBlock;


    private SugiliteBooleanExpression sugiliteBooleanExpression;

    public SugiliteConditionBlock(SugiliteBlock ifBlock, SugiliteBlock elseBlock, SugiliteBooleanExpression sugiliteBooleanExpression, SugiliteBlock previousBlock) {
        super();
        this.blockType = SugiliteBlock.CONDITION;
        this.setDescription("");
        this.setScreenshot(null);
        this.ifBlock = ifBlock;
        //this.nextBlock = nextBlock;
        this.elseBlock = elseBlock;
        this.sugiliteBooleanExpression = sugiliteBooleanExpression;
        this.previousBlock = previousBlock;

        //set the parentBlock for ifBlock and nextBlock so that the control flow can be correctly merged
        if (ifBlock != null) {
            ifBlock.setParentBlock(this);
        }
        if (elseBlock != null) {
            elseBlock.setParentBlock(this);
        }
        /*if (nextBlock != null) {
            nextBlock.setParentBlock(this);
        }*/
    }


    public SugiliteBlock getNextBlockToRun(SugiliteData sugiliteData) {///added sugiliteData parameter
        //TODO: evaluate sugiliteBooleanExpression at runtime, and then return either ifBlock, nextBlock or elseBlock
        if (sugiliteBooleanExpression.evaluate(sugiliteData)) {///added sugiliteData parameter
            return ifBlock;
        } else {
            if (elseBlock != null) {
                return elseBlock;
            } else {
                return nextBlock;
            }
        }
    }

    @Override
    public String toString() {
        //TODO: implement

        if(elseBlock != null) {
            return "(IF " + sugiliteBooleanExpression.toString() + " " + ifBlock.toString() + " " + elseBlock.toString() + ")";
        }
        else {
            return "(IF " + sugiliteBooleanExpression.toString() + " " + ifBlock.toString() + ")";
        }

    }

    public SugiliteBooleanExpression getSugiliteBooleanExpression() {
        return sugiliteBooleanExpression;
    }

    public SugiliteBlock getIfBlock() {
        return ifBlock;
    }

    public SugiliteBlock getElseBlock() {
        return elseBlock;
    }

    public void setElseBlock(SugiliteBlock e) {elseBlock = e;}

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

    /**
     * (IF (expression) (block(s) 1) (<optional> block(s) 2))
     *
     * Other things to implement:
     * 1. in edu.cmu.hcii.sugilite.automation.Automater: need to correctly execute scripts with SugiliteConditionalBlock
     * 2. in edu.cmu.hcii.sugilite.source_parsing.SugiliteScriptParser: need to be able to parse source codes with conditionals
     */

}
