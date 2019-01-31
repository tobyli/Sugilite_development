package edu.cmu.hcii.sugilite.model.block;

import java.io.File;
import java.io.Serializable;
import java.util.Calendar;
import java.util.Random;

import edu.cmu.hcii.sugilite.SugiliteData;///
/**
 * @author toby
 * @date 6/10/16
 * @time 2:02 PM
 */
public abstract class SugiliteBlock implements Serializable{
    private static final long serialVersionUID = 6174630121022335205L;

    public SugiliteBlock(){
        Random rand = new Random();
        blockId = rand.nextInt(Integer.MAX_VALUE);
        createdTime = Calendar.getInstance().getTimeInMillis();
    }
    public int blockType;
    public boolean inScope = false;
    public static int REGULAR_OPERATION = 1, CONDITION = 2, FOR_EACH_LOOP = 3, RETURN_VALUE = 4, END_BLOCK = 5, STARTING_BLOCK = 6, SPECIAL_OPERATION = 8;
    //each block can only have 1 previous block

    SugiliteBlock previousBlock;

    //for storing e.g., the parent condition block
    SugiliteBlock parentBlock;


    private String description;
    private File screenshot;
    private int blockId;
    private long createdTime;
    SugiliteBlock nextBlock;
    public long getCreatedTime(){
        return createdTime;
    }
    public int getBlockId(){
        return blockId;
    }
    public void setDescription(String description){
        this.description = description;
    }
    public String getDescription() {
        return description;
    }
    public SugiliteBlock getPreviousBlock(){
        return previousBlock;
    }
    public void setPreviousBlock (SugiliteBlock block){
        this.previousBlock = block;
    }
    public void setScreenshot (File screenshot){
        this.screenshot = screenshot;
    }

    public void setParentBlock(SugiliteBlock parentBlock) {
        this.parentBlock = parentBlock;
    }

    public SugiliteBlock getParentBlock() {
        return parentBlock;
    }

    public File getScreenshot(){
        return screenshot;
    }

    public SugiliteBlock getNextBlock() {
        if(nextBlock == null && parentBlock != null){
            //handle the "merge" of condition blocks
            return parentBlock.getNextBlock();
        }
        return nextBlock;
    }

    public void setNextBlock(SugiliteBlock nextBlock) {
        this.nextBlock = nextBlock;
    }

    //each "run" method should execute the task wrapped in the block, and call the "run" method of the next block
}
