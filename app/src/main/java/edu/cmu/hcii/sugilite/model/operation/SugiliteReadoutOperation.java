package edu.cmu.hcii.sugilite.model.operation;

import java.io.Serializable;

/**
 * @author toby
 * @date 2/25/18
 * @time 10:49 PM
 */
public class SugiliteReadoutOperation extends SugiliteBinaryOperation implements Serializable {
    private String propertyToReadout;
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

    @Override
    public String getParameter1() {
        return propertyToReadout;
    }

    @Override
    public void setParameter1(String value) {
        this.propertyToReadout = value;
    }
}
