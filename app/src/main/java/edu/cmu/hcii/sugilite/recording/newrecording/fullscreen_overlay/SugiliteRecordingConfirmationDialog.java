package edu.cmu.hcii.sugilite.recording.newrecording.fullscreen_overlay;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.text.Html;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import edu.cmu.hcii.sugilite.model.Node;
import edu.cmu.hcii.sugilite.R;
import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.model.block.util.SugiliteAvailableFeaturePack;
import edu.cmu.hcii.sugilite.model.block.SugiliteOperationBlock;
import edu.cmu.hcii.sugilite.ontology.SerializableOntologyQuery;
import edu.cmu.hcii.sugilite.ontology.SugiliteEntity;
import edu.cmu.hcii.sugilite.ontology.UISnapshot;
import edu.cmu.hcii.sugilite.ontology.description.OntologyDescriptionGenerator;
import edu.cmu.hcii.sugilite.recording.newrecording.SugiliteBlockBuildingHelper;
import edu.cmu.hcii.sugilite.recording.newrecording.dialog_management.SugiliteDialogManager;
import edu.cmu.hcii.sugilite.recording.newrecording.dialog_management.SugiliteDialogSimpleState;
import edu.cmu.hcii.sugilite.recording.newrecording.dialog_management.SugiliteDialogUtteranceFilter;

/**
 * @author toby
 * @date 2/16/18
 * @time 4:37 PM
 */
public class SugiliteRecordingConfirmationDialog extends SugiliteDialogManager {
    private SugiliteOperationBlock block;
    private SugiliteAvailableFeaturePack featurePack;
    private List<Pair<SerializableOntologyQuery, Double>> queryScoreList;
    private Runnable clickRunnable;
    private SugiliteBlockBuildingHelper blockBuildingHelper;
    private LayoutInflater layoutInflater;
    private UISnapshot uiSnapshot;
    private SugiliteEntity<Node> actualClickedNode;
    private SugiliteData sugiliteData;
    private SharedPreferences sharedPreferences;
    private OntologyDescriptionGenerator ontologyDescriptionGenerator;
    private Dialog dialog;
    private View dialogView;
    private TextView confirmationPromptTextView;
    private ImageButton speakButton;


    //construct the 2 states
    private SugiliteDialogSimpleState askingForConfirmationState = new SugiliteDialogSimpleState("ASKING_FOR_CONFIRMATION", this);
    private SugiliteDialogSimpleState detailPromptState = new SugiliteDialogSimpleState("DETAIL_PROMPT", this);


