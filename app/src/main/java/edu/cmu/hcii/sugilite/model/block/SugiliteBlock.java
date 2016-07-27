package edu.cmu.hcii.sugilite.model.block;

import java.io.File;
import java.io.Serializable;
import java.util.Calendar;
import java.util.Random;


/**
 * @author toby
 * @date 6/10/16
 * @time 2:02 PM
 */
public abstract class SugiliteBlock implements Serializable{
    public SugiliteBlock(){
        Random rand = new Random();
        blockId = rand.nextInt(65535);
        createdTime = Calendar.getInstance().getTimeInMillis();
    }
    public int blockType;
    public static int REGULAR_OPERATION = 1, IF_CONDITION = 2, FOR_EACH_LOOP = 3, RETURN_VALUE = 4, END_BLOCK = 5, STARTING_BLOCK = 6;
    //each block can only have 1 previous block
    SugiliteBlock previousBlock;
    private String description;
    private File screenshot;
    private int blockId;
    private long createdTime;
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
    public File getScreenshot(){
        return screenshot;
    }
    //each "run" method should execute the task wrapped in the block, and call the "run" method of the next block
    public abstract boolean run() throws Exception;
}
