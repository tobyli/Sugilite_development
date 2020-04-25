package edu.cmu.hcii.sugilite.pumice.communication;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.cmu.hcii.sugilite.ontology.helper.annotator.SugiliteTextParentAnnotator;

/**
 * @author toby
 * @date 11/13/18
 * @time 1:32 PM
 */
public class PumiceSemanticParsingResultPacket implements Serializable {
    public class QueryGroundingPair implements Serializable {
        public int id;
        public String formula;
        public List<String> grounding;

        QueryGroundingPair(int id, String formula, List<String> grounding) {
            this.id = id;
            this.formula = formula;
            this.grounding = grounding;
        }
    }

    public void cleanFormula(){
        for (QueryGroundingPair queryGroundingPair : queries) {
            Pattern pattern = Pattern.compile("\"[\\S\\s]+ +'[\\S\\s]+\"");
            Matcher matcher = pattern.matcher(queryGroundingPair.formula);
            while (matcher.find()) {
                try {
                    String matchedString = queryGroundingPair.formula.substring(matcher.start(), matcher.end());
                    String newString = matchedString.replace(" '", "'");
                    queryGroundingPair.formula = queryGroundingPair.formula.replace(matchedString, newString);
                    matcher = pattern.matcher(queryGroundingPair.formula);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }


    public String utteranceType;
    public String userUtterance;
    public Long queryId;
    public List<QueryGroundingPair> queries;
}
