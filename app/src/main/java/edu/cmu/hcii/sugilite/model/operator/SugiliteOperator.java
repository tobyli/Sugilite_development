package edu.cmu.hcii.sugilite.model.operator;

/**
 * @author toby
 * @date 3/13/18
 * @time 1:47 AM
 */
public class SugiliteOperator {
    private String operatorType;
    public static final String EQUAL = "EQUAL", NOT_EQUAL = "NOT_EQUAL", GREATER_THAN = "GREATER_THAN", SMALLER_THAN = "SMALLER_TAN", GREATER_THAN_OR_EQUAL_TO = "GREATER_THAN_OR_EQUAL_TO", SMALLER_THAN_OR_EQUAL_TO = "SMALLER_THAN_OR_EQUAL_TO", TEXT_CONTAINS = "TEXT_CONTAINS";
    public SugiliteOperator(String operatorType){
        this.operatorType = operatorType;
    }

    public void setOperatorType(String operatorType) {
        this.operatorType = operatorType;
    }

    public String getOperatorType() {
        return operatorType;
    }
}
