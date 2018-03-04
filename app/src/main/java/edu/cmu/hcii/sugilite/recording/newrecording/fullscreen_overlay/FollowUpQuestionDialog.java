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
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
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
import edu.cmu.hcii.sugilite.ontology.UISnapshot;
import edu.cmu.hcii.sugilite.ontology.description.OntologyDescriptionGenerator;
import edu.cmu.hcii.sugilite.recording.ReadableDescriptionGenerator;
import edu.cmu.hcii.sugilite.recording.newrecording.SugiliteBlockBuildingHelper;
import edu.cmu.hcii.sugilite.recording.newrecording.dialog_management.SugiliteDialogManager;
import edu.cmu.hcii.sugilite.recording.newrecording.dialog_management.SugiliteDialogSimpleState;
import edu.cmu.hcii.sugilite.recording.newrecording.dialog_management.SugiliteDialogUtteranceFilter;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.server_comm.SugiliteVerbalInstructionHTTPQueryInterface;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.server_comm.SugiliteVerbalInstructionHTTPQueryManager;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.server_comm.VerbalInstructionServerQuery;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.server_comm.VerbalInstructionServerResults;

/**
 * @author toby
 * @date 2/23/18
 * @time 2:55 PM
 */

/**
 * dialog class used for maintaining follow-up quersions with users in case of ambiguities after the user has given a verbal instruction
 */
public class FollowUpQuestionDialog extends SugiliteDialogManager implements SugiliteVerbalInstructionHTTPQueryInterface {
    private OntologyQuery previousQuery;
    private OntologyQuery currentQuery;

    private ImageButton speakButton;
    private View dialogView;
    private TextView currentQueryTextView;
    private Runnable clickRunnable;
    private LayoutInflater layoutInflater;
    private EditText verbalInstructionEditText;
    private UISnapshot uiSnapshot;
    private SerializableUISnapshot serializableUISnapshot;
    private SugiliteVerbalInstructionHTTPQueryManager sugiliteVerbalInstructionHTTPQueryManager;
    private SharedPreferences sharedPreferences;
    private Gson gson;
    private Node actualClickedNode;
    private SugiliteAvailableFeaturePack featurePack;
    private List<Map.Entry<SerializableOntologyQuery, Double>> queryScoreList;
    private SugiliteBlockBuildingHelper blockBuildingHelper;
    private SugiliteData sugiliteData;
    private OntologyDescriptionGenerator ontologyDescriptionGenerator;

    private SugiliteDialogSimpleState askingForVerbalInstructionFollowUpState = new SugiliteDialogSimpleState("ASKING_FOR_VERBAL_INSTRUCTION", this);
    private SugiliteDialogSimpleState askingForInstructionConfirmationState = new SugiliteDialogSimpleState("ASKING_FOR_INSTRUCTION_CONFIRMATION", this);
    private SugiliteDialogSimpleState emptyResultState = new SugiliteDialogSimpleState("EMPTY_RESULT_STATE", this);

    private int numberOfMatchedNodes = -1;


    private Dialog dialog;
    private Dialog progressDialog;

