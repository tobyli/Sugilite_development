package edu.cmu.hcii.sugilite.automation;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Set;

import edu.cmu.hcii.sugilite.Const;
import edu.cmu.hcii.sugilite.model.Node;
import edu.cmu.hcii.sugilite.SugiliteAccessibilityService;
import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptDao;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptFileDao;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptSQLDao;
import edu.cmu.hcii.sugilite.model.block.SugiliteBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteConditionBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteErrorHandlingForkBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteOperationBlock;
import edu.cmu.hcii.sugilite.model.block.operation.special_operation.SugiliteSpecialOperationBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;
import edu.cmu.hcii.sugilite.model.operation.SugiliteLoadVariableOperation;
import edu.cmu.hcii.sugilite.model.operation.SugiliteOperation;
import edu.cmu.hcii.sugilite.model.operation.SugiliteReadoutConstOperation;
import edu.cmu.hcii.sugilite.model.operation.SugiliteReadoutOperation;
import edu.cmu.hcii.sugilite.model.operation.SugiliteSetTextOperation;
import edu.cmu.hcii.sugilite.model.variable.StringVariable;
import edu.cmu.hcii.sugilite.model.variable.Variable;
import edu.cmu.hcii.sugilite.model.variable.VariableHelper;
import edu.cmu.hcii.sugilite.ontology.OntologyQuery;
import edu.cmu.hcii.sugilite.ontology.SugiliteEntity;
import edu.cmu.hcii.sugilite.ontology.UISnapshot;
import edu.cmu.hcii.sugilite.ontology.helper.annotator.SugiliteTextAnnotator;
import edu.cmu.hcii.sugilite.recording.SugiliteScreenshotManager;
import edu.cmu.hcii.sugilite.ui.BoundingBoxManager;
import edu.cmu.hcii.sugilite.ui.StatusIconManager;

import android.speech.tts.TextToSpeech;

import static edu.cmu.hcii.sugilite.Const.DEBUG_DELAY;
import static edu.cmu.hcii.sugilite.Const.DELAY;
import static edu.cmu.hcii.sugilite.Const.SQL_SCRIPT_DAO;


/**
 * Created by toby on 6/13/16.
 */
public class Automator {
    private SugiliteData sugiliteData;
    private SugiliteAccessibilityService serviceContext;
    private Context context;
    private BoundingBoxManager boundingBoxManager;
    private VariableHelper variableHelper;
    //private TextToSpeech tts;
    private SugiliteScriptDao sugiliteScriptDao;
    private LayoutInflater layoutInflater;
    private SharedPreferences sharedPreferences;
    private TextToSpeech tts = null;
    private boolean ttsReady = false;
    private SugiliteScreenshotManager screenshotManager;
    private SugiliteTextAnnotator sugiliteTextAnnotator;

