package edu.cmu.hcii.sugilite;

import android.app.Application;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.google.gson.Gson;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import edu.cmu.hcii.sugilite.automation.ErrorHandler;
import edu.cmu.hcii.sugilite.communication.SugiliteCommunicationController;
import edu.cmu.hcii.sugilite.communication.SugiliteEventBroadcastingActivity;
import edu.cmu.hcii.sugilite.model.block.SugiliteBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteOperationBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;
import edu.cmu.hcii.sugilite.model.variable.Variable;
import edu.cmu.hcii.sugilite.recording.RecordingPopUpDialog;
import edu.cmu.hcii.sugilite.ui.dialog.AbstractSugiliteDialog;

/**
 * @author toby
 * @date 6/13/16
 * @time 2:02 PM
 */
public class SugiliteData extends Application {
    //used to store the current active script
    private SugiliteStartingBlock scriptHead, trackingHead;
    private SugiliteBlock currentScriptBlock, currentTrackingBlock;

    //the queue used for execution. the system should be in the execution mode whenever the queue is non-empty
    private Queue<SugiliteBlock> instructionQueue = new ArrayDeque<>();
    //this queue is used for storing the content of instruction queue for pausing
    public Queue<SugiliteBlock> storedInstructionQueueForPause = new ArrayDeque<>();

    public Map<String, Variable> stringVariableMap = new HashMap<>();
    public Set<String> registeredBroadcastingListener = new HashSet<>();
    public SugiliteBlock afterExecutionOperation = null;


    //caches for file IO through the SugiliteScriptFileDao
    public Map<String, SugiliteStartingBlock> sugiliteFileScriptDaoSavingCache = new HashMap<>();
    public Map<String, SugiliteStartingBlock> sugiliteFileScriptDaoReadingCache = new HashMap<>();


    private Gson gson = new Gson();

    //true if the current recording script is initiated externally
    public boolean initiatedExternally  = false;
    public SugiliteCommunicationController communicationController;
    public ErrorHandler errorHandler = null;
    public String trackingName = "default";
    private boolean startRecordingWhenFinishExecuting = false;

    //used to manage the recording popup, so the later ones won't cover the eariler ones.
    public Queue<RecordingPopUpDialog> recordingPopupDialogQueue = new ArrayDeque<>();
    public boolean hasRecordingPopupActive = false;

    public List<AccessibilityNodeInfo> elementsWithTextLabels = new ArrayList<>();

    private int currentSystemState = DEFAULT_STATE;



    //used to indicate the state of the sugilite system
    public static final int DEFAULT_STATE = 0, RECORDING_STATE = 1, RECORDING_FOR_ERROR_HANDLING_STATE = 2, EXECUTION_STATE = 3, REGULAR_DEBUG_STATE = 4, PAUSED_FOR_DUCK_MENU_IN_REGULAR_EXECUTION_STATE = 6, PAUSED_FOR_ERROR_HANDLING_STATE = 7, PAUSED_FOR_CRUCIAL_STEP_STATE = 8, PAUSED_FOR_BREAKPOINT_STATE = 9, PAUSED_FOR_DUCK_MENU_IN_DEBUG_MODE = 10;
    public int getCurrentSystemState(){
        return currentSystemState;
    }
    public void setCurrentSystemState(int systemState){
        this.currentSystemState = systemState;
    }

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

    public void runScript(SugiliteStartingBlock startingBlock, SugiliteBlock afterExecutionOperation, int state){
        startRecordingWhenFinishExecuting = false;
        this.afterExecutionOperation = afterExecutionOperation;
        this.instructionQueue.clear();
        errorHandler.relevantPackages.clear();
        errorHandler.relevantPackages.addAll(startingBlock.relevantPackages);
        errorHandler.reportSuccess(Calendar.getInstance().getTimeInMillis());
        List<SugiliteBlock> blocks = traverseBlock(startingBlock);
        addInstruction(startingBlock);

        //set the system state to the execution state
        setCurrentSystemState(state);
    }

    public void runScript(SugiliteStartingBlock startingBlock, boolean isForResuming, int state){
        runScript(startingBlock, null, state);
        startRecordingWhenFinishExecuting = isForResuming;
    }

