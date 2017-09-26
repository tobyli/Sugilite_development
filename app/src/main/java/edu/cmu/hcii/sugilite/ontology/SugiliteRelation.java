package edu.cmu.hcii.sugilite.ontology;

import java.util.Objects;

/**
 * @author toby
 * @date 9/25/17
 * @time 5:57 PM
 */
public class SugiliteRelation {
    private Integer relationId;
    private String relationName;

    public SugiliteRelation(Integer relationId, String relationName){
        this.relationId = relationId;
        this.relationName = relationName;
        //TODO: initiate a unique relationId
    }

    public Integer getRelationId() {
        return relationId;
    }

    public String getRelationName() {
        return relationName;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == this){
            return true;
        }
        if(obj instanceof SugiliteRelation){
            return ((SugiliteRelation) obj).relationId.equals(this.relationId);
        }
        else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.relationId);
    }

    //examples of relations
    public static final SugiliteRelation HAS_CLASS_NAME = new SugiliteRelation(0, "HAS_CLASS_NAME");
    public static final SugiliteRelation HAS_TEXT = new SugiliteRelation(1, "HAS_TEXT");
}
