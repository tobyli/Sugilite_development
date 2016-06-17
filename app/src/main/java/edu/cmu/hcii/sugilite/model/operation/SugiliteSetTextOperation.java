package edu.cmu.hcii.sugilite.model.operation;

import java.io.Serializable;

import edu.cmu.hcii.sugilite.model.block.SugiliteBlock;

/**
 * @author toby
 * @date 6/10/16
 * @time 2:23 PM
 */
public class SugiliteSetTextOperation extends SugiliteOperation implements Serializable {
    private String text;
    public SugiliteSetTextOperation(){
        this.setOperationType(SET_TEXT);
    }
    public String getText(){
        return text;
    }
    public void setText(String text){
        this.text = text;
    }
}
