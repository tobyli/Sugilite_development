package edu.cmu.hcii.sugilite.recording;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import edu.cmu.hcii.sugilite.model.block.SerializableNodeInfo;
import edu.cmu.hcii.sugilite.model.block.UIElementMatchingFilter;

/**
 * Created by toby on 8/8/16.
 */
public class AlternativeNodesFilterTester {
    public AlternativeNodesFilterTester(){

    }
    public int getFilteredAlternativeNodesCount(Collection<SerializableNodeInfo> alternativeNodes, UIElementMatchingFilter filter) {
        Set<SerializableNodeInfo> matchedNodes = new HashSet<>();
        for (SerializableNodeInfo node : alternativeNodes) {
            if (filter.filter(node)) {
                //check duplicate
                boolean duplicated = false;
                for (SerializableNodeInfo existingNode : matchedNodes) {
                    if (existingNode.isTheSameNode(node)) {
                        duplicated = true;
                        break;
                    }
                }
                if (!duplicated)
                    matchedNodes.add(node);
            }
        }
        if(matchedNodes.size() > 1){
            int count = 0;
            System.out.println("MATECHED NODES: \n");
            for(SerializableNodeInfo node : matchedNodes){
                System.out.println("*** NODE " + count++ + " ***\n");
                System.out.println("Text: " + (node.text == null ? "NULL" : node.text) + "\n");
                System.out.println("ContentDescription: " + (node.contentDescription == null ? "NULL" : node.contentDescription) + "\n");
                System.out.println("ViewId: " + (node.viewId == null ? "NULL" : node.viewId) + "\n");
                System.out.println("BoundsInScreen: " + (node.boundsInScreen == null ? "NULL" : node.boundsInScreen) + "\n");

                System.out.println("BoundsInParent: " + (node.boundsInParent == null ? "NULL" : node.boundsInParent) + "\n");
            }
        }
        return matchedNodes.size();
    }

    //TODO: add remove duplicate


}
