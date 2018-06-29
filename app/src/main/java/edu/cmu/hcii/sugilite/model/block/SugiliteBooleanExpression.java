package edu.cmu.hcii.sugilite.model.block;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import edu.cmu.hcii.sugilite.Const;
import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.model.variable.VariableHelper;
import edu.cmu.hcii.sugilite.ontology.helper.annotator.SugiliteTextAnnotator;
import edu.cmu.hcii.sugilite.recording.ReadableDescriptionGenerator;

/**
 * @author toby
 * @date 6/4/18
 * @time 8:57 AM
 */
public class SugiliteBooleanExpression implements Serializable {
    private static final long serialVersionUID = 3904255251843766926L;
    //should support comparison between variables & comparision between a variable and a constant -- probably won't need to support nested/composite expressions for now
    //should support common operators (e.g., >, <, <=, >=, ==, !=, stringContains)

    private String booleanExpression;
    private SugiliteData sugiliteData;

    public SugiliteBooleanExpression(String booleanExpression) {
        this.booleanExpression = booleanExpression;
        this.sugiliteData = null;
    }

    public void setSugiliteData(SugiliteData s) {
        sugiliteData = s;
    }

    public SugiliteData getSugiliteData() {
        return sugiliteData;
    }

    public Boolean evaluate(SugiliteData sugiliteData) {
        //TODO: implement -- returns the eval result of this expression at runtime
        String be = booleanExpression.substring(1,booleanExpression.length()-1).trim();
        String[] split = be.split(" ");
        String operator;
        Boolean annotate = false;
        Boolean not = false;
        if(split[0].equals("ANNOTATE")) {
            be = be.substring(9);
            annotate = true;
            split = be.split(" ");
        }
        if(split[0].equals("NOT")) {
            be = be.substring(4);
            not = true;
            split = be.split(" ");
        }
        operator = split[0];

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
                        if(not) {
                            return true;
                        }
                        return false;
                    }
                }
                if(not) {
                    return false;
                }
                return true;
            } else {
                for (Boolean ch : checks) {
                    if (ch) {
                        if(not) {
                            return false;
                        }
                        return true;
                    }
                }
                if(not) {
                    return true;
                }
                return false;
            }
        }
        else {
            String exp1, exp2;
            expression1 = split[1];
            expression2 = split[2];
            exp1 = split[1];
            exp2 = split[2];

            VariableHelper variableHelper = new VariableHelper(sugiliteData.stringVariableMap);
            expression1 = variableHelper.parse(expression1);
            expression2 = variableHelper.parse(expression2);

            //need to check if want to annotate b/c if don't, might still be annotatable and mess things up. (ex.: (stringContains 100ml 0ml)
            // if annotated would produce expressions 100 and 0, while the user wants to compare expressions 100ml and 0ml)
            if(annotate) {
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

                switch(operator) {
                    case ">=": {
                        if (not) {
                            return e1 < e2;
                        }
                        return e1 >= e2;
                    }
                    case "<=": {
                        if (not) {
                            return e1 > e2;
                        }
                        return e1 <= e2;
                    }
                    case ">": {
                        if (not) {
                            return e1 <= e2;
                        }
                        return e1 > e2;
                    }
                    case "<": {
                        if (not) {
                            return e1 >= e2;
                        }
                        return e1 < e2;
                    }
                    case "==": {
                        if (not) {
                            return e1 != e2;
                        }
                        return e1 == e2;
                    }
                    case "!=": {
                        if (not) {
                            return e1 == e2;
                        }
                        return e1 != e2;
                    }
                }
            }
            switch(operator) {
                case "stringContainsIgnoreCase": {
                    expression1 = expression1.toLowerCase();
                    expression2 = expression2.toLowerCase();
                    if (not) {
                        return !(expression1.contains(expression2));
                    }
                    return expression1.contains(expression2);
                }
                case "stringContains": {
                    if (not) {
                        return !(expression1.contains(expression2));
                    }
                    return expression1.contains(expression2);
                }
                case "stringEqualsIgnoreCase": {
                    if (not) {
                        return !(expression1.equalsIgnoreCase(expression2));
                    }
                    return expression1.equalsIgnoreCase(expression2);
                }
                case "stringEquals": {
                    if (not) {
                        return !(expression1.equals(expression2));
                    }
                    return expression1.equals(expression2);
                }
                default: {
                    System.out.println("There was a problem with the following condition: (" + operator + " " + exp1 + " " + exp2 + "). Please make sure the condition makes sense.");
                    throw new IllegalArgumentException();
                }
            }
        }
    }

    public String breakdown() {
        String hex = Const.SCRIPT_CONDITIONAL_COLOR_2;
        //String hex2 = Const.SCRIPT_CONDITIONAL_COLOR_3;//"#FF3396", "#950884", "#FFE633", "#e24141"};
        String be = booleanExpression.substring(1,booleanExpression.length()-1).trim();
        String[] split = be.split(" ");
        String operator;
        Boolean not = false;
        if(split[0].equals("ANNOTATE")) {
            be = be.substring(9);
            split = be.split(" ");
        }
        if(split[0].equals("NOT")) {
            be = be.substring(4);
            not = true;
            split = be.split(" ");
        }
        operator = split[0];

        String expression1 = "";
        String expression2 = "";

        if (operator.equals("&&") || operator.equals("||")) {
            List<String> subs = new ArrayList<String>();
            List<String> pieces = new ArrayList<>();
            String combined = "";
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
                String piece = sbe.breakdown();
                pieces.add(piece);
            }

            if(operator.equals("&&")) {
                int count = 0;
                while(count < pieces.size()) {
                    if(count == pieces.size()-1) {
                        combined += ReadableDescriptionGenerator.setColor(" and ", hex) + ReadableDescriptionGenerator.setColor(pieces.get(count) + " )", hex);
                    }
                    else if(count == 0) {
                        combined += ReadableDescriptionGenerator.setColor("( " + pieces.get(0), hex);
                    }
                    else {
                        combined += ReadableDescriptionGenerator.setColor(" and ", hex);
                        if(pieces.get(count).substring(0,5).equals("<font")) {
                            combined += pieces.get(count);
                        }
                        else {
                            combined += ReadableDescriptionGenerator.setColor(pieces.get(count), hex);
                        }
                    }
                    count++;
                }
            }
            else {
                int count = 0;
                while(count < pieces.size()) {
                    if(count == pieces.size()-1) {
                        combined += ReadableDescriptionGenerator.setColor(" or ", hex) + ReadableDescriptionGenerator.setColor(pieces.get(count) + " )", hex);
                    }
                    else if(count == 0) {
                        combined += ReadableDescriptionGenerator.setColor("( " + pieces.get(0), hex);
                    }
                    else {
                        combined += ReadableDescriptionGenerator.setColor(" or ", hex);
                        if(pieces.get(count).substring(0,5).equals("<font")) {
                            combined += pieces.get(count);
                        }
                        else {
                            combined += ReadableDescriptionGenerator.setColor(pieces.get(count), hex);
                        }
                    }
                    count++;
                }
            }
            if(not) {
                return ReadableDescriptionGenerator.setColor("it is not true that ", hex) + combined;
            }
            return combined;
        }
        else {
            expression1 = split[1];
            expression2 = split[2];

            if(operator.contains(">=")) {
                if(not) {
                    return expression1 + " ≱ " + expression2;
                }
                return expression1 + " ≥ " + expression2;
            }
            else if(operator.contains("<=")) {
                if(not) {
                    return expression1 + " ≰ " + expression2;
                }
                return expression1 + " ≤ " + expression2;
            }
            else if(operator.contains(">")) {
                if(not) {
                    return expression1 + " ≯ " + expression2;
                }
                return expression1 + " &gt; " + expression2;
            }
            else if(operator.contains("<")) {
                if(not) {
                    return expression1 + " ≮ " + expression2;
                }
                return expression1 + " &lt; " + expression2;
            }
            else if(operator.contains("==")) {
                if(not) {
                    return expression1 + " ≠ " + expression2;
                }
                return expression1 + " = " + expression2;
            }
            else if(operator.contains("!=")) {
                if(not) {
                    return expression1 + " = " + expression2;
                }
                return expression1 + " ≠ " + expression2;
            }
        }

        if(!expression1.substring(0,1).equals("@")) {
            expression1 = "'" + expression1 + "'";
        }
        else {
            expression1 = expression1.substring(1);
            String[] s = expression1.split("(?=\\p{Upper})");
            expression1 = "";
            for (String x : s) {
                expression1 += x + " ";
            }
            expression1 = "variable for " + expression1.toLowerCase();
        }
        if(!expression2.substring(0,1).equals("@")) {
            expression2 = "'" + expression2 + "'";
        }
        else {
            expression2 = expression2.substring(1);
            String[] s2 = expression2.split("(?=\\p{Upper})");
            expression2 = "";
            for (String y : s2) {
                expression2 += y + " ";
            }
            expression2 = "variable for " + expression2.toLowerCase();
        }

        if(operator.contains("stringContainsIgnoreCase")) {
            if(not) {
                return expression1 + " does not contain " + expression2 + " [ignoring capitalization]";
            }
            return expression1 + " contains " + expression2 + " [ignoring capitalization]";
        }
        else if(operator.contains("stringContains")) {
            if(not) {
                return expression1 + " does not contain " + expression2;
            }
            return expression1 + " contains " + expression2;
        }
        else if(operator.contains("stringEqualsIgnoreCase")) {
            if(not) {
                return expression1 + " ≠ " + expression2 + " [ignoring capitalization]";
            }
            return expression1 + " = " + expression2 + " [ignoring capitalization]";
        }
        else if(operator.contains("stringEquals")) {
            if(not) {
                return expression1 + " ≠ " + expression2;
            }
            return expression1 + " = " + expression2;
        }
        else {
            throw new IllegalArgumentException();
        }
    }

    @Override
    public String toString() {
        //TODO: implement
        return booleanExpression;
    }
}
