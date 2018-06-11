package edu.cmu.hcii.sugilite.model.block;

import java.io.Serializable;

import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.model.variable.VariableHelper;

import org.apache.commons.lang3.StringUtils;///
import java.util.*;///

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
        String be = booleanExpression.substring(1,booleanExpression.length()-1).trim();
        System.out.println(be);
        String[] split = be.split(" ");
        String operator = split[0];

        if(operator.equals("conj") || operator.equals("disj")) {
            System.out.println("IF");
            List<String> subs = new ArrayList<String>();
            List<Boolean> checks = new ArrayList<Boolean>();
            String sub0 = be;

            while (sub0.contains("(")) {
                System.out.println("WHILE");
                int ind1 = sub0.indexOf("(");
                String sub1 = sub0.substring(ind1);
                System.out.println(sub1);
                int count2 = 0;
                for (char c : sub1.toCharArray()) {
                    System.out.println("FOR0");
                    if(c == ')') {
                        break;
                    }
                    if (c == '(') {
                        count2 = count2 + 1;
                    }
                }
                int count3 = 0;
                int count4 = ind1;
                int ind2 = 0;
                for (char k : sub1.toCharArray()) {
                    System.out.println("FOR1");
                    if (k == ')') {
                        count3 = count3 + 1;
                    }
                    if (count3 == count2) {
                        ind2 = count4-1;
                        break;
                    }
                    count4 = count4 + 1;
                }
                String sub2 = sub1;
                sub1 = sub0.substring(ind1+1, ind2);
                subs.add("("+sub1+")");
                sub0 = sub0.substring(ind2);
            }

            for(String sub : subs) {
                System.out.println("FOR2");
                SugiliteBooleanExpression sbe = new SugiliteBooleanExpression(sub);
                Boolean check = sbe.evaluate(sugiliteData);
                checks.add(check);
            }

            if(operator.equals("conj")) {
                for(Boolean ch : checks) {
                    if(ch == false) {
                        return false;
                    }
                }
                return true;
            }
            else {
                for(Boolean ch : checks) {
                    if(ch == true) {
                        return true;
                    }
                }
                return false;
            }
        }
        else {
            System.out.println("ELSE");
            String exp1 = split[1];
            String exp2 = split[2];

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

            if (num1 && num2) {
                double e1 = Double.parseDouble(expression1);
                double e2 = Double.parseDouble(expression2);
                switch (operator) {
                    case ">":
                        return e1 > e2;
                    case "<":
                        return e1 < e2;
                    case ">=":
                        return e1 >= e2;
                    case "<=":
                        return e1 <= e2;
                    case "==":
                        return e1 == e2;
                    case "!=":
                        return e1 != e2;
                }
            }
            else {
                switch(operator) {
                    case "stringContains":
                        return expression1.contains(expression2);
                    case "stringEquals":
                        return expression1.equals(expression2);
                    case "stringEqualsIgnoreCase":
                        return expression1.equalsIgnoreCase(expression2);
                }
            }
            return null;
        }
    }

    @Override
    public String toString() {
        //TODO: implement
        return booleanExpression;
    }
}
