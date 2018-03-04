package edu.cmu.hcii.sugilite.model.block;

import java.io.Serializable;

import edu.cmu.hcii.sugilite.model.operation.SugiliteOperation;

/**
 * Created by toby on 8/11/16.
 */
public class SugiliteConditionBlock extends SugiliteBlock implements Serializable {
    private SugiliteBlock block1, block2;
    static final int TEXT_EQUALS = 1, TEXT_CONTAINS = 2, VALUE_GREATER_OR_EQUAL_THAN = 3, VALUE_SMALLER_THAN = 4, IF_CAN_MATCH_BLOCK_1 = 5;
    private int conditionType;
    private String parameter1, parameter2;

    public SugiliteConditionBlock (SugiliteBlock block1, SugiliteBlock block2, String parameter1, int conditionType, String parameter2, SugiliteBlock previousBlock){
        super();
        this.blockType = SugiliteBlock.CONDITION;
        this.setDescription("Condition");
        this.setScreenshot(null);


        this.block1 = block1;
        this.block2 = block2;
        this.parameter1 = parameter1;
        this.conditionType = conditionType;
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
    public void setConditionType(int conditionType){
        this.conditionType = conditionType;
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
    public int getConditionType(){
        return conditionType;
    }

    public SugiliteBlock getNextBlock(){
        switch (conditionType){
            case TEXT_EQUALS:
                if(parameter1.contentEquals(parameter2))
                    return block1;
                else
                    return block2;
            case TEXT_CONTAINS:
                if(parameter1.contains(parameter2))
                    return block1;
                else
                    return block2;
            case VALUE_GREATER_OR_EQUAL_THAN:
                try{
                    if(Integer.valueOf(parameter1) >= Integer.valueOf(parameter2))
                        return block1;
                    else
                        return block2;
                }
                catch (Exception e){
                    e.printStackTrace();
                    return block2;
                }
            case VALUE_SMALLER_THAN:
                try{
                    if(Integer.valueOf(parameter1) < Integer.valueOf(parameter2))
                        return block1;
                    else
                        return block2;
                }
                catch (Exception e){
                    e.printStackTrace();
                    return block2;
                }
            case IF_CAN_MATCH_BLOCK_1:
                //TODO:
                break;

        }
        return null;
    }

    @Override
    public String toString() {
        return "(CONDITION_BLOCK)";
    }
}
