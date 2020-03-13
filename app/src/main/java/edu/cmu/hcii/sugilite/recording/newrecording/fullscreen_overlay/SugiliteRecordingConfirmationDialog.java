package edu.cmu.hcii.sugilite.recording.newrecording.fullscreen_overlay;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import edu.cmu.hcii.sugilite.Const;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptDao;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptFileDao;
import edu.cmu.hcii.sugilite.dao.SugiliteScriptSQLDao;
import edu.cmu.hcii.sugilite.model.Node;
import edu.cmu.hcii.sugilite.R;
import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.model.block.util.SugiliteAvailableFeaturePack;
import edu.cmu.hcii.sugilite.model.block.SugiliteOperationBlock;
import edu.cmu.hcii.sugilite.model.operation.trinary.SugiliteLoadVariableOperation;
import edu.cmu.hcii.sugilite.ontology.OntologyQuery;
import edu.cmu.hcii.sugilite.ontology.SugiliteEntity;
import edu.cmu.hcii.sugilite.ontology.UISnapshot;
import edu.cmu.hcii.sugilite.ontology.description.OntologyDescriptionGenerator;
import edu.cmu.hcii.sugilite.pumice.PumiceDemonstrationUtil;
import edu.cmu.hcii.sugilite.recording.newrecording.SugiliteBlockBuildingHelper;
import edu.cmu.hcii.sugilite.recording.newrecording.dialog_management.SugiliteDialogManager;
import edu.cmu.hcii.sugilite.recording.newrecording.dialog_management.SugiliteDialogSimpleState;
import edu.cmu.hcii.sugilite.recording.newrecording.dialog_management.SugiliteDialogUtteranceFilter;

import static edu.cmu.hcii.sugilite.Const.OVERLAY_TYPE;
import static edu.cmu.hcii.sugilite.Const.SQL_SCRIPT_DAO;

/**
 * @author toby
 * @date 2/16/18
 * @time 4:37 PM
 */
public class SugiliteRecordingConfirmationDialog extends SugiliteDialogManager {
    private SugiliteOperationBlock block;
    private SugiliteAvailableFeaturePack featurePack;
    private List<Pair<OntologyQuery, Double>> queryScoreList;
    private Runnable clickUnderlyingButtonRunnable;
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
    private SugiliteScriptDao sugiliteScriptDao;


    //construct the 2 states
    private SugiliteDialogSimpleState askingForConfirmationState = new SugiliteDialogSimpleState("ASKING_FOR_CONFIRMATION", this, true);
    private SugiliteDialogSimpleState detailPromptState = new SugiliteDialogSimpleState("DETAIL_PROMPT", this, true);


