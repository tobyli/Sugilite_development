package edu.cmu.hcii.sugilite.model.operation;

import edu.cmu.hcii.sugilite.model.block.SugiliteBlock;

/**
 * @author toby
 * @date 6/10/16
 * @time 2:23 PM
 */
public class SugiliteSetTextOperation extends SugiliteOperation {
    private String text;
    public String getText(){
        return text;
    }
    public void setText(String text){
        this.text = text;
    }
}
