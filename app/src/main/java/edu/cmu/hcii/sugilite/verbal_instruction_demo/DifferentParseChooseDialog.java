package edu.cmu.hcii.sugilite.verbal_instruction_demo;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.WindowManager;
import android.widget.Toast;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.cmu.hcii.sugilite.Const;
import edu.cmu.hcii.sugilite.model.Node;
import edu.cmu.hcii.sugilite.R;
import edu.cmu.hcii.sugilite.ontology.SerializableUISnapshot;
import edu.cmu.hcii.sugilite.ontology.SugiliteSerializableEntity;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.server_comm.VerbalInstructionServerResults;

/**
 * @author toby
 * @date 12/10/17
 * @time 4:17 AM
 */
public class DifferentParseChooseDialog {
    //this dialog should allow the user to confirm the currently selected query, or switch to a different query
    private Context context;
    private AlertDialog dialog;
    private VerbalInstructionOverlayManager overlayManager;

    public DifferentParseChooseDialog(Context context, LayoutInflater inflater, VerbalInstructionOverlayManager overlayManager, List<VerbalInstructionServerResults.VerbalInstructionResult> allResults, SerializableUISnapshot serializableUISnapshot, String utterance){
        this.context = context;
        this.overlayManager = overlayManager;
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(Const.appNameUpperCase + " Verbal Instruction");

        List<String> parseList = new ArrayList<>();
        Map<String, VerbalInstructionServerResults.VerbalInstructionResult> formulaParseMap = new HashMap<>();
        for(VerbalInstructionServerResults.VerbalInstructionResult result : allResults){
            parseList.add(result.getFormula());
            formulaParseMap.put(result.getFormula(), result);
        }

        String[] parses = new String[parseList.size()];
        parses = parseList.toArray(parses);
        final String[] parsesClone = parses.clone();
        builder.setItems(parsesClone, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(which < allResults.size()) {
                    VerbalInstructionServerResults.VerbalInstructionResult chosenResult = allResults.get(which);

                    //refresh the overlay manager
                    Map<String, SugiliteSerializableEntity> idEntityMap = serializableUISnapshot.getSugiliteEntityIdSugiliteEntityMap();
                    List<Node> filteredNodes = new ArrayList<>();
                    Map<Node, String> filteredNodeNodeIdMap = new HashMap<>();
                    for (String nodeId : chosenResult.getGrounding()){
                        if (idEntityMap.containsKey(nodeId)) {
                            SugiliteSerializableEntity entity = idEntityMap.get(nodeId);
                            if (entity.getType().equals(Node.class)){
                                Node node = (Node)entity.getEntityValue();
                                if(node.getClickable()){
                                    filteredNodes.add(node);
                                    filteredNodeNodeIdMap.put(node, nodeId);
                                }
                            }
                        }
                        else{
                            continue;
                        }
                    }
                    if(filteredNodes.size() > 0){
                        //matched
                        overlayManager.removeOverlays();

                        //=== print debug info ===
                        System.out.println("MATCHED " + chosenResult.getId()  + ": " + chosenResult.getFormula());
                        int nodeCount = 0;
                        for(Node node : filteredNodes){
                            System.out.println("Node " + ++nodeCount + ": " + new Gson().toJson(node));
                        }
                        //=== done printing debug info ===

                        Toast.makeText(context, chosenResult.getFormula(), Toast.LENGTH_SHORT).show();
                        for(Node node : filteredNodes){
                            //TODO: show overlay

                            //node, nodeId, corresponding VerbalInstructionResult, VerbalInstructionResults
                            overlayManager.addOverlay(node, filteredNodeNodeIdMap.get(node), chosenResult, allResults, serializableUISnapshot, utterance);
                        }
                    }
                    else {
                        Toast.makeText(context, "No clickable element found for the selected parse!", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
        dialog = builder.create();
    }

    public void show(){
        dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
        dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_box);
        dialog.show();
    }
}
