package edu.cmu.hcii.sugilite.model.operation;

import java.io.Serializable;

/**
 * @author toby
 * @date 3/21/18
 * @time 6:19 PM
 */

/**
 * the operation used for reading out a constant
 */
public class SugiliteReadoutConstOperation extends SugiliteBinaryOperation implements Serializable {
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
    public String getParameter1() {
        return textToReadout;
    }

    @Override
    public void setParameter1(String value) {
        this.textToReadout = value;
    }
}
