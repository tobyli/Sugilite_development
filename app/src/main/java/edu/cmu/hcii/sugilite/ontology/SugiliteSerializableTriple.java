package edu.cmu.hcii.sugilite.ontology;

import java.io.Serializable;
import java.util.Objects;

/**
 * @author nancy
 * @date 10/25/17
 * @time 10:42 PM
 */
public class SugiliteSerializableTriple implements Serializable{

    private String subjectId;
    private String objectId;
//    private String objectId;
    // the id if it's a node, else: the actual string value
    private String objectStringValue;
    private String predicateStringValue;

    public SugiliteSerializableTriple(String subjectId, String objectId, String objectStringValue, String predicateStringValue){
        this.subjectId = subjectId;
        this.objectId = objectId;
        this.objectStringValue = objectStringValue;
        this.predicateStringValue = predicateStringValue;
    }

    public SugiliteSerializableTriple(SugiliteTriple t){
        this.objectId = "@" + t.getObjectId();
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



    public String getPredicateStringValue() {
        return predicateStringValue;
    }

    public String getSubjectId() {
        return subjectId;
    }

    public String getObjectStringValue() {
        return objectStringValue;
    }

    public String getObjectId() {
        return objectId;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == this){
            return true;
        }
        if(obj instanceof SugiliteSerializableTriple){
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
        return Objects.hash(
                subjectId == null ? null : subjectId,
                predicateStringValue == null ? null : predicateStringValue,
                objectStringValue == null ? null : objectStringValue);
    }


}
