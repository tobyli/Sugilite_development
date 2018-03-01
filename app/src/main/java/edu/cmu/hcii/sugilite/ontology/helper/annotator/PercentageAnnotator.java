package edu.cmu.hcii.sugilite.ontology.helper.annotator;

import java.util.ArrayList;
import java.util.List;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.cmu.hcii.sugilite.ontology.SugiliteRelation;

/**
 * Given the input as a string containing percentage (% or percent), parse the value and store it
 * directly in percentage value (e.g, 3.5 % is stored as 3.5)
 *
 * Created by shi on 2/22/18.
 */

public class PercentageAnnotator extends SugiliteTextAnnotator {
    public PercentageAnnotator() { super(); }

    @Override
    public List<AnnotatingResult> annotate(String text) {
        List<AnnotatingResult> results = new ArrayList<>();
        String regex = "\\b\\d+?(.\\d+?)?( )?(%|percent)";
        Pattern pattern = Pattern.compile(regex);

        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            String matchedString = text.substring(matcher.start(), matcher.end());
            if (matcher.start() > 0 && text.charAt(matcher.start()-1)=='-')
                matchedString = text.substring(matcher.start()-1, matcher.end());
            String[] parsed = matchedString.split("[p %]");
            results.add(new AnnotatingResult(RELATION, text.substring(matcher.start(), matcher.end()),
                    matcher.start(), matcher.end(), Double.valueOf(parsed[0])));
        }
        return results;
    }

    private static final SugiliteRelation RELATION = SugiliteRelation.CONTAINS_PERCENTAGE;
}
