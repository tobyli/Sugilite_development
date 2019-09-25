package edu.cmu.hcii.sugilite.recording.newrecording;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.text.Html;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.cmu.hcii.sugilite.accessibility_service.SugiliteAccessibilityService;
import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.model.Node;
import edu.cmu.hcii.sugilite.model.block.util.SugiliteAvailableFeaturePack;
import edu.cmu.hcii.sugilite.model.block.SugiliteOperationBlock;
import edu.cmu.hcii.sugilite.model.operation.SugiliteOperation;
import edu.cmu.hcii.sugilite.ontology.OntologyQuery;
import edu.cmu.hcii.sugilite.ontology.SugiliteEntity;
import edu.cmu.hcii.sugilite.ontology.UISnapshot;
import edu.cmu.hcii.sugilite.recording.ReadableDescriptionGenerator;

import static edu.cmu.hcii.sugilite.Const.OVERLAY_TYPE;

/**
 * @author toby
 * @date 1/5/18
 * @time 4:59 PM
 */

//singleton class
public class NewDemonstrationHandler {
    private SugiliteData sugiliteData;
    private SharedPreferences sharedPreferences;
    private LayoutInflater layoutInflater;
    private SugiliteBlockBuildingHelper blockBuildingHelper;
    private ReadableDescriptionGenerator readableDescriptionGenerator;
    private SugiliteAccessibilityService accessibilityService;

    private static NewDemonstrationHandler instance = null;

    public static NewDemonstrationHandler getInstance(SugiliteData sugiliteData, LayoutInflater layoutInflater, SharedPreferences sharedPreferences, SugiliteAccessibilityService accessibilityService){
        if (instance == null) {
            instance = new NewDemonstrationHandler(sugiliteData, SugiliteData.getAppContext(), layoutInflater, sharedPreferences, accessibilityService);
        }
        return instance;
    }

    public static NewDemonstrationHandler getInstance(){
        return instance;
    }

    private NewDemonstrationHandler(SugiliteData sugiliteData, Context context, LayoutInflater layoutInflater, SharedPreferences sharedPreferences, SugiliteAccessibilityService accessibilityService){
        this.sugiliteData = sugiliteData;
        this.accessibilityService = accessibilityService;;
        this.sharedPreferences = sharedPreferences;
        this.layoutInflater = layoutInflater;
        this.blockBuildingHelper = new SugiliteBlockBuildingHelper(context, sugiliteData);
        this.readableDescriptionGenerator = new ReadableDescriptionGenerator(context);
    }

    //handles the demonstration

