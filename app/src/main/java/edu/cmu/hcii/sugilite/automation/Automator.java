package edu.cmu.hcii.sugilite.automation;

import android.content.Context;
import android.os.Bundle;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.model.block.SugiliteBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteOperationBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;
import edu.cmu.hcii.sugilite.model.operation.SugiliteOperation;
import edu.cmu.hcii.sugilite.model.operation.SugiliteSetTextOperation;

/**
 * Created by toby on 6/13/16.
 */
public class Automator {
    private SugiliteData sugiliteData;
    private Context context;
    public Automator(SugiliteData sugiliteData){
        this.sugiliteData = sugiliteData;
    }
    public boolean handleLiveEvent (AccessibilityNodeInfo rootNode, Context context){
        //TODO: fix the highlighting for matched element
        if(sugiliteData.getInstructionQueueSize() == 0 || rootNode == null)
            return false;
        this.context = context;
        SugiliteBlock blockToMatch = sugiliteData.peekInstructionQueue();
        if (!(blockToMatch instanceof SugiliteOperationBlock)){
            if(blockToMatch instanceof SugiliteStartingBlock){
                Toast.makeText(context, "Start running script " + ((SugiliteStartingBlock)blockToMatch).getScriptName(), Toast.LENGTH_SHORT).show();
            }
            sugiliteData.removeInstructionQueueItem();
            return false;
        }
        SugiliteOperationBlock operationBlock = (SugiliteOperationBlock)blockToMatch;
        //if we can match this event, perform the action and remove the head object
        List<AccessibilityNodeInfo> allNodes = preOrderTraverse(rootNode);
        List<AccessibilityNodeInfo> filteredNodes = new ArrayList<>();
        for(AccessibilityNodeInfo node : allNodes){
            if(operationBlock.getElementMatchingFilter().filter(node))
                filteredNodes.add(node);
        }
        if(filteredNodes.size() == 0)
            return false;
        for(AccessibilityNodeInfo node : filteredNodes){
            //TODO: scrolling
            boolean retVal = performAction(node, operationBlock);
            if(retVal) {
                try {
                    Thread.sleep(2000);
                }
                catch (Exception e){
                    // do nothing
                }
                sugiliteData.removeInstructionQueueItem();
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
            String text = ((SugiliteSetTextOperation)block.getOperation()).getText();
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


        return false;
    }

    public static List<AccessibilityNodeInfo> preOrderTraverse(AccessibilityNodeInfo root){
        if(root == null)
            return null;
        List<AccessibilityNodeInfo> list = new ArrayList<>();
        list.add(root);
        int childCount = root.getChildCount();
        for(int i = 0; i < childCount; i ++){
            if(root.getChild(i) != null)
                list.addAll(preOrderTraverse(root.getChild(i)));
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
}
