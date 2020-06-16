package edu.cmu.hcii.sugilite.model.block.booleanexp;

import org.apache.commons.lang3.math.NumberUtils;

import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.model.operation.binary.SugiliteGetBoolExpOperation;
import edu.cmu.hcii.sugilite.model.operation.binary.SugiliteGetOperation;
import edu.cmu.hcii.sugilite.model.operation.binary.SugiliteGetProcedureOperation;
import edu.cmu.hcii.sugilite.model.operation.binary.SugiliteGetValueOperation;
import edu.cmu.hcii.sugilite.model.operation.unary.SugiliteResolveBoolExpOperation;
import edu.cmu.hcii.sugilite.model.operation.unary.SugiliteResolveValueQueryOperation;
import edu.cmu.hcii.sugilite.model.value.SugiliteSimpleConstant;
import edu.cmu.hcii.sugilite.model.value.SugiliteValue;
import edu.cmu.hcii.sugilite.ontology.helper.annotator.SugiliteTextParentAnnotator;
import edu.cmu.hcii.sugilite.pumice.kb.default_query.BuiltInValueQuery;
import edu.cmu.hcii.sugilite.source_parsing.SugiliteScriptExpression;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * @author toby
 * @date 11/14/18
 * @time 1:02 AM
 */
public class SugiliteBooleanExpressionNew implements SugiliteValue<Boolean>, Serializable {
    private String booleanExpression;


    private transient SugiliteData sugiliteData;
    private BoolOperator boolOperator;
    private SugiliteValue arg0;
    private SugiliteValue arg1;

    //IF boolOperation is set, it will be used for evaluate() instead of the boolOperator, arg0 and arg1. It should be either a resolve_boolExp() operation or a boolExpName typed get() operation
    private SugiliteValue<Boolean> boolOperation;

    public SugiliteBooleanExpressionNew(SugiliteScriptExpression sugiliteScriptExpression) {
        this.sugiliteData = null;
        List<List<SugiliteScriptExpression>> argList = sugiliteScriptExpression.getArguments();
        if (sugiliteScriptExpression.getOperationName().equalsIgnoreCase("resolve_boolExp") && argList.get(0) != null) {
            //the expression is a resolve_boolExp query
            this.boolOperation = new SugiliteResolveBoolExpOperation();
            ((SugiliteResolveBoolExpOperation) boolOperation).setParameter0(parseSugiliteValueFromScriptExpression(argList.get(0).get(0)).evaluate(null).toString());
        } else if (sugiliteScriptExpression.getOperationName().equalsIgnoreCase("get") && argList.get(0) != null && argList.get(1) != null &&
                argList.get(1).get(0).isConstant() && argList.get(1).get(0).getConstantValue().toString().contains("boolFunctionName")) {
            //the expression is a get boolFunctionName query
            this.boolOperation = new SugiliteGetBoolExpOperation();
            ((SugiliteGetOperation) boolOperation).setName(parseSugiliteValueFromScriptExpression(argList.get(0).get(0)).evaluate(null).toString());
            ((SugiliteGetOperation) boolOperation).setType(parseSugiliteValueFromScriptExpression(argList.get(1).get(0)).evaluate(null).toString());
        } else {
            //regular boolean expression
            this.boolOperator = getBoolOperatorFromString(sugiliteScriptExpression.getOperationName());
            if (sugiliteScriptExpression.getArguments() != null) {
                if (sugiliteScriptExpression.getArguments().get(0) != null) {
                    this.arg0 = parseSugiliteValueFromScriptExpression(argList.get(0).get(0));
                }
                if (sugiliteScriptExpression.getArguments().get(1) != null) {
                    this.arg1 = parseSugiliteValueFromScriptExpression(argList.get(1).get(0));
                }
            }
        }
        this.booleanExpression = this.toString();
    }

    private SugiliteBooleanExpressionNew(BoolOperator boolOperator, SugiliteValue arg0, SugiliteValue arg1){
        this.boolOperator = boolOperator;
        this.arg0 = arg0;
        this.arg1 = arg1;
    }

