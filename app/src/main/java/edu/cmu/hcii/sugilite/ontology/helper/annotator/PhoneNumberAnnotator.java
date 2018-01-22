package edu.cmu.hcii.sugilite.ontology.helper.annotator;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.cmu.hcii.sugilite.ontology.SugiliteRelation;

/**
 * @author toby
 * @date 1/17/18
 * @time 11:59 PM
 */
public class PhoneNumberAnnotator extends SugiliteTextAnnotator {
    public PhoneNumberAnnotator(){
        super();
    }

    @Override
    public List<AnnotatingResult> annotate(String text) {
        List<AnnotatingResult> results = new ArrayList<>();
        Pattern pattern = Pattern.compile("((\\([0-9]{3}\\)) |([0-9]{3}-))[0-9]{3}-[0-9]{4}");
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()){
            AnnotatingResult result = new AnnotatingResult(RELATION, text.substring(matcher.start(), matcher.end()), matcher.start(), matcher.end());
            results.add(result);
        }
        return results;
    }

    private static final SugiliteRelation RELATION = SugiliteRelation.CONTAINS_PHONE_NUMBER;

    public static void main(String[] args ){
        PhoneNumberAnnotator phoneNumberAnnotator = new PhoneNumberAnnotator();
        List<AnnotatingResult> results = phoneNumberAnnotator.annotate("you can call me at 612 756 8886, i'm available everyday");
        System.out.println(results.size());
    }
}
