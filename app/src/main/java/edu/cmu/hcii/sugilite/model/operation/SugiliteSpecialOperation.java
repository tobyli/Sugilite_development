package edu.cmu.hcii.sugilite.model.operation;

import edu.cmu.hcii.sugilite.ontology.SerializableOntologyQuery;

/**
 * @author toby
 * @date 11/14/18
 * @time 12:23 AM
 */
public class SugiliteSpecialOperation extends SugiliteOperation {
    public SugiliteSpecialOperation (int operationType) {
        super(operationType);
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
