package edu.cmu.hcii.sugilite.model.operation.unary;

import edu.cmu.hcii.sugilite.ontology.SerializableOntologyQuery;

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
    public SerializableOntologyQuery getDataDescriptionQueryIfAvailable() {
        return null;
    }
}
