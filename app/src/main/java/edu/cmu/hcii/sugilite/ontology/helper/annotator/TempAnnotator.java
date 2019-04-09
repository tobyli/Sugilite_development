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
 * Given the input as a string containing temperatures of the specified formats, parse it and store the
 * temperatures in terms of Celsius.
 *
 * @author marissa
 * @date 6/15/18
 * @time 1:16 PM
 */
public class TempAnnotator implements SugiliteTextAnnotator {
    private Map<String, List<AnnotatingResult>> cache;

    TempAnnotator(){
        cache = new HashMap<>();
    }

    @Override
    public List<AnnotatingResult> annotate(String text) {
        if (cache.containsKey(text)){
            return cache.get(text);
        }

        List<AnnotatingResult> results = new ArrayList<>();
        Pattern pattern = Pattern.compile("\\b\\d+?(.\\d+?)?( )?((((deg(ree(s)?|s)?)|°)( )?([fF](ahrenheit)?|[cC](elsius)?))|((deg(ree(s)?|s)?)|°)|([fF](ahrenheit)?|[cC](elsius)?))(?:(?<![\\w°])(?=[\\w°])|(?<=[\\w°])(?![\\w°]))");
        Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            String matchedString = text.substring(matcher.start(), matcher.end());
            String split1 = matchedString.split("[^0-9]")[0];
            double value = Double.parseDouble(split1);
            if (matchedString.contains("F") || matchedString.contains("f")) {
                value = (value - 32) / 1.8;
            }
            AnnotatingResult result = new AnnotatingResult(RELATION, text.substring(matcher.start(), matcher.end()), matcher.start(), matcher.end(), value);
            results.add(result);
        }

        cache.put(text, results);
        return results;
    }

    private static final SugiliteRelation RELATION = SugiliteRelation.CONTAINS_TEMPERATURE;

    public static void main(String[] args) {
        TempAnnotator tempAnnotator = new TempAnnotator();
        List<AnnotatingResult> results = tempAnnotator.annotate("If it's less than 32 degs fahrenheit outside, remind me to take a sweater.");
        System.out.println(results.size());
        System.out.println(results.get(0).getNumericValue().doubleValue());
    }
}
