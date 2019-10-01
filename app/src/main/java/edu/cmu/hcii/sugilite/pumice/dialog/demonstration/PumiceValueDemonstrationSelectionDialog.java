package edu.cmu.hcii.sugilite.pumice.dialog.demonstration;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.speech.tts.TextToSpeech;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.List;
import java.util.Map;

import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.model.Node;
import edu.cmu.hcii.sugilite.model.block.SugiliteOperationBlock;
import edu.cmu.hcii.sugilite.model.block.util.SugiliteAvailableFeaturePack;
import edu.cmu.hcii.sugilite.model.operation.trinary.SugiliteLoadVariableOperation;
import edu.cmu.hcii.sugilite.ontology.OntologyQuery;
import edu.cmu.hcii.sugilite.ontology.SugiliteEntity;
import edu.cmu.hcii.sugilite.ontology.SugiliteRelation;
import edu.cmu.hcii.sugilite.ontology.UISnapshot;
import edu.cmu.hcii.sugilite.ontology.description.OntologyDescriptionGenerator;
import edu.cmu.hcii.sugilite.pumice.PumiceDemonstrationUtil;
import edu.cmu.hcii.sugilite.recording.newrecording.SugiliteBlockBuildingHelper;
import edu.cmu.hcii.sugilite.recording.newrecording.fullscreen_overlay.FullScreenRecordingOverlayManager;
import edu.cmu.hcii.sugilite.recording.newrecording.fullscreen_overlay.SugiliteRecordingConfirmationDialog;

import static edu.cmu.hcii.sugilite.Const.OVERLAY_TYPE;


/**
 * @author toby
 * @date 1/8/19
 * @time 10:19 AM
 */

/**
 * dialog for selecting the text value if the user's long press matches multiple -- constructed and called from RecordingOverlayContextClickDialog
 */
public class PumiceValueDemonstrationSelectionDialog {
    private AlertDialog dialog;
    private Context context;
    private Map<String, SugiliteEntity<Node>> textLabelEntityMap;
    private UISnapshot uiSnapshot;
    private OntologyDescriptionGenerator ontologyDescriptionGenerator;
    private SugiliteBlockBuildingHelper blockBuildingHelper;
    private FullScreenRecordingOverlayManager recordingOverlayManager;
    private float x, y;
    private SugiliteData sugiliteData;
    private SharedPreferences sharedPreferences;
    private LayoutInflater layoutInflater;
    private TextToSpeech tts;




    public PumiceValueDemonstrationSelectionDialog(Context context, Map<String, SugiliteEntity<Node>> textLabelEntityMap, UISnapshot uiSnapshot, FullScreenRecordingOverlayManager recordingOverlayManager, SugiliteData sugiliteData, TextToSpeech tts, LayoutInflater layoutInflater, SharedPreferences sharedPreferences, float x, float y) {
        this.context = context;
        this.textLabelEntityMap = textLabelEntityMap;
        this.uiSnapshot = uiSnapshot;
        this.sugiliteData = sugiliteData;
        this.sharedPreferences = sharedPreferences;
        this.layoutInflater = layoutInflater;
        this.ontologyDescriptionGenerator = new OntologyDescriptionGenerator();
        this.blockBuildingHelper = new SugiliteBlockBuildingHelper(context, sugiliteData);
        this.recordingOverlayManager = recordingOverlayManager;
        this.tts = tts;
        this.x = x;
        this.y = y;
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        ListView mainListView = new ListView(context);
        String[] stringArray = new String[textLabelEntityMap.size()];
        stringArray = textLabelEntityMap.keySet().toArray(stringArray);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, stringArray);
        mainListView.setAdapter(adapter);
        builder.setTitle("Which text value would you like to select?");
        builder.setView(mainListView);
        mainListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final String item = (String) parent.getItemAtPosition(position);
                SugiliteEntity<Node> selectedNode = textLabelEntityMap.get(item);
                processSelectedItem(item, selectedNode, uiSnapshot);
            }
        });
        dialog = builder.create();

    }

    public void show() {
        if (textLabelEntityMap.size() == 1){
            //skip showing this dialog if there is only one option
            for(Map.Entry<String, SugiliteEntity<Node>> entry : textLabelEntityMap.entrySet()){
                processSelectedItem(entry.getKey(), entry.getValue(), uiSnapshot);
                break;
            }
        } else {
            if (dialog.getWindow() != null) {
                dialog.getWindow().setType(OVERLAY_TYPE);
            }
            dialog.show();
        }
    }

    private void processSelectedItem (String text, SugiliteEntity<Node> nodeEntity, UISnapshot uiSnapshot){
        //generate the feature pack
        SugiliteAvailableFeaturePack featurePack = new SugiliteAvailableFeaturePack(nodeEntity, uiSnapshot);
        List<Pair<OntologyQuery, Double>> queryScoreList = SugiliteBlockBuildingHelper.generateDefaultQueries(uiSnapshot, nodeEntity, SugiliteRelation.HAS_TEXT, SugiliteRelation.HAS_CHILD_TEXT);

        //TODO: determine if the data description is ambiguous
        if (queryScoreList.size() > 0){
            OntologyQuery selectedQuery = queryScoreList.get(0).first;

            //create a extract operation
            SugiliteLoadVariableOperation loadVariableOperation = new SugiliteLoadVariableOperation();
            loadVariableOperation.setPropertyToSave("hasText");
            loadVariableOperation.setQuery(selectedQuery);
            if(sugiliteData.valueDemonstrationVariableName != null && sugiliteData.valueDemonstrationVariableName.length() > 0) {
                loadVariableOperation.setVariableName(sugiliteData.valueDemonstrationVariableName);
            } else {
                loadVariableOperation.setVariableName("RETURN_VALUE");
            }

            final SugiliteOperationBlock operationBlock = new SugiliteOperationBlock();
            operationBlock.setOperation(loadVariableOperation);
            operationBlock.setFeaturePack(featurePack);
            operationBlock.setScreenshot(featurePack.screenshot);

            //description is set
            operationBlock.setDescription(ontologyDescriptionGenerator.getDescriptionForOperation(loadVariableOperation, selectedQuery));

            //need to run on ui thread
            showConfirmation(operationBlock, featurePack, queryScoreList, nodeEntity);


        } else {
            PumiceDemonstrationUtil.showSugiliteToast("Failed to generate data description!", Toast.LENGTH_SHORT);
        }

    }

    /**
     * show the popup for recording confirmation
     *
     * @param block
     * @param featurePack
     * @param queryScoreList
     */
    private void showConfirmation(SugiliteOperationBlock block, SugiliteAvailableFeaturePack featurePack, List<Pair<OntologyQuery, Double>> queryScoreList, SugiliteEntity<Node> nodeEntity) {
        Runnable clickRunnable = new Runnable() {
            @Override
            public void run() {
                //recordingOverlayManager.clickNode(nodeEntity.getEntityValue(), x, y, recordingOverlayManager.getOverlay(), false);
                PumiceDemonstrationUtil.showSugiliteToast("Value query saved!", Toast.LENGTH_SHORT);
            }
        };
        SugiliteRecordingConfirmationDialog confirmationDialog = new SugiliteRecordingConfirmationDialog(context, block, featurePack, queryScoreList, clickRunnable, blockBuildingHelper, layoutInflater, uiSnapshot, nodeEntity, sugiliteData, sharedPreferences, tts);
        confirmationDialog.show();
    }

}
