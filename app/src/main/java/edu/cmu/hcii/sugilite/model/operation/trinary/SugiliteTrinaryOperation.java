package edu.cmu.hcii.sugilite.model.operation.trinary;

import edu.cmu.hcii.sugilite.model.operation.SugiliteOperation;

/**
 * @author toby
 * @date 3/13/18
 * @time 12:49 AM
 */
public abstract class SugiliteTrinaryOperation<T, S, U> extends SugiliteOperation {
    public SugiliteTrinaryOperation(){
        super();
    }

    public SugiliteTrinaryOperation(int operationType){
        super(operationType);
    }

    public abstract T getParameter0();
    public abstract S getParameter1();
    public abstract U getParameter2();

    public abstract void setParameter0(T value);
    public abstract void setParameter1(S value);
    public abstract void setParameter2(U value);


    public static boolean isTrinaryOperation(String operation){
        if(operation.contentEquals("load_as_variable")){
            return true;
        } else{
            return false;
        }
    }
}
