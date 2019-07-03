package edu.cmu.hcii.sugilite.model.operation.unary;

import edu.cmu.hcii.sugilite.ontology.OntologyQuery;

import static edu.cmu.hcii.sugilite.source_parsing.SugiliteScriptExpression.addQuoteToTokenIfNeeded;

/**
 * @author toby
 * @date 11/13/18
 * @time 11:46 PM
 */
public class SugiliteLongClickOperation extends SugiliteUnaryOperation<OntologyQuery> {
    private OntologyQuery targetUIElementDataDescriptionQuery;
    public SugiliteLongClickOperation(){
        super();
        this.setOperationType(LONG_CLICK);
    }

    public void setQuery(OntologyQuery targetUIElementDataDescriptionQuery) {
        setParameter0(targetUIElementDataDescriptionQuery);
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
        return "(" + "call long_click " + addQuoteToTokenIfNeeded(getParameter0().toString()) + ")";
    }

    @Override
    public String getPumiceUserReadableDecription() {
        return String.format("long click on %s", targetUIElementDataDescriptionQuery);
    }
}
