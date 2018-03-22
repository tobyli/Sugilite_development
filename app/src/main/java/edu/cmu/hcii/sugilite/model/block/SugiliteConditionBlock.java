package edu.cmu.hcii.sugilite.model.block;

import java.io.Serializable;

import edu.cmu.hcii.sugilite.model.operator.SugiliteOperator;

import static edu.cmu.hcii.sugilite.source_parsing.SugiliteScriptExpression.addQuoteToTokenIfNeeded;

/**
 * Created by toby on 8/11/16.
 */

public class SugiliteConditionBlock extends SugiliteBlock implements Serializable {
    private SugiliteBlock block1, block2;
    private SugiliteOperator operator;
    private String parameter1, parameter2;

    public SugiliteConditionBlock (SugiliteBlock block1, SugiliteBlock block2, String parameter1, SugiliteOperator operator, String parameter2, SugiliteBlock previousBlock){
        super();
        this.blockType = SugiliteBlock.CONDITION;
        this.setDescription("Conditional Block");
        this.setScreenshot(null);


        this.block1 = block1;
        this.block2 = block2;
        this.parameter1 = parameter1;
        this.operator = operator;
        this.parameter2 = parameter2;
    }

    public void setBlock1(SugiliteBlock block){
        this.block1 = block;
    }
    public void setBlock2(SugiliteBlock block){
        this.block2 = block;
    }
    public void setParameter1(String parameter){
        this.parameter1 = parameter;
    }
    public void setParameter2(String parameter){
        this.parameter2 = parameter;
    }

    public void setOperator(SugiliteOperator operator) {
        this.operator = operator;
    }

    public SugiliteBlock getBlock1(){
        return block1;
    }
    public SugiliteBlock getBlock2(){
        return block2;
    }
    public String getParameter1(){
        return parameter1;
    }
    public String getParameter2(){
        return parameter2;
    }

    public SugiliteOperator getOperator() {
        return operator;
    }

    public SugiliteBlock getNextBlock(){
        switch (operator.getOperatorType()){
            case SugiliteOperator.EQUAL:
                if(parameter1.contentEquals(parameter2)) return block1;
                else return block2;
            case SugiliteOperator.NOT_EQUAL:
                if(!parameter1.contentEquals(parameter2)) return block1;
                else return block2;
            case SugiliteOperator.GREATER_THAN:
                if(Double.valueOf(parameter1) > Double.valueOf(parameter2)) return block1;
                else return block2;
            case SugiliteOperator.SMALLER_THAN:
                if(Double.valueOf(parameter1) < Double.valueOf(parameter2)) return block1;
                else return block2;
            case SugiliteOperator.GREATER_THAN_OR_EQUAL_TO:
                if(Double.valueOf(parameter1) >= Double.valueOf(parameter2)) return block1;
                else return block2;
            case SugiliteOperator.SMALLER_THAN_OR_EQUAL_TO:
                if(Double.valueOf(parameter1) <= Double.valueOf(parameter2)) return block1;
                else return block2;
            case SugiliteOperator.TEXT_CONTAINS:
                if(parameter1.contains(parameter2)) return block1;
                else return block2;
        }
        return null;
    }

    @Override
    public String toString() {
        return "(IF" + " " + "(" + operator.getOperatorType() + " " + addQuoteToTokenIfNeeded(parameter1) + " " + addQuoteToTokenIfNeeded(parameter2) + ") " + block1.toString() + " " + block2.toString() + ")";
    }
}
