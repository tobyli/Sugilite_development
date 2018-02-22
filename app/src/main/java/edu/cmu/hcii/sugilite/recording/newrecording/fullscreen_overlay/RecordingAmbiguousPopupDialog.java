package edu.cmu.hcii.sugilite.recording.newrecording.fullscreen_overlay;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.speech.tts.TextToSpeech;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.cmu.hcii.sugilite.Node;
import edu.cmu.hcii.sugilite.R;
import edu.cmu.hcii.sugilite.SugiliteAccessibilityService;
import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.model.block.SugiliteAvailableFeaturePack;
import edu.cmu.hcii.sugilite.model.block.SugiliteOperationBlock;
import edu.cmu.hcii.sugilite.model.operation.SugiliteOperation;
import edu.cmu.hcii.sugilite.ontology.OntologyQuery;
import edu.cmu.hcii.sugilite.ontology.OntologyQueryUtils;
import edu.cmu.hcii.sugilite.ontology.SerializableOntologyQuery;
import edu.cmu.hcii.sugilite.ontology.SerializableUISnapshot;
import edu.cmu.hcii.sugilite.ontology.SugiliteEntity;
import edu.cmu.hcii.sugilite.ontology.SugiliteRelation;
import edu.cmu.hcii.sugilite.ontology.SugiliteSerializableEntity;
import edu.cmu.hcii.sugilite.ontology.UISnapshot;
import edu.cmu.hcii.sugilite.recording.ReadableDescriptionGenerator;
import edu.cmu.hcii.sugilite.recording.newrecording.SugiliteBlockBuildingHelper;
import edu.cmu.hcii.sugilite.recording.newrecording.dialog_management.SugiliteDialogManager;
import edu.cmu.hcii.sugilite.recording.newrecording.dialog_management.SugiliteDialogSimpleState;
import edu.cmu.hcii.sugilite.recording.newrecording.dialog_management.SugiliteDialogUtteranceFilter;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.VerbalInstructionRecordingManager;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.server_comm.SugiliteVerbalInstructionHTTPQueryInterface;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.server_comm.SugiliteVerbalInstructionHTTPQueryManager;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.server_comm.VerbalInstructionServerQuery;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.server_comm.VerbalInstructionServerResults;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.speech.SugiliteVoiceRecognitionListener;

/**
 * @author toby
 * @date 2/11/18
 * @time 11:55 PM
 */
public class RecordingAmbiguousPopupDialog extends SugiliteDialogManager implements SugiliteVerbalInstructionHTTPQueryInterface {
    private List<Map.Entry<SerializableOntologyQuery, Double>> queryScoreList;
    private SugiliteAvailableFeaturePack featurePack;
    private EditText verbalInstructionEditText;
    private SugiliteVerbalInstructionHTTPQueryManager sugiliteVerbalInstructionHTTPQueryManager;
    private Dialog dialog;
    private View dialogView;
    private AlertDialog progressDialog;
    private Gson gson;
    private ImageButton speakButton;
    private SerializableUISnapshot serializableUISnapshot;
    private UISnapshot uiSnapshot;
    private Node actualClickedNode;
    private SugiliteBlockBuildingHelper blockBuildingHelper;
    private Runnable clickRunnable;
    private ReadableDescriptionGenerator readableDescriptionGenerator;
    private LayoutInflater layoutInflater;
    private VerbalInstructionRecordingManager verbalInstructionRecordingManager;
    private SugiliteData sugiliteData;
    private SharedPreferences sharedPreferences;

    //states
    private SugiliteDialogSimpleState askingForVerbalInstructionState = new SugiliteDialogSimpleState("ASKING_FOR_VERBAL_INSTRUCTION", this);
    private SugiliteDialogSimpleState askingForInstructionConfirmationState = new SugiliteDialogSimpleState("ASKING_FOR_INSTRUCTION_CONFIRMATION", this);
    private SugiliteDialogSimpleState emptyResultState = new SugiliteDialogSimpleState("EMPTY_RESULT_STATE", this);


