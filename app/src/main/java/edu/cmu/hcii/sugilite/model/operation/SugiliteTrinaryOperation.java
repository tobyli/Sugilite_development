package edu.cmu.hcii.sugilite.model.operation;

/**
 * @author toby
 * @date 3/13/18
 * @time 12:49 AM
 */
public abstract class SugiliteTrinaryOperation extends SugiliteOperation {
    public SugiliteTrinaryOperation(){
        super();
    }

    public SugiliteTrinaryOperation(int operationType){
        super(operationType);
    }

    public abstract String getParameter1();
    public abstract String getParameter2();
    public abstract void setParameter1(String value);
    public abstract void setParameter2(String value);


    public static boolean isTrinaryOperation(String operation){
        if(operation.contentEquals("LOAD_AS_VARIABLE")){
            return true;
        } else{
            return false;
        }
    }
}