    /**
     * this method is for handling demonstration from an accessibility event
     * @param featurePack
     * @param availableAlternatives
     * @param uiSnapshot
     */
    public void handleEvent(SugiliteAvailableFeaturePack featurePack, Set<Map.Entry<String, String>> availableAlternatives, UISnapshot uiSnapshot){
        System.out.println("HANDLE");
        //determine if disambiguation is needed

        //extract the targetEntity from uiSnapshot based on featurePack
        SugiliteEntity<Node> targetEntity = null;
        if(uiSnapshot != null) {
            for (Map.Entry<Node, SugiliteEntity<Node>> entityEntry : uiSnapshot.getNodeSugiliteEntityMap().entrySet()) {
                if (entityEntry.getKey().getBoundsInScreen().equals(featurePack.boundsInScreen) &&
                        entityEntry.getKey().getClassName().equals(featurePack.className)) {
                    //found
                    targetEntity = entityEntry.getValue();
                    break;
                }
            }
        }

        if (targetEntity == null) {
            return;
        }


        //show the confirmation popup if not ambiguous
        List<Pair<OntologyQuery, Double>> queryScoreList = SugiliteBlockBuildingHelper.generateDefaultQueries(uiSnapshot, targetEntity);
        if(queryScoreList.size() > 0) {
            //threshold for determine whether the results are ambiguous
            if (queryScoreList.size() <= 1 || (queryScoreList.get(1).second.intValue() - queryScoreList.get(0).second.intValue() > 2)) {
                //not ambiguous, show the confirmation popup
                SugiliteOperationBlock block = blockBuildingHelper.getUnaryOperationBlockWithOntologyQueryFromQuery(queryScoreList.get(0).first, SugiliteOperation.CLICK, featurePack);

                //need to run on ui thread
                SugiliteData.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showConfirmation(block, featurePack, queryScoreList);
                    }
                });

            }
            else{
                //ask for clarification if ambiguous
                //need to run on ui thread
                SugiliteData.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(SugiliteData.getAppContext(), "Ambiguous!", Toast.LENGTH_SHORT).show();
                        showAmbiguousPopup(queryScoreList, featurePack);
                    }
                });
            }
        }
        else{
            //empty result
            SugiliteData.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(SugiliteData.getAppContext(), "Empty Results!", Toast.LENGTH_SHORT).show();
                }
            });
        }

    }


    public void showAmbiguousPopup(List<Pair<OntologyQuery, Double>> queryScoreList, SugiliteAvailableFeaturePack featurePack){
        //the temporary popup to show for when the demonstration is ambiguous
        AlertDialog.Builder builder = new AlertDialog.Builder(SugiliteData.getAppContext());
        builder.setTitle("Select from disambiguation results");
        ListView mainListView = new ListView(SugiliteData.getAppContext());
        Map<TextView, SugiliteOperationBlock> textViews = new HashMap<>();
        String[] stringArray = new String[queryScoreList.size()];
        SugiliteOperationBlock[] sugiliteOperationBlockArray = new SugiliteOperationBlock[queryScoreList.size()];

        int i = 0;
        for(Pair<OntologyQuery, Double> entry : queryScoreList){
            SugiliteOperationBlock block = blockBuildingHelper.getUnaryOperationBlockWithOntologyQueryFromQuery(entry.first, SugiliteOperation.CLICK, featurePack);
            sugiliteOperationBlockArray[i++] = block;
        }

        Map<SugiliteOperationBlock, String> descriptions = blockBuildingHelper.getDescriptionsInDifferences(sugiliteOperationBlockArray);

        i = 0;
        for(SugiliteOperationBlock block : sugiliteOperationBlockArray){
            stringArray[i++] = descriptions.get(block);
        }


        ArrayAdapter<String> adapter = new ArrayAdapter<String>(SugiliteData.getAppContext(), android.R.layout.simple_list_item_1, stringArray)
        {
            //override the arrayadapter to show HTML-styled textviews in the listview
            @Override
            public View getView(int position, View convertView, ViewGroup parent)
            {
                View row;
                if (null == convertView) {
                    row = layoutInflater.inflate(android.R.layout.simple_list_item_1, null);
                } else {
                    row = convertView;
                }
                TextView tv = (TextView) row.findViewById(android.R.id.text1);
                tv.setText(Html.fromHtml(getItem(position)));
                textViews.put(tv, sugiliteOperationBlockArray[position]);
                return row;
            }

        };
        mainListView.setAdapter(adapter);
        builder.setView(mainListView);
        AlertDialog dialog = builder.create();
        mainListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //showConfirmation(sugiliteOperationBlockArray[position], featurePack, queryScoreList);
                blockBuildingHelper.saveBlock(sugiliteOperationBlockArray[position], featurePack);
                dialog.dismiss();
            }
        });
        dialog.getWindow().setType(OVERLAY_TYPE);
        dialog.show();
    }

    private void showConfirmation(SugiliteOperationBlock block, SugiliteAvailableFeaturePack featurePack, List<Pair<OntologyQuery, Double>> queryScoreList){
        AlertDialog.Builder builder = new AlertDialog.Builder(SugiliteData.getAppContext());
        String newDescription = readableDescriptionGenerator.generateDescriptionForVerbalBlock(block, blockBuildingHelper.stripOntologyQuery(block.getOperation().getDataDescriptionQueryIfAvailable()).toString(), "UTTERANCE");
        builder.setTitle("Save Operation Confirmation").setMessage(Html.fromHtml("Are you sure you want to record the operation: " + newDescription));
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //save the block
                        blockBuildingHelper.saveBlock(block, featurePack);
                    }
                })
                .setNegativeButton("Skip", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                })
                .setNeutralButton("Edit", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        showAmbiguousPopup(queryScoreList, featurePack);
                    }
                });
        final AlertDialog dialog = builder.create();
        dialog.getWindow().setType(OVERLAY_TYPE);
        dialog.show();
    }
}
