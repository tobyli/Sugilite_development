package edu.cmu.hcii.sugilite.recording;

import java.util.HashSet;
import java.util.Set;

import edu.cmu.hcii.sugilite.model.block.SerializableNodeInfo;
import edu.cmu.hcii.sugilite.model.block.SugiliteAvailableFeaturePack;

/**
 * Created by toby on 8/7/16.
 */
public class RecordingSkipManager {
    Set<String> packageToSkip;
    public RecordingSkipManager(){
        packageToSkip = new HashSet<>();
        packageToSkip.add("com.google.android.googlequicksearchbox");

    }
    public boolean checkSkip(SugiliteAvailableFeaturePack featurePack, int triggerMode){
        //never skip editing interface
        if(triggerMode == RecordingPopUpDialog.TRIGGERED_BY_EDIT)
            return false;

        //skip if the package name is among the "to skip" list
        if(packageToSkip.contains(featurePack.packageName))
            return true;

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
            return true;

        return false;
    }
}
