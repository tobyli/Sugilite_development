package edu.cmu.hcii.sugilite.ontology.helper.annotator;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.cmu.hcii.sugilite.ontology.SugiliteRelation;

/**
 * Given input as a string containing duration (with keywords hr, min, etc.), parse the duration and
 * store it with millimeter unit.
 *
 * Created by shi on 3/1/18.
 */

public class VolumeAnnotator extends SugiliteTextAnnotator {
    public VolumeAnnotator() {super();}

    static final double MILLILITER = 1.0;
    static final double LITER = 1000.0;
    static final double OUNCE = 29.547;
    static final double TABLESPOON = 4.929;
    static final double CUP = 236.588;
    static final double PINT = 473.176;
    static final double QUART = 946.353;
    static final double GALLON = 3785.412;

    @Override
    public List<AnnotatingResult> annotate(String text) {
        List<AnnotatingResult> results = new ArrayList<>();
        String regex = "\\b\\d+?(.\\d+?)? (m[Ll]?|L|(fl )?oz|ounce(s)?|tsp|cp|pt|qt|gal)\\b";
        Pattern pattern = Pattern.compile(regex);

        int curEnd = -3;
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            String matchedString = text.substring(matcher.start(), matcher.end());
            String[] parsed = matchedString.split(" ");
            double num = Double.valueOf(parsed[0]);
            if (parsed[1].startsWith("m")) num *= MILLILITER;
            else if (parsed[1].startsWith("L")) num *= LITER;
            else if (parsed[1].startsWith("f")) num *= OUNCE;
            else if (parsed[1].startsWith("o")) num *= OUNCE;
            else if (parsed[1].startsWith("t")) num *= TABLESPOON;
            else if (parsed[1].startsWith("c")) num *= CUP;
            else if (parsed[1].startsWith("p")) num *= PINT;
            else if (parsed[1].startsWith("q")) num *= QUART;
            else if (parsed[1].startsWith("g")) num *= GALLON;
            if (matcher.start() - curEnd == 1 && text.charAt(curEnd) == ' ') {
                AnnotatingResult last = results.get(results.size() - 1);
                last.setNumericValue(last.getNumericValue() + num);
            }
            else {
                AnnotatingResult res = new AnnotatingResult(RELATION, text.substring(matcher.start(), matcher.end()),
                        matcher.start(), matcher.end(), num);
                results.add(res);
            }
            curEnd = matcher.end();
        }
        return results;
    }

    private static final SugiliteRelation RELATION = SugiliteRelation.CONTAINS_VOLUME;
}
