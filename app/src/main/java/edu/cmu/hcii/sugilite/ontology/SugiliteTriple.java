package edu.cmu.hcii.sugilite.ontology;

import java.util.Objects;

/**
 * @author toby
 * @date 9/25/17
 * @time 5:58 PM
 */
public class SugiliteTriple {
    private SugiliteEntity subject;
    private SugiliteRelation predicate;
    private SugiliteEntity object;

    public SugiliteTriple(SugiliteEntity subject, SugiliteRelation predicate, SugiliteEntity object){
        this.subject = subject;
        this.predicate = predicate;
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
