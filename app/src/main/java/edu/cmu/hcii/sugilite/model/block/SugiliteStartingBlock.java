package edu.cmu.hcii.sugilite.model.block;

import java.util.ArrayList;

/**
 * @author toby
 * @date 6/13/16
 * @time 1:48 PM
 */
public class SugiliteStartingBlock extends SugiliteBlock {
    private SugiliteBlock nextBlock;
    private String scriptName;
    public SugiliteStartingBlock(){
        super();
        this.blockType = SugiliteBlock.REGULAR_OPERATION;
        this.setDescription("Starting Block");
    }
    public SugiliteStartingBlock(String scriptName){
        super();
        this.scriptName = scriptName;
        this.blockType = SugiliteBlock.REGULAR_OPERATION;
        this.setDescription("Starting Block");
    }
    public SugiliteBlock getNextBlock(){
        return nextBlock;
    }
    public void setNextBlock(SugiliteBlock sugiliteBlock){
        this.nextBlock = sugiliteBlock;
    }
    public String getScriptName(){
        return scriptName;
    }
    @Override
    public boolean run() throws Exception {
        if (nextBlock == null)
            throw new Exception("null next block!");

        else
            return nextBlock.run();
    }



}
