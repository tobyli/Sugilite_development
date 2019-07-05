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
 * Parse any number separated by word boundary. Allowing commas inside numbers (e.g 12,345.67)
 * Created by shi on 4/5/18.
 */

public class NumberAnnotator implements SugiliteTextAnnotator {
    private Map<String, List<AnnotatingResult>> cache;

    NumberAnnotator(){
        cache = new HashMap<>();
    }

    @Override
    public List<AnnotatingResult> annotate(String text) {
        if (cache.containsKey(text)){
            return cache.get(text);
        }

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

        cache.put(text, results);
        return results;
    }

    private static final SugiliteRelation RELATION = SugiliteRelation.CONTAINS_NUMBER;
}
