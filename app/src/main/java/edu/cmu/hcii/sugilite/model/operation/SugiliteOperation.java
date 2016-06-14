package edu.cmu.hcii.sugilite.model.operation;

/**
 * @author toby
 * @date 6/10/16
 * @time 2:19 PM
 */
public class SugiliteOperation {
    private int operationType;
    private String parameter;
    public static int CLICK = 1, LONG_CLICK = 2, SET_TEXT = 3, CLEAR_TEXT = 4, CHECK = 5, UNCHECK = 6;
    public SugiliteOperation(){
        operationType = 0;
    }
    public SugiliteOperation(int operationType){
        this.operationType = operationType;
    }
    public int getOperationType(){
        return operationType;
    }
    public String getParameter(){
        return parameter;
    }
    public void setOperationType(int operationType){
        this.operationType = operationType;
    }
    public void setParameter(String parameter){
        this.parameter = parameter;
    }
}

