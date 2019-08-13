package edu.cmu.hcii.sugilite.ontology;

import edu.cmu.hcii.sugilite.sharing.StringAlternativeGenerator;
import edu.cmu.hcii.sugilite.source_parsing.SugiliteScriptExpression;

import java.util.Set;

public class StringAlternativeOntologyQuery extends OntologyQuery {
    protected SugiliteRelation r = null;
    private StringAlternativeGenerator.StringAlternative alt;

    public StringAlternativeOntologyQuery(SugiliteRelation r, StringAlternativeGenerator.StringAlternative alt) {
        this.r = r;
        this.alt = alt;
    }

    @Override
    protected boolean overallQueryFunction(SugiliteSerializableEntity currNode, SerializableUISnapshot graph) {
        Set<SugiliteSerializableTriple> sugiliteTriples = graph.getSubjectTriplesMap().get(currNode.toString());
        if (sugiliteTriples != null) {
            for (SugiliteSerializableTriple triple : sugiliteTriples) {
                if (triple.getPredicateStringValue().equals(r.getRelationName())) {
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

    @Override
    protected boolean overallQueryFunction(SugiliteEntity currNode, UISnapshot graph) {
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
