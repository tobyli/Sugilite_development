package edu.cmu.hcii.sugilite.ontology.helper.annotator;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.cmu.hcii.sugilite.ontology.SugiliteRelation;

/**
 * Given the input as a string containing times of the specified formats, parse it and store the
 * times as the number of milliseconds passed from 12 am until the given time.
 *
 * Created by shi on 1/25/18.
 */

public class TimeAnnotator extends SugiliteTextAnnotator {
    public TimeAnnotator() {
        super();
    }

    @Override
    public List<AnnotatingResult> annotate(String text) {
        List<AnnotatingResult> results = new ArrayList<>();
        String regex12h = "\\b(0?[1-9]|1[0-2]):([0-5][0-9])(\\s)?([apAP][mM]?)\\b";
        String regex24h = "\\b([0-1]?[0-9]|2[0-3]):([0-5][0-9])\\b";
        String regexH = "\\b((0?[1-9])|(1[0-2]))(\\s)?([apAP][mM]?)\\b";
        Pattern pattern12h = Pattern.compile(regex12h);
        Pattern pattern24h = Pattern.compile(regex24h);
        Pattern patternH = Pattern.compile(regexH);

        boolean[] checkList = new boolean[text.length()];
        for (boolean b : checkList) b = false;

        Matcher matcher = pattern12h.matcher(text);
        while (matcher.find()) {
            String matchedString = text.substring(matcher.start(), matcher.end());
            boolean foundBefore = false;
            for (int i = matcher.start(); i < matcher.end(); i++) {
                if (checkList[i]) {
                    foundBefore = true;
                    break;
                }
                checkList[i] = true;
            }
            if (foundBefore)
                continue;
            String hourString = matchedString.split(":")[0];
            String minuteString = matchedString.split(":")[1].substring(0, 2);

            Double value = null;
            try {
                int hour = Integer.valueOf(hourString) % 12;
                if (matchedString.contains("p") || matchedString.contains("P"))
                    hour += 12;
                int minute = Integer.valueOf(minuteString);
                value = (double) (hour*60 + minute)*1000;
            } catch (Exception e) {
                e.printStackTrace();
            }
            AnnotatingResult result = new AnnotatingResult(RELATION, text.substring(matcher.start(), matcher.end()), matcher.start(), matcher.end(), value);
            results.add(result);
        }

        matcher = pattern24h.matcher(text);
        while (matcher.find()) {
            String matchedString = text.substring(matcher.start(), matcher.end());
            boolean foundBefore = false;
            for (int i = matcher.start(); i < matcher.end(); i++) {
                if (checkList[i]) {
                    foundBefore = true;
                    break;
                }
                checkList[i] = true;
            }
            if (foundBefore)
                continue;
            String hourString = matchedString.split(":")[0];
            String minuteString = matchedString.split(":")[1];

            Double value = null;
            try {
                int hour = Integer.valueOf(hourString);
                int minute = Integer.valueOf(minuteString);
                value = (double) (hour*60 + minute)*1000;
            } catch (Exception e) {
                e.printStackTrace();
            }
            AnnotatingResult result = new AnnotatingResult(RELATION, text.substring(matcher.start(), matcher.end()), matcher.start(), matcher.end(), value);
            results.add(result);
        }

        matcher = patternH.matcher(text);
        while (matcher.find()) {
            String matchedString = text.substring(matcher.start(), matcher.end());
            boolean foundBefore = false;
            for (int i = matcher.start(); i < matcher.end(); i++) {
                if (checkList[i]) {
                    foundBefore = true;
                    break;
                }
                checkList[i] = true;
            }
            if (foundBefore)
                continue;
            String hourString = matchedString.split("(\\s)?[aApP]")[0];

            Double value = null;
            try {
                int hour = Integer.valueOf(hourString) % 12;
                if (matchedString.contains("p") || matchedString.contains("P"))
                    hour += 12;
                value = (double) (hour*60)*1000;
            } catch (Exception e) {
                e.printStackTrace();
            }
            AnnotatingResult result = new AnnotatingResult(RELATION, text.substring(matcher.start(), matcher.end()), matcher.start(), matcher.end(), value);
            results.add(result);
        }
        return results;
    }

    private static final SugiliteRelation RELATION = SugiliteRelation.CONTAINS_TIME;
}
