package edu.cmu.hcii.sugilite.model.operation.trinary;

import java.io.Serializable;

import edu.cmu.hcii.sugilite.model.operation.trinary.SugiliteTrinaryOperation;
import edu.cmu.hcii.sugilite.ontology.OntologyQuery;

import static edu.cmu.hcii.sugilite.source_parsing.SugiliteScriptExpression.addQuoteToTokenIfNeeded;

/**
 * @author toby
 * @date 8/6/16
 * @time 11:42 PM
 */
public class SugiliteLoadVariableOperation extends SugiliteTrinaryOperation<String, String, OntologyQuery> implements Serializable{
    private String variableName;
    private String propertyToSave;
    private OntologyQuery targetUIElementDataDescriptionQuery;
    public SugiliteLoadVariableOperation(){
        super();
        this.setOperationType(LOAD_AS_VARIABLE);
    }
    public String getVariableName(){
        return variableName;
    }

    public void setVariableName(String variableName){
        this.variableName = variableName;
    }

    public String getPropertyToSave() {
        return propertyToSave;
    }

    public void setPropertyToSave(String propertyToSave) {
        this.propertyToSave = propertyToSave;
    }

    public void setQuery(OntologyQuery targetUIElementDataDescriptionQuery) {
        setParameter2(targetUIElementDataDescriptionQuery);
    }

    @Override
    public String getParameter0() {
        return variableName;
    }

    @Override
    public String getParameter1() {
        return propertyToSave;
    }

    @Override
    public OntologyQuery getParameter2() {
        return targetUIElementDataDescriptionQuery;
    }

    @Override
    public void setParameter0(String value) {
        this.variableName = value;
    }

    @Override
    public void setParameter1(String value) {
        this.propertyToSave = value;
    }

    @Override
    public void setParameter2(OntologyQuery value) {
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
        return "(" + "call load_as_variable " + addQuoteToTokenIfNeeded(getParameter0().toString()) + " " + addQuoteToTokenIfNeeded(getParameter1().toString()) + " " + addQuoteToTokenIfNeeded(getParameter2().toString()) + ")";
    }

    @Override
    public String getPumiceUserReadableDecription() {
        return String.format("set the value of the variable %s to the %s property of %s", variableName, propertyToSave, targetUIElementDataDescriptionQuery.toString());
    }
}
