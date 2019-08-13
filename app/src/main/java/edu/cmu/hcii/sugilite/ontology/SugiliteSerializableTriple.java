package edu.cmu.hcii.sugilite.ontology;

import java.io.Serializable;
import java.util.Objects;

/**
 * @author nancy
 * @date 10/25/17
 * @time 10:42 PM
 */
public class SugiliteSerializableTriple implements Serializable{
    private transient SugiliteSerializableEntity subject;
    private transient SugiliteRelation predicate;
    private transient SugiliteSerializableEntity object;

    private String subjectId;
//    private String objectId;
    // the id if it's a node, else: the actual string value
    private String objectStringValue;
    private String predicateStringValue;

    public SugiliteSerializableTriple(String subjectId, String objectStringValue, String predicateStringValue){
        this.subjectId = subjectId;
        this.objectStringValue = objectStringValue;
        this.predicateStringValue = predicateStringValue;
    }

    public SugiliteSerializableTriple(SugiliteTriple t){
        this.subject = new SugiliteSerializableEntity(t.getSubject());
        this.predicate = t.getPredicate();
        this.object = new SugiliteSerializableEntity(t.getObject());
        this.subjectId = "@" + t.getSubjectId();

        if(t.getObjectStringValue() != null) {
            // object is NOT a node
            this.objectStringValue = t.getObjectStringValue();
        }
        else{
            this.objectStringValue = "@" + t.getObjectId();
        }

        this.predicateStringValue = t.getPredicateStringValue();
    }

    @Deprecated
    public SugiliteRelation getPredicate() {
        return SugiliteRelation.stringRelationMap.get(predicateStringValue);
    }

    public String getPredicateStringValue() {
        return predicateStringValue;
    }

    public String getSubjectId() {
        return subjectId;
    }

    public String getObjectStringValue() {
        return objectStringValue;
    }

    // TODO rename to getObject
    public SugiliteSerializableEntity getObjectAsSerializableEntity() {
        return object;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == this){
            return true;
        }
        if(obj instanceof SugiliteSerializableTriple){
            if(((SugiliteSerializableTriple) obj).subject != null && this.subject == null){
                return false;
            }
            if(((SugiliteSerializableTriple) obj).subject == null && this.subject != null){
                return false;
            }
            if(((SugiliteSerializableTriple) obj).subject != null && this.subject != null && (!((SugiliteSerializableTriple) obj).subject.equals(this.subject))){
                return false;
            }

            if(((SugiliteSerializableTriple) obj).predicate != null && this.predicate == null){
                return false;
            }
            if(((SugiliteSerializableTriple) obj).predicate == null && this.predicate != null){
                return false;
            }
            if(((SugiliteSerializableTriple) obj).predicate != null && this.predicate != null && (!((SugiliteSerializableTriple) obj).predicate.equals(this.predicate))){
                return false;
            }

            if(((SugiliteSerializableTriple) obj).object != null && this.object == null){
                return false;
            }
            if(((SugiliteSerializableTriple) obj).object == null && this.object != null){
                return false;
            }
            if(((SugiliteSerializableTriple) obj).object != null && this.object != null && (!((SugiliteSerializableTriple) obj).object.equals(this.object))){
                return false;
            }

            if(((SugiliteSerializableTriple) obj).subjectId != null && this.subjectId == null){
                return false;
            }
            if(((SugiliteSerializableTriple) obj).subjectId == null && this.subjectId != null){
                return false;
            }
            if(((SugiliteSerializableTriple) obj).subjectId != null && this.subjectId != null && (!((SugiliteSerializableTriple) obj).subjectId.equals(this.subjectId))){
                return false;
            }

            if(((SugiliteSerializableTriple) obj).objectStringValue != null && this.objectStringValue == null){
                return false;
            }
            if(((SugiliteSerializableTriple) obj).objectStringValue == null && this.objectStringValue != null){
                return false;
            }
            if(((SugiliteSerializableTriple) obj).objectStringValue != null && this.objectStringValue != null && (!((SugiliteSerializableTriple) obj).objectStringValue.equals(this.objectStringValue))){
                return false;
            }

            if(((SugiliteSerializableTriple) obj).predicateStringValue != null && this.predicateStringValue == null){
                return false;
            }
            if(((SugiliteSerializableTriple) obj).predicateStringValue == null && this.predicateStringValue != null){
                return false;
            }
            if(((SugiliteSerializableTriple) obj).predicateStringValue != null && this.predicateStringValue != null && (!((SugiliteSerializableTriple) obj).predicateStringValue.equals(this.predicateStringValue))){
                return false;
            }

            return true;
        }
        else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(subject == null ? null : subject.getEntityValue(),
                subject == null ? null : predicate.getRelationId(),
                object == null ? null : object.getEntityValue(),
                subjectId == null ? null : subjectId,
                predicateStringValue == null ? null : predicateStringValue,
                objectStringValue == null ? null : objectStringValue);
    }


}
