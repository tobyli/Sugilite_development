package edu.cmu.hcii.sugilite.ontology;

import java.io.Serializable;
import java.util.Objects;

/**
 * @author nancy
 * @date 10/25/17
 * @time 10:42 PM
 */
public class SugiliteSerializableTriple implements Serializable{
    private SugiliteSerializableEntity subject;
    private SugiliteRelation predicate;
    private SugiliteSerializableEntity object;

    public SugiliteSerializableTriple(SugiliteTriple t){
        this.subject = new SugiliteSerializableEntity(t.getSubject());
        this.predicate = t.getPredicate();
        this.object = new SugiliteSerializableEntity(t.getObject());
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == this){
            return true;
        }
        if(obj instanceof SugiliteSerializableTriple){
            return ((SugiliteSerializableTriple) obj).subject.equals(this.subject) &&
                    ((SugiliteSerializableTriple) obj).predicate.equals(this.predicate) &&
                    ((SugiliteSerializableTriple) obj).object.equals(this.object);
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
