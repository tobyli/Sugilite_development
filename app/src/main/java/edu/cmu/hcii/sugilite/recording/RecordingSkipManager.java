package edu.cmu.hcii.sugilite.recording;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import edu.cmu.hcii.sugilite.model.block.SerializableNodeInfo;
import edu.cmu.hcii.sugilite.model.block.SugiliteAvailableFeaturePack;
import edu.cmu.hcii.sugilite.model.block.UIElementMatchingFilter;

/**
 * Created by toby on 8/7/16.
 */
public class RecordingSkipManager {
    Set<String> packageToSkip;
    AlternativeNodesFilterTester filterTester;
    public RecordingSkipManager(){
        packageToSkip = new HashSet<>();
        packageToSkip.add("com.google.android.googlequicksearchbox");
        filterTester = new AlternativeNodesFilterTester();

    }
    public String checkSkip(SugiliteAvailableFeaturePack featurePack, int triggerMode, UIElementMatchingFilter filter, Collection<SerializableNodeInfo> alternativeNodes){
        //never skip editing interface
        if(triggerMode == RecordingPopUpDialog.TRIGGERED_BY_EDIT)
            return "full";
        //never skip edit text
        if(featurePack.isEditable)
            return "full";

        //don't skip if the recommended selection can have more than 1 match in alternative nodes
        if(filterTester.getFilteredAlternativeNodesCount(alternativeNodes, filter) > 1)
            return "full";


        //skip if the package name is among the "to skip" list
        if(packageToSkip.contains(featurePack.packageName))
            return "skip";

        //skip if only one label available OR no label available
        Set<String> availableLabel = new HashSet<>();
        if(featurePack.text != null && (!featurePack.text.contentEquals("NULL")))
            availableLabel.add(featurePack.text);
        if(featurePack.contentDescription != null && (!featurePack.contentDescription.contentEquals("NULL")))
            availableLabel.add(featurePack.contentDescription);
        for(SerializableNodeInfo node : featurePack.childNodes){
            if(node.text != null)
                availableLabel.add(node.text);
            if(node.contentDescription != null)
                availableLabel.add(node.contentDescription);
        }

        if(availableLabel.size() == 0 || availableLabel.size() == 1)
            return "skip";
        if(availableLabel.size() > 1)
            return "disambiguation";


        return "full";
    }
}
