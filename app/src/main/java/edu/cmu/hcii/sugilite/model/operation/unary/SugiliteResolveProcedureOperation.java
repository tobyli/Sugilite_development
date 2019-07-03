package edu.cmu.hcii.sugilite.model.operation.unary;

import edu.cmu.hcii.sugilite.ontology.OntologyQuery;

import static edu.cmu.hcii.sugilite.source_parsing.SugiliteScriptExpression.addQuoteToTokenIfNeeded;

/**
 * @author toby
 * @date 11/13/18
 * @time 11:46 PM
 */
public class SugiliteResolveProcedureOperation extends SugiliteUnaryOperation<String> {
    private String text;
    public SugiliteResolveProcedureOperation(){
        super();
        this.setOperationType(RESOLVE_PROCEDURE);
    }

    @Override
    public String getParameter0() {
        return text;
    }

    @Override
    public void setParameter0(String value) {
        this.text = value;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public boolean containsDataDescriptionQuery() {
        return false;
    }

    @Override
    public OntologyQuery getDataDescriptionQueryIfAvailable() {
        return null;
    }

    @Override
    public String toString() {
        return "(" + "call resolve_procedure " + addQuoteToTokenIfNeeded(getParameter0().toString()) + ")";
    }

    @Override
    public String getPumiceUserReadableDecription() {
        return String.format("perform an action named \"%s\"", text);
    }
}
