package edu.cmu.hcii.sugilite.recording.newrecording.fullscreen_overlay;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.text.Html;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import edu.cmu.hcii.sugilite.model.Node;
import edu.cmu.hcii.sugilite.R;
import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.model.block.SugiliteOperationBlock;
import edu.cmu.hcii.sugilite.model.block.util.SugiliteAvailableFeaturePack;
import edu.cmu.hcii.sugilite.model.operation.SugiliteOperation;
import edu.cmu.hcii.sugilite.ontology.OntologyQuery;
import edu.cmu.hcii.sugilite.ontology.SugiliteEntity;
import edu.cmu.hcii.sugilite.ontology.UISnapshot;
import edu.cmu.hcii.sugilite.ontology.description.OntologyDescriptionGenerator;
import edu.cmu.hcii.sugilite.recording.newrecording.SugiliteBlockBuildingHelper;
import edu.cmu.hcii.sugilite.recording.newrecording.dialog_management.SugiliteDialogManager;
import edu.cmu.hcii.sugilite.recording.newrecording.dialog_management.SugiliteDialogSimpleState;

import static edu.cmu.hcii.sugilite.Const.OVERLAY_TYPE;

/**
 * @author toby
 * @date 4/4/18
 * @time 1:00 PM
 */
public class ChooseParsingDialog extends SugiliteDialogManager {
    //skip the dialog and choose the top item by default if TO_SKIP is true
    private static final boolean TO_SKIP = true;

    private Context context;
    private SugiliteBlockBuildingHelper blockBuildingHelper;
    private LayoutInflater layoutInflater;
    private Runnable clickableRunnable;
    private UISnapshot uiSnapshot;
    private SugiliteEntity<Node> actualClickedNode;
    private SugiliteData sugiliteData;
    private SharedPreferences sharedPreferences;
    private OntologyDescriptionGenerator ontologyDescriptionGenerator;

    private View dialogView;
    private ListView mainListView;
    private List<Pair<OntologyQuery, List<Node>>> matchingQueriesMatchedNodesList;
    private List<OntologyQuery> resultQueries;
    private Dialog dialog;
    private SugiliteDialogSimpleState askingForChoosingParsingState = new SugiliteDialogSimpleState("ASKING_FOR_CHOOSING_PARSING", this, true);

    private List<Pair<OntologyQuery, Double>> queryScoreList;
    private SugiliteAvailableFeaturePack featurePack;

    public ChooseParsingDialog(Context context, List<Pair<OntologyQuery, List<Node>>> matchingQueriesMatchedNodesList, SugiliteBlockBuildingHelper blockBuildingHelper, Runnable clickRunnable, UISnapshot uiSnapshot, SugiliteEntity<Node> actualClickedNode, SugiliteData sugiliteData, SharedPreferences sharedPreferences, TextToSpeech tts, SugiliteAvailableFeaturePack featurePack, List<Pair<OntologyQuery, Double>> queryScoreList){
        super(context, tts);
        this.context = context;
        this.blockBuildingHelper = blockBuildingHelper;
        this.layoutInflater = LayoutInflater.from(context);
        this.clickableRunnable = clickRunnable;
        this.uiSnapshot = uiSnapshot;
        this.actualClickedNode = actualClickedNode;
        this.sugiliteData = sugiliteData;
        this.sharedPreferences = sharedPreferences;
        this.matchingQueriesMatchedNodesList = matchingQueriesMatchedNodesList;
        this.queryScoreList = queryScoreList;
        this.featurePack = featurePack;
        this.resultQueries = new ArrayList<>();
        for(Pair<OntologyQuery, List<Node>> entry : matchingQueriesMatchedNodesList){
            resultQueries.add(entry.first);
        }
        ontologyDescriptionGenerator = new OntologyDescriptionGenerator();
        //build the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        //builder.setTitle("Select from disambiguation results");
        dialogView = layoutInflater.inflate(R.layout.dialog_choosing_parsing, null);

        //set the list view for query parse candidates
        mainListView = (ListView) dialogView.findViewById(R.id.listview_query_candidates);
        //Map<TextView, OntologyQuery> textViews = new HashMap<>();
        String[] stringArray = new String[resultQueries.size()];
        OntologyQuery[] ontologyQueryArray = new OntologyQuery[resultQueries.size()];

        int i = 0;
        for (OntologyQuery query : resultQueries) {
            ontologyQueryArray[i++] = query;
        }

        i = 0;
        for (OntologyQuery query : resultQueries) {
            stringArray[i++] = ontologyDescriptionGenerator.getSpannedDescriptionForOntologyQuery(query, true).toString();
        }


        ArrayAdapter<String> adapter = new ArrayAdapter<String>(context, android.R.layout.simple_list_item_2, stringArray) {
            //override the arrayadapter to show HTML-styled textviews in the listview
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                View row;
                if (null == convertView) {
                    row = layoutInflater.inflate(android.R.layout.simple_list_item_2, null);
                } else {
                    row = convertView;
                }
                TextView tv1 = (TextView) row.findViewById(android.R.id.text1);
                tv1.setText(Html.fromHtml(getItem(position)));
                TextView tv2 = (TextView) row.findViewById(android.R.id.text2);
                tv2.setText(ontologyQueryArray[position].toString() + "\n" + "Matched " + String.valueOf(matchingQueriesMatchedNodesList.get(position).second.size()) + " nodes");
                //textViews.put(tv1, ontologyQueryArray[position]);
                return row;
            }

        };
        mainListView.setAdapter(adapter);
        //finished setting up the parse result candidate list
        builder.setView(dialogView);
        //on item click for query candidates
        mainListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //showConfirmation(sugiliteOperationBlockArray[position], featurePack, queryScoreList);

