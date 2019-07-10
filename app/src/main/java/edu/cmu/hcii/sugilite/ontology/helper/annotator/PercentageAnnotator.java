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
 * Given the input as a string containing percentage (% or percent), parse the value and store it
 * directly in percentage value (e.g, 3.5 % is stored as 3.5)
 * <p>
 * Created by shi on 2/22/18.
 */

public class PercentageAnnotator implements SugiliteTextAnnotator {
    private Map<String, List<AnnotatingResult>> cache;

    PercentageAnnotator(){
        cache = new HashMap<>();
    }


    @Override
    public List<AnnotatingResult> annotate(String text) {
        text = text.replaceAll("[\\u00A0\\u2007\\u202F]+", " ");

        if (cache.containsKey(text)){
            return cache.get(text);
        }

        List<AnnotatingResult> results = new ArrayList<>();
        String regex = "\\b\\d+?(.\\d+?)?(\\s)?(%|percent)";
        Pattern pattern = Pattern.compile(regex);

        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            try {
                String matchedString = text.substring(matcher.start(), matcher.end());
                if (matcher.start() > 0 && text.charAt(matcher.start() - 1) == '-')
                    matchedString = text.substring(matcher.start() - 1, matcher.end());
                String[] parsed = matchedString.split("[p %]");
                results.add(new AnnotatingResult(RELATION, text.substring(matcher.start(), matcher.end()),
                        matcher.start(), matcher.end(), Double.valueOf(parsed[0])));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        cache.put(text, results);
        return results;
    }

    private static final SugiliteRelation RELATION = SugiliteRelation.CONTAINS_PERCENTAGE;
}
