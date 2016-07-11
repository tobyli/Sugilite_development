package edu.cmu.hcii.sugilite.model.block;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * @author toby
 * @date 6/13/16
 * @time 1:48 PM
 */
public class SugiliteStartingBlock extends SugiliteBlock implements Serializable {
    private SugiliteBlock nextBlock;
    private String scriptName;
    public Set<String> relevantPackages;
    public SugiliteStartingBlock(){
        super();
        relevantPackages = new HashSet<>();
        this.blockType = SugiliteBlock.STARTING_BLOCK;
        this.setDescription("<b>STARTING SCRIPT</b>");
    }
    public SugiliteStartingBlock(String scriptName){
        super();
        relevantPackages = new HashSet<>();
        this.scriptName = scriptName;
        this.blockType = SugiliteBlock.STARTING_BLOCK;
        this.setDescription("<b>STARTING SCRIPT</b>");
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

    public SugiliteBlock getTail(){
        SugiliteBlock currentBlock = this;
        while(true){
            if(currentBlock instanceof SugiliteStartingBlock){
                if(((SugiliteStartingBlock) currentBlock).getNextBlock() == null)
                    return currentBlock;
                else
                    currentBlock = ((SugiliteStartingBlock) currentBlock).getNextBlock();
            }
            else if(currentBlock instanceof SugiliteOperationBlock){
                if(((SugiliteOperationBlock) currentBlock).getNextBlock() == null)
                    return currentBlock;
                else
                    currentBlock = ((SugiliteOperationBlock) currentBlock).getNextBlock();
            }
        }
    }



}
