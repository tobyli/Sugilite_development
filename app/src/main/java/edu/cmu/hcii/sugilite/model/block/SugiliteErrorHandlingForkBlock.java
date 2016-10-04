package edu.cmu.hcii.sugilite.model.block;

import android.app.AlertDialog;

import java.io.Serializable;

/**
 * @author toby
 * @date 9/1/16
 * @time 12:06 AM
 */
public class SugiliteErrorHandlingForkBlock extends SugiliteBlock implements Serializable {
    SugiliteBlock originalNextBlock, alternativeNextBlock;

    public SugiliteErrorHandlingForkBlock(){
        super();
        this.blockType = SugiliteBlock.CONDITION;
        this.setDescription("ERROR HANDLING FORK");
        this.setScreenshot(null);
    }

    public SugiliteErrorHandlingForkBlock (SugiliteBlock originalNextBlock, SugiliteBlock alternativeNextBlock){
        super();
        this.blockType = SugiliteBlock.CONDITION;
        this.setDescription("ERROR HANDLING FORK");
        this.setScreenshot(null);
        this.originalNextBlock = originalNextBlock;
        this.alternativeNextBlock = alternativeNextBlock;
    }

    public void setOriginalNextBlock(SugiliteBlock block){
        this.originalNextBlock = block;
    }

    public void setAlternativeNextBlock(SugiliteBlock block){
        this.alternativeNextBlock = block;
    }

    public SugiliteBlock getOriginalNextBlock(){
        return originalNextBlock;
    }

    public SugiliteBlock getAlternativeNextBlock(){
        return alternativeNextBlock;
    }
}
