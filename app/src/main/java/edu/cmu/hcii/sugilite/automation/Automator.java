package edu.cmu.hcii.sugilite.automation;

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.model.block.SugiliteBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteOperationBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;
import edu.cmu.hcii.sugilite.model.operation.SugiliteLoadVariableOperation;
import edu.cmu.hcii.sugilite.model.operation.SugiliteOperation;
import edu.cmu.hcii.sugilite.model.operation.SugiliteSetTextOperation;
import edu.cmu.hcii.sugilite.model.variable.StringVariable;
import edu.cmu.hcii.sugilite.model.variable.Variable;
import edu.cmu.hcii.sugilite.model.variable.VariableHelper;
import edu.cmu.hcii.sugilite.ui.BoundingBoxManager;
import edu.cmu.hcii.sugilite.ui.StatusIconManager;
import android.speech.tts.TextToSpeech;


/**
 * Created by toby on 6/13/16.
 */
public class Automator {
    private SugiliteData sugiliteData;
    private Context context;
    private BoundingBoxManager boundingBoxManager;
    private VariableHelper variableHelper;
    private static final int DELAY = 2000;
    private TextToSpeech tts;
    private boolean ttsReady = false;


    public Automator(SugiliteData sugiliteData, Context context, StatusIconManager statusIconManager){
        this.sugiliteData = sugiliteData;
        this.boundingBoxManager = new BoundingBoxManager(context);
        Intent checkIntent = new Intent();
        tts = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                ttsReady = true;
            }
        });

    }


    public boolean handleLiveEvent (AccessibilityNodeInfo rootNode, Context context){
        //TODO: fix the highlighting for matched element
        if(sugiliteData.getInstructionQueueSize() == 0 || rootNode == null)
            return false;
        this.context = context;
        SugiliteBlock blockToMatch = sugiliteData.peekInstructionQueue();
        if (!(blockToMatch instanceof SugiliteOperationBlock)){
            if(blockToMatch instanceof SugiliteStartingBlock){
                //Toast.makeText(context, "Start running script " + ((SugiliteStartingBlock)blockToMatch).getScriptName(), Toast.LENGTH_SHORT).show();
            }
            sugiliteData.removeInstructionQueueItem();
            return false;
        }
        SugiliteOperationBlock operationBlock = (SugiliteOperationBlock)blockToMatch;
        variableHelper = new VariableHelper(sugiliteData.stringVariableMap);
        //if we can match this event, perform the action and remove the head object
        List<AccessibilityNodeInfo> allNodes = preOrderTraverse(rootNode);
        List<AccessibilityNodeInfo> filteredNodes = new ArrayList<>();
        for(AccessibilityNodeInfo node : allNodes){
            if(operationBlock.getElementMatchingFilter().filter(node, variableHelper))
                filteredNodes.add(node);
        }
        if(filteredNodes.size() == 0)
            return false;
        for(AccessibilityNodeInfo node : filteredNodes){
            //TODO: scrolling
            if(operationBlock.getOperation().getOperationType() == SugiliteOperation.CLICK && (!node.isClickable()))
                continue;
            try {
                //Thread.sleep(DELAY / 2);
            }
            catch (Exception e){
                // do nothing
            }
            boolean retVal = performAction(node, operationBlock);
            if(retVal) {
                /*
                Rect tempRect = new Rect();
                node.getBoundsInScreen(tempRect);
                statusIconManager.moveIcon(tempRect.centerX(), tempRect.centerY());
                */
                sugiliteData.errorHandler.reportSuccess(Calendar.getInstance().getTimeInMillis());
                sugiliteData.removeInstructionQueueItem();
                try {
                    Thread.sleep(DELAY / 2);
                }
                catch (Exception e){
                    // do nothing
                }
                return true;
            }
        }
        return false;
    }

    public boolean performAction(AccessibilityNodeInfo node, SugiliteOperationBlock block) {

        AccessibilityNodeInfo nodeToAction = node;
        if(block.getOperation().getOperationType() == SugiliteOperation.CLICK){
            return nodeToAction.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        }
        if(block.getOperation().getOperationType() == SugiliteOperation.SET_TEXT){
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

        if(block.getOperation().getOperationType() == SugiliteOperation.READ_OUT){
            if(block.getOperation().getParameter().contentEquals("Text")) {
                if (ttsReady && node != null && node.getText() != null) {
                    tts.speak("Return Value", TextToSpeech.QUEUE_ADD, null);
                    tts.speak(node.getText().toString(), TextToSpeech.QUEUE_ADD, null);
                }
            }
            else if (block.getOperation().getParameter().contentEquals("Child Text")) {
                List<AccessibilityNodeInfo> children = preOrderTraverse(node);
                if (ttsReady && node != null && children != null && children.size() > 0) {
                    String childText = "";
                    for(AccessibilityNodeInfo childNode : children){
                        if(childNode.getText() != null)
                            childText += childNode.getText();
                    }
                    if(childText.length() > 0) {
                        tts.speak("Return Value", TextToSpeech.QUEUE_ADD, null);
                        tts.speak(childText, TextToSpeech.QUEUE_ADD, null);
                    }
                }
            }
            else if (block.getOperation().getParameter().contentEquals("Content Description")){
                if (ttsReady && node != null && node.getContentDescription() != null) {
                    tts.speak("Return Value", TextToSpeech.QUEUE_ADD, null);
                    tts.speak(node.getContentDescription().toString(), TextToSpeech.QUEUE_ADD, null);
                }
            }
            return true;
        }

        if(block.getOperation().getOperationType() == SugiliteOperation.LOAD_AS_VARIABLE) {
            if (block.getOperation() instanceof SugiliteLoadVariableOperation) {
                String variableName = ((SugiliteLoadVariableOperation) block.getOperation()).getVariableName();
                StringVariable stringVariable = new StringVariable(variableName);
                stringVariable.type = Variable.LOAD_RUNTIME;

                if (block.getOperation().getParameter().contentEquals("Text")) {
                    if (node.getText() != null) {
                        stringVariable.setValue(node.getText().toString());
                    }
                } else if (block.getOperation().getParameter().contentEquals("Child Text")) {
                    List<AccessibilityNodeInfo> children = preOrderTraverse(node);
                    if (ttsReady && node != null && children != null && children.size() > 0) {
                        String childText = "";
                        for (AccessibilityNodeInfo childNode : children) {
                            if (childNode.getText() != null)
                                childText += childNode.getText();
                        }
                        if (childText.length() > 0) {
                            stringVariable.setValue(childText);
                        }
                    }
                } else if (block.getOperation().getParameter().contentEquals("Content Description")) {
                    if (node.getContentDescription() != null) {
                        stringVariable.setValue(node.getContentDescription().toString());
                    }
                }
                if(stringVariable.getValue() != null && stringVariable.getValue().length() > 0){
                    //save the string variable to run time symbol table
                    Toast.makeText(context, "Get value \"" + stringVariable.getValue() + "\" for the variable " + stringVariable.getName(), Toast.LENGTH_SHORT).show();
                    sugiliteData.stringVariableMap.put(stringVariable.getName(), stringVariable);
                    return true;
                }
                else
                    return false;
            }
        }
        return false;
    }

    public static List<AccessibilityNodeInfo> preOrderTraverse(AccessibilityNodeInfo root){
        if(root == null)
            return null;
        List<AccessibilityNodeInfo> list = new ArrayList<>();
        list.add(root);
        int childCount = root.getChildCount();
        for(int i = 0; i < childCount; i ++){
            AccessibilityNodeInfo node = root.getChild(i);
            if(node != null)
                list.addAll(preOrderTraverse(node));
        }
        return list;
    }

    public List<AccessibilityNodeInfo> getClickableList (List<AccessibilityNodeInfo> nodeInfos){
        List<AccessibilityNodeInfo> retList = new ArrayList<>();
        for(AccessibilityNodeInfo node : nodeInfos){
            if(node.isClickable())
                retList.add(node);
        }
        return retList;
    }

    private String textVariableParse (String text, Set<String> variableSet, Map<String, Variable> variableValueMap){
        if(variableSet == null || variableValueMap == null)
            return text;
        String currentText = new String(text);
        for(Map.Entry<String, Variable> entry : variableValueMap.entrySet()){
            if(!variableSet.contains(entry.getKey()))
                continue;
            if(entry.getValue() instanceof StringVariable)
                currentText = currentText.replace("@" + entry.getKey(), ((StringVariable) entry.getValue()).getValue());
        }
        return currentText;
    }
}
