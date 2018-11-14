package edu.cmu.hcii.sugilite.model.operation.binary;

import java.io.Serializable;

import edu.cmu.hcii.sugilite.ontology.SerializableOntologyQuery;

/**
 * @author toby
 * @date 6/10/16
 * @time 2:23 PM
 */
public class SugiliteSetTextOperation extends SugiliteBinaryOperation<String, SerializableOntologyQuery> implements Serializable {
    private String text;
    private SerializableOntologyQuery targetUIElementDataDescriptionQuery;
    public SugiliteSetTextOperation(){
        super();
        this.setOperationType(SET_TEXT);
    }
    public String getText(){
        return text;
    }
    public void setText(String text){
        this.text = text;
    }

    public void setQuery(SerializableOntologyQuery targetUIElementDataDescriptionQuery) {
        this.targetUIElementDataDescriptionQuery = targetUIElementDataDescriptionQuery;
    }

    @Override
    public String getParameter0() {
        return text;
    }
    @Override
    public void setParameter0(String value) {
        this.text = value;
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

