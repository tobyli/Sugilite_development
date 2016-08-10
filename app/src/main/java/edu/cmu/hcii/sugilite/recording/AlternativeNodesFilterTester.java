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
        return matchedNodes.size();
    }

    //TODO: add remove duplicate


}
