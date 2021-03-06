package edu.cmu.hcii.sugilite.ontology.helper.annotator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.cmu.hcii.sugilite.ontology.SugiliteRelation;
import edu.cmu.hcii.sugilite.ontology.helper.annotator.SugiliteTextParentAnnotator.AnnotatingResult;

/**
 * Given input as a string containing length (with units such as ft, km, mile, etc.), parse the length
 * data and store it with the unit millimeter (e.g, 1.5km is stored as 1500000.0)
 * <p>
 * Created by shi on 2/15/18.
 */

public class LengthAnnotator implements SugiliteTextAnnotator {
    private Map<String, List<AnnotatingResult>> cache;

    LengthAnnotator(){
        cache = new HashMap<>();
    }

    private static final int MILE = 1609344;
    private static final int KILOMETER = 1000000;
    private static final int METER = 1000;
    private static final double YARD = 914.4;
    private static final double FEET = 304.8;
    private static final double INCH = 25.4;
    private static final int CENTIMETER = 10;

    @Override
    public List<AnnotatingResult> annotate(String text) {
        text = text.replaceAll("[\\u00A0\\u2007\\u202F]+", " ");

        if (cache.containsKey(text)){
            return cache.get(text);
        }

        List<AnnotatingResult> results = new ArrayList<>();
        String regex = "\\b[0-9]+?(.)?[0-9]*?(\\s)?(km|m|ft|feet|mile(s)?|mi|yd|inch|cm)\\b";
        Pattern pattern = Pattern.compile(regex);

        int curEnd = -3;
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            String matchedString = text.substring(matcher.start(), matcher.end());
            String[] parsed;
            if (matchedString.contains(" "))
                parsed = matchedString.split(" ");
            else {
                parsed = new String[2];
                parsed[0] = matchedString.split("[a-z]")[0];
                parsed[1] = matchedString.substring(parsed[0].length());
            }
            try {
                double value = -1;
                double num = Double.valueOf(parsed[0]);
                if (parsed[1].contains("mi")) value = (num * MILE);
                else if (parsed[1].contains("k")) value = (num * KILOMETER);
                else if (parsed[1].startsWith("m")) value = (num * METER);
                else if (parsed[1].contains("y")) value = (num * YARD);
                else if (parsed[1].contains("f")) value = (num * FEET);
                else if (parsed[1].startsWith("i")) value = (num * INCH);
                else if (parsed[1].startsWith("c")) value = (num * CENTIMETER);
                if (value >= 0) {
                    //all these values are saved in meter
                    if (matcher.start() - curEnd == 1 && text.charAt(curEnd) == ' ') {
                        AnnotatingResult last = results.get(results.size() - 1);
                        last.setNumericValue(last.getNumericValue() + value);
                    } else {
                        AnnotatingResult res = new AnnotatingResult(RELATION, text.substring(matcher.start(), matcher.end()),
                                matcher.start(), matcher.end(), value);
                        results.add(res);
                    }
                    curEnd = matcher.end();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        cache.put(text, results);
        return results;
    }

    private static final SugiliteRelation RELATION = SugiliteRelation.CONTAINS_LENGTH;

    public static void main(String[] args) {
        LengthAnnotator lengthAnnotator = new LengthAnnotator();
        List<AnnotatingResult> results = lengthAnnotator.annotate("1ft or 0.3048m");
        System.out.println(results.size());
        System.out.println(results.get(0).getNumericValue().doubleValue());
        System.out.println(results.get(1).getNumericValue().doubleValue());
    }
}