    public static Boolean evaluate(SugiliteData sugiliteData, SugiliteValue arg0, SugiliteValue arg1, BoolOperator boolOperator) {
        Object arg0Value = null, arg1Value = null;
        if (arg0 != null) {
            arg0Value = arg0.evaluate(sugiliteData);
        }
        if (arg1 != null) {
            arg1Value = arg1.evaluate(sugiliteData);
        }
        if (boolOperator.equals(BoolOperator.NOT) && arg0 != null && arg0Value != null && arg0Value instanceof Boolean) {
            return !((Boolean) arg0Value);
        }
        if (arg0Value != null && arg1Value != null) {
            //normalize the results
            arg0Value = normalizeValue(arg0Value);
            arg1Value = normalizeValue(arg1Value);

            if (arg0Value instanceof Comparable && arg1Value instanceof Comparable) {
                //note: Boolean is also Comparable

                //normalize arg0Value and arg1Value using SugiliteTextAnnotator
                SugiliteTextParentAnnotator.AnnotatingResult annotatingResult0 = null;
                if (arg0 instanceof SugiliteSimpleConstant) {
                    annotatingResult0 = ((SugiliteSimpleConstant) arg0).toAnnotatingResult();
                } else if (arg0Value instanceof String) {
                    annotatingResult0 = SugiliteTextParentAnnotator.AnnotatingResult.fromString((String) arg0Value);
                }

                SugiliteTextParentAnnotator.AnnotatingResult annotatingResult1 = null;
                if (arg1 instanceof SugiliteSimpleConstant) {
                    annotatingResult1 = ((SugiliteSimpleConstant) arg1).toAnnotatingResult();
                } else if (arg1Value instanceof String) {
                    annotatingResult1 = SugiliteTextParentAnnotator.AnnotatingResult.fromString((String) arg1Value);
                }
                //compare using annotatingResults if both are available
                if (annotatingResult0 != null && annotatingResult1 != null) {
                    arg0Value = annotatingResult0;
                    arg1Value = annotatingResult1;
                }

                //turn strings into numbers if needed
                if (arg0Value instanceof String && NumberUtils.isParsable((String)arg0Value)) {
                    arg0Value = NumberUtils.createNumber((String)arg0Value);
                }

                if (arg1Value instanceof String && NumberUtils.isParsable((String)arg1Value)) {
                    arg1Value = NumberUtils.createNumber((String)arg1Value);
                }

                //TODO: need to implement better comparison method
                switch (boolOperator) {
                    case GREATER_THAN:
                        return ((Comparable) arg0Value).compareTo(arg1Value) > 0;
                    case LESS_THAN:
                        return ((Comparable) arg0Value).compareTo(arg1Value) < 0;
                    case GREATER_THAN_OR_EQUAL_TO:
                        return ((Comparable) arg0Value).compareTo(arg1Value) >= 0;
                    case LESS_THAN_OR_EQUAL_TO:
                        return ((Comparable) arg0Value).compareTo(arg1Value) <= 0;
                    case AND:
                        return ((Comparable) arg0Value).compareTo(true) == 0 && ((Comparable) arg1Value).compareTo(true) == 0;
                    case OR:
                        return ((Comparable) arg0Value).compareTo(true) == 0 || ((Comparable) arg1Value).compareTo(true) == 0;
                    case TEXT_CONTAINS:
                        return arg0Value.toString().contains(arg1Value.toString());
                    case EQUAL:
                        return arg0Value.equals(arg1Value);
                }
            }
        }
        throw new RuntimeException("failed to evaluate!");
    }

    private static Object normalizeValue (Object argValue) {
        if (argValue instanceof BuiltInValueQuery.WeatherResult) {
            return ((BuiltInValueQuery.WeatherResult) argValue).temperature;
        }

        if (argValue instanceof Date) {
            return ((Date) argValue).getTime();
        }

        return argValue;
    }


