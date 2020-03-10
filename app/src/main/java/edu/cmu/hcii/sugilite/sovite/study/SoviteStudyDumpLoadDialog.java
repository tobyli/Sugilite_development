package edu.cmu.hcii.sugilite.sovite.study;

import android.app.Activity;
import android.util.Pair;

import java.util.ArrayList;
import java.util.List;

import edu.cmu.hcii.sugilite.pumice.dialog.PumiceDialogManager;
import edu.cmu.hcii.sugilite.pumice.kb.PumiceProceduralKnowledge;
import edu.cmu.hcii.sugilite.sovite.dialog.SoviteItemSelectionDialog;
import edu.cmu.hcii.sugilite.sovite.dialog.SoviteReturnValueCallbackInterface;

/**
 * @author toby
 * @date 3/9/20
 * @time 5:53 PM
 */
public class SoviteStudyDumpLoadDialog extends SoviteItemSelectionDialog {

    public SoviteStudyDumpLoadDialog(Activity context, PumiceDialogManager pumiceDialogManager, List<SoviteStudyDumpManager.SoviteStudyDumpPacket> soviteStudyDumpPackets) {
        super(context, pumiceDialogManager);

        List<Pair<String, Runnable>> listItemLabelRunnableList = new ArrayList<>();
        for (SoviteStudyDumpManager.SoviteStudyDumpPacket soviteStudyDumpPacket : soviteStudyDumpPackets) {
            listItemLabelRunnableList.add(new Pair<>(soviteStudyDumpPacket.getName(), new Runnable() {
                @Override
                public void run() {
                    dismiss();
                    //TODO: load the packet
                }
            }));
        }
        initDialog(listItemLabelRunnableList, "Please select the stored state you want to load.", false);
    }
}
