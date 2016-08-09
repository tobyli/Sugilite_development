package edu.cmu.hcii.sugilite.recording;

import java.util.Collection;

import edu.cmu.hcii.sugilite.model.block.SerializableNodeInfo;
import edu.cmu.hcii.sugilite.model.block.UIElementMatchingFilter;

/**
 * Created by toby on 8/8/16.
 */
public class AlternativeNodesFilterTester {
    public AlternativeNodesFilterTester(){

    }
    public int getFilteredAlternativeNodesCount(Collection<SerializableNodeInfo> alternativeNodes, UIElementMatchingFilter filter){
        int count = 0;
        for(SerializableNodeInfo node : alternativeNodes){
            if(filter.filter(node))
                count ++;
        }
        return count;
    }
}
