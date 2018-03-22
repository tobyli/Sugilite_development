package edu.cmu.hcii.sugilite.model.operation;

import edu.cmu.hcii.sugilite.model.operator.SugiliteOperator;

/**
 * @author toby
 * @date 3/13/18
 * @time 12:44 AM
 */
public class SugiliteUnaryOperation extends SugiliteOperation {
    public SugiliteUnaryOperation(){
        super();
    }
    public SugiliteUnaryOperation(int operationType){
        super(operationType);
    }
    public static boolean isUnaryOperation(String operation){
        if(operation.contentEquals("CLICK") ||
                operation.contentEquals("LONG_CLICK") ||
                operation.contentEquals("SELECT")){
            return true;
        } else{
            return false;
        }
    }
    public static int getOperationType(String operation){
        switch (operation){
            case "CLICK":
                return SugiliteOperation.CLICK;
            case "LONG_CLICK":
                return SugiliteOperation.LONG_CLICK;
            case "SELECT":
                return SugiliteOperation.SELECT;
        }
        return -1;
    }
}
