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

    private SugiliteBlock getNextBlock(){
        switch (conditionType){
            case TEXT_EQUALS:
                break;
            case TEXT_CONTAINS:
                break;
            case VALUE_GREATER_OR_EQUAL_THAN:
                break;
            case VALUE_SMALLER_THAN:
                break;
            case IF_CAN_MATCH_BLOCK_1:
                break;

        }
        return null;
    }
    //private UIElementMatchingFilter elementMatchingFilter;
    //private SugiliteOperation operation;
    //private SugiliteAvailableFeaturePack featurePack;

    @Override
    public boolean run() throws Exception{

        //
        //perform the operation
        //

        return block1.run();
    }


}
