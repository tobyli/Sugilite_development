package edu.cmu.hcii.sugilite.recording.newrecording.fullscreen_overlay;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.speech.tts.TextToSpeech;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import edu.cmu.hcii.sugilite.Const;
import edu.cmu.hcii.sugilite.model.Node;
import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.model.block.util.SugiliteAvailableFeaturePack;
import edu.cmu.hcii.sugilite.model.block.SugiliteOperationBlock;
import edu.cmu.hcii.sugilite.model.operation.SugiliteOperation;
import edu.cmu.hcii.sugilite.ontology.SerializableOntologyQuery;
import edu.cmu.hcii.sugilite.ontology.SugiliteEntity;
import edu.cmu.hcii.sugilite.ontology.UISnapshot;
import edu.cmu.hcii.sugilite.recording.newrecording.SugiliteBlockBuildingHelper;


/**
 * @author toby
 * @date 2/7/18
 * @time 7:39 PM
 */

/**
 * dummy dialog -> will lead to either RecordingAmbiguousPopupDialog or SugiliteRecordingConfirmationDialog
 */
public class OverlayClickedDialog {
    private Context context;
    private SugiliteEntity<Node> node;
    private UISnapshot uiSnapshot;
    private LayoutInflater layoutInflater;
    private float x, y;
    private View overlay;
    private SugiliteAvailableFeaturePack featurePack;
    private Dialog dialog;
    private TextToSpeech tts;
    private FullScreenRecordingOverlayManager recordingOverlayManager;
    private SugiliteBlockBuildingHelper blockBuildingHelper;
    private SharedPreferences sharedPreferences;
    private SugiliteData sugiliteData;


    public OverlayClickedDialog(Context context, SugiliteEntity<Node> node, UISnapshot uiSnapshot, float x, float y, FullScreenRecordingOverlayManager recordingOverlayManager, View overlay, SugiliteData sugiliteData, LayoutInflater layoutInflater, SharedPreferences sharedPreferences, TextToSpeech tts) {
        this.context = context;
        this.node = node;
        this.uiSnapshot = uiSnapshot;
        this.layoutInflater = layoutInflater;
        this.overlay = overlay;
        this.x = x;
        this.y = y;
        this.tts = tts;
        this.recordingOverlayManager = recordingOverlayManager;
        this.blockBuildingHelper = new SugiliteBlockBuildingHelper(context, sugiliteData);
        this.sugiliteData = sugiliteData;
        this.sharedPreferences = sharedPreferences;
        featurePack = new SugiliteAvailableFeaturePack(node, uiSnapshot);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(Const.appNameUpperCase + " Demonstration");

        List<String> operationList = new ArrayList<>();

        //fill in the options
        operationList.add("Record");
        operationList.add("Click without Recording");
        operationList.add("Cancel");
        String[] operations = new String[operationList.size()];
        operations = operationList.toArray(operations);
        final String[] operationClone = operations.clone();


        builder.setItems(operationClone, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (operationClone[which]) {
                    case "Record":
                        //recording
                        handleRecording();
                        dialog.dismiss();
                        break;
                    case "Click without Recording":
                        recordingOverlayManager.clickNode(node.getEntityValue(), x, y, overlay, false);
                        dialog.dismiss();
                        break;
                    case "Cancel":
                        dialog.dismiss();
                        break;
                }
            }
        });
        dialog = builder.create();
    }

    /**
     * handle when the operation is to be recorded
     */
    private void handleRecording() {
        List<Pair<SerializableOntologyQuery, Double>> queryScoreList = SugiliteBlockBuildingHelper.generateDefaultQueries(featurePack, uiSnapshot, false);
        if (queryScoreList.size() > 0) {
            System.out.println("Query Score List: " + queryScoreList);

            //threshold for determine whether the results are ambiguous
            /*
            if (queryScoreList.size() <= 1 || (queryScoreList.get(1).second.doubleValue() - queryScoreList.get(0).second.doubleValue() >= 2)) {
                //not ambiguous, show the confirmation popup
                SugiliteOperationBlock block = blockBuildingHelper.getOperationBlockFromQuery(queryScoreList.get(0).first, SugiliteOperation.CLICK, featurePack);
                showConfirmation(block, featurePack, queryScoreList);

            } else {
                //ask for clarification if ambiguous
                //need to run on ui thread
                showAmbiguousPopup(queryScoreList, featurePack, node);
            }
            */

            //TODO: 19/03/11 temporarily disable the ambiguous pop-up for PUMICE study
            SugiliteOperationBlock block = blockBuildingHelper.getOperationBlockFromQuery(queryScoreList.get(0).first, SugiliteOperation.CLICK, featurePack);
            showConfirmation(block, featurePack, queryScoreList);
        } else {
            //empty result
            Toast.makeText(context, "Empty Results!", Toast.LENGTH_SHORT).show();
        }
    }


    public void show() {
        /*
        dialog.getWindow().setType(OVERLAY_TYPE);
        dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_box);
        dialog.show();
        */


        //TODO: bypass the dialog
        handleRecording();
    }

    /**
     * show the popup for choosing from ambiguous options
     *
     * @param queryScoreList
     * @param featurePack
     */
    //TODO: add support for verbal instruction here
    private void showAmbiguousPopup(List<Pair<SerializableOntologyQuery, Double>> queryScoreList, SugiliteAvailableFeaturePack featurePack, SugiliteEntity<Node> actualClickedNode) {
        RecordingAmbiguousPopupDialog recordingAmbiguousPopupDialog = new RecordingAmbiguousPopupDialog(context, queryScoreList, featurePack, blockBuildingHelper, layoutInflater, new Runnable() {
            @Override
            public void run() {
                recordingOverlayManager.clickNode(node.getEntityValue(), x, y, overlay, false);
            }
        },
                uiSnapshot, actualClickedNode, sugiliteData, sharedPreferences, tts, 0);
        recordingAmbiguousPopupDialog.show();
    }

    /**
     * show the popup for recording confirmation
     *
     * @param block
     * @param featurePack
     * @param queryScoreList
     */
    private void showConfirmation(SugiliteOperationBlock block, SugiliteAvailableFeaturePack featurePack, List<Pair<SerializableOntologyQuery, Double>> queryScoreList) {
        Runnable clickRunnable = new Runnable() {
            @Override
            public void run() {
                recordingOverlayManager.clickNode(node.getEntityValue(), x, y, overlay, false);
            }
        };
        SugiliteRecordingConfirmationDialog confirmationDialog = new SugiliteRecordingConfirmationDialog(context, block, featurePack, queryScoreList, clickRunnable, blockBuildingHelper, layoutInflater, uiSnapshot, node, sugiliteData, sharedPreferences, tts);
        confirmationDialog.show();
    }
}
