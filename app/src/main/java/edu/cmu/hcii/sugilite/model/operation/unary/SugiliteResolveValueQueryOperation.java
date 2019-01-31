package edu.cmu.hcii.sugilite.model.operation.unary;

import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.model.value.SugiliteValue;
import edu.cmu.hcii.sugilite.ontology.SerializableOntologyQuery;

import static edu.cmu.hcii.sugilite.source_parsing.SugiliteScriptExpression.addQuoteToTokenIfNeeded;

/**
 * @author toby
 * @date 11/13/18
 * @time 11:46 PM
 */
public class SugiliteResolveValueQueryOperation extends SugiliteUnaryOperation<String> implements SugiliteValue<String> {
    private String text;
    public SugiliteResolveValueQueryOperation(){
        super();
        this.setOperationType(RESOLVE_VALUEQUERY);
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
    public SerializableOntologyQuery getDataDescriptionQueryIfAvailable() {
        return null;
    }

    @Override
    public String evaluate(SugiliteData sugiliteData) {
        //TODO: this should actually execute the query to get the result
        return null;
    }

    @Override
    public String toString() {
        return "(" + "call resolve_value " + addQuoteToTokenIfNeeded(getParameter0().toString()) + ")";
    }

    @Override
    public String getReadableDescription() {
        return text;
    }
}