    public SugiliteRecordingConfirmationDialog(Context context, SugiliteOperationBlock block, SugiliteAvailableFeaturePack featurePack, List<Pair<SerializableOntologyQuery, Double>> queryScoreList, Runnable clickRunnable, SugiliteBlockBuildingHelper blockBuildingHelper, LayoutInflater layoutInflater, UISnapshot uiSnapshot, SugiliteEntity<Node> actualClickedNode, SugiliteData sugiliteData, SharedPreferences sharedPreferences, TextToSpeech tts) {
        super(context, tts);
        this.context = context;
        this.block = block;
        this.featurePack = featurePack;
        this.queryScoreList = queryScoreList;
        this.clickRunnable = clickRunnable;
        this.blockBuildingHelper = blockBuildingHelper;
        this.layoutInflater = layoutInflater;
        this.uiSnapshot = uiSnapshot;
        this.actualClickedNode = actualClickedNode;
        this.sugiliteData = sugiliteData;
        this.sharedPreferences = sharedPreferences;
        this.ontologyDescriptionGenerator = new OntologyDescriptionGenerator(context);


        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        String newDescription = ontologyDescriptionGenerator.getDescriptionForOperation(block.getOperation(), block.getOperation().getDataDescriptionQueryIfAvailable());
        builder.setTitle("Save Operation Confirmation");

        dialogView = layoutInflater.inflate(R.layout.dialog_confirmation_popup_spoken, null);
        confirmationPromptTextView = (TextView) dialogView.findViewById(R.id.text_confirmation_prompt);
        if(confirmationPromptTextView != null){
            //TODO: show the source code temporarily
            confirmationPromptTextView.setText(Html.fromHtml("Are you sure you want to record the operation: " + newDescription));
            Toast.makeText(context, block.toString(), Toast.LENGTH_SHORT).show();
            //confirmationPromptTextView.setText(Html.fromHtml("Are you sure you want to record the operation: " + block.toString()));

        }
        speakButton = (ImageButton) dialogView.findViewById(R.id.button_verbal_instruction_talk);


        if(speakButton != null){
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
            refreshSpeakButtonStyle(speakButton);
        }

        builder.setView(dialogView);

        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //save the block
                positiveButtonOnClick();
            }
        })
                .setNegativeButton("Skip", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        skipButtonOnClick();
                    }
                })
                .setNeutralButton("Modify", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        editButtonOnClick();
                    }
                });

        dialog = builder.create();
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                //stop ASR and TTS when the dialog is dismissed
                stopASRandTTS();
            }
        });

    }

    public void show() {
        dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        dialog.show();
        refreshSpeakButtonStyle(speakButton);

        //initiate the dialog manager when the dialog is shown
        initDialogManager();
    }

    private void positiveButtonOnClick() {
        dialog.dismiss();
        if (sharedPreferences.getBoolean("recording_in_process", false)) {
            try {
                blockBuildingHelper.saveBlock(block, featurePack);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        clickRunnable.run();
    }

    private void skipButtonOnClick() {
        dialog.cancel();
        clickRunnable.run();
    }

    private void editButtonOnClick() {
        dialog.dismiss();
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                RecordingAmbiguousPopupDialog recordingAmbiguousPopupDialog = new RecordingAmbiguousPopupDialog(context, queryScoreList, featurePack, blockBuildingHelper, layoutInflater, clickRunnable, uiSnapshot, actualClickedNode, sugiliteData, sharedPreferences, tts, 0);
                recordingAmbiguousPopupDialog.show();
            }
        }, 500);

    }

    @Override
    public void speakingStarted() {
        super.speakingStarted();
        refreshSpeakButtonStyle(speakButton);
    }

    @Override
    public void speakingEnded() {
        super.speakingEnded();
        refreshSpeakButtonStyle(speakButton);
    }

    @Override
    public void listeningStarted() {
        super.listeningStarted();
        refreshSpeakButtonStyle(speakButton);
    }

    @Override
    public void listeningEnded() {
        super.listeningEnded();
        refreshSpeakButtonStyle(speakButton);
    }

    /**
     * initiate the dialog manager
     */
    @Override
    public void initDialogManager() {
        //set the prompt
        String newDescription = ontologyDescriptionGenerator.getDescriptionForOperation(block.getOperation(), block.getOperation().getDataDescriptionQueryIfAvailable());
        newDescription = Html.fromHtml(context.getString(R.string.ask_if_record, newDescription)).toString();
        askingForConfirmationState.setPrompt(newDescription);
        detailPromptState.setPrompt(context.getString(R.string.expand_ask_if_record));


        //link the states
        askingForConfirmationState.setNoASRResultState(detailPromptState);
        askingForConfirmationState.setUnmatchedState(detailPromptState);
        askingForConfirmationState.addNextStateUtteranceFilter(detailPromptState, SugiliteDialogUtteranceFilter.getConstantFilter(true));

        detailPromptState.setNoASRResultState(detailPromptState);
        detailPromptState.setUnmatchedState(detailPromptState);

        //set exit runnables
        askingForConfirmationState.addExitRunnableUtteranceFilter(SugiliteDialogUtteranceFilter.getSimpleContainingFilter("yes", "yeah"), new Runnable() {
            @Override
            public void run() {
                positiveButtonOnClick();
            }
        });

        detailPromptState.addExitRunnableUtteranceFilter(SugiliteDialogUtteranceFilter.getSimpleContainingFilter("confirm"), new Runnable() {
            @Override
            public void run() {
                positiveButtonOnClick();
            }
        });
        detailPromptState.addExitRunnableUtteranceFilter(SugiliteDialogUtteranceFilter.getSimpleContainingFilter("skip"), new Runnable() {
            @Override
            public void run() {
                skipButtonOnClick();
            }
        });
        detailPromptState.addExitRunnableUtteranceFilter(SugiliteDialogUtteranceFilter.getSimpleContainingFilter("cancel"), new Runnable() {
            @Override
            public void run() {
                dialog.dismiss();
            }
        });
        detailPromptState.addExitRunnableUtteranceFilter(SugiliteDialogUtteranceFilter.getSimpleContainingFilter("modify"), new Runnable() {
            @Override
            public void run() {
                editButtonOnClick();
            }
        });

        //set current sate
        setCurrentState(askingForConfirmationState);
        initPrompt();
    }

}
