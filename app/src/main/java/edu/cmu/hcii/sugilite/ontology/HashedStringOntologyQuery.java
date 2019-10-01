package edu.cmu.hcii.sugilite.ontology;

import edu.cmu.hcii.sugilite.Const;
import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.sharing.SugiliteScriptSharingHTTPQueryManager;
import edu.cmu.hcii.sugilite.sharing.model.HashedString;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static edu.cmu.hcii.sugilite.sharing.SugiliteSharingScriptPreparer.POTENTIALLY_PRIVATE_RELATIONS;

public class HashedStringOntologyQuery extends OntologyQuery {

    // the relation we want to match
    protected SugiliteRelation r = null;

    // the hashed string we want to match
    protected HashedString hashedString;

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
                    if (hashedString.isServerSalted()) {
                        //need to compare with server results
                        if (hashedString.equals(SugiliteData.getScreenStringSaltedHashMap().get(graph.getPackageName() + graph.getActivityName()+ new HashedString(triple.getObjectStringValue()).toString()))) {
                            return true;
                        }
                    } else {
                        //can be compare locally
                        if (hashedString.equals(new HashedString(triple.getObjectStringValue()))) {
                            return true;
                        }
                    }
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
    @Deprecated
    public static HashedStringOntologyQuery hashLeafOntologyQueryIfPossible(OntologyQuery query) {

        if (query instanceof HashedStringOntologyQuery) return (HashedStringOntologyQuery)query;

        if (query instanceof LeafOntologyQuery) {
            LeafOntologyQuery loq = (LeafOntologyQuery)query;
            HashedStringOntologyQuery result = null;

            // if we can hash this kind of relation
            if (Arrays.stream(POTENTIALLY_PRIVATE_RELATIONS).anyMatch(loq.getR()::equals) && loq.getObject().size() == 1) {
                HashedString hashedString = new HashedString(loq.getObject().toArray(new SugiliteSerializableEntity[1])[0].toString());
                result = new HashedStringOntologyQuery(loq.getR(), hashedString);
            }

            return result;
        }

        return null;
    }

    @Deprecated
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

        if (query instanceof PlaceholderOntologyQuery) {
            PlaceholderOntologyQuery poq = (PlaceholderOntologyQuery)query;
            return new PlaceholderOntologyQuery(hashQuery(poq.getInnerQuery()));
        }

        // non-hashable leaf or already hashed
        return query;
    }
}
