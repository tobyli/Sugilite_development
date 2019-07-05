package edu.cmu.hcii.sugilite.model.operation.unary;

import java.io.Serializable;

import edu.cmu.hcii.sugilite.model.operation.binary.SugiliteBinaryOperation;
import edu.cmu.hcii.sugilite.ontology.OntologyQuery;

import static edu.cmu.hcii.sugilite.source_parsing.SugiliteScriptExpression.addQuoteToTokenIfNeeded;

/**
 * @author toby
 * @date 3/21/18
 * @time 6:19 PM
 */

/**
 * the operation used for reading out a constant
 */
public class SugiliteReadoutConstOperation extends SugiliteUnaryOperation<String> implements Serializable {
    private String textToReadout;
    public SugiliteReadoutConstOperation(){
        super();
        this.setOperationType(READOUT_CONST);
    }
    public String getTextToReadout(){
        return textToReadout;
    }

    public void setTextToReadout(String textToReadout) {
        this.textToReadout = textToReadout;
    }

    @Override
    public String getParameter0() {
        return textToReadout;
    }

    @Override
    public void setParameter0(String value) {
        this.textToReadout = value;
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
    public String toString() {
        return "(" + "call read_out " + addQuoteToTokenIfNeeded(getParameter0().toString()) + ")";
    }

    @Override
    public String getPumiceUserReadableDecription() {
        return String.format("read out \"%s\"", textToReadout);
    }
}
