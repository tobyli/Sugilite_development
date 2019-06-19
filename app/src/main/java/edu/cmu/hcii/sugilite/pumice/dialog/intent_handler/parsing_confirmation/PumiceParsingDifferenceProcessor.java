package edu.cmu.hcii.sugilite.pumice.dialog.intent_handler.parsing_confirmation;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.Nullable;
import android.util.Pair;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


import edu.cmu.hcii.sugilite.model.block.booleanexp.SugiliteBooleanExpressionNew;
import edu.cmu.hcii.sugilite.model.value.SugiliteValue;
import edu.cmu.hcii.sugilite.pumice.dialog.PumiceDialogManager;
import edu.cmu.hcii.sugilite.source_parsing.SugiliteScriptParser;

/**
 * @author toby
 * @date 3/4/19
 * @time 12:17 PM
 */
public class PumiceParsingDifferenceProcessor {
    private Activity context;
    private SugiliteScriptParser sugiliteScriptParser;
    private PumiceDialogManager pumiceDialogManager;

    public PumiceParsingDifferenceProcessor(Activity context, PumiceDialogManager pumiceDialogManager){
        this.context = context;
        this.sugiliteScriptParser = new SugiliteScriptParser();
        this.pumiceDialogManager = pumiceDialogManager;
}


