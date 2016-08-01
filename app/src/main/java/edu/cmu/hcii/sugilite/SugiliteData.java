package edu.cmu.hcii.sugilite;

import android.app.Application;
import android.content.Intent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import edu.cmu.hcii.sugilite.automation.ErrorHandler;
import edu.cmu.hcii.sugilite.communication.SugiliteCommunicationController;
import edu.cmu.hcii.sugilite.model.block.SugiliteBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteOperationBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;
import edu.cmu.hcii.sugilite.model.variable.Variable;

/**
 * @author toby
 * @date 6/13/16
 * @time 2:02 PM
 */
public class SugiliteData extends Application {
    //used to store the current active script
    private SugiliteStartingBlock scriptHead, trackingHead;
    private SugiliteBlock currentScriptBlock, currentTrackingBlock;
    private Queue<SugiliteBlock> instructionQueue = new ArrayDeque<>();
    public Map<String, Variable> stringVariableMap = new HashMap<>();
    //true if the current recording script is initiated externally
    public boolean initiatedExternally  = false;
    public SugiliteCommunicationController communicationController;
    public ErrorHandler errorHandler = null;
    public String trackingName = "default";


    public SugiliteStartingBlock getScriptHead(){
        return scriptHead;
    }
    public SugiliteStartingBlock getTrackingHead(){
        return trackingHead;
    }
    public SugiliteBlock getCurrentScriptBlock(){
        return currentScriptBlock;
    }
    public SugiliteBlock getCurrentTrackingBlock(){
        return currentTrackingBlock;
    }
    public void setScriptHead(SugiliteStartingBlock scriptHead){
        this.scriptHead = scriptHead;
    }
    public void setTrackingHead(SugiliteStartingBlock trackingHead){
        this.trackingHead = trackingHead;
    }

    /**
     * set the script head to a new SugiliteStartingBlock with name = scriptName, and set the current script block to that block
     * @param scriptName
     */
    public void initiateScript(String scriptName){
        this.instructionQueue.clear();
        this.stringVariableMap.clear();
        this.setScriptHead(new SugiliteStartingBlock(scriptName));
        this.setCurrentScriptBlock(scriptHead);
    }

    public void initiateTracking(String trackingName){
        this.setTrackingHead(new SugiliteStartingBlock(trackingName));
        this.setCurrentTrackingBlock(trackingHead);
        this.trackingName = trackingName;
    }

    public void runScript(SugiliteStartingBlock startingBlock){
        this.instructionQueue.clear();
        errorHandler.relevantPackages.clear();
        errorHandler.relevantPackages.addAll(startingBlock.relevantPackages);
        errorHandler.reportSuccess(Calendar.getInstance().getTimeInMillis());
        List<SugiliteBlock> blocks = traverseBlock(startingBlock);
        for(SugiliteBlock block : blocks){
            addInstruction(block);
        }
    }

    public void setCurrentScriptBlock(SugiliteBlock currentScriptBlock){
        this.currentScriptBlock = currentScriptBlock;
    }
    public void setCurrentTrackingBlock(SugiliteBlock currentTrackingBlock){
        this.currentTrackingBlock = currentTrackingBlock;
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

    private List<SugiliteBlock> traverseBlock(SugiliteStartingBlock startingBlock){
        List<SugiliteBlock> sugiliteBlocks = new ArrayList<>();
        SugiliteBlock currentBlock = startingBlock;
        while(currentBlock != null){
            sugiliteBlocks.add(currentBlock);
            if(currentBlock instanceof SugiliteStartingBlock){
                currentBlock = ((SugiliteStartingBlock)currentBlock).getNextBlock();
            }
            else if (currentBlock instanceof SugiliteOperationBlock){
                currentBlock = ((SugiliteOperationBlock)currentBlock).getNextBlock();
            }
            else{
                currentBlock = null;
            }
        }
        return sugiliteBlocks;
    }

    /**
     * send a new intent to the location specified in callbackString
     * @param messageType
     * @param messageBody
     * @param callbackString
     */

    /*
    messageType, messageBody
    -------------------------
    "FINISHED_RECORDING", scriptName
    "START_RECORDING_EXCEPTION", exceptionMessage
    "END_RECORDING_EXCEPTION", exceptionMessage
    "RUN_SCRIPT_EXCEPTION, exceptionMessage
    "RUN_JSON_EXCEPTION", exceptionMessage
    "ADD_JOSON_AS_SCRIPT_EXCEPTION", exceptionMessage

     */
    public String callbackString = "";
    public void sendCallbackMsg(String messageType, String messageBody, String callbackString){
        Intent intent = new Intent(callbackString);
        intent.putExtra("messageType", messageType);
        intent.putExtra("messageBody", messageBody);
        intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        try {
            startActivity(intent);
        }
        catch (Exception e){
            //do nothing
        }
    }


}
