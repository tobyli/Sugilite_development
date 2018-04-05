package edu.cmu.hcii.sugilite.ontology.helper.annotator;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.cmu.hcii.sugilite.ontology.SugiliteRelation;

/**
 * Parse any number separated by word boundary. Allowing commas inside numbers (e.g 12,345.67)
 * Created by shi on 4/5/18.
 */

public class NumberAnnotator extends SugiliteTextAnnotator {
    public NumberAnnotator() { super(); }

    @Override
    public List<AnnotatingResult> annotate(String text) {
        List<AnnotatingResult> results = new ArrayList<>();
        String regex = "\\b[0-9]+(,[0-9]{3})*(\\.[0-9]+)?\\b";
        Pattern pattern = Pattern.compile(regex);

        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            String matchedString = text.substring(matcher.start(), matcher.end()).replaceAll(",", "");
            double value = Double.valueOf(matchedString);
            results.add(new AnnotatingResult(RELATION, text.substring(matcher.start(), matcher.end()),
                    matcher.start(), matcher.end(), value));
        }
        return results;
    }

    private static final SugiliteRelation RELATION = SugiliteRelation.CONTAINS_NUMBER;
}
