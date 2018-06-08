package edu.cmu.hcii.sugilite.model.block;

import java.io.Serializable;

import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.model.variable.VariableHelper;

import org.apache.commons.lang3.StringUtils;///

/**
 * @author toby
 * @date 6/4/18
 * @time 8:57 AM
 */
public class SugiliteBooleanExpression implements Serializable {

    //should support comparison between variables & comparision between a variable and a constant -- probably won't need to support nested/composite expressions for now
    //should support common operators (e.g., >, <, <=, >=, ==, !=, stringContains)
    private String booleanExpression;

    public SugiliteBooleanExpression(String booleanExpression) {
        this.booleanExpression = booleanExpression;
    }

    public Boolean evaluate(SugiliteData sugiliteData) {
        //TODO: implement -- returns the eval result of this expression at runtime
        String exp1 = "";
        String exp2 = "";
        String operator = "";
        int ind1 = 0;

        if(booleanExpression.contains(">")) {
            ind1 = booleanExpression.indexOf(">");
            operator = ">";
        }
        else if(booleanExpression.contains("<")) {
            ind1 = booleanExpression.indexOf("<");
            operator = "<";
        }
        else if(booleanExpression.contains(">=")) {
            ind1 = booleanExpression.indexOf(">=");
            operator = ">=";
        }
        else if(booleanExpression.contains("<=")) {
            ind1 = booleanExpression.indexOf("<=");
            operator = "<=";
        }
        else if(booleanExpression.contains("==")) {
            ind1 = booleanExpression.indexOf("==");
            operator = "==";
        }
        else if(booleanExpression.contains("!=")) {
            ind1 = booleanExpression.indexOf("!=");
            operator = "!=";
        }
        exp1 = booleanExpression.substring(1,ind1).trim();
        exp2 = booleanExpression.substring(ind1+1,booleanExpression.length()-1).trim();
        //need to implement for situations with more than 2 operators like (x == 1 && y == 2)

        VariableHelper variableHelper = new VariableHelper(sugiliteData.stringVariableMap);
        String expression1 = variableHelper.parse(exp1);
        String expression2 = variableHelper.parse(exp2);

        Boolean num1 = true;
        Boolean num2 = true;
        try {
            Double num = Double.parseDouble(expression1);
        } catch (NumberFormatException e) {
            num1 = false;
        }
        try {
            Double num = Double.parseDouble(expression2);
        } catch (NumberFormatException e) {
            num2 = false;
        }
        if(num1 && num2) {
            double e1 = Double.parseDouble(expression1);
            double e2 = Double.parseDouble(expression2);
            if(operator == ">") {
                return e1 > e2;
            }
            else if(operator == "<") {
                return e1 < e2;
            }
            else if(operator == ">=") {
                return e1 >= e2;
            }
            else if(operator == "<=") {
                return e1 <= e2;
            }
            else if(operator == "==") {
                System.out.println(e1==e2);
                return e1 == e2;
            }
            else if(operator == "!=") {
                return e1 != e2;
            }
        }
        else {
            if(operator == "stringContains") {
                return expression1.contains(expression2);
            }
            else if(operator == "stringEquals") {
                return expression1.equals(expression2);
            }
            else if(operator == "stringEqualsIgnoreCase") {
                return expression1.equalsIgnoreCase(expression2);
            }
            else if(operator == "==") {
                return expression1 == expression2;
            }
            else if(operator == "!=") {
                return expression1 != expression2;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        //TODO: implement
        return booleanExpression;
    }
}