    public BoolOperator getBoolOperator() {
        return boolOperator;
    }

    public String getBooleanExpression() {
        return booleanExpression;
    }

    public SugiliteValue<Boolean> getBoolOperation() {
        return boolOperation;
    }

    public void setBoolOperation(SugiliteValue<Boolean> boolOperation) {
        this.boolOperation = boolOperation;
    }

    public SugiliteValue getArg0() {
        return arg0;
    }

    public void setArg0(SugiliteValue arg0) {
        this.arg0 = arg0;
    }

    public SugiliteValue getArg1() {
        return arg1;
    }

    public void setArg1(SugiliteValue arg1) {
        this.arg1 = arg1;
    }

    public SugiliteData getSugiliteData() {
        return sugiliteData;
    }

    public void setSugiliteData(SugiliteData s) {
        sugiliteData = s;
    }

    public Boolean evaluate(SugiliteData sugiliteData) {
        if (boolOperation != null) {
            //either a resolve_boolExp() operation or a SugiliteGetBoolExpOperation
            return boolOperation.evaluate(sugiliteData);
        } else {
            return evaluate(sugiliteData, arg0, arg1, boolOperator);
        }
    }

    private SugiliteValue parseSugiliteValueFromScriptExpression(SugiliteScriptExpression scriptExpression) {
        if (scriptExpression.isConstant()) {
            if (scriptExpression.getConstantValue() instanceof SugiliteSimpleConstant) {
                //is a constant
                return (SugiliteSimpleConstant) scriptExpression.getConstantValue();
            } else {
                throw new RuntimeException("unknown type of constant!");
            }
        } else if (scriptExpression.getOperationName() != null) {
            if (getBoolOperatorFromString(scriptExpression.getOperationName()) != null) {
                //is a boolean expression
                return new SugiliteBooleanExpressionNew(scriptExpression);

            } else if (scriptExpression.getArguments() != null) {
                List<List<SugiliteScriptExpression>> argList = scriptExpression.getArguments();
                if (scriptExpression.getOperationName().equals("resolve_boolExp") && scriptExpression.getArguments().size() == 1) {
                    //is a resolve_boolExp expression
                    SugiliteResolveBoolExpOperation boolExpOperation = new SugiliteResolveBoolExpOperation();
                    String parameter0 = argList.get(0).get(0).getConstantValue().toString();
                    if (argList.get(0).get(0).getConstantValue() instanceof SugiliteSimpleConstant) {
                        parameter0 = ((SugiliteSimpleConstant) argList.get(0).get(0).getConstantValue()).evaluate(null).toString();
                    }
                    boolExpOperation.setParameter0(parameter0);
                    return boolExpOperation;

                } else if (scriptExpression.getOperationName().equals("resolve_valueQuery") && scriptExpression.getArguments().size() == 1) {
                    //is a resolve_valueQuery expression
                    SugiliteResolveValueQueryOperation valueQueryOperation = new SugiliteResolveValueQueryOperation();
                    String parameter0 = argList.get(0).get(0).getConstantValue().toString();
                    if (argList.get(0).get(0).getConstantValue() instanceof SugiliteSimpleConstant) {
                        parameter0 = ((SugiliteSimpleConstant) argList.get(0).get(0).getConstantValue()).evaluate(null).toString();
                    }
                    valueQueryOperation.setParameter0(parameter0);
                    return valueQueryOperation;

                } else if (scriptExpression.getOperationName().equals("get") && scriptExpression.getArguments().size() == 2) {
                    //is a get expression
                    String parameter1 = argList.get(1).get(0).getConstantValue().toString();
                    if (argList.get(1).get(0).getConstantValue() instanceof SugiliteSimpleConstant) {
                        parameter1 = ((SugiliteSimpleConstant) argList.get(1).get(0).getConstantValue()).evaluate(null).toString();
                    }
                    SugiliteGetOperation getOperation = null;
                    if (parameter1.equals(SugiliteGetOperation.VALUE_QUERY_NAME)) {
                        getOperation = new SugiliteGetValueOperation();
                    } else if (parameter1.equals(SugiliteGetOperation.BOOL_FUNCTION_NAME)) {
                        getOperation = new SugiliteGetBoolExpOperation();
                    } else if (parameter1.equals(SugiliteGetOperation.PROCEDURE_NAME)) {
                        getOperation = new SugiliteGetProcedureOperation();
                    }
                    String parameter0 = argList.get(0).get(0).getConstantValue().toString();
                    if (argList.get(0).get(0).getConstantValue() instanceof SugiliteSimpleConstant) {
                        parameter0 = ((SugiliteSimpleConstant) argList.get(0).get(0).getConstantValue()).evaluate(null).toString();
                    }
                    if (getOperation != null) {
                        getOperation.setParameter0(parameter0);
                        getOperation.setParameter1(parameter1);
                    }
                    return getOperation;
                }
            }

        }
        throw new RuntimeException("failed to parse sugilite value from script expression");
    }

