package edu.cmu.hcii.sugilite;

import android.app.Application;

import java.util.ArrayDeque;
import java.util.Queue;

import edu.cmu.hcii.sugilite.model.block.SugiliteBlock;

/**
 * @author toby
 * @date 6/13/16
 * @time 2:02 PM
 */
public class SugiliteData extends Application {
    private SugiliteBlock scriptHead;
    private SugiliteBlock currentScriptBlock;
    private Queue<SugiliteBlock> instructionQueue = new ArrayDeque<>();

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
    public void addInstruction(SugiliteBlock block){
        instructionQueue.add(block);
    }
    public void clearInstructionQueue(){
        instructionQueue.clear();
    }
    public int getInstructionQueueSize(){
        return instructionQueue.size();
    }
    public void removeInstructionQueueItem(){
        instructionQueue.remove();
    }
    public SugiliteBlock peekInstructionQueue(){
        return instructionQueue.peek();
    }
    public SugiliteBlock pollInstructionQueue(){
        return instructionQueue.poll();
    }


}
