package edu.cmu.hcii.sugilite.automation;

import android.content.Context;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.model.block.SugiliteBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteOperationBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteStartingBlock;
import edu.cmu.hcii.sugilite.model.operation.SugiliteOperation;

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
            boolean retVal = performAction(node, operationBlock);
            if(retVal) {
                sugiliteData.removeInstructionQueueItem();
                return true;
            }
        }
        return false;
    }

    public boolean performAction(AccessibilityNodeInfo node, SugiliteOperationBlock block) {
        //TODO: scrolling
        try {
            Thread.sleep(1000);
        }
        catch (Exception e){
            // do nothing
        }
        AccessibilityNodeInfo nodeToAction = node;


        if(block.getOperation().getOperationType() == SugiliteOperation.CLICK){
            return nodeToAction.performAction(AccessibilityNodeInfo.ACTION_CLICK);
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
}
