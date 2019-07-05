package edu.cmu.hcii.sugilite.model.operation.binary;

import java.io.Serializable;

import edu.cmu.hcii.sugilite.ontology.OntologyQuery;

import static edu.cmu.hcii.sugilite.source_parsing.SugiliteScriptExpression.addQuoteToTokenIfNeeded;

/**
 * @author toby
 * @date 6/10/16
 * @time 2:23 PM
 */
public class SugiliteSetTextOperation extends SugiliteBinaryOperation<String, OntologyQuery> implements Serializable {
    private String text;
    private OntologyQuery targetUIElementDataDescriptionQuery;
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

    public void setQuery(OntologyQuery targetUIElementDataDescriptionQuery) {
        setParameter1(targetUIElementDataDescriptionQuery);
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
    public OntologyQuery getParameter1() {
        return targetUIElementDataDescriptionQuery;
    }

    @Override
    public void setParameter1(OntologyQuery value) {
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
        return "(" + "call set_text " + addQuoteToTokenIfNeeded(getParameter0().toString()) + " " + addQuoteToTokenIfNeeded(getParameter1().toString()) + ")";
    }

    @Override
    public String getPumiceUserReadableDecription() {
        return String.format("set the text of %s to %s", targetUIElementDataDescriptionQuery.toString(), text);
    }
}

