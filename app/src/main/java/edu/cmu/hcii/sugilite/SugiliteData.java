package edu.cmu.hcii.sugilite;

import android.app.Application;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

import edu.cmu.hcii.sugilite.model.block.SugiliteBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;
import edu.cmu.hcii.sugilite.model.variable.Variable;

/**
 * @author toby
 * @date 6/13/16
 * @time 2:02 PM
 */
public class SugiliteData extends Application {
    //used to store the current active script
    private SugiliteStartingBlock scriptHead;
    private SugiliteBlock currentScriptBlock;
    private Queue<SugiliteBlock> instructionQueue = new ArrayDeque<>();
    public Map<String, Variable> stringVariableMap = new HashMap<>();
    //true if the current recording script is initiated externally
    public boolean initiatedExternally  = false;

    public SugiliteStartingBlock getScriptHead(){
        return scriptHead;
    }
    public SugiliteBlock getCurrentScriptBlock(){
        return currentScriptBlock;
    }
    public void setScriptHead(SugiliteStartingBlock scriptHead){
        this.scriptHead = scriptHead;
    }

    /**
     * set the script head to a new SugiliteStartingBlock with name = scriptName, and set the current script block to that block
     * @param scriptName
     */
    public void initiateScript(String scriptName){
        this.setScriptHead(new SugiliteStartingBlock(scriptName));
        this.setCurrentScriptBlock(scriptHead);
    }
    public void setCurrentScriptBlock(SugiliteBlock currentScriptBlock){
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
