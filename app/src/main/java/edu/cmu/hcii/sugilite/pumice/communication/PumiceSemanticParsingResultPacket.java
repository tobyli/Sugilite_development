package edu.cmu.hcii.sugilite.pumice.communication;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author toby
 * @date 11/13/18
 * @time 1:32 PM
 */
public class PumiceSemanticParsingResultPacket implements Serializable {
    public class QueryGroundingPair {
        public int id;
        public String formula;
        public List<String> grounding;

        QueryGroundingPair(int id, String formula, List<String> grounding) {
            this.id = id;
            this.formula = formula;
            this.grounding = grounding;
        }
    }

    public String utteranceType;
    public String userUtterance;
    public Long queryId;
    public List<QueryGroundingPair> queries;
}
