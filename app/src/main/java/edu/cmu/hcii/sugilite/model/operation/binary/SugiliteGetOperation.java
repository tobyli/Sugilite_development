package edu.cmu.hcii.sugilite.model.operation.binary;

import java.io.Serializable;

import edu.cmu.hcii.sugilite.model.operation.binary.SugiliteBinaryOperation;
import edu.cmu.hcii.sugilite.model.value.SugiliteValue;
import edu.cmu.hcii.sugilite.ontology.SerializableOntologyQuery;

import static edu.cmu.hcii.sugilite.source_parsing.SugiliteScriptExpression.addQuoteToTokenIfNeeded;

/**
 * @author toby
 * @date 3/21/18
 * @time 6:19 PM
 */

/**
 * the operation used for reading out a constant
 */
public class SugiliteGetOperation extends SugiliteBinaryOperation<String, String> implements Serializable, SugiliteValue<String> {
    private String name, type;
    public SugiliteGetOperation(){
        super();
        this.setOperationType(GET);
    }
    public String getName(){
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String getParameter0() {
        return name;
    }

    @Override
    public void setParameter0(String value) {
        this.name = value;
    }

    @Override
    public String getParameter1() {
        return type;
    }

    @Override
    public void setParameter1(String value) {
        this.type = value;
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
    public String evaluate() {
        //TODO: this should actually execute the get operation to get the result
        return null;
    }

    @Override
    public String toString() {
        return "(" + "call get " + addQuoteToTokenIfNeeded(getParameter0().toString()) + " " + addQuoteToTokenIfNeeded(getParameter1().toString()) + ")";
    }
}
