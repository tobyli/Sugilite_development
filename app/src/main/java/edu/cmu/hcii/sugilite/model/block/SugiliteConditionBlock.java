package edu.cmu.hcii.sugilite.model.block;

import java.io.Serializable;

import edu.cmu.hcii.sugilite.model.operator.SugiliteOperator;

import static edu.cmu.hcii.sugilite.source_parsing.SugiliteScriptExpression.addQuoteToTokenIfNeeded;

/**
 * Created by toby on 8/11/16.
 */

public class SugiliteConditionBlock extends SugiliteBlock implements Serializable {
    private SugiliteBlock ifBlock;
    private SugiliteBlock nextBlock;

    //optional
    private SugiliteBlock elseBlock;

    private SugiliteBooleanExpression sugiliteBooleanExpression;

    public SugiliteConditionBlock(SugiliteBlock ifBlock, SugiliteBlock nextBlock, SugiliteBlock elseBlock, SugiliteBooleanExpression sugiliteBooleanExpression, SugiliteBlock previousBlock) {
        super();
        this.blockType = SugiliteBlock.CONDITION;
        this.setDescription("Conditional Block");
        this.setScreenshot(null);
        this.ifBlock = ifBlock;
        this.nextBlock = nextBlock;
        this.elseBlock = elseBlock;
        this.sugiliteBooleanExpression = sugiliteBooleanExpression;
        this.previousBlock = previousBlock;

        //set the parentBlock for ifBlock and nextBlock so that the control flow can be correctly merged
        if (ifBlock != null) {
            ifBlock.setParentBlock(this);
        }
        if (nextBlock != null) {
            nextBlock.setParentBlock(this);
        }
    }


    @Override
    public SugiliteBlock getNextBlock() {
        //TODO: evaluate sugiliteBooleanExpression at runtime, and then return either ifBlock, nextBlock or elseBlock
        if (sugiliteBooleanExpression.evaluate()) {
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

        return null;
    }

    /**
     * (IF (expression) (block(s) 1) (<optional> block(s) 2))
     *
     * Other things to implement:
     * 1. in edu.cmu.hcii.sugilite.automation.Automater: need to correctly execute scripts with SugiliteConditionalBlock
     * 2. in edu.cmu.hcii.sugilite.source_parsing.SugiliteScriptParser: need to be able to parse source codes with conditionals
     */


}
