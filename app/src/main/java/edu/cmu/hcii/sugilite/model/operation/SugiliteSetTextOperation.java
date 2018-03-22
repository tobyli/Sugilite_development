package edu.cmu.hcii.sugilite.model.operation;

import java.io.Serializable;

import edu.cmu.hcii.sugilite.model.block.SugiliteBlock;

/**
 * @author toby
 * @date 6/10/16
 * @time 2:23 PM
 */
public class SugiliteSetTextOperation extends SugiliteBinaryOperation implements Serializable {
    private String text;
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

    @Override
    public String getParameter1() {
        return text;
    }
    @Override
    public void setParameter1(String value) {
        this.text = value;
    }
}