    public RecordingAmbiguousPopupDialog(Context context, List<Map.Entry<SerializableOntologyQuery, Double>> queryScoreList, SugiliteAvailableFeaturePack featurePack, SugiliteBlockBuildingHelper blockBuildingHelper, LayoutInflater layoutInflater, Runnable clickRunnable, UISnapshot uiSnapshot, Node actualClickedNode, SugiliteData sugiliteData, SharedPreferences sharedPreferences, TextToSpeech tts) {
        super(context, tts);
        this.queryScoreList = queryScoreList;
        this.featurePack = featurePack;
        this.sugiliteVerbalInstructionHTTPQueryManager = new SugiliteVerbalInstructionHTTPQueryManager(this, sharedPreferences);
        this.uiSnapshot = uiSnapshot;
        this.serializableUISnapshot = new SerializableUISnapshot(uiSnapshot);
        this.actualClickedNode = actualClickedNode;
        this.blockBuildingHelper = blockBuildingHelper;
        this.clickRunnable = clickRunnable;
        this.layoutInflater = layoutInflater;
        this.sugiliteData = sugiliteData;
        this.sharedPreferences = sharedPreferences;
        this.gson = new Gson();
        this.readableDescriptionGenerator = new ReadableDescriptionGenerator(context);
        this.verbalInstructionRecordingManager = new VerbalInstructionRecordingManager(context, sugiliteData, sharedPreferences);


        //build the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        //builder.setTitle("Select from disambiguation results");
        dialogView = layoutInflater.inflate(R.layout.dialog_ambiguous_popup_spoken, null);

        //set the list view for query parse candidates
        ListView mainListView = (ListView) dialogView.findViewById(R.id.listview_query_candidates);
        verbalInstructionEditText = (EditText) dialogView.findViewById(R.id.edittext_instruction_content);
        Map<TextView, SugiliteOperationBlock> textViews = new HashMap<>();
        String[] stringArray = new String[queryScoreList.size()];
        SugiliteOperationBlock[] sugiliteOperationBlockArray = new SugiliteOperationBlock[queryScoreList.size()];

        int i = 0;
        for (Map.Entry<SerializableOntologyQuery, Double> entry : queryScoreList) {
            SugiliteOperationBlock block = blockBuildingHelper.getOperationFromQuery(entry.getKey(), SugiliteOperation.CLICK, featurePack);
            sugiliteOperationBlockArray[i++] = block;
        }

        Map<SugiliteOperationBlock, String> descriptions = blockBuildingHelper.getDescriptionsInDifferences(sugiliteOperationBlockArray);

        i = 0;
        for (SugiliteOperationBlock block : sugiliteOperationBlockArray) {
            stringArray[i++] = descriptions.get(block);
        }


        ArrayAdapter<String> adapter = new ArrayAdapter<String>(context, android.R.layout.simple_list_item_1, stringArray) {
            //override the arrayadapter to show HTML-styled textviews in the listview
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
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
        //finished setting up the parse result candidate list


        //initiate the speak button
        speakButton = (ImageButton) dialogView.findViewById(R.id.button_verbal_instruction_talk);
        speakButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // speak button
                if (isListening() || tts.isSpeaking()) {
                    stopASRandTTS();
                } else {
                    initDialogManager();
                }
            }
        });


        builder.setView(dialogView);

        //set the buttons
        builder.setPositiveButton("Send Instruction", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                sendInstructionButtonOnClick();
            }
        }).setNegativeButton("Skip", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                skipButtonOnClick();
            }
        }).setNeutralButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        //on item click for query candidates
        mainListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //showConfirmation(sugiliteOperationBlockArray[position], featurePack, queryScoreList);
                if (sharedPreferences.getBoolean("recording_in_process", false)) {
                    try {
                        blockBuildingHelper.saveBlock(sugiliteOperationBlockArray[position], featurePack);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                clickRunnable.run();
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        dialog = builder.create();
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                stopASRandTTS();
            }
        });
    }

    static private boolean isSameNode(Node a, Node b) {
        if (a.getClassName() != null && (!a.getClassName().equals(b.getClassName()))) {
            return false;
        }
        if (a.getPackageName() != null && (!a.getPackageName().equals(b.getPackageName()))) {
            return false;
        }
        if (a.getBoundsInScreen() != null && (!a.getBoundsInScreen().equals(b.getBoundsInScreen()))) {
            return false;
        }
        return true;
    }

    private void skipButtonOnClick(){
        clickRunnable.run();
        dialog.cancel();
    }

    public void show() {
        dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        dialog.show();

        //initiate the dialog manager when the dialog is shown
        initDialogManager();
    }

    private void showProgressDialog() {
        progressDialog = new AlertDialog.Builder(context).setMessage("Processing the query ...").create();
        progressDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.show();
    }

    private void sendInstructionButtonOnClick() {
        //send the instruction out to the server for semantic parsing
        if (verbalInstructionEditText != null) {
            String userInput = verbalInstructionEditText.getText().toString();
            //send out the ASR result
            VerbalInstructionServerQuery query = new VerbalInstructionServerQuery(userInput, serializableUISnapshot.triplesToString());
            //send the query
            Thread thread = new Thread() {
                @Override
                public void run() {
                    try {
                        sugiliteVerbalInstructionHTTPQueryManager.sendQueryRequest(query);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            };
            thread.start();

            //show loading popup
            dialog.dismiss();
            showProgressDialog();
        }
    }

    @Override
    /**
     * callback for the HTTP query
     */
    public void resultReceived(int responseCode, String result) {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        //raw response
        System.out.print(responseCode + ": " + result);

        //de-serialize to VerbalInstructionResults
        VerbalInstructionServerResults results = gson.fromJson(result, VerbalInstructionServerResults.class);

        if (results.getQueries() == null) {
            //error in parsing the server reply
            Toast.makeText(context, String.valueOf(responseCode) + ": Error in parsing server reply", Toast.LENGTH_SHORT).show();
            dialog.show();
            setCurrentState(emptyResultState);
            initPrompt();
            return;
        }

        //print for debug purpose
        for (VerbalInstructionServerResults.VerbalInstructionResult verbalInstructionResult : results.getQueries()) {
            System.out.println(gson.toJson(verbalInstructionResult));
        }

        //find matches
        List<Map.Entry<OntologyQuery, List<Node>>> matchingQueriesMatchedNodesList = new ArrayList<>();

        for (VerbalInstructionServerResults.VerbalInstructionResult verbalInstructionResult : results.getQueries()) {
            boolean matched = false;
            List<Node> filteredNodes = new ArrayList<>();
            Map<Node, Integer> filteredNodeNodeIdMap = new HashMap<>();

            //construct the query, run the query, and compare the result against the actually clicked on node

            String queryFormula = verbalInstructionResult.getFormula();
            OntologyQuery query = OntologyQueryUtils.getQueryWithClassAndPackageConstraints(OntologyQuery.deserialize(queryFormula), actualClickedNode);
            Set<SugiliteEntity> queryResults =  query.executeOn(uiSnapshot);

            for(SugiliteEntity entity : queryResults){
                if(entity.getType().equals(Node.class)){
                    Node node = (Node) entity.getEntityValue();
                    if (node.getClickable()) {
                        filteredNodes.add(node);
                        filteredNodeNodeIdMap.put(node, entity.getEntityId());
                    }
                    if (isSameNode(actualClickedNode, node)) {
                        matched = true;
                    }
                }
            }

            if (filteredNodes.size() > 0 && matched) {
                //matched, add the result to the list
                matchingQueriesMatchedNodesList.add(new AbstractMap.SimpleEntry<>(query, filteredNodes));
            }
        }

        Collections.sort(matchingQueriesMatchedNodesList, new Comparator<Map.Entry<OntologyQuery, List<Node>>>() {
            @Override
            public int compare(Map.Entry<OntologyQuery, List<Node>> o1, Map.Entry<OntologyQuery, List<Node>> o2) {
                if(o1.getValue().size() != o2.getValue().size()){
                    return o1.getValue().size() - o2.getValue().size();
                }
                else{
                    return o1.getKey().toString().length() - o2.getKey().toString().length();
                }
            }
        });

        //TODO: sort the list by the size of matched node and length, and see if the top result has filteredNodes.size() = 1
        if (matchingQueriesMatchedNodesList != null && (!matchingQueriesMatchedNodesList.isEmpty())) {
            OntologyQuery query = matchingQueriesMatchedNodesList.get(0).getKey();

            //TODO: check if this has filteredNodes.size() = 1
            if(matchingQueriesMatchedNodesList.get(0).getValue().size() == 1) {
                //save the block and show a confirmation dialog for the block
                Toast.makeText(context, query.toString(), Toast.LENGTH_SHORT).show();

                //construct the block from the query formula
                OntologyQuery parentQuery = OntologyQueryUtils.getQueryWithClassAndPackageConstraints(query, actualClickedNode);
                SerializableOntologyQuery serializableOntologyQuery = new SerializableOntologyQuery(parentQuery);

                SugiliteOperationBlock block = blockBuildingHelper.getOperationFromQuery(serializableOntologyQuery, SugiliteOperation.CLICK, featurePack);
                showConfirmationDialog(block, featurePack, queryScoreList, clickRunnable);
                dialog.dismiss();
            } else {
                //TODO: will get into further disambiguate mode
                Toast.makeText(context, "Matched " + matchingQueriesMatchedNodesList.get(0).getValue().size() + " Nodes, Need further disambiguation", Toast.LENGTH_SHORT).show();
            }

        } else {
            //empty result, show the dialog and switch to empty result state
            dialog.show();
            setCurrentState(emptyResultState);
            initPrompt();
        }
    }

    private void showConfirmationDialog(SugiliteOperationBlock block, SugiliteAvailableFeaturePack featurePack, List<Map.Entry<SerializableOntologyQuery, Double>> queryScoreList, Runnable clickRunnable) {
        SugiliteRecordingConfirmationDialog sugiliteRecordingConfirmationDialog = new SugiliteRecordingConfirmationDialog(context, block, featurePack, queryScoreList, clickRunnable, blockBuildingHelper, layoutInflater, uiSnapshot, actualClickedNode, sugiliteData, sharedPreferences, tts);
        sugiliteRecordingConfirmationDialog.show();
    }

    /**
     * initiate the dialog manager
     */
    @Override
    public void initDialogManager() {

        //set the prompt
        emptyResultState.setPrompt(context.getString(R.string.disambiguation_error));
        askingForVerbalInstructionState.setPrompt(context.getString(R.string.disambiguation_prompt));

        //set on switched away runnable - the verbal instruction state should set the value for the text box
        askingForVerbalInstructionState.setOnSwitchedAwayRunnable(new Runnable() {
            @Override
            public void run() {
                if (askingForVerbalInstructionState.getASRResult() != null && (!askingForVerbalInstructionState.getASRResult().isEmpty())) {
                    verbalInstructionEditText.setText(askingForVerbalInstructionState.getASRResult().get(0));
                }
            }
        });
        emptyResultState.setOnSwitchedAwayRunnable(new Runnable() {
            @Override
            public void run() {
                if (emptyResultState.getASRResult() != null && (!emptyResultState.getASRResult().isEmpty())) {
                    verbalInstructionEditText.setText(emptyResultState.getASRResult().get(0));
                }
            }
        });

        //set on initiate runnable - the instruction confirmation state should use the content in the text box as the prompt
        askingForInstructionConfirmationState.setOnInitiatedRunnable(new Runnable() {
            @Override
            public void run() {
                askingForInstructionConfirmationState.setPrompt(context.getString(R.string.disambiguation_confirm, verbalInstructionEditText.getText()));
            }
        });

        //link the states
        askingForVerbalInstructionState.setNoASRResultState(askingForVerbalInstructionState);
        askingForVerbalInstructionState.setUnmatchedState(askingForVerbalInstructionState);
        askingForVerbalInstructionState.addNextStateUtteranceFilter(askingForInstructionConfirmationState, SugiliteDialogUtteranceFilter.getConstantFilter(true));

        emptyResultState.setNoASRResultState(askingForVerbalInstructionState);
        emptyResultState.setUnmatchedState(askingForVerbalInstructionState);
        emptyResultState.addNextStateUtteranceFilter(askingForInstructionConfirmationState, SugiliteDialogUtteranceFilter.getConstantFilter(true));

        askingForInstructionConfirmationState.setNoASRResultState(askingForInstructionConfirmationState);
        askingForInstructionConfirmationState.setUnmatchedState(askingForInstructionConfirmationState);
        askingForInstructionConfirmationState.addNextStateUtteranceFilter(askingForVerbalInstructionState, SugiliteDialogUtteranceFilter.getSimpleContainingFilter("no", "nah"));

        //set exit runnables
        askingForVerbalInstructionState.addExitRunnableUtteranceFilter(SugiliteDialogUtteranceFilter.getSimpleContainingFilter("skip"), new Runnable() {
            @Override
            public void run() {
                skipButtonOnClick();
            }
        });
        askingForVerbalInstructionState.addExitRunnableUtteranceFilter(SugiliteDialogUtteranceFilter.getSimpleContainingFilter("cancel"), new Runnable() {
            @Override
            public void run() {
                dialog.cancel();
            }
        });
        askingForInstructionConfirmationState.addExitRunnableUtteranceFilter(SugiliteDialogUtteranceFilter.getSimpleContainingFilter("yes", "yeah"), new Runnable() {
            @Override
            public void run() {
                sendInstructionButtonOnClick();
            }
        });


        //set current sate
        setCurrentState(askingForVerbalInstructionState);
        initPrompt();
    }

    @Override
    public void runOnMainThread(Runnable r) {
        try {
            if (context instanceof SugiliteAccessibilityService) {
                ((SugiliteAccessibilityService) context).runOnUiThread(r);
            } else {
                throw new Exception("no access to ui thread");
            }
        } catch (Exception e) {
            //do nothing
            e.printStackTrace();
        }
    }
}
