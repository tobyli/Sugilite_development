package edu.cmu.hcii.sugilite.ontology.helper.annotator;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.cmu.hcii.sugilite.ontology.SugiliteRelation;

/**
 * @author toby
 * @date 1/17/18
 * @time 11:49 PM
 */
public class EmailAddressAnnotator extends SugiliteTextAnnotator {
    public EmailAddressAnnotator(){
        super();
    }

    @Override
    public List<AnnotatingResult> annotate(String text) {
        List<AnnotatingResult> results = new ArrayList<>();
        Pattern pattern = Pattern.compile("\\b\\w+([-+.']\\w+)*@\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*\\b");
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()){
            AnnotatingResult result = new AnnotatingResult(RELATION, text.substring(matcher.start(), matcher.end()), matcher.start(), matcher.end());
            results.add(result);
        }
        return results;
    }

    private static final SugiliteRelation RELATION = SugiliteRelation.CONTAINS_EMAIL_ADDRESS;

    public static void main(String[] args ){
        EmailAddressAnnotator emailAnnotator = new EmailAddressAnnotator();
        List<AnnotatingResult> results = emailAnnotator.annotate("marissaradensky@gmail.com or mradensky19@amherst.edu or blah.");
        System.out.println(results.size());
        System.out.println(results.get(0).getMatchedString());
        System.out.println(results.get(1).getMatchedString());
    }
}
