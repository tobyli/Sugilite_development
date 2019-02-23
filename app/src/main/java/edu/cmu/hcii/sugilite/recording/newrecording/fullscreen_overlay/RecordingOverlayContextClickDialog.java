package edu.cmu.hcii.sugilite.recording.newrecording.fullscreen_overlay;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.model.Node;
import edu.cmu.hcii.sugilite.ontology.SugiliteEntity;
import edu.cmu.hcii.sugilite.ontology.UISnapshot;
import edu.cmu.hcii.sugilite.pumice.dialog.demonstration.PumiceValueDemonstrationSelectionDialog;

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
    private List<SugiliteEntity<Node>> matchedAllNodeEntities;
    private String variableName;
    private List<String> supportedActions = new ArrayList<>();


    public RecordingOverlayContextClickDialog(Context context, FullScreenRecordingOverlayManager parentOverlayManager, SugiliteEntity<Node> topLongClickableNode, List<SugiliteEntity<Node>> matchedAllNodeEntities, UISnapshot uiSnapshot, String variableName, SugiliteData sugiliteData, TextToSpeech tts, float x, float y){
        this.context = context;
        this.parentOverlayManager = parentOverlayManager;
        this.x = x;
        this.y = y;
        this.topLongClickableNode = topLongClickableNode;
        this.matchedAllNodeEntities = matchedAllNodeEntities;
        this.variableName = variableName;
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        ListView mainListView = new ListView(context);

        if (topLongClickableNode != null) {
            supportedActions.add("Long click on the underlying app");
        }

        if (getTextLabelNodeEntityMap().size() > 0){
            supportedActions.add("Select this item for its value");
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
                    case "Select this item for its value":
                        //present a list of matched items with text labels for the user to select
                        dialog.dismiss();
                        Map<String, SugiliteEntity<Node>> textLabelEntityMap = getTextLabelNodeEntityMap();
                        System.out.println(matchedAllNodeEntities.toString());
                        System.out.println("SELECTED TEXTS: " + textLabelEntityMap.keySet());

                        LayoutInflater layoutInflater = LayoutInflater.from(context);
                        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

                        //TODO: handle the selected texts
                        PumiceValueDemonstrationSelectionDialog valueDemonstrationSelectionDialog = new PumiceValueDemonstrationSelectionDialog(context, textLabelEntityMap, uiSnapshot, variableName, parentOverlayManager, sugiliteData, tts, layoutInflater, sharedPreferences, x, y);
                        valueDemonstrationSelectionDialog.show();
                        break;
                    case "Long click on the underlying app":
                        //send a long click to the underlying app
                        dialog.dismiss();
                        parentOverlayManager.addSugiliteOperationBlockBasedOnNode(topLongClickableNode.getEntityValue(), true);
                        /*
                        parentOverlayManager.clickWithRootPermission(x, y, new Runnable() {
                            @Override
                            public void run() {
                                //allow the overlay to get touch event after finishing the simulated click
                                parentOverlayManager.setOverlayOnTouchListener(parentOverlayManager.getOverlay(), true);
                            }
                        }, topClickableNode.getEntityValue(), true);
                        */
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