    public String handleBoolParsing(List<String> formulas, Runnable runnableForRetry){
        //called when the user consider the top matched parsing NOT correct
        LinkedHashMap<SugiliteBooleanExpressionNew.BoolOperator, Integer> boolOperatorCountMap = new LinkedHashMap<>();
        LinkedHashMap<SugiliteValue, Integer> arg0IntegerMap = new LinkedHashMap<>();
        LinkedHashMap<SugiliteValue, Integer> arg1IntegerMap = new LinkedHashMap<>();
        LinkedHashMap<SugiliteValue, Integer> booleanOperationIntegerMap = new LinkedHashMap<>();
        int booleanOperationCount = 0;

        for (String formula : formulas) {
            SugiliteBooleanExpressionNew booleanExpression = sugiliteScriptParser.parseBooleanExpressionFromString(formula);

            SugiliteBooleanExpressionNew.BoolOperator boolOperator = booleanExpression.getBoolOperator();
            SugiliteValue arg0 = booleanExpression.getArg0();
            SugiliteValue arg1 = booleanExpression.getArg1();

            if (boolOperator != null && arg0 != null && arg1 != null) {
                if (boolOperatorCountMap.containsKey(boolOperator)) {
                    boolOperatorCountMap.put(boolOperator, boolOperatorCountMap.get(boolOperator) + 1);
                } else {
                    boolOperatorCountMap.put(boolOperator, 1);
                }

                if (arg0IntegerMap.containsKey(arg0)) {
                    arg0IntegerMap.put(arg0, arg0IntegerMap.get(arg0) + 1);
                } else {
                    arg0IntegerMap.put(arg0, 1);
                }

                if (arg1IntegerMap.containsKey(arg1)) {
                    arg1IntegerMap.put(arg1, arg1IntegerMap.get(arg1) + 1);
                } else {
                    arg1IntegerMap.put(arg1, 1);
                }
            } else {
                //handle bool expressions without a boolOperator, such as get and resolve ones
                if (booleanExpression.getBoolOperation() == null) {
                    throw new RuntimeException("bad boolean expression!");
                }
                SugiliteValue boolOperation = booleanExpression.getBoolOperation();
                booleanOperationCount ++;
                if (booleanOperationIntegerMap.containsKey(boolOperation)) {
                    booleanOperationIntegerMap.put(boolOperation, booleanOperationIntegerMap.get(boolOperation) + 1);
                } else {
                    booleanOperationIntegerMap.put(boolOperation, 1);
                }
            }
        }

        List<Pair<SugiliteBooleanExpressionNew.BoolOperator, Integer>> boolOperatorCountList = sortByValue(boolOperatorCountMap, true);
        List<Pair<SugiliteValue, Integer>> arg0CountList = sortByValue(arg0IntegerMap, true);
        List<Pair<SugiliteValue, Integer>> arg1CountList = sortByValue(arg1IntegerMap, true);
        List<Pair<SugiliteValue, Integer>> boolOperationCountList = sortByValue(booleanOperationIntegerMap, true);



        //check if should resolve to a booleanOperation (e.g. resolve, get) instead of a real boolean expression
        if (booleanOperationCount >= (formulas.size() - 1)){
            final ParsingWrapper<SugiliteValue> finalBoolOperation = new ParsingWrapper<>(null);

            //should use booleanOperation
            List<SugiliteValue> boolOperations = new ArrayList<>();
            List<Pair<SugiliteValue, String>> boolOperationBoolOperationNameList = new ArrayList<>();
            boolOperationCountList.forEach(pair -> boolOperations.add(pair.first));
            boolOperations.forEach(boolOperation -> boolOperationBoolOperationNameList.add(new Pair<>(boolOperation, boolOperation.getReadableDescription())));

            context.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    PumiceParsingDifferenceDialog<SugiliteValue> boolOperationPumiceParsingDifferenceDialog = new PumiceParsingDifferenceDialog<SugiliteValue>(context, pumiceDialogManager, boolOperationBoolOperationNameList, getPromptForBooleanOperations(boolOperations), runnableForRetry, finalBoolOperation);
                    boolOperationPumiceParsingDifferenceDialog.show();
                }
            });

            synchronized (finalBoolOperation) {
                try {
                    System.out.println("start waiting 1");
                    finalBoolOperation.wait();
                } catch (InterruptedException e){
                    e.printStackTrace();
                }
            }
            if (finalBoolOperation.getObject() == null) {
                //return null -- will trigger runnableForRetry
                return null;
            }
            //return the result
            return finalBoolOperation.getObject().toString();
        }

        ParsingWrapper<SugiliteBooleanExpressionNew.BoolOperator> finalBoolOperator = new ParsingWrapper<>(null);
        ParsingWrapper<SugiliteValue> finalArg0 = new ParsingWrapper<>(null);
        ParsingWrapper<SugiliteValue> finalArg1 = new ParsingWrapper<>(null);

        //first check if need to ask about the bool operator
        Pair<SugiliteBooleanExpressionNew.BoolOperator, Integer> topMatchedOperator = boolOperatorCountList.get(0);
        Pair<SugiliteValue, Integer> topMatchedArg0 = arg0CountList.get(0);
        Pair<SugiliteValue, Integer> topMatchedArg1 = arg1CountList.get(0);

        if (topMatchedOperator.second >= (formulas.size() - booleanOperationCount)) {
            //confident
            finalBoolOperator.setObject(topMatchedOperator.first);
        }

        if (topMatchedArg0.second >= (formulas.size() - booleanOperationCount - 1)) {
            //confident
            finalArg0.setObject(topMatchedArg0.first);
        }

        if (topMatchedArg1.second >= (formulas.size() - booleanOperationCount - 1)) {
            //confident
            finalArg1.setObject(topMatchedArg1.first);
        }

        if (finalBoolOperator.getObject() == null) {
            //set arg0 and arg1 to the top match
            finalArg0.setObject(topMatchedArg0.first);
            finalArg1.setObject(topMatchedArg1.first);

            //check if need to ask about boolOperator
            List<Pair<SugiliteBooleanExpressionNew.BoolOperator, String>> boolOperatorBoolOperatorNameList = new ArrayList<>();
            List<SugiliteBooleanExpressionNew.BoolOperator> boolOperatorList = new ArrayList<>();
            boolOperatorCountList.forEach(pair -> boolOperatorBoolOperatorNameList.add(new Pair<>(pair.first, pair.first.name().replace("_", " ").toLowerCase())));
            boolOperatorBoolOperatorNameList.forEach(pair -> boolOperatorList.add(pair.first));

            context.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    PumiceParsingDifferenceDialog<SugiliteBooleanExpressionNew.BoolOperator> boolOperatorPumiceParsingDifferenceDialog = new PumiceParsingDifferenceDialog<SugiliteBooleanExpressionNew.BoolOperator>(context, pumiceDialogManager, boolOperatorBoolOperatorNameList, getPromptForBoolOperator(finalArg0.getObject(), finalArg1.getObject(), boolOperatorList), runnableForRetry, finalBoolOperator);
                    boolOperatorPumiceParsingDifferenceDialog.show();
                }
            });

            synchronized (finalBoolOperator) {
                try {
                    System.out.println("start waiting 2");
                    finalBoolOperator.wait();
                } catch (InterruptedException e){
                    e.printStackTrace();
                }
            }
            if (finalBoolOperator.getObject() == null) {
                //return null -- will trigger runnableForRetry
                return null;
            }
        }

        if (finalArg0.getObject() == null) {
            //check if need to ask about arg0
            List<SugiliteValue> arg0CandidateList = new ArrayList<>();
            List<Pair<SugiliteValue, String>> arg0CandidateCandidateNameList = new ArrayList<>();
            arg0CountList.forEach(pair -> arg0CandidateList.add(pair.first));
            arg0CountList.forEach(pair -> arg0CandidateCandidateNameList.add(new Pair<>(pair.first, pair.first.getReadableDescription().replace("the value of a concept ", ""))));
            context.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    PumiceParsingDifferenceDialog<SugiliteValue> arg0PumiceParsingDifferenceDialog = new PumiceParsingDifferenceDialog<SugiliteValue>(context, pumiceDialogManager, arg0CandidateCandidateNameList, getPromptForArgs(finalBoolOperator.getObject(), finalArg0.getObject(), finalArg1.getObject(), arg0CandidateList), runnableForRetry, finalArg0);
                    arg0PumiceParsingDifferenceDialog.show();
                }
            });

            synchronized (finalArg0) {
                try {
                    System.out.println("start waiting 3");
                    finalArg0.wait();
                } catch (InterruptedException e){
                    e.printStackTrace();
                }
            }
            if (finalArg0.getObject() == null) {
                //return null -- will trigger runnableForRetry
                return null;
            }

        }

        if (finalArg1.getObject() == null) {
            //check if need to ask about arg1
            List<SugiliteValue> arg1CandidateList = new ArrayList<>();
            List<Pair<SugiliteValue, String>> arg1CandidateCandidateNameList = new ArrayList<>();
            arg1CountList.forEach(pair -> arg1CandidateList.add(pair.first));
            arg1CountList.forEach(pair -> arg1CandidateCandidateNameList.add(new Pair<>(pair.first, pair.first.getReadableDescription().replace("the value of a concept ", ""))));

            context.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    PumiceParsingDifferenceDialog<SugiliteValue> arg1PumiceParsingDifferenceDialog = new PumiceParsingDifferenceDialog<SugiliteValue>(context, pumiceDialogManager, arg1CandidateCandidateNameList, getPromptForArgs(finalBoolOperator.getObject(), finalArg0.getObject(), finalArg1.getObject(), arg1CandidateList), runnableForRetry, finalArg1);
                    arg1PumiceParsingDifferenceDialog.show();
                }
            });


            synchronized (finalArg1) {
                try {
                    System.out.println("start waiting 4");
                    finalArg1.wait();
                } catch (InterruptedException e){
                    e.printStackTrace();
                }
            }
            if (finalArg1.getObject() == null) {
                //return null -- will trigger runnableForRetry
                return null;
            }

        }

        //return an assembled new SugiliteBooleanExpression from the bool operator, arg0 and arg1
        return SugiliteBooleanExpressionNew.getBooleanExpressionFormula(finalBoolOperator.getObject(), finalArg0.getObject(), finalArg1.getObject());
    }


    private static String getPromptForBoolOperator (SugiliteValue arg0, SugiliteValue arg1, List<SugiliteBooleanExpressionNew.BoolOperator> candidates) {
        StringBuilder output = new StringBuilder();
        String arg0Representation = arg0 == null ? "something" : arg0.getReadableDescription();
        String arg1Representation = arg1 == null ? "something" : arg1.getReadableDescription();
        List<String> candidateStrings = new ArrayList<>();
        candidates.forEach(candidate -> candidateStrings.add(candidate.name().replace("_", " ").toLowerCase()));

        //don't know the bool operator
        output.append(String.format("I understand that you are comparing %s to %s. ", arg0Representation, arg1Representation));
        output.append(String.format("Should %s be %s %s?", arg0Representation, separateWordsBy(candidateStrings, "or") , arg1Representation));
        return output.toString();
    }

    private static String getPromptForArgs (SugiliteBooleanExpressionNew.BoolOperator boolOperator, @Nullable SugiliteValue arg0, @Nullable SugiliteValue arg1, List<SugiliteValue> candidates) {
        StringBuilder output = new StringBuilder();
        String booleanOperatorRepresentation = boolOperator == null ? "something" : boolOperator.name().replace("_", " ").toLowerCase();
        String arg0Representation = arg0 == null ? "something" : arg0.getReadableDescription();
        String arg1Representation = arg1 == null ? "something" : arg1.getReadableDescription();
        List<String> candidateStrings = new ArrayList<>();
        candidates.forEach(candidate -> candidateStrings.add(candidate.getReadableDescription().replace("the value of a concept ", "")));

        if (arg0 == null || arg1 == null) {
            //don't know arg0 or arg1
            output.append(String.format("I understand that you are comparing if %s is %s %s. ", arg0Representation, booleanOperatorRepresentation, arg1Representation));
            output.append(String.format("Are you comparing with %s?", separateWordsBy(candidateStrings, "or")));
            return output.toString();

        } else {
            throw new RuntimeException("Arg0 or arg1 should be null!");
        }
    }

    private static String getPromptForBooleanOperations (List<SugiliteValue> candidates) {
        StringBuilder output = new StringBuilder();

        List<String> candidateStrings = new ArrayList<>();
        candidates.subList(1, candidates.size()).forEach(candidate -> candidateStrings.add(candidate.getReadableDescription()));
        output.append(String.format("Do you mean %s?", separateWordsBy(candidateStrings, "or")));
        return output.toString();
    }


    public static String separateWordsBy(List<String> words, String lastWordSeparator){
        String result = StringUtils.join(words, ", ");
        if (words.size() >= 2) {
            result = replaceLast(result, ", ", String.format(", %s ", lastWordSeparator));
        }
        return result;
    }

    private static String replaceLast(String str, String oldValue, String newValue) {
        str = StringUtils.reverse(str);
        str = str.replaceFirst(StringUtils.reverse(oldValue), StringUtils.reverse(newValue));
        str = StringUtils.reverse(str);
        return str;
    }


    private static List sortByValue(Map map, boolean descendingOrder)
    {
        // Create a list from elements of HashMap
        List<Map.Entry<Object, Comparable> > list =
                new LinkedList<Map.Entry<Object, Comparable> >(map.entrySet());

        // Sort the list
        Collections.sort(list, new Comparator<Map.Entry<Object, Comparable> >() {
            public int compare(Map.Entry<Object, Comparable> o1,
                               Map.Entry<Object, Comparable> o2)
            {   int result = (o1.getValue()).compareTo(o2.getValue());
                return descendingOrder ? result * -1 : result;
            }
        });

        // put data from sorted list to hashmap
        List<Pair<Object, Comparable>> temp = new ArrayList<>();
        for (Map.Entry<Object, Comparable> aa : list) {
            temp.add(new Pair<>(aa.getKey(), aa.getValue()));
        }
        return temp;
    }

    protected class ParsingWrapper<T> {
        T object;
        public ParsingWrapper (T object) {
            this.object = object;
        }

        public synchronized void setObject(T object) {
            this.object = object;
        }

        public synchronized T getObject() {
            return object;
        }
    }



}
