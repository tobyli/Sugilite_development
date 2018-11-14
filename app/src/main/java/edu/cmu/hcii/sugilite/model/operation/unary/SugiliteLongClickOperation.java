package edu.cmu.hcii.sugilite.model.operation.unary;

import edu.cmu.hcii.sugilite.ontology.SerializableOntologyQuery;

/**
 * @author toby
 * @date 11/13/18
 * @time 11:46 PM
 */
public class SugiliteLongClickOperation extends SugiliteUnaryOperation<SerializableOntologyQuery> {
    private SerializableOntologyQuery targetUIElementDataDescriptionQuery;
    public SugiliteLongClickOperation(){
        super();
        this.setOperationType(LONG_CLICK);
    }

    public void setQuery(SerializableOntologyQuery targetUIElementDataDescriptionQuery) {
        this.targetUIElementDataDescriptionQuery = targetUIElementDataDescriptionQuery;
    }

    @Override
    public SerializableOntologyQuery getParameter0() {
        return targetUIElementDataDescriptionQuery;
    }

    @Override
    public void setParameter0(SerializableOntologyQuery value) {
        this.targetUIElementDataDescriptionQuery = value;
    }

    @Override
    public boolean containsDataDescriptionQuery() {
        return true;
    }

    @Override
    public SerializableOntologyQuery getDataDescriptionQueryIfAvailable() {
        return targetUIElementDataDescriptionQuery;
    }
}
