package edu.cmu.hcii.sugilite.recording.newrecording.fullscreen_overlay;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.cmu.hcii.sugilite.Const;
import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.model.Node;
import edu.cmu.hcii.sugilite.ontology.OntologyQuery;
import edu.cmu.hcii.sugilite.ontology.SugiliteEntity;
import edu.cmu.hcii.sugilite.ontology.UISnapshot;
import edu.cmu.hcii.sugilite.pumice.dialog.demonstration.PumiceReadOutDemonstrationSelectionDialog;
import edu.cmu.hcii.sugilite.pumice.dialog.demonstration.PumiceValueDemonstrationSelectionDialog;
import edu.cmu.hcii.sugilite.recording.newrecording.SugiliteBlockBuildingHelper;

import static edu.cmu.hcii.sugilite.Const.OVERLAY_TYPE;

/**
 * @author toby
 * @date 1/7/19
 * @time 9:06 PM
 */
public class RecordingOverlayContextClickDialog {
    private AlertDialog dialog;
    private Context context;
    private float x, y;
    private FullScreenRecordingOverlayManager parentOverlayManager;
    private SugiliteEntity<Node> topLongClickableNode;
    private SugiliteEntity<Node> topClickableNode;
    private List<SugiliteEntity<Node>> matchedAllNodeEntities;
    private LayoutInflater layoutInflater;
    private SharedPreferences sharedPreferences;
    private List<String> supportedActions = new ArrayList<>();


    public RecordingOverlayContextClickDialog(Context context, FullScreenRecordingOverlayManager parentOverlayManager, SugiliteEntity<Node> topLongClickableNode, SugiliteEntity<Node> topClickableNode, List<SugiliteEntity<Node>> matchedAllNodeEntities, UISnapshot uiSnapshot, SugiliteData sugiliteData, TextToSpeech tts, float x, float y){
        this.context = context;
        this.parentOverlayManager = parentOverlayManager;
        this.x = x;
        this.y = y;
        this.topLongClickableNode = topLongClickableNode;
        this.topClickableNode = topClickableNode;
        this.matchedAllNodeEntities = matchedAllNodeEntities;
        this.layoutInflater = LayoutInflater.from(context);
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        ListView mainListView = new ListView(context);

        if (topLongClickableNode != null) {
            supportedActions.add("Long click on this item in the app");
        }

        if (topClickableNode != null) {
            supportedActions.add("Click on this item in the app");
        }

        if (getTextLabelNodeEntityMap().size() > 0){
            supportedActions.add("Record a \"read out\" operation");
            supportedActions.add("Select this value for Pumice to learn");
            //supportedActions.add("Mark this value as \"commute time\"");
        }




        String[] stringArray = new String[supportedActions.size()];
        stringArray = supportedActions.toArray(stringArray);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, stringArray);
        mainListView.setAdapter(adapter);

        if(getTextLabelNodeEntityMap().size() > 0) {
            builder.setTitle("You've long clicked on the item " + getTextLabelNodeEntityMap().keySet().toString() + ". What do you want to do?");
        } else {
            builder.setTitle("You've long clicked on the item. What do you want to do?");
        }

        builder.setView(mainListView);
        mainListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final String item = (String) parent.getItemAtPosition(position);
                switch (item){
                    case "Select this value for Pumice to learn":
                        //present a list of matched items with text labels for the user to select
                        dialog.dismiss();
                        //check if in a Pumice value concept learning session
                        //TODO: for debug use -- allow displaying this item while not in the recording mode
                        if(true || sugiliteData.valueDemonstrationVariableName != null && sugiliteData.valueDemonstrationVariableName.length() > 0) {
                            Map<String, SugiliteEntity<Node>> textLabelEntityMap = getTextLabelNodeEntityMap();
                            //handle the selected texts
                            PumiceValueDemonstrationSelectionDialog valueDemonstrationSelectionDialog = new PumiceValueDemonstrationSelectionDialog(context, textLabelEntityMap, uiSnapshot, parentOverlayManager, sugiliteData, tts, layoutInflater, sharedPreferences, x, y);
                            valueDemonstrationSelectionDialog.show();
                        } else {
                            Toast.makeText(context, "Not in a Pumice value concept learning session!!", Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case "Record a \"read out\" operation":
                        dialog.dismiss();
                        //check if recording is is progress
                        //TODO: for debug use -- allow displaying this item while not in the recording mode
                        if(true || sharedPreferences.getBoolean("recording_in_process", false)) {
                            Map<String, SugiliteEntity<Node>> textLabelEntityMap = getTextLabelNodeEntityMap();
                            //TODO: handle the selected texts
                            PumiceReadOutDemonstrationSelectionDialog readOutDemonstrationSelectionDialog = new PumiceReadOutDemonstrationSelectionDialog(context, textLabelEntityMap, uiSnapshot, parentOverlayManager, sugiliteData, tts, layoutInflater, sharedPreferences, x, y);
                            readOutDemonstrationSelectionDialog.show();
                        } else {
                            Toast.makeText(context, "Not in the recording mode!!", Toast.LENGTH_SHORT).show();
                        }

                        break;
                    case "Long click on this item in the app":
                        //send a long click to the underlying app
                        dialog.dismiss();
                        if (topLongClickableNode.getEntityValue() != null) {
                            OverlayClickedDialog overlayClickedDialog = new OverlayClickedDialog(context, topClickableNode, uiSnapshot, x, y, parentOverlayManager, parentOverlayManager.getOverlay(), sugiliteData, layoutInflater, sharedPreferences, tts, true);
                            overlayClickedDialog.show();
                        }
                        /*
                        parentOverlayManager.clickWithRootPermission(x, y, new Runnable() {
                            @Override
                            public void run() {
                                //allow the overlay to get touch event after finishing the simulated click
                                parentOverlayManager.setOverlayOnTouchListener(parentOverlayManager.getOverlay(), true);
                            }
                        }, topClickableNode.getEntityValue(), true);
                        */
                        break;
                    case "Click on this item in the app":
                        dialog.dismiss();
                        if (topClickableNode.getEntityValue() != null) {
                            OverlayClickedDialog overlayClickedDialog = new OverlayClickedDialog(context, topClickableNode, uiSnapshot, x, y, parentOverlayManager, parentOverlayManager.getOverlay(), sugiliteData, layoutInflater, sharedPreferences, tts, false);
                            overlayClickedDialog.show();
                        }
                }
            }
        });
        dialog = builder.create();
    }

    private Map<String, SugiliteEntity<Node>> getTextLabelNodeEntityMap(){
        Map<String, SugiliteEntity<Node>> textLabelEntityMap = new HashMap<>();

        for(SugiliteEntity<Node> node : matchedAllNodeEntities){
            if(node.getEntityValue() != null && node.getEntityValue().getText() != null){
                textLabelEntityMap.put(node.getEntityValue().getText(), node);
            }
        }
        System.out.println(matchedAllNodeEntities.toString());
        System.out.println("SELECTED TEXTS: " + textLabelEntityMap.keySet());
        return textLabelEntityMap;
    }


    public void show() {
        if (supportedActions.size() > 0) {
            if (dialog.getWindow() != null) {
                dialog.getWindow().setType(OVERLAY_TYPE);
            }
            dialog.show();
        }
    }

}
