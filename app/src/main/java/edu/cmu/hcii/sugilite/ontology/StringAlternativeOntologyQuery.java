package edu.cmu.hcii.sugilite.ontology;

import edu.cmu.hcii.sugilite.sharing.StringAlternativeGenerator;
import edu.cmu.hcii.sugilite.sharing.model.HashedString;
import edu.cmu.hcii.sugilite.source_parsing.SugiliteScriptExpression;

import java.util.Map;
import java.util.Set;

public class StringAlternativeOntologyQuery extends OntologyQuery {
    protected SugiliteRelation r = null;
    private StringAlternativeGenerator.StringAlternative alt;

    public StringAlternativeOntologyQuery(SugiliteRelation r, StringAlternativeGenerator.StringAlternative alt) {
        this.r = r;
        this.alt = alt;
    }


    @Override
    public boolean overallQueryFunction(SugiliteEntity currNode, UISnapshot graph) {
        Set<SugiliteTriple> sugiliteTriples = graph.getSubjectTriplesMap().get(currNode.getEntityId());
        if (sugiliteTriples != null) {
            for (SugiliteTriple triple : sugiliteTriples) {
                if (triple.getPredicate().equals(r)) {
                    Set<StringAlternativeGenerator.StringAlternative> alts = StringAlternativeGenerator.generateAlternatives(triple.getObjectStringValue());
                    for (StringAlternativeGenerator.StringAlternative a : alts) {
                        // TODO maybe also check if priority is equal?
                        if (a.altText.equals(this.alt.altText)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public SugiliteRelation getR() {
        return r;
    }

    @Override
    public String toString() {
        // TODO add to parser
        return "(patternMatch " + r.getRelationName() + " " + SugiliteScriptExpression.addQuoteToTokenIfNeeded(alt.altText) + ")";
    }

    @Override
    public OntologyQuery clone() {
        return new StringAlternativeOntologyQuery(r, alt.clone());
    }
}