    public void setCurrentScriptBlock(SugiliteBlock currentScriptBlock){
        this.currentScriptBlock = currentScriptBlock;
    }
    public void setCurrentTrackingBlock(SugiliteBlock currentTrackingBlock){
        this.currentTrackingBlock = currentTrackingBlock;
    }
    public void addInstruction(SugiliteBlock block){
        if(block == null) {
            //note: nullable -> see Automator.addNextBlockToQueue
            if(afterExecutionOperation != null) {
                instructionQueue.add(afterExecutionOperation);
                afterExecutionOperation = null;
            }
            setCurrentSystemState(DEFAULT_STATE);
            return;
        }
        instructionQueue.add(block);
    }
    public void addInstructions(Queue<SugiliteBlock> blocks){
        if(blocks == null)
            return;
        this.instructionQueue.addAll(blocks);
    }
    public void clearInstructionQueue(){
        instructionQueue.clear();
    }
    public int getInstructionQueueSize(){
        return instructionQueue.size();
    }
    public void removeInstructionQueueItem(){
        instructionQueue.remove();
        if(instructionQueue.size() == 0 && startRecordingWhenFinishExecuting){
            //start recording at the end of "resume recording" operation
            final Handler handler = new Handler(Looper.getMainLooper());
            final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
            handler.postDelayed(new Runnable() {
                //1.5 sec delay to start recording -> to avoid catching operations from automation execution
                @Override
                public void run() {
                    System.out.println("Turning on recording - resuming");
                    SharedPreferences.Editor prefEditor = sharedPreferences.edit();
                    prefEditor.putBoolean("recording_in_process", true);
                    prefEditor.commit();
                    setCurrentSystemState(RECORDING_STATE);
                }
            }, 1500);
        }
    }
    public SugiliteBlock peekInstructionQueue(){
        return instructionQueue.peek();
    }
    public SugiliteBlock pollInstructionQueue(){
        return instructionQueue.poll();
    }
    public Queue<SugiliteBlock> getCopyOfInstructionQueue(){
        return new ArrayDeque<>(instructionQueue);
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
    Const.FINISHED_RECORDING, scriptName
    Const.START_RECORDING_EXCEPTION, exceptionMessage
    "STOP_RECORDING_EXCEPTION", exceptionMessage
    "RUN_SCRIPT_EXCEPTION, exceptionMessage
    Const.RUN_JSON_EXCEPTION, exceptionMessage
    Const.ADD_JSON_AS_SCRIPT_EXCEPTION, exceptionMessage

     */
    public String callbackString = "";
    public void sendCallbackMsg(int messageType, String messageBody, String callbackString){
        Intent intent = new Intent(callbackString);
        intent.putExtra("messageType", messageType);
        intent.putExtra("messageBody", messageBody);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_HISTORY);
        try {
            startActivity(intent);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    public void handleBroadcastingEvent(AccessibilityEvent event){
        if(registeredBroadcastingListener.size() < 1)
            return;
        SugiliteEventBroadcastingActivity.BroadcastingEvent broadcastingEvent = new SugiliteEventBroadcastingActivity.BroadcastingEvent(event);
        String messageToSend = gson.toJson(broadcastingEvent);
        for (String dest : registeredBroadcastingListener){
            Intent intent = new Intent(dest);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_HISTORY);
            intent.putExtra("messageType", "SUGILITE_EVENT");
            intent.putExtra("eventBody", messageToSend);
            try {
                startActivity(intent);
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    public static String getStringforState(int state){
        switch (state){
            case DEFAULT_STATE:
                return "DEFAULT_STATE";
            case RECORDING_STATE:
                return "RECORDING_STATE";
            case RECORDING_FOR_ERROR_HANDLING_STATE:
                return "RECORDING_FOR_ERROR_HANDLING_STATE";
            case EXECUTION_STATE:
                return "EXECUTION_STATE";
            case REGULAR_DEBUG_STATE:
                return "REGULAR_DEBUG_STATE";
            case PAUSED_FOR_DUCK_MENU_IN_REGULAR_EXECUTION_STATE:
                return "PAUSED_FOR_DUCK_MENU_IN_REGULAR_EXECUTION_STATE";
            case PAUSED_FOR_ERROR_HANDLING_STATE:
                return "PAUSED_FOR_ERROR_HANDLING_STATE";
            case PAUSED_FOR_CRUCIAL_STEP_STATE:
                return "PAUSED_FOR_CRUCIAL_STEP_STATE";
            case PAUSED_FOR_BREAKPOINT_STATE:
                return "PAUSED_FOR_BREAKPOINT_STATE";
            case PAUSED_FOR_DUCK_MENU_IN_DEBUG_MODE:
                return "PAUSED_FOR_DUCK_MENU_IN_DEBUG_MODE";
        }
        return "";

    }


}
