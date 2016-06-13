package edu.cmu.hcii.sugilite;

import android.app.Application;

import edu.cmu.hcii.sugilite.model.block.SugiliteBlock;

/**
 * @author toby
 * @date 6/13/16
 * @time 2:02 PM
 */
public class SugiliteData extends Application {
    private SugiliteBlock scriptHead;
    private SugiliteBlock currentScriptBlock;

    public SugiliteBlock getScriptHead(){
        return scriptHead;
    }
    public SugiliteBlock getCurrentScriptBlock(){
        return currentScriptBlock;
    }
    void setScriptHead(SugiliteBlock scriptHead){
        this.scriptHead = scriptHead;
    }
    void setCurrentScriptBlock(SugiliteBlock currentScriptBlock){
        this.currentScriptBlock = currentScriptBlock;
    }

}