    public BoolOperator getBoolOperatorFromString(String operatorName) {
        System.out.println("operatorName: " + operatorName);
        if (operatorName.equalsIgnoreCase("not")) {
            return BoolOperator.NOT;
        } else if (operatorName.equalsIgnoreCase("equal")) {
            return BoolOperator.EQUAL;
        } else if (operatorName.equalsIgnoreCase("greater_than") || operatorName.equalsIgnoreCase("greaterthan")) {
            return BoolOperator.GREATER_THAN;
        } else if (operatorName.equalsIgnoreCase("less_than") || operatorName.equalsIgnoreCase("lessthan")) {
            return BoolOperator.LESS_THAN;
        } else if (operatorName.equalsIgnoreCase("greater_than_or_equal_to") || operatorName.equalsIgnoreCase("greaterthanorequalto")) {
            return BoolOperator.GREATER_THAN_OR_EQUAL_TO;
        } else if (operatorName.equalsIgnoreCase("less_than_or_equal_to") || operatorName.equalsIgnoreCase("lessthanorequalto")) {
            return BoolOperator.LESS_THAN_OR_EQUAL_TO;
        } else if (operatorName.equalsIgnoreCase("text_contains") || operatorName.equalsIgnoreCase("contains")) {
            return BoolOperator.TEXT_CONTAINS;
        } else if (operatorName.equalsIgnoreCase("and")) {
            return BoolOperator.AND;
        } else if (operatorName.equalsIgnoreCase("or")) {
            return BoolOperator.OR;
        } else {
            return null;
        }
    }

    public static String getBooleanExpressionFormula(BoolOperator boolOperator, SugiliteValue arg0, SugiliteValue arg1){
        SugiliteBooleanExpressionNew temp = new SugiliteBooleanExpressionNew(boolOperator, arg0, arg1);
        return temp.toString();
    }


    @Override
    public String getReadableDescription() {
        if (boolOperation != null) {
            return boolOperation.getReadableDescription();
        } else {
            //TODO: implement
            return arg0.getReadableDescription() + " is " + boolOperator.name().replace("_", " ").toLowerCase() + " " + arg1.getReadableDescription();
        }
    }

    @Override
    public String toString() {
        if (boolOperation != null) {
            return boolOperation.toString();
        } else {
            return "(call " + boolOperator.name() + " " + arg0.toString() + " " + arg1.toString() + ")";
        }
    }

    public enum BoolOperator {NOT, EQUAL, GREATER_THAN, LESS_THAN, GREATER_THAN_OR_EQUAL_TO, LESS_THAN_OR_EQUAL_TO, TEXT_CONTAINS, AND, OR}

    @Override
    public boolean equals(Object obj) {
        if (obj != null && obj instanceof SugiliteBooleanExpressionNew) {
            return this.toString().equals(obj.toString());
        } else {
            return super.equals(obj);
        }
    }

    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }
}