    public FollowUpQuestionDialog(Context context, TextToSpeech tts, OntologyQuery initialQuery, UISnapshot uiSnapshot, Node actualClickedNode, SugiliteAvailableFeaturePack featurePack, List<Map.Entry<SerializableOntologyQuery, Double>> queryScoreList, SugiliteBlockBuildingHelper blockBuildingHelper, LayoutInflater layoutInflater, Runnable clickRunnable, SugiliteData sugiliteData, SharedPreferences sharedPreferences){
        super(context, tts);
        this.previousQuery = null;
        this.currentQuery = initialQuery;
        this.clickRunnable = clickRunnable;
        this.layoutInflater = layoutInflater;
        this.uiSnapshot = uiSnapshot;
        this.serializableUISnapshot = new SerializableUISnapshot(uiSnapshot);
        this.sugiliteVerbalInstructionHTTPQueryManager = new SugiliteVerbalInstructionHTTPQueryManager(this, sharedPreferences);
        this.sharedPreferences = sharedPreferences;
        this.actualClickedNode = actualClickedNode;
        this.featurePack = featurePack;
        this.queryScoreList = queryScoreList;
        this.blockBuildingHelper = blockBuildingHelper;
        this.sugiliteData = sugiliteData;
        this.ontologyDescriptionGenerator = new OntologyDescriptionGenerator(context);
        this.gson = new Gson();

        //build the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        dialogView = layoutInflater.inflate(R.layout.dialog_followup_popup_spoken, null);
        currentQueryTextView = (TextView) dialogView.findViewById(R.id.text_current_query_content);
        verbalInstructionEditText = (EditText) dialogView.findViewById(R.id.edittext_instruction_content);

        refreshPreviewTextView();

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
        }).setNeutralButton("Back", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                backButtonOnClick();
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

    public void show(){
        dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        dialog.show();

        //initiate the dialog manager when the dialog is shown
        initDialogManager();
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

    private void skipButtonOnClick(){
        clickRunnable.run();
        dialog.cancel();
    }

    private void backButtonOnClick(){
        if(previousQuery != null) {
            //go back to the followup question dialog with the previous query
            FollowUpQuestionDialog followUpQuestionDialog = new FollowUpQuestionDialog(context, tts, previousQuery, uiSnapshot, actualClickedNode, featurePack, queryScoreList, blockBuildingHelper, layoutInflater, clickRunnable, sugiliteData, sharedPreferences);
            dialog.dismiss();
            followUpQuestionDialog.setNumberOfMatchedNodes(-1);
            followUpQuestionDialog.show();
        }
        else{
            //go back to the original ambiguous popup dialog
            RecordingAmbiguousPopupDialog ambiguousPopupDialog = new RecordingAmbiguousPopupDialog(context, queryScoreList, featurePack, blockBuildingHelper, layoutInflater, clickRunnable, uiSnapshot, actualClickedNode, sugiliteData, sharedPreferences, tts);
            dialog.dismiss();
            ambiguousPopupDialog.show();
        }
    }

    private void showProgressDialog() {
        progressDialog = new AlertDialog.Builder(context).setMessage("Processing the query ...").create();
        progressDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.show();
    }

    private void refreshPreviewTextView(){
        String html = ontologyDescriptionGenerator.getDescriptionForOntologyQuery(SugiliteBlockBuildingHelper.stripSerializableOntologyQuery(new SerializableOntologyQuery(currentQuery)));
        currentQueryTextView.setText(Html.fromHtml(html));
    }

    public void setNumberOfMatchedNodes(int numberOfMatchedNodes) {
        this.numberOfMatchedNodes = numberOfMatchedNodes;
        if(numberOfMatchedNodes > 0) {
            askingForVerbalInstructionFollowUpState.setPrompt(context.getString(R.string.disambiguation_followup_prompt, numberOfMatchedNodes));
        }
        else{
            //when the numberOfMatchedNodes < 0 (i.e. the dialog was generated through the back button)
            askingForVerbalInstructionFollowUpState.setPrompt(context.getString(R.string.disambiguation_followup_no_count_prompt));
        }
    }

    @Override
    public void initDialogManager() {
        //initiate the dialog states
        //set the prompt
        emptyResultState.setPrompt(context.getString(R.string.disambiguation_error));

        if(numberOfMatchedNodes > 0) {
            askingForVerbalInstructionFollowUpState.setPrompt(context.getString(R.string.disambiguation_followup_prompt, numberOfMatchedNodes));
        }
        else{
            //when the numberOfMatchedNodes < 0 (i.e. the dialog was generated through the back button)
            askingForVerbalInstructionFollowUpState.setPrompt(context.getString(R.string.disambiguation_followup_no_count_prompt));
        }

        //set on switched away runnable - the verbal instruction state should set the value for the text box
        askingForVerbalInstructionFollowUpState.setOnSwitchedAwayRunnable(new Runnable() {
            @Override
            public void run() {
                if (askingForVerbalInstructionFollowUpState.getASRResult() != null && (!askingForVerbalInstructionFollowUpState.getASRResult().isEmpty())) {
                    verbalInstructionEditText.setText(askingForVerbalInstructionFollowUpState.getASRResult().get(0));
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
        askingForVerbalInstructionFollowUpState.setNoASRResultState(askingForVerbalInstructionFollowUpState);
        askingForVerbalInstructionFollowUpState.setUnmatchedState(askingForVerbalInstructionFollowUpState);
        askingForVerbalInstructionFollowUpState.addNextStateUtteranceFilter(askingForInstructionConfirmationState, SugiliteDialogUtteranceFilter.getConstantFilter(true));

        emptyResultState.setNoASRResultState(askingForVerbalInstructionFollowUpState);
        emptyResultState.setUnmatchedState(askingForVerbalInstructionFollowUpState);
        emptyResultState.addNextStateUtteranceFilter(askingForInstructionConfirmationState, SugiliteDialogUtteranceFilter.getConstantFilter(true));

        askingForInstructionConfirmationState.setNoASRResultState(askingForInstructionConfirmationState);
        askingForInstructionConfirmationState.setUnmatchedState(askingForInstructionConfirmationState);
        askingForInstructionConfirmationState.addNextStateUtteranceFilter(askingForVerbalInstructionFollowUpState, SugiliteDialogUtteranceFilter.getSimpleContainingFilter("no", "nah"));

        //set exit runnables
        askingForVerbalInstructionFollowUpState.addExitRunnableUtteranceFilter(SugiliteDialogUtteranceFilter.getSimpleContainingFilter("skip"), new Runnable() {
            @Override
            public void run() {
                skipButtonOnClick();
            }
        });
        askingForVerbalInstructionFollowUpState.addExitRunnableUtteranceFilter(SugiliteDialogUtteranceFilter.getSimpleContainingFilter("cancel"), new Runnable() {
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
        setCurrentState(askingForVerbalInstructionFollowUpState);
        initPrompt();
    }

    @Override
    public void resultReceived(int responseCode, String result) {
        //dismiss the progress dialog
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        //update currentQuery based on the result received
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
            OntologyQuery resolvedQuery = OntologyQueryUtils.getQueryWithClassAndPackageConstraints(OntologyQuery.deserialize(queryFormula), actualClickedNode);
            OntologyQuery combinedQuery = OntologyQueryUtils.combineTwoQueries(currentQuery, resolvedQuery);

            OntologyQuery queryClone = OntologyQuery.deserialize(combinedQuery.toString());

            //TODO: fix the bug in query.executeOn -- it should not change the query
            Set<SugiliteEntity> queryResults =  queryClone.executeOn(uiSnapshot);

            for(SugiliteEntity entity : queryResults){
                if(entity.getType().equals(Node.class)){
                    Node node = (Node) entity.getEntityValue();
                    if (node.getClickable()) {
                        filteredNodes.add(node);
                        filteredNodeNodeIdMap.put(node, entity.getEntityId());
                    }
                    if (OntologyQueryUtils.isSameNode(actualClickedNode, node)) {
                        matched = true;
                    }
                }
            }

            if (filteredNodes.size() > 0 && matched) {
                //matched, add the result to the list
                matchingQueriesMatchedNodesList.add(new AbstractMap.SimpleEntry<>(combinedQuery, filteredNodes));
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

        //sort the list by the size of matched node and length, and see if the top result has filteredNodes.size() = 1
        if (matchingQueriesMatchedNodesList != null && (!matchingQueriesMatchedNodesList.isEmpty())) {
            OntologyQuery query = matchingQueriesMatchedNodesList.get(0).getKey();

            if(matchingQueriesMatchedNodesList.get(0).getValue().size() > 1) {
                //need to prompt for further generalization
                Toast.makeText(context, "Matched " + matchingQueriesMatchedNodesList.get(0).getValue().size() + " Nodes, Need further disambiguation", Toast.LENGTH_SHORT).show();

                //update the query
                previousQuery = currentQuery;
                currentQuery = query;

                dialog.show();
                refreshPreviewTextView();
                setNumberOfMatchedNodes(matchingQueriesMatchedNodesList.get(0).getValue().size());
                setCurrentState(askingForVerbalInstructionFollowUpState);
                initPrompt();
            }
            else {
                //save the block and show a confirmation dialog for the block
                Toast.makeText(context, query.toString(), Toast.LENGTH_SHORT).show();
                System.out.println("Result Query: " + query.toString());
                //construct the block from the query formula

                SerializableOntologyQuery serializableOntologyQuery = new SerializableOntologyQuery(query);

                SugiliteOperationBlock block = blockBuildingHelper.getOperationBlockFromQuery(serializableOntologyQuery, SugiliteOperation.CLICK, featurePack);
                showConfirmationDialog(block, featurePack, queryScoreList, clickRunnable);
                dialog.dismiss();
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
