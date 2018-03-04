package edu.cmu.hcii.sugilite.ontology.helper.annotator;

import java.util.ArrayList;
import java.util.List;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.cmu.hcii.sugilite.ontology.SugiliteRelation;

/**
 * Given input as a string containing length (with units such as ft, km, mile, etc.), parse the length
 * data and store it with the unit millimeter (e.g, 1.5km is stored as 1500000.0)
 *
 * Created by shi on 2/15/18.
 */

public class LengthAnnotator extends SugiliteTextAnnotator {
    public LengthAnnotator() { super(); }

    private static final int MILE = 1609344;
    private static final int KILOMETER = 1000000;
    private static final int METER = 1000;
    private static final double YARD = 914.4;
    private static final double FEET = 304.8;
    private static final double INCH = 25.4;
    private static final int CENTIMETER = 10;

    @Override
    public List<AnnotatingResult> annotate(String text) {
        List<AnnotatingResult> results = new ArrayList<>();
        String regex = "\\b[0-9]+?(.)?[0-9]*? (km|m|ft|feet|mile(s)?|mi|yd|inch|cm)\\b";
        Pattern pattern = Pattern.compile(regex);

        int curEnd = -3;
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            String matchedString = text.substring(matcher.start(), matcher.end());
            String[] parsed = matchedString.split(" ");
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
                    }
                    else {
                        AnnotatingResult res = new AnnotatingResult(RELATION, text.substring(matcher.start(), matcher.end()),
                                matcher.start(), matcher.end(), value);
                        results.add(res);
                    }
                    curEnd = matcher.end();
                }
            } catch (Exception e) {}
        }
        return results;
    }

    private static final SugiliteRelation RELATION = SugiliteRelation.CONTAINS_LENGTH;
}
