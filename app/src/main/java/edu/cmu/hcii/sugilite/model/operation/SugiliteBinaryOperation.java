package edu.cmu.hcii.sugilite.model.operation;

/**
 * @author toby
 * @date 3/13/18
 * @time 12:45 AM
 */
public abstract class SugiliteBinaryOperation extends SugiliteOperation {
    public SugiliteBinaryOperation(){
        super();
    }

    public SugiliteBinaryOperation(int operationType){
        super(operationType);
    }

    public abstract String getParameter1();
    public abstract void setParameter1(String value);

    public static boolean isBinaryOperation(String operation){
        if(operation.contentEquals("READ_OUT") ||
                operation.contentEquals("SET_TEXT") ||
                operation.contentEquals("READOUT_CONST")){
            return true;
        } else{
            return false;
        }
    }
}
