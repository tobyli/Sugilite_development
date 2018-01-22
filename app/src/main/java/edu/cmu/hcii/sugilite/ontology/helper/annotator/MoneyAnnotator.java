package edu.cmu.hcii.sugilite.ontology.helper.annotator;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.cmu.hcii.sugilite.ontology.SugiliteRelation;

/**
 * @author toby
 * @date 1/18/18
 * @time 12:24 PM
 */
public class MoneyAnnotator extends SugiliteTextAnnotator {
    public MoneyAnnotator(){
        super();
    }

    @Override
    public List<AnnotatingResult> annotate(String text) {
        List<AnnotatingResult> results = new ArrayList<>();
        String regex = "[$€£¥] *[0-9]+(\\.[0-9][0-9]?)?";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()){
            String matchedString = text.substring(matcher.start(), matcher.end());
            String numericValue = matchedString.replace("$", "")
                    .replace("€", "")
                    .replace("£", "")
                    .replace("¥", "").trim();
            Double value = null;
            try {
                value = Double.valueOf(numericValue);
            }
            catch (Exception e){
                e.printStackTrace();
            }
            AnnotatingResult result = new AnnotatingResult(RELATION, text.substring(matcher.start(), matcher.end()), matcher.start(), matcher.end(), value);
            results.add(result);
        }
        return results;
    }

    private static final SugiliteRelation RELATION = SugiliteRelation.CONTAINS_MONEY;

}
