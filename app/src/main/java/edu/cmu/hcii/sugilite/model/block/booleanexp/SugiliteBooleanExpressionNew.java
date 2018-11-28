package edu.cmu.hcii.sugilite.model.block.booleanexp;

import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.model.operation.binary.SugiliteGetOperation;
import edu.cmu.hcii.sugilite.model.operation.unary.SugiliteResolveBoolExpOperation;
import edu.cmu.hcii.sugilite.model.operation.unary.SugiliteResolveValueQueryOperation;
import edu.cmu.hcii.sugilite.model.value.SugiliteSimpleConstant;
import edu.cmu.hcii.sugilite.model.value.SugiliteValue;
import edu.cmu.hcii.sugilite.source_parsing.SugiliteScriptExpression;
import java.util.List;

/**
 * @author toby
 * @date 11/14/18
 * @time 1:02 AM
 */
public class SugiliteBooleanExpressionNew implements SugiliteValue<Boolean> {
    public enum BoolOperator {NOT, EQUAL, GREATER_THAN, LESS_THAN, GREATER_THAN_OR_EQUAL_TO, LESS_THAN_OR_EQUAL_TO, TEXT_CONTAINS, AND, OR};
    private String booleanExpression;
    private SugiliteData sugiliteData;

    private BoolOperator boolOperator;
    private SugiliteValue arg0;
    private SugiliteValue arg1;



    public SugiliteBooleanExpressionNew(SugiliteScriptExpression sugiliteScriptExpression) {
        this.booleanExpression = sugiliteScriptExpression.getScriptContent();
        this.sugiliteData = null;
        this.boolOperator = getBoolOperatorFromString(sugiliteScriptExpression.getOperationName());
        if(sugiliteScriptExpression.getArguments() != null) {
            List<SugiliteScriptExpression> argList = sugiliteScriptExpression.getArguments();
            if(sugiliteScriptExpression.getArguments().get(0) != null) {
                this.arg0 = parseSugiliteValueFromScriptExpression(argList.get(0));
            }
            if(sugiliteScriptExpression.getArguments().get(0) != null) {
                this.arg1 = parseSugiliteValueFromScriptExpression(argList.get(1));
            }
        }
    }


    public void setSugiliteData(SugiliteData s) {
        sugiliteData = s;
    }

    public SugiliteData getSugiliteData() {
        return sugiliteData;
    }

    public Boolean evaluate(){
        if(boolOperator.equals(BoolOperator.NOT) && arg0 != null && arg0.evaluate() != null && arg0.evaluate() instanceof Boolean){
            return !((Boolean) arg0.evaluate());
        }
        if (arg0 != null && arg1 != null && arg0.evaluate() != null && arg1.evaluate() != null) {
            switch (boolOperator){
                case TEXT_CONTAINS:
                    return arg0.evaluate().toString().contains(arg1.evaluate().toString());
                case EQUAL:
                    return arg0.evaluate().equals(arg1.evaluate());
            }
            if (arg0.evaluate() instanceof Comparable && arg1.evaluate() instanceof Comparable) {
                //note: Boolean is also Comparable
                switch (boolOperator) {
                    case GREATER_THAN:
                        return ((Comparable) arg0.evaluate()).compareTo(arg1.evaluate()) > 0;
                    case LESS_THAN:
                        return ((Comparable) arg0.evaluate()).compareTo(arg1.evaluate()) < 0;
                    case GREATER_THAN_OR_EQUAL_TO:
                        return ((Comparable) arg0.evaluate()).compareTo(arg1.evaluate()) >= 0;
                    case LESS_THAN_OR_EQUAL_TO:
                        return ((Comparable) arg0.evaluate()).compareTo(arg1.evaluate()) <= 0;
                    case AND:
                        return ((Comparable) arg0.evaluate()).compareTo(true) == 0 && ((Comparable) arg1.evaluate()).compareTo(true) == 0;
                    case OR:
                        return ((Comparable) arg0.evaluate()).compareTo(true) == 0 || ((Comparable) arg1.evaluate()).compareTo(true) == 0;
                }
            }
        }
        throw new RuntimeException("failed to evaluate!");
    }

    private SugiliteValue parseSugiliteValueFromScriptExpression(SugiliteScriptExpression scriptExpression){
        if(scriptExpression.isConstant()){
            return new SugiliteSimpleConstant(scriptExpression.getConstantValue());
        } else if (scriptExpression.getOperationName() != null){
            if (getBoolOperatorFromString(scriptExpression.getOperationName()) != null){
                //is a boolean express
                return new SugiliteBooleanExpressionNew(scriptExpression);
            } else if (scriptExpression.getArguments() != null) {
                List<SugiliteScriptExpression> argList = scriptExpression.getArguments();
                if (scriptExpression.getOperationName().equals("resolve_boolExp") && scriptExpression.getArguments().size() == 1){
                    SugiliteResolveBoolExpOperation boolExpOperation = new SugiliteResolveBoolExpOperation();
                    boolExpOperation.setParameter0(argList.get(0).getConstantValue().toString());
                    return boolExpOperation;
                } else if (scriptExpression.getOperationName().equals("resolve_valueQuery") && scriptExpression.getArguments().size() == 1){
                    SugiliteResolveValueQueryOperation valueQueryOperation = new SugiliteResolveValueQueryOperation();
                    valueQueryOperation.setParameter0(argList.get(0).getConstantValue().toString());
                    return valueQueryOperation;
                } else if (scriptExpression.getOperationName().equals("get") && scriptExpression.getArguments().size() == 2){
                    SugiliteGetOperation getOperation = new SugiliteGetOperation();
                    getOperation.setParameter0(argList.get(0).getConstantValue().toString());
                    getOperation.setParameter1(argList.get(1).getConstantValue().toString());
                    return getOperation;
                }
            }

        }
        throw new RuntimeException("failed to parse sugilite value from script expression");
    }

    public BoolOperator getBoolOperatorFromString(String operatorName){
        System.out.println("operatorName: " + operatorName);
        if (operatorName.equalsIgnoreCase("not")){
            return BoolOperator.NOT;
        } else if (operatorName.equalsIgnoreCase("equal")){
            return BoolOperator.EQUAL;
        } else if (operatorName.equalsIgnoreCase("greater_than") || operatorName.equalsIgnoreCase("greaterthan")){
            return BoolOperator.GREATER_THAN;
        } else if (operatorName.equalsIgnoreCase("less_than") || operatorName.equalsIgnoreCase("lessthan")){
            return BoolOperator.LESS_THAN;
        } else if (operatorName.equalsIgnoreCase("greater_than_or_equal_to") || operatorName.equalsIgnoreCase("greaterthanorequalto")){
            return BoolOperator.GREATER_THAN_OR_EQUAL_TO;
        } else if (operatorName.equalsIgnoreCase("less_than_or_equal_to") || operatorName.equalsIgnoreCase("lessthanorequalto")){
            return BoolOperator.LESS_THAN_OR_EQUAL_TO;
        } else if (operatorName.equalsIgnoreCase("text_contains") || operatorName.equalsIgnoreCase("contains")) {
            return BoolOperator.TEXT_CONTAINS;
        } else if (operatorName.equalsIgnoreCase("and")){
            return BoolOperator.AND;
        } else if (operatorName.equalsIgnoreCase("or")){
            return BoolOperator.OR;
        } else {
            return null;
        }
    }
}
