package edu.cmu.hcii.sugilite.model.operation.unary;

import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.model.value.SugiliteValue;
import edu.cmu.hcii.sugilite.ontology.OntologyQuery;

import static edu.cmu.hcii.sugilite.source_parsing.SugiliteScriptExpression.addQuoteToTokenIfNeeded;

/**
 * @author toby
 * @date 11/13/18
 * @time 11:46 PM
 */
public class SugiliteResolveBoolExpOperation extends SugiliteUnaryOperation<String> implements SugiliteValue<Boolean> {
    private String text;
    public SugiliteResolveBoolExpOperation(){
        super();
        this.setOperationType(RESOLVE_BOOLEXP);
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
    public Boolean evaluate(SugiliteData sugiliteData) {
        //the evaluation of Resolve() functions are handled by the parser
        return null;
    }

    @Override
    public String toString() {
        return "(" + "call resolve_boolExp " + addQuoteToTokenIfNeeded(getParameter0().toString()) + ")";
    }

    @Override
    public String getReadableDescription() {
        return getPumiceUserReadableDecription();
    }

    @Override
    public String getPumiceUserReadableDecription() {
        return String.format("a condition named \"%s\" is true", text);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj != null && obj instanceof SugiliteValue) {
            return this.toString().equals(obj.toString());
        } else {
            return super.equals(obj);
        }
    }

    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }
}
