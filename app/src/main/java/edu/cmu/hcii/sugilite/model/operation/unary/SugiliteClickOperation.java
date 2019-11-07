package edu.cmu.hcii.sugilite.model.operation.unary;

import edu.cmu.hcii.sugilite.ontology.OntologyQuery;

import static edu.cmu.hcii.sugilite.source_parsing.SugiliteScriptExpression.addQuoteToTokenIfNeeded;

/**
 * @author toby
 * @date 11/13/18
 * @time 11:46 PM
 */
public class SugiliteClickOperation extends SugiliteUnaryOperation<OntologyQuery> {
    private OntologyQuery targetUIElementDataDescriptionQuery;
    private OntologyQuery alternativeTargetUIElementDataDescriptionQuery;
    public SugiliteClickOperation(){
        super();
        this.setOperationType(CLICK);
    }

    public void setQuery(OntologyQuery targetUIElementDataDescriptionQuery) {
        setParameter0(targetUIElementDataDescriptionQuery);
    }

    public void setAlternativeTargetUIElementDataDescriptionQuery(OntologyQuery alternativeTargetUIElementDataDescriptionQuery) {
        this.alternativeTargetUIElementDataDescriptionQuery = alternativeTargetUIElementDataDescriptionQuery;
    }

    public OntologyQuery getAlternativeTargetUIElementDataDescriptionQuery() {
        return alternativeTargetUIElementDataDescriptionQuery;
    }

    @Override
    public OntologyQuery getParameter0() {
        return targetUIElementDataDescriptionQuery;
    }

    @Override
    public void setParameter0(OntologyQuery value) {
        this.targetUIElementDataDescriptionQuery = value;
    }

    @Override
    public boolean containsDataDescriptionQuery() {
        return true;
    }

    @Override
    public OntologyQuery getDataDescriptionQueryIfAvailable() {
        return targetUIElementDataDescriptionQuery;
    }

    @Override
    public String toString() {
        return "(" + "call click " + addQuoteToTokenIfNeeded(getParameter0().toString()) + ")";
    }

    @Override
    public String getPumiceUserReadableDecription() {
        return String.format("click on %s", targetUIElementDataDescriptionQuery);
    }
}
