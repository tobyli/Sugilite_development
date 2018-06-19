package edu.cmu.hcii.sugilite.model.block;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.model.variable.VariableHelper;
import edu.cmu.hcii.sugilite.ontology.helper.annotator.DateAnnotator;
import edu.cmu.hcii.sugilite.ontology.helper.annotator.DurationAnnotator;
import edu.cmu.hcii.sugilite.ontology.helper.annotator.LengthAnnotator;
import edu.cmu.hcii.sugilite.ontology.helper.annotator.MoneyAnnotator;
import edu.cmu.hcii.sugilite.ontology.helper.annotator.PercentageAnnotator;
import edu.cmu.hcii.sugilite.ontology.helper.annotator.PhoneNumberAnnotator;
import edu.cmu.hcii.sugilite.ontology.helper.annotator.SugiliteTextAnnotator;
import edu.cmu.hcii.sugilite.ontology.helper.annotator.TimeAnnotator;
import edu.cmu.hcii.sugilite.ontology.helper.annotator.VolumeAnnotator;

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
        String[] split = be.split(" ");
        String operator = split[0];
        String expression1 = "";
        String expression2 = "";

        if (operator.equals("&&") || operator.equals("||")) {
            List<String> subs = new ArrayList<String>();
            List<Boolean> checks = new ArrayList<Boolean>();
            String sub0 = be;

            while (sub0.contains("(")) {
                int ind1 = sub0.indexOf("(");
                String sub1 = sub0.substring(ind1);

                int seen4 = 0;
                int seen3 = 0;
                int ind2 = ind1;
                for (char x : sub1.toCharArray()) {
                    if (x == ')') {
                        seen3 += 1;
                    } else if (x == '(') {
                        seen4 += 1;
                    }
                    if (seen3 == seen4) {
                        break;
                    }

                    ind2 += 1;
                }

                sub1 = sub0.substring(ind1, ind2);
                subs.add(sub1);
                sub0 = sub0.substring(ind2);
            }

            for (String sub : subs) {
                SugiliteBooleanExpression sbe = new SugiliteBooleanExpression(sub);
                Boolean check = sbe.evaluate(sugiliteData);
                checks.add(check);
            }

            if (operator.equals("&&")) {
                for (Boolean ch : checks) {
                    if (!ch) {
                        return false;
                    }
                }
                return true;
            } else {
                for (Boolean ch : checks) {
                    if (ch) {
                        return true;
                    }
                }
                return false;
            }
        }
        else {
            expression1 = split[1];
            expression2 = split[2];
            String exp1 = split[1];
            String exp2 = split[2];

            VariableHelper variableHelper = new VariableHelper(sugiliteData.stringVariableMap);
            expression1 = variableHelper.parse(expression1);
            expression2 = variableHelper.parse(expression2);

            double e1;
            double e2;
            Boolean num1 = true;
            Boolean num2 = true;

            if(operator.contains("Annotate")) {
                SugiliteTextAnnotator annotator = new SugiliteTextAnnotator(true);
                List<SugiliteTextAnnotator.AnnotatingResult> result1 = annotator.annotate(expression1);
                List<SugiliteTextAnnotator.AnnotatingResult> result2 = annotator.annotate(expression2);
                if (!result1.isEmpty()) {
                    if(operator.contains("string")) {
                        expression1 = result1.get(0).getMatchedString();
                    }
                    else {
                        expression1 = Double.toString(result1.get(0).getNumericValue().doubleValue());
                    }
                    if(result1.get(0).getRelation().toString().equals("CONTAINS_PHONE_NUMBER")) {
                        expression1 = expression1.replace(" ","").replace("-","").replace(")","").replace("(","");
                    }
                    if (!result2.isEmpty()) {
                        if(operator.contains("string")) {
                            expression2 = result2.get(0).getMatchedString();
                        }
                        else {
                            expression2 = Double.toString(result2.get(0).getNumericValue().doubleValue());
                        }
                        if(result2.get(0).getRelation().toString().equals("CONTAINS_PHONE_NUMBER")) {
                            expression2 = expression2.replace(" ","").replace("-","").replace(")","").replace("(","");
                        }
                    }
                    else {
                        System.out.println("Unable to annotate the following expression: " + expression2 + ". Please make sure the expression makes sense.");
                        throw new IllegalArgumentException();
                    }
                }
                else {
                    System.out.println("Unable to annotate the following expression: " + expression1 + ". Please make sure the expression makes sense.");
                    throw new IllegalArgumentException();
                }
            }

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
                e1 = Double.parseDouble(expression1);
                e2 = Double.parseDouble(expression2);

                if(operator.contains(">=")) {
                    return e1 >= e2;
                }
                else if(operator.contains("<=")) {
                    return e1 <= e2;
                }
                else if(operator.contains(">")) {
                    return e1 > e2;
                }
                else if(operator.contains("<")) {
                    return e1 < e2;
                }
                else if(operator.contains("==")) {
                    return e1 == e2;
                }
                else if(operator.contains("!=")) {
                    return e1 != e2;
                }
            }
            if(operator.contains("stringContainsIgnoreCase")) {
                expression1 = expression1.toLowerCase();
                expression2 = expression2.toLowerCase();
                return expression1.contains(expression2);
            }
            else if(operator.contains("stringContains")) {
                return expression1.contains(expression2);
            }
            else if(operator.contains("stringEqualsIgnoreCase")) {
                return expression1.equalsIgnoreCase(expression2);
            }
            else if(operator.contains("stringEquals")) {
                return expression1.equals(expression2);
            }
            else {
                System.out.println("There was a problem with the following condition: (" + operator + " " + exp1 + " " + exp2 + "). Please make sure the condition makes sense.");
                throw new IllegalArgumentException();
            }
        }
    }

    @Override
    public String toString() {
        //TODO: implement
        return booleanExpression;
    }
}
