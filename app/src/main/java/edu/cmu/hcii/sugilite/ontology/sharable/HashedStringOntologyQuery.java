package edu.cmu.hcii.sugilite.ontology.sharable;

import edu.cmu.hcii.sugilite.ontology.*;

import java.util.Arrays;
import java.util.Set;

public class HashedStringOntologyQuery extends OntologyQuery {

    // SugiliteRelations we can translate
    private static final SugiliteRelation[] USABLE_RELATIONS = {
            SugiliteRelation.HAS_TEXT,
            SugiliteRelation.HAS_CHILD_TEXT
    };

    // the relation we want to match
    protected SugiliteRelation r = null;

    // the hashed string we want to match
    private HashedString hashedString;

    public HashedStringOntologyQuery(SugiliteRelation r, HashedString hashedString) {
        this.r = r;
        this.hashedString = hashedString;
    }

    @Override
    protected boolean overallQueryFunction(SugiliteEntity currNode, UISnapshot graph) {
        Set<SugiliteTriple> sugiliteTriples = graph.getSubjectTriplesMap().get(currNode.getEntityId());
        if (sugiliteTriples != null) {
            for (SugiliteTriple triple : sugiliteTriples) {
                if (triple.getPredicate().equals(r)) {
                    System.out.println(triple.getObjectStringValue() + " " + new HashedString(triple.getObjectStringValue()));
                }
                if (triple.getPredicate().equals(r) && hashedString.equals(new HashedString(triple.getObjectStringValue()))) {
                    System.out.println(triple.getObjectStringValue()); // TODO debug only; remove later
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return "(privateMatch " + r.getRelationName() + " " + hashedString + ")";
    }

    @Override
    public OntologyQuery clone() {
        return new HashedStringOntologyQuery(r, hashedString);
    }

    public SugiliteRelation getR() {
        return r;
    }

    public void setR(SugiliteRelation r) {
        this.r = r;
    }

    /**
     * Hashes a LeafOntologyQuery if possible or returns null otherwise.
     * @param query
     * @return the equivalent HashedStringOntologyQuery if an equivalent could be made, or null otherwise.
     */
    public static HashedStringOntologyQuery hashLeafOntologyQueryIfPossible(OntologyQuery query) {

        if (query instanceof HashedStringOntologyQuery) return (HashedStringOntologyQuery)query;

        if (query instanceof LeafOntologyQuery) {
            LeafOntologyQuery loq = (LeafOntologyQuery)query;
            HashedStringOntologyQuery result = null;

            // if we can hash this kind of relation
            if (Arrays.stream(USABLE_RELATIONS).anyMatch(loq.getR()::equals) && loq.getObject().size() == 1) {
                HashedString hashedString = new HashedString(loq.getObject().toArray(new SugiliteSerializableEntity[1])[0].toString());
                result = new HashedStringOntologyQuery(loq.getR(), hashedString);
            }

            return result;
        }

        return null;
    }

    public static OntologyQuery hashQuery(OntologyQuery query) {
        OntologyQuery leafHash = hashLeafOntologyQueryIfPossible(query);
        if (leafHash != null) return leafHash;

        if (query instanceof CombinedOntologyQuery) {
            CombinedOntologyQuery coq = (CombinedOntologyQuery)query;
            CombinedOntologyQuery result = new CombinedOntologyQuery(coq.getSubRelation());
            for (OntologyQuery subQ : coq.getSubQueries()) {
                result.addSubQuery(hashQuery(subQ));
            }
            return result;
        }

        // non-hashable leaf or already hashed
        return query;
    }
}
