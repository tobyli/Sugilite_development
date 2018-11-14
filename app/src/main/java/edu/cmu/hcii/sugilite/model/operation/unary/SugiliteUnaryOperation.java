package edu.cmu.hcii.sugilite.model.operation.unary;

import edu.cmu.hcii.sugilite.model.operation.SugiliteOperation;

/**
 * @author toby
 * @date 3/13/18
 * @time 12:44 AM
 */
public abstract class SugiliteUnaryOperation<T> extends SugiliteOperation {
    public SugiliteUnaryOperation(){
        super();
    }

    public SugiliteUnaryOperation(int operationType){
        super(operationType);
    }

    public abstract T getParameter0();
    public abstract void setParameter0(T value);


    public static boolean isUnaryOperation(String operation){
        if(operation.contentEquals("click") ||
                operation.contentEquals("long_click") ||
                operation.contentEquals("select") ||
                operation.contentEquals("resolve_procedure") ||
                operation.contentEquals("resolve_valueQuery") ||
                operation.contentEquals("resolve_boolExp") ||
                operation.contentEquals("readout_const")){
            return true;
        } else{
            return false;
        }
    }

    public static int getOperationType(String operation){
        switch (operation){
            case "click":
                return SugiliteOperation.CLICK;
            case "long_click":
                return SugiliteOperation.LONG_CLICK;
            case "select":
                return SugiliteOperation.SELECT;
            case "resolve_procedure":
                return SugiliteOperation.RESOLVE_PROCEDURE;
            case "resolve_valueQuery":
                return SugiliteOperation.RESOLVE_VALUEQUERY;
            case "resolve_boolExp":
                return SugiliteOperation.RESOLVE_BOOLEXP;
            case "readout_const":
                return READOUT_CONST;
        }
        return -1;
    }
}