    public SugiliteRecordingConfirmationDialog(Context context, SugiliteOperationBlock block, SugiliteAvailableFeaturePack featurePack, List<Pair<OntologyQuery, Double>> queryScoreList, Runnable clickUnderlyingButtonRunnable, SugiliteBlockBuildingHelper blockBuildingHelper, UISnapshot uiSnapshot, SugiliteEntity<Node> actualClickedNode, SugiliteData sugiliteData, SharedPreferences sharedPreferences, TextToSpeech tts) {
        super(context, tts);
        this.context = context;
        this.block = block;
        this.featurePack = featurePack;
        this.queryScoreList = queryScoreList;
        this.clickUnderlyingButtonRunnable = clickUnderlyingButtonRunnable;
        this.blockBuildingHelper = blockBuildingHelper;
        this.layoutInflater = LayoutInflater.from(context);
        this.uiSnapshot = uiSnapshot;
        this.actualClickedNode = actualClickedNode;
        this.sugiliteData = sugiliteData;
        this.sharedPreferences = sharedPreferences;
        this.ontologyDescriptionGenerator = new OntologyDescriptionGenerator();

        if(Const.DAO_TO_USE == SQL_SCRIPT_DAO) {
            sugiliteScriptDao = new SugiliteScriptSQLDao(context);
        }
        else {
            sugiliteScriptDao = new SugiliteScriptFileDao(context, sugiliteData);
        }


        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        Spanned newDescription = ontologyDescriptionGenerator.getSpannedDescriptionForOperation(block.getOperation(), block.getOperation().getDataDescriptionQueryIfAvailable());
        builder.setTitle("Save Operation Confirmation");

        dialogView = layoutInflater.inflate(R.layout.dialog_confirmation_popup_spoken, null);
        confirmationPromptTextView = (TextView) dialogView.findViewById(R.id.text_confirmation_prompt);
        if(confirmationPromptTextView != null){
            //TODO: show the source code temporarily
            SpannableStringBuilder text = new SpannableStringBuilder();
            text.append("Are you sure you want to record the operation: ");
            text.append(newDescription);
            confirmationPromptTextView.setText(text);
            // PumiceDemonstrationUtil.showSugiliteToast(block.toString(), Toast.LENGTH_SHORT);

            //confirmationPromptTextView.setText(Html.fromHtml("Are you sure you want to record the operation: " + block.toString()));

        }
        speakButton = (ImageButton) dialogView.findViewById(R.id.button_verbal_instruction_talk);


        if(speakButton != null){
            speakButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // speak button
                    if (isListening() || (tts != null && tts.isSpeaking())) {
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
                onDestroy();
            }
        });

    }

    public void show() {
        dialog.getWindow().setType(OVERLAY_TYPE);
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
        clickUnderlyingButtonRunnable.run();

        //
        if (block != null && block.getOperation() instanceof SugiliteLoadVariableOperation) {
            if (sugiliteData.currentPumiceValueDemonstrationType != null && sugiliteData.valueDemonstrationVariableName != null) {
                //value demonstration
                SharedPreferences.Editor prefEditor = sharedPreferences.edit();
                prefEditor.putBoolean("recording_in_process", false);
                prefEditor.apply();
                new Thread(new Runnable() {
                    @Override
                    public void run()
                    {
                        //commit the script
                        try {
                            sugiliteScriptDao.commitSave(new Runnable() {
                                @Override
                                public void run() {
                                    //call the after recording callback
                                    if (sugiliteData.getScriptHead() != null && sugiliteData.afterRecordingCallback != null){
                                        //call the endRecordingCallback
                                        Runnable r = sugiliteData.afterRecordingCallback;
                                        sugiliteData.afterRecordingCallback = null;
                                        r.run();
                                    }
                                }
                            });
                        }
                        catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                }).start();




                //turn off the recording overlay if any
                if(sugiliteData.verbalInstructionIconManager != null){
                    sugiliteData.verbalInstructionIconManager.turnOffCatOverlay();
                }

                //reset the valueDemonstrationVariableName
                sugiliteData.valueDemonstrationVariableName = "";


                sugiliteData.setCurrentSystemState(SugiliteData.DEFAULT_STATE);
                PumiceDemonstrationUtil.showSugiliteToast("end recording", Toast.LENGTH_SHORT);

            }
        }
    }

    private void skipButtonOnClick() {
        dialog.cancel();
        clickUnderlyingButtonRunnable.run();
    }

    private void editButtonOnClick() {
        dialog.dismiss();
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                RecordingAmbiguousPopupDialog recordingAmbiguousPopupDialog = new RecordingAmbiguousPopupDialog(context, queryScoreList, featurePack, blockBuildingHelper, clickUnderlyingButtonRunnable, uiSnapshot, actualClickedNode, sugiliteData, sharedPreferences, getTTS(), 0);
                recordingAmbiguousPopupDialog.show();
            }
        }, 500);

    }

    @Override
    public void speakingStartedCallback() {
        super.speakingStartedCallback();
        refreshSpeakButtonStyle(speakButton);
    }

    @Override
    public void speakingEndedCallback() {
        super.speakingEndedCallback();
        refreshSpeakButtonStyle(speakButton);
    }

    @Override
    public void listeningStartedCallback() {
        super.listeningStartedCallback();
        refreshSpeakButtonStyle(speakButton);
    }

    @Override
    public void listeningEndedCallback() {
        super.listeningEndedCallback();
        refreshSpeakButtonStyle(speakButton);
    }

    /**
     * initiate the dialog manager
     */
    @Override
    public void initDialogManager() {
        //set the prompt
        Spanned newDescription = ontologyDescriptionGenerator.getSpannedDescriptionForOperation(block.getOperation(), block.getOperation().getDataDescriptionQueryIfAvailable());
        askingForConfirmationState.setPrompt(context.getString(R.string.ask_if_record) + newDescription.toString());
       // askingForConfirmationState.setPrompt(R.string.ask_if_record + newDescription.toString());

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
