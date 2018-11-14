package edu.cmu.hcii.sugilite.model.operation.binary;

import edu.cmu.hcii.sugilite.model.operation.SugiliteOperation;

/**
 * @author toby
 * @date 3/13/18
 * @time 12:45 AM
 */
public abstract class SugiliteBinaryOperation<T, S> extends SugiliteOperation {
    public SugiliteBinaryOperation(){
        super();
    }

    public SugiliteBinaryOperation(int operationType){
        super(operationType);
    }

    public abstract T getParameter0();
    public abstract void setParameter0(T value);


    public abstract S getParameter1();
    public abstract void setParameter1(S value);

    public static boolean isBinaryOperation(String operation){
        if(operation.contentEquals("read_out") ||
                operation.contentEquals("set_text") ||
                operation.contentEquals("readout_const") ||
                operation.contentEquals("get")){
            return true;
        } else{
            return false;
        }
    }
}
