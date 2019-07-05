package edu.cmu.hcii.sugilite.ontology;

import java.util.Objects;

/**
 * @author toby
 * @date 9/25/17
 * @time 5:58 PM
 */
public class SugiliteTriple {
    private transient SugiliteEntity subject;
    private SugiliteRelation predicate;
    private transient SugiliteEntity object;

    private Integer subjectId;
    private Integer objectId;
    private String objectStringValue = null;
    private String predicateStringValue;


    public SugiliteTriple() {

    }

    public SugiliteTriple(SugiliteEntity subject, SugiliteRelation predicate, SugiliteEntity object){
        this.subject = subject;
        this.predicate = predicate;
        this.predicateStringValue = predicate.getRelationName();
        this.subjectId = subject.getEntityId();
        this.objectId = object.getEntityId();
        this.object = object;
    }

    public SugiliteEntity getSubject() {
        return subject;
    }

    public SugiliteRelation getPredicate() {
        return predicate;
    }

    public SugiliteEntity getObject() {
        return object;
    }

    public Integer getSubjectId() {
        return subjectId;
    }

    public Integer getObjectId() {
        return objectId;
    }

    public String getObjectStringValue() {
        return objectStringValue;
    }

    public String getPredicateStringValue() {
        return predicateStringValue;
    }

    public void setObjectId(Integer objectId) {
        this.objectId = objectId;
    }

    public void setSubjectId(Integer subjectId) {
        this.subjectId = subjectId;
    }

    public void setObjectStringValue(String objectStringValue) {
        this.objectStringValue = objectStringValue;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == this){
            return true;
        }
        if(obj instanceof SugiliteTriple){
            return ((SugiliteTriple) obj).subject.equals(this.subject) &&
                    ((SugiliteTriple) obj).predicate.equals(this.predicate) &&
                    ((SugiliteTriple) obj).object.equals(this.object);
        }
        else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.subject.getEntityValue(),
                this.predicate.getRelationId(),
                this.object.getEntityValue());
    }


}
