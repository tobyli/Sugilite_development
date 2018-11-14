package edu.cmu.hcii.sugilite.model.operation.binary;

import java.io.Serializable;

import edu.cmu.hcii.sugilite.ontology.SerializableOntologyQuery;

/**
 * @author toby
 * @date 2/25/18
 * @time 10:49 PM
 */
public class SugiliteReadoutOperation extends SugiliteBinaryOperation<String, SerializableOntologyQuery> implements Serializable {
    private String propertyToReadout;
    private SerializableOntologyQuery targetUIElementDataDescriptionQuery;
    public SugiliteReadoutOperation(){
        super();
        this.setOperationType(READ_OUT);
    }
    public String getPropertyToReadout(){
        return propertyToReadout;
    }
    public void setPropertyToReadout(String propertyToReadout){
        this.propertyToReadout = propertyToReadout;
    }

    public void setQuery(SerializableOntologyQuery targetUIElementDataDescriptionQuery) {
        this.targetUIElementDataDescriptionQuery = targetUIElementDataDescriptionQuery;
    }

    @Override
    public String getParameter0() {
        return propertyToReadout;
    }

    @Override
    public void setParameter0(String value) {
        this.propertyToReadout = value;
    }


    @Override
    public SerializableOntologyQuery getParameter1() {
        return targetUIElementDataDescriptionQuery;
    }

    @Override
    public void setParameter1(SerializableOntologyQuery value) {
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