    public Automator(SugiliteData sugiliteData, SugiliteAccessibilityService context, StatusIconManager statusIconManager, SharedPreferences sharedPreferences, SugiliteTextAnnotator sugiliteTextAnnotator){
        this.sugiliteData = sugiliteData;
        this.serviceContext = context;
        this.sugiliteTextAnnotator = sugiliteTextAnnotator;
        this.boundingBoxManager = new BoundingBoxManager(context);
        if(Const.DAO_TO_USE == SQL_SCRIPT_DAO)
            this.sugiliteScriptDao = new SugiliteScriptSQLDao(context);
        else
            this.sugiliteScriptDao = new SugiliteScriptFileDao(context, sugiliteData);
        this.layoutInflater = (LayoutInflater) context.getSystemService( Context.LAYOUT_INFLATER_SERVICE );
        this.sharedPreferences = sharedPreferences;

        //load tts
        try {
            tts = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
                @Override
                public void onInit(int status) {
                    ttsReady = true;
                }
            });
        }
        catch (Exception e){
            e.printStackTrace();
        }

        screenshotManager = new SugiliteScreenshotManager(sharedPreferences, context);
    }

    List<Node> lastTimeFailed = new ArrayList<>();
    //the return value is not used?

    @Deprecated
    public boolean handleLiveEvent (AccessibilityNodeInfo rootNode, Context context){
        //TODO: fix the highlighting for matched element
        if(sugiliteData.getInstructionQueueSize() == 0 || rootNode == null)
            return false;
        this.context = context;
        final SugiliteBlock blockToMatch = sugiliteData.peekInstructionQueue();
        if(blockToMatch == null)
            return false;

        if (!(blockToMatch instanceof SugiliteOperationBlock)) {
            //handle non Sugilite operation blocks
            /**
             * nothing really special needed for starting blocks, just add the next block to the queue
             */
            if (blockToMatch instanceof SugiliteStartingBlock) {
                //Toast.makeText(context, "Start running script " + ((SugiliteStartingBlock)blockToMatch).getScriptName(), Toast.LENGTH_SHORT).show();
                sugiliteData.removeInstructionQueueItem();
                addNextBlockToQueue(blockToMatch);
                return true;
            }
            /**
             * handle error handling block - "addNextBlockToQueue" will determine which block to add
             */
            else if (blockToMatch instanceof SugiliteErrorHandlingForkBlock){
                sugiliteData.removeInstructionQueueItem();
                addNextBlockToQueue(blockToMatch);
                return true;
            }
            /**
             * for subscript operation blocks, the subscript should be executed
             */
            else if (blockToMatch instanceof SugiliteSpecialOperationBlock){
                sugiliteData.removeInstructionQueueItem();
                SugiliteSpecialOperationBlock specialOperationBlock = (SugiliteSpecialOperationBlock) blockToMatch;
                try{
                    specialOperationBlock.run(context, sugiliteData, sugiliteScriptDao, sharedPreferences);
                    return true;
                }
                catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
            }
            else {
                throw new RuntimeException("Unsupported Block Type!");
            }
        }

        else {
            //the blockToMatch is an operation block
            SugiliteOperationBlock operationBlock = (SugiliteOperationBlock) blockToMatch;

            if(operationBlock.isSetAsABreakPoint) {
                sugiliteData.storedInstructionQueueForPause.clear();
                sugiliteData.storedInstructionQueueForPause.addAll(sugiliteData.getCopyOfInstructionQueue());
                sugiliteData.clearInstructionQueue();
                sugiliteData.setCurrentSystemState(SugiliteData.PAUSED_FOR_BREAKPOINT_STATE);
                return false;
            }

            if (operationBlock.getQuery() == null) {
                //there is no query in the operation block
                if (operationBlock.getOperation().getOperationType() == SugiliteOperation.SPECIAL_GO_HOME) {
                    //perform the go home operation - because the go home operation will have a null filter
                    boolean retVal = performAction(null, operationBlock);
                    if (retVal) {
                        sugiliteData.errorHandler.reportSuccess(Calendar.getInstance().getTimeInMillis());
                        sugiliteData.removeInstructionQueueItem();
                        addNextBlockToQueue(operationBlock);
                        if(sugiliteData.getCurrentSystemState() == SugiliteData.REGULAR_DEBUG_STATE) {
                            try {
                                //----not taking screenshot----
                                //screenshotManager.take(false, SugiliteScreenshotManager.DIRECTORY_PATH, SugiliteScreenshotManager.getDebugScreenshotFileNameWithDate());
                            }
                            catch (Exception e){
                                e.printStackTrace();
                            }
                        }

                        try {
                            //wait for DELAY/2 after adding the next block to queue
                            if(sugiliteData.getCurrentSystemState() == SugiliteData.REGULAR_DEBUG_STATE)
                                Thread.sleep(DEBUG_DELAY / 2);
                            else
                                Thread.sleep(DELAY / 2);
                        } catch (Exception e) {
                            // do nothing
                        }
                    }
                    return retVal;

                } else
                    return false;
            }
            else {
                //the operation has a query, try to use the query to match a node

                variableHelper = new VariableHelper(sugiliteData.stringVariableMap);
                //if we can match this event, perform the action and remove the head object

                if(true/*has last time failed*/ && lastTimeFailed != null && lastTimeFailed.size() > 0){
                    List<AccessibilityNodeInfo> allNodes = AutomatorUtil.preOrderTraverse(rootNode);
                    List<AccessibilityNodeInfo> filteredNode = new ArrayList<>();
                    boolean succeeded = false;
                    for(AccessibilityNodeInfo node : allNodes){
                        for(Node lasttimeFailedNode : lastTimeFailed){
                            Rect rect = new Rect();
                            node.getBoundsInScreen(rect);
                            if(((lasttimeFailedNode.getPackageName() == null && node.getPackageName() == null) || lasttimeFailedNode.getPackageName().equals(node.getPackageName().toString())) &&
                                    ((lasttimeFailedNode.getClassName() == null && node.getClassName() == null) || lasttimeFailedNode.getClassName().equals(node.getClassName().toString())) &&
                                    ((lasttimeFailedNode.getBoundsInScreen() == null && rect.flattenToString() == null) || lasttimeFailedNode.getBoundsInScreen().equals(rect.flattenToString())) &&
                                    ((lasttimeFailedNode.getViewId() == null && node.getViewIdResourceName() == null) || (lasttimeFailedNode.getViewId() != null && lasttimeFailedNode.getViewId().equals(node.getViewIdResourceName())))){
                                //TODO: execute on node
                                boolean retVal = performAction(node, operationBlock);
                                if (retVal) {
                                    //the action is performed successfully
                                    if (!succeeded) {
                                        sugiliteData.errorHandler.reportSuccess(Calendar.getInstance().getTimeInMillis());
                                        if (sugiliteData.getInstructionQueueSize() > 0) {
                                            sugiliteData.removeInstructionQueueItem();
                                        }
                                        addNextBlockToQueue(operationBlock);
                                    }
                                    succeeded = true;

                                    try {
                                        //delay delay/2 length after successfuly performing the action
                                        if(sugiliteData.getCurrentSystemState() == SugiliteData.DEFAULT_STATE)
                                            Thread.sleep(DEBUG_DELAY / 2);
                                        else
                                            Thread.sleep(DELAY / 2);
                                    } catch (Exception e) {
                                        // do nothing
                                    }
                                }

                            }
                        }
                    }
                    lastTimeFailed.clear();
                    if(succeeded){
                        return succeeded;
                    }
                }


                UISnapshot uiSnapshot = new UISnapshot(rootNode, true, sugiliteTextAnnotator);

                //de-serialize the OntologyQuery
                OntologyQuery q = new OntologyQuery(operationBlock.getQuery());
                Set<SugiliteEntity> querySet = q.executeOn(uiSnapshot);

                List<AccessibilityNodeInfo> filteredNodes = new ArrayList<AccessibilityNodeInfo>();
                for(SugiliteEntity e : querySet) {
                    if(e.getEntityValue() instanceof Node){
                        filteredNodes.add(uiSnapshot.getNodeAccessibilityNodeInfoMap().get(e.getEntityValue()));
                    }
                }

                if (filteredNodes.size() == 0) {
                    //couldn't find a matched node in the current UISnapshot
                    return false;
                }

                boolean succeeded = false;
                for (AccessibilityNodeInfo node : filteredNodes) {
                    //TODO: scrolling to find more nodes -- not only the ones displayed on the current screen
                    if (operationBlock.getOperation().getOperationType() == SugiliteOperation.CLICK && (!node.isClickable()))
                        //continue;
                    try {
                    } catch (Exception e) {
                        // do nothing
                    }
                    boolean retVal = performAction(node, operationBlock);
                    if (retVal) {
                        //the action is performed successfully

                        /*
                        Rect tempRect = new Rect();
                        node.getBoundsInScreen(tempRect);
                        statusIconManager.moveIcon(tempRect.centerX(), tempRect.centerY());
                        */
                        if (!succeeded) {
                            sugiliteData.errorHandler.reportSuccess(Calendar.getInstance().getTimeInMillis());
                            if (sugiliteData.getInstructionQueueSize() > 0) {
                                sugiliteData.removeInstructionQueueItem();
                            }
                            addNextBlockToQueue(operationBlock);
                        }
                        succeeded = true;

                        try {
                            //delay delay/2 length after successfuly performing the action
                            if(sugiliteData.getCurrentSystemState() == SugiliteData.DEFAULT_STATE)
                                Thread.sleep(DEBUG_DELAY / 2);
                            else
                                Thread.sleep(DELAY / 2);
                        } catch (Exception e) {
                            // do nothing
                        }
                    }
                }
                if(! succeeded){
                    lastTimeFailed.clear();
                    for(AccessibilityNodeInfo node : filteredNodes){
                        lastTimeFailed.add(new Node(node));
                    }
                }
                else{
                    lastTimeFailed.clear();
                }
                return succeeded;
            }
        }
    }

    /**
     * for running the script -- called when a new UI snapshot is available
     * @param uiSnapshot
     * @param context
     * @param allNodes
     * @return
     */
    public boolean handleLiveEvent (UISnapshot uiSnapshot, Context context, List<AccessibilityNodeInfo> allNodes){
        //TODO: fix the highlighting for matched element
        if(sugiliteData.getInstructionQueueSize() == 0 || uiSnapshot == null)
            return false;
        this.context = context;
        final SugiliteBlock blockToMatch = sugiliteData.peekInstructionQueue();
        if(blockToMatch == null)
            return false;

        if (!(blockToMatch instanceof SugiliteOperationBlock)) {
            //handle non Sugilite operation blocks
            /**
             * nothing really special needed for starting blocks, just add the next block to the queue
             */
            if (blockToMatch instanceof SugiliteStartingBlock) {
                sugiliteData.removeInstructionQueueItem();
                addNextBlockToQueue(blockToMatch);
                return true;
            }
            /**
             * handle error handling block - "addNextBlockToQueue" will determine which block to add
             */
            else if (blockToMatch instanceof SugiliteErrorHandlingForkBlock){
                sugiliteData.removeInstructionQueueItem();
                addNextBlockToQueue(blockToMatch);
                return true;
            }
            /**
             * nothing special needed for conditional blocks, just add next block to queue
             */
            else if (blockToMatch instanceof SugiliteConditionBlock) {///
                sugiliteData.removeInstructionQueueItem();///
                addNextBlockToQueue(blockToMatch);///
                return true;///
            }///
            /**
             * for special operation blocks, the run() method should be executed
             */
            else if (blockToMatch instanceof SugiliteSpecialOperationBlock){
                sugiliteData.removeInstructionQueueItem();
                SugiliteSpecialOperationBlock specialOperationBlock = (SugiliteSpecialOperationBlock) blockToMatch;
                try{
                    specialOperationBlock.run(context, sugiliteData, sugiliteScriptDao, sharedPreferences);
                    return true;
                }
                catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
            }
            else {
                throw new RuntimeException("Unsupported Block Type!");
            }
        }

        else {
            //the blockToMatch is an operation block
            SugiliteOperationBlock operationBlock = (SugiliteOperationBlock) blockToMatch;

            //for the debugging mode - handle the breakpoint
            if(operationBlock.isSetAsABreakPoint) {
                sugiliteData.storedInstructionQueueForPause.clear();
                sugiliteData.storedInstructionQueueForPause.addAll(sugiliteData.getCopyOfInstructionQueue());
                sugiliteData.clearInstructionQueue();
                sugiliteData.setCurrentSystemState(SugiliteData.PAUSED_FOR_BREAKPOINT_STATE);
                return false;
            }

            if (operationBlock.getQuery() == null) {
                //there is no query in the operation block
                if (operationBlock.getOperation().getOperationType() == SugiliteOperation.SPECIAL_GO_HOME ||
                        operationBlock.getOperation().getOperationType() == SugiliteOperation.READOUT_CONST) {


                    //** perform the operation with node = null - because the special_go_home operation and readout_const operations will have a null filter
                    boolean retVal = performAction(null, operationBlock);
                    if (retVal) {
                        sugiliteData.errorHandler.reportSuccess(Calendar.getInstance().getTimeInMillis());
                        sugiliteData.removeInstructionQueueItem();
                        addNextBlockToQueue(operationBlock);
                        if(sugiliteData.getCurrentSystemState() == SugiliteData.REGULAR_DEBUG_STATE) {
                            try {
                                //----not taking screenshot----
                                //screenshotManager.take(false, SugiliteScreenshotManager.DIRECTORY_PATH, SugiliteScreenshotManager.getDebugScreenshotFileNameWithDate());
                            }
                            catch (Exception e){
                                e.printStackTrace();
                            }
                        }

                        try {
                            //wait for DELAY/2 after adding the next block to queue
                            if(sugiliteData.getCurrentSystemState() == SugiliteData.REGULAR_DEBUG_STATE)
                                Thread.sleep(DEBUG_DELAY / 2);
                            else
                                Thread.sleep(DELAY / 2);
                        } catch (Exception e) {
                            // do nothing
                        }
                    }
                    return retVal;

                }

                else {
                    //error, because the block contains no query
                    return false;
                }
            }

            else {
                //the operation has a query, try to use the query to match a node
                variableHelper = new VariableHelper(sugiliteData.stringVariableMap);

                //try to perform operations that have failed last time
                if(/*has last time failed*/ lastTimeFailed != null && lastTimeFailed.size() > 0){
                    boolean succeeded = false;
                    for(AccessibilityNodeInfo node : allNodes){
                        for(Node lasttimeFailedNode : lastTimeFailed){
                            Rect rect = new Rect();
                            node.getBoundsInScreen(rect);
                            if(((lasttimeFailedNode.getPackageName() == null && node.getPackageName() == null) || lasttimeFailedNode.getPackageName().equals(node.getPackageName().toString())) &&
                                    ((lasttimeFailedNode.getClassName() == null && node.getClassName() == null) || lasttimeFailedNode.getClassName().equals(node.getClassName().toString())) &&
                                    ((lasttimeFailedNode.getBoundsInScreen() == null && rect.flattenToString() == null) || lasttimeFailedNode.getBoundsInScreen().equals(rect.flattenToString())) &&
                                    ((lasttimeFailedNode.getViewId() == null && node.getViewIdResourceName() == null) || (lasttimeFailedNode.getViewId() != null && lasttimeFailedNode.getViewId().equals(node.getViewIdResourceName())))){

                                //!!!execute on node
                                boolean retVal = performAction(node, operationBlock);
                                if (retVal) {
                                    //the action is performed successfully
                                    if (!succeeded) {
                                        sugiliteData.errorHandler.reportSuccess(Calendar.getInstance().getTimeInMillis());
                                        if (sugiliteData.getInstructionQueueSize() > 0) {
                                            sugiliteData.removeInstructionQueueItem();
                                        }
                                        addNextBlockToQueue(operationBlock);
                                    }
                                    succeeded = true;

                                    try {
                                        //delay delay/2 length after successfuly performing the action
                                        if(sugiliteData.getCurrentSystemState() == SugiliteData.DEFAULT_STATE)
                                            Thread.sleep(DEBUG_DELAY / 2);
                                        else
                                            Thread.sleep(DELAY / 2);
                                    } catch (Exception e) {
                                        // do nothing
                                    }
                                }

                            }
                        }
                    }
                    lastTimeFailed.clear();
                    if(succeeded){
                        return succeeded;
                    }
                }




                //de-serialize the OntologyQuery
                OntologyQuery q = new OntologyQuery(operationBlock.getQuery());

                //replace variables in the query
                q = OntologyQuery.deserialize(variableHelper.parse(q.toString()));

                //execute the OntologyQuery on the current UI snapshot
                Set<SugiliteEntity> querySet = q.executeOn(uiSnapshot);

                List<AccessibilityNodeInfo> filteredNodes = new ArrayList<AccessibilityNodeInfo>();
                for(SugiliteEntity e : querySet) {
                    if(e.getEntityValue() instanceof Node){
                        filteredNodes.add(uiSnapshot.getNodeAccessibilityNodeInfoMap().get(e.getEntityValue()));
                    }
                }

                if (filteredNodes.size() == 0) {
                    //couldn't find a matched node in the current UISnapshot
                    return false;
                }

                boolean succeeded = false;
                for (AccessibilityNodeInfo node : filteredNodes) {
                    //TODO: scrolling to find more nodes -- not only the ones displayed on the current screen
                    boolean retVal = performAction(node, operationBlock);

                    if (retVal) {
                        if (!succeeded) {
                            //report success
                            sugiliteData.errorHandler.reportSuccess(Calendar.getInstance().getTimeInMillis());
                            if (sugiliteData.getInstructionQueueSize() > 0) {
                                sugiliteData.removeInstructionQueueItem();
                            }
                            addNextBlockToQueue(operationBlock);
                        }
                        succeeded = true;

                        try {
                            //delay delay/2 length after successfuly performing the action
                            if(sugiliteData.getCurrentSystemState() == SugiliteData.DEFAULT_STATE)
                                Thread.sleep(DEBUG_DELAY / 2);
                            else
                                Thread.sleep(DELAY / 2);
                        } catch (Exception e) {
                            // do nothing
                        }
                    }
                }

                if(! succeeded){
                    lastTimeFailed.clear();
                    for(AccessibilityNodeInfo node : filteredNodes){
                        lastTimeFailed.add(new Node(node));
                    }
                }

                else{
                    lastTimeFailed.clear();
                }
                return succeeded;
            }
        }
    }

    private boolean performAction(AccessibilityNodeInfo node, SugiliteOperationBlock block) {

        AccessibilityNodeInfo nodeToAction = node;

        if(block.getOperation().getOperationType() == SugiliteOperation.CLICK){
            return nodeToAction.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        }

        if(block.getOperation().getOperationType() == SugiliteOperation.SET_TEXT){

            //variable helper helps parse variables in the argument
            variableHelper = new VariableHelper(sugiliteData.stringVariableMap);
            String text = variableHelper.parse(((SugiliteSetTextOperation)block.getOperation()).getText());
            Bundle arguments = new Bundle();
            arguments.putCharSequence(AccessibilityNodeInfo
                    .ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text);
            return nodeToAction.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments);
        }

        if(block.getOperation().getOperationType() == SugiliteOperation.LONG_CLICK){
            return nodeToAction.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK);
        }

        if(block.getOperation().getOperationType() == SugiliteOperation.SELECT){
            return nodeToAction.performAction(AccessibilityNodeInfo.ACTION_SELECT);
        }

        if(block.getOperation().getOperationType() == SugiliteOperation.SPECIAL_GO_HOME){
            //perform the GO_HOME operation
            Intent startMain = new Intent(Intent.ACTION_MAIN);
            startMain.addCategory(Intent.CATEGORY_HOME);
            startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(startMain);
            return true;
        }

        if(block.getOperation().getOperationType() == SugiliteOperation.READ_OUT){
            if(tts != null && ttsReady) {
                if (((SugiliteReadoutOperation)(block.getOperation())).getPropertyToReadout().contentEquals("hasText")) {
                    if (node != null && node.getText() != null) {
                        tts.speak(node.getText().toString(), TextToSpeech.QUEUE_ADD, null);
                    }
                } else if (((SugiliteReadoutOperation)(block.getOperation())).getPropertyToReadout().contentEquals("HAS_CHILD_TEXT")) {
                    List<AccessibilityNodeInfo> children = AutomatorUtil.preOrderTraverse(node);
                    if (node != null && children != null && children.size() > 0) {
                        String childText = "";
                        for (AccessibilityNodeInfo childNode : children) {
                            if (childNode.getText() != null)
                                childText += childNode.getText();
                        }
                        if (childText.length() > 0) {
                            tts.speak(childText, TextToSpeech.QUEUE_ADD, null);
                        }
                    }
                } else if (((SugiliteReadoutOperation)(block.getOperation())).getPropertyToReadout().contentEquals("HAS_CONTENT_DESCRIPTION")) {
                    if (node != null && node.getContentDescription() != null) {
                        tts.speak(node.getContentDescription().toString(), TextToSpeech.QUEUE_ADD, null);
                    }
                }
            }

            else {
                System.out.println("TTS Failed!");
            }
            return true;
        }

        if(block.getOperation().getOperationType() == SugiliteOperation.READOUT_CONST){
            if(tts != null && ttsReady && block.getOperation() instanceof SugiliteReadoutConstOperation) {
                variableHelper = new VariableHelper(sugiliteData.stringVariableMap);
                String text = variableHelper.parse(((SugiliteReadoutConstOperation) block.getOperation()).getTextToReadout());
                tts.speak(text, TextToSpeech.QUEUE_ADD, null);
            }

            else {
                System.out.println("TTS Failed!");
            }
            return true;
        }

        //TODO: LOAD_AS_VARIABLE
        if(block.getOperation().getOperationType() == SugiliteOperation.LOAD_AS_VARIABLE) {
            if (block.getOperation() instanceof SugiliteLoadVariableOperation) {
                String variableName = ((SugiliteLoadVariableOperation) block.getOperation()).getVariableName();
                //create a new variable
                StringVariable stringVariable = new StringVariable(variableName);
                stringVariable.type = Variable.LOAD_RUNTIME;

                if (((SugiliteLoadVariableOperation)(block.getOperation())).getPropertyToSave().contentEquals("hasText")) {
                    if (node.getText() != null) {
                        stringVariable.setValue(node.getText().toString());
                    }
                } else if (((SugiliteLoadVariableOperation)(block.getOperation())).getPropertyToSave().contentEquals("HAS_CHILD_TEXT")) {
                    List<AccessibilityNodeInfo> children = AutomatorUtil.preOrderTraverse(node);
                    if (node != null && children != null && children.size() > 0) {
                        String childText = "";
                        for (AccessibilityNodeInfo childNode : children) {
                            if (childNode.getText() != null)
                                childText += childNode.getText();
                        }
                        if (childText.length() > 0) {
                            stringVariable.setValue(childText);
                        }
                    }
                } else if (((SugiliteLoadVariableOperation)(block.getOperation())).getPropertyToSave().contentEquals("HAS_CONTENT_DESCRIPTION")) {
                    if (node.getContentDescription() != null) {
                        stringVariable.setValue(node.getContentDescription().toString());
                    }
                }
                if(stringVariable.getValue() != null && stringVariable.getValue().length() > 0){
                    //save the string variable to run time symbol table
                    sugiliteData.stringVariableMap.put(stringVariable.getName(), stringVariable);
                    return true;
                }
                else
                    return false;
            }
        }
        return false;
    }


    private void addNextBlockToQueue(final SugiliteBlock block){
        if(block == null) {
            return;
        }
        if(block instanceof SugiliteStartingBlock) {
            sugiliteData.addInstruction(block.getNextBlock());
        }
        else if (block instanceof SugiliteOperationBlock) {
            sugiliteData.addInstruction(block.getNextBlock());
        }
        //if the current block is a fork, then SUGILITE needs to determine which "next block" to add to the queue
        else if (block instanceof SugiliteErrorHandlingForkBlock){
            //TODO: add automatic feature if can only find solution for one
            final AlertDialog.Builder builder = new AlertDialog.Builder(context)
                    .setTitle("Choose which branch to execute")
                    .setMessage(Html.fromHtml("<b>Original: </b>" + ((SugiliteErrorHandlingForkBlock) block).getOriginalNextBlock().getDescription() +
                            "<br><br>" + "<b>Alternative: </b>" + ((SugiliteErrorHandlingForkBlock) block).getAlternativeNextBlock().getDescription()))
                    .setNegativeButton("Original", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            sugiliteData.addInstruction(((SugiliteErrorHandlingForkBlock) block).getOriginalNextBlock());
                        }
                    })
                    .setPositiveButton("Alternative", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            sugiliteData.addInstruction(((SugiliteErrorHandlingForkBlock) block).getAlternativeNextBlock());
                        }
                    });
            serviceContext.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    AlertDialog dialog = builder.create();
                    dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
                    dialog.show();
                }
            });
        }
        else if (block instanceof SugiliteSpecialOperationBlock) {
            sugiliteData.addInstruction(block.getNextBlock());
        }
        else if (block instanceof SugiliteConditionBlock) {///
            sugiliteData.addInstruction(block.getNextBlockToRun(sugiliteData));///
        }///
        else {
            throw new RuntimeException("Unsupported Block Type!");
        }
        //TODO: add handling for conditionals
    }

}