                if (dialog != null) {
                    stopASRandTTS();
                    dialog.dismiss();
                }

                try {
                    //TODO: selected
                    //check if this has filteredNodes.size() = 1 -- whether need to show the followup question dialog
                    OntologyQuery query = matchingQueriesMatchedNodesList.get(position).first;
                    if(matchingQueriesMatchedNodesList.get(position).second.size() > 1) {
                        //prompt for further generalization
                        FollowUpQuestionDialog followUpQuestionDialog = new FollowUpQuestionDialog(context, tts, query, uiSnapshot, actualClickedNode, matchingQueriesMatchedNodesList.get(0).second, featurePack, queryScoreList, blockBuildingHelper, clickRunnable, sugiliteData, sharedPreferences, 0);
                        followUpQuestionDialog.setNumberOfMatchedNodes(matchingQueriesMatchedNodesList.get(0).second.size());
                        followUpQuestionDialog.show();
                    } else {
                        //save the block and show a confirmation dialog for the block
                        System.out.println("Result Query: " + query.toString());

                        //construct a block from the query formula
                        SugiliteOperationBlock block = blockBuildingHelper.getUnaryOperationBlockWithOntologyQueryFromQuery(query.clone(), SugiliteOperation.CLICK, featurePack, null);

                        //construct a confirmation dialog from the block
                        SugiliteRecordingConfirmationDialog sugiliteRecordingConfirmationDialog = new SugiliteRecordingConfirmationDialog(context, block, featurePack, queryScoreList, clickRunnable, blockBuildingHelper, uiSnapshot, actualClickedNode, sugiliteData, sharedPreferences, tts);
                        sugiliteRecordingConfirmationDialog.show();
                    }


                } catch (Exception e) {
                    e.printStackTrace();
                }


            }
        });

        dialog = builder.create();
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                stopASRandTTS();
                onDestroy();
            }
        });
    }

    public void show() {
        if(dialog.getWindow() != null) {
            dialog.getWindow().setType(OVERLAY_TYPE);
        }
        dialog.show();
        if(TO_SKIP){
            mainListView.performItemClick(mainListView.getAdapter().getView(0, null, null), 0, mainListView.getItemIdAtPosition(0));
        }
        else{
            //initiate the dialog manager when the dialog is shown
            initDialogManager();
            //refreshSpeakButtonStyle(speakButton);
        }
    }

    /**
     * initiate the dialog manager
     */
    @Override
    public void initDialogManager() {
        //set the prompt
        askingForChoosingParsingState.setPrompt(context.getString(R.string.disambiguation_choosing_parsing_prompt));
        //set current sate
        setCurrentState(askingForChoosingParsingState);
        initPrompt();
    }

}
