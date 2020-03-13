package edu.cmu.hcii.sugilite.sovite.dialog;

import android.app.Activity;
import android.util.Pair;

import java.util.ArrayList;
import java.util.List;

import edu.cmu.hcii.sugilite.pumice.dialog.PumiceDialogManager;
import edu.cmu.hcii.sugilite.pumice.kb.PumiceProceduralKnowledge;
import edu.cmu.hcii.sugilite.sovite.dialog.intent_handler.SoviteReturnValueCallbackInterface;

/**
 * @author toby
 * @date 3/2/20
 * @time 3:59 PM
 */
public class SoviteScriptsWithMatchedAppDialog extends SoviteItemSelectionDialog {
    private SoviteReturnValueCallbackInterface<PumiceProceduralKnowledge> returnValueCallbackObject;

    public SoviteScriptsWithMatchedAppDialog(Activity context, PumiceDialogManager pumiceDialogManager, List<PumiceProceduralKnowledge> proceduralKnowledgesWithMatchedApps, SoviteReturnValueCallbackInterface<PumiceProceduralKnowledge> returnValueCallbackObject) {
        super(context, pumiceDialogManager);
        this.returnValueCallbackObject = returnValueCallbackObject;
        List<Pair<String, Runnable>> listItemLabelRunnableList = new ArrayList<>();
        for (PumiceProceduralKnowledge pumiceProceduralKnowledge : proceduralKnowledgesWithMatchedApps) {
            listItemLabelRunnableList.add(new Pair<>(pumiceProceduralKnowledge.getProcedureDescription(pumiceDialogManager.getPumiceKnowledgeManager(), false), new Runnable() {
                @Override
                public void run() {
                    dismiss();
                    pumiceProceduralKnowledge.isNewlyLearned = false;
                    returnValueCallbackObject.callReturnValueCallback(pumiceProceduralKnowledge);
                }
            }));
        }
        //TODO: to skip if only one item
        initDialog(listItemLabelRunnableList, null, null, "Select the script to execute", "Please select the script you want to execute.", false);
    }
}
