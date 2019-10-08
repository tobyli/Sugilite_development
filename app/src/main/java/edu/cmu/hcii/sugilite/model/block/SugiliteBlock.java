package edu.cmu.hcii.sugilite.model.block;

import android.text.Html;
import android.text.SpannableString;
import android.text.Spanned;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

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
    protected SugiliteBlock previousBlock;

    //for storing e.g., the parent condition block
    protected SugiliteBlock parentBlock;

    //not used for e.g. SugiliteConditionBlock
    protected SugiliteBlock nextBlock;


    private transient Spanned description;
    private String plainDescription;
    private File screenshot;
    private int blockId;
    private long createdTime;

    public long getCreatedTime(){
        return createdTime;
    }
    public int getBlockId(){
        return blockId;
    }
    public void setDescription(Spanned description){
        this.description = description;
        this.plainDescription = description.toString();
    }
    @Deprecated
    public void setDescription(String htmlDescription){
        this.description = Html.fromHtml(htmlDescription);
        this.plainDescription = Html.fromHtml(htmlDescription).toString();
    }
    public Spanned getDescription() {
        return description;
    }

    public String getPlainDescription() {
        return plainDescription;
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
        return nextBlock;
    }


    public SugiliteBlock getNextBlockToRun() {
        if(nextBlock == null){
            if (parentBlock != null) {
                //handle the "merge" of condition blocks - if the currentBlock has no nextBlock, and has a parentBlock, should return the nextBlock of parentBlock
                return parentBlock.getNextBlockToRun();
            } else {
                return null;
            }
        } else {
            return nextBlock;
        }
    }

    public void setNextBlock(SugiliteBlock nextBlock) {
        this.nextBlock = nextBlock;
    }

    public List<SugiliteBlock> getFollowingBlocks() {
        List<SugiliteBlock> result = new ArrayList<SugiliteBlock>();
        if (nextBlock != null) {
            result.add(nextBlock);
            result.addAll(nextBlock.getFollowingBlocks());
        }
        return result;
    }


    public abstract String getPumiceUserReadableDecription();

    //each "run" method should execute the task wrapped in the block, and call the "run" method of the next block
}
